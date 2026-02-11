package org.wildfly.bot.util;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHCommitQueryBuilder;
import org.kohsuke.github.GHCommitState;
import org.kohsuke.github.GHIssueComment;
import org.kohsuke.github.GHLabel;
import org.kohsuke.github.GHPerson;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHPullRequestCommitDetail;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.HttpException;
import org.kohsuke.github.PagedIterable;
import org.wildfly.bot.config.WildFlyBotConfig;
import org.wildfly.bot.model.RuntimeConstants;
import org.wildfly.bot.model.WildFlyConfigFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.SequencedMap;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.LinkedHashMap;

@Dependent
public class GithubProcessor {

    public static final String COLLABORATOR_MISSING_SUBJECT = "Missing collaborator in the %s repository";

    public static final String COLLABORATOR_MISSING_BODY = """
            Hello,

            The following people are not collaborators in the repository %s, however they were requested to review Pull Request number #%d

            %s

            ---
            This is generated message, please do not respond.""";

    private static final Logger LOG_DELEGATE = Logger.getLogger(GithubProcessor.class);
    public final PullRequestLogger LOG = new PullRequestLogger(LOG_DELEGATE);
    private Pattern SKIP_FORMAT_COMMAND;

    @Inject
    WildFlyBotConfig wildFlyBotConfig;

    @Inject
    GitHubBotContextProvider botContextProvider;

    @Inject
    Mailer mailer;

    @ConfigProperty(name = "quarkus.mailer.username")
    Optional<String> username;

    @PostConstruct
    void construct() {
        SKIP_FORMAT_COMMAND = Pattern.compile("@%s skip format".formatted(botContextProvider.getBotName()),
                Pattern.DOTALL | Pattern.LITERAL);
    }

    public void commitStatusSuccess(GHPullRequest pullRequest, String checkName, String description) throws IOException {
        String sha = pullRequest.getHead().getSha();

        if (wildFlyBotConfig.isDryRun()) {
            LOG.infof(RuntimeConstants.DRY_RUN_PREPEND.formatted("Commit status success {%s, %s, %s}"), sha, checkName,
                    description);
        } else {
            pullRequest.getRepository().createCommitStatus(sha, GHCommitState.SUCCESS, "", description, checkName);
        }
    }

    public void commitStatusError(GHPullRequest pullRequest, String checkName, String description) throws IOException {
        String sha = pullRequest.getHead().getSha();

        if (wildFlyBotConfig.isDryRun()) {
            LOG.infof(RuntimeConstants.DRY_RUN_PREPEND.formatted("Commit status failure {%s, %s, %s}"), sha, checkName,
                    description);
        } else {
            pullRequest.getRepository().createCommitStatus(sha, GHCommitState.ERROR, "", description, checkName);
        }
    }

    public void processNotifies(GHPullRequest pullRequest, GitHub gitHub,
            SequencedMap<String, List<String>> ccMentionsWithRules, Set<String> reviewers,
            List<String> emails) throws IOException {
        if (ccMentionsWithRules.isEmpty() && reviewers.isEmpty()) {
            updateCCMentions(pullRequest, new LinkedHashMap<>());
            return;
        }

        reviewers.forEach(ccMentionsWithRules::remove);

        List<String> currentReviewers = pullRequest.getRequestedReviewers()
                .stream()
                .map(GHPerson::getLogin)
                .toList();

        LOG.infof("Current reviewers already added to the PR: %s", currentReviewers);

        currentReviewers.forEach(reviewers::remove);

        LOG.infof("Reviewers to be added to the PR: %s", reviewers);

        updateCCMentions(pullRequest, ccMentionsWithRules);

        if (!reviewers.isEmpty()) {
            if (wildFlyBotConfig.isDryRun()) {
                LOG.infof(RuntimeConstants.DRY_RUN_PREPEND.formatted("PR review requested from \"%s\""),
                        String.join(",", reviewers));
            } else {
                List<String> failedReviewers = new ArrayList<>();
                for (String requestedReviewer : reviewers) {
                    try {
                        GHUser ghUser = gitHub.getUser(requestedReviewer);
                        pullRequest.requestReviewers(List.of(ghUser));
                    } catch (HttpException e) {
                        String responseMessage = e.getResponseMessage() != null ? e.getResponseMessage()
                                : "No response message available.";
                        LOG.warnf(
                                "Failed to add reviewer '%s'. Error: %s, Response: %s",
                                requestedReviewer, e.getMessage(), responseMessage);
                        failedReviewers.add(requestedReviewer);
                    } catch (RuntimeException e) {
                        LOG.warnf(
                                "Unexpected error while adding reviewer '%s'. Error: %s",
                                requestedReviewer, e.getMessage());
                        failedReviewers.add(requestedReviewer);
                    }
                }

                // TODO Revert after debugging request reviewers bug
                if (!failedReviewers.isEmpty()) {
                    GHRepository repository = pullRequest.getRepository();
                    // Check if actually failedReviewers are not collaborators
                    for (String failedReviewer : failedReviewers) {
                        if (repository.isCollaborator(gitHub.getUser(failedReviewer))) {
                            LOG.warnf("Reviewer '%s' failed to be added, but they are a collaborator in the '%s' repository.",
                                    failedReviewer, repository.getFullName());
                        }
                    }
                    List<String> finalRequestedReviewers = pullRequest.getRequestedReviewers()
                            .stream()
                            .map(GHPerson::getLogin)
                            .toList();

                    LOG.infof("Final reviewers added to the PR: %s", finalRequestedReviewers);

                    // Remove successfully added reviewers from the failed list
                    failedReviewers.removeAll(finalRequestedReviewers);

                    // Log the actual failed reviewers that were not added
                    if (!failedReviewers.isEmpty()) {
                        LOG.warnf("Bot failed to request PR review from the following people: %s", failedReviewers);

                        sendEmail(
                                COLLABORATOR_MISSING_SUBJECT.formatted(repository.getFullName()),
                                COLLABORATOR_MISSING_BODY.formatted(repository.getFullName(), pullRequest.getNumber(),
                                        failedReviewers),
                                emails);
                    } else {
                        LOG.info("All initially failed reviewers were successfully added to the PR after verification.");
                    }
                }
            }
        }
    }

    private void updateCCMentions(GHPullRequest pullRequest, SequencedMap<String, List<String>> newMentionsWithRules)
            throws IOException {
        for (GHIssueComment comment : pullRequest.listComments()) {
            if (comment.getUser().getLogin().equals(botContextProvider.getBotName())
                    && comment.getBody().startsWith("/cc")) {
                if (newMentionsWithRules.isEmpty()) {
                    if (wildFlyBotConfig.isDryRun()) {
                        LOG.infof(RuntimeConstants.DRY_RUN_PREPEND.formatted("Delete comment %s"), comment);
                    } else {
                        comment.delete();
                    }
                } else {
                    // deserializes "/cc @user [rule1, rule2], @user2 [rule3]"
                    SequencedMap<String, List<String>> commentMentionsAndRules = Arrays.stream(comment.getBody().split(" @"))
                            .skip(1) // skips "/cc"
                            .collect(Collectors.toMap(
                                    s -> s.split(" \\[")[0],
                                    s -> Arrays.stream(s.split(" \\[")[1]
                                            .split(", "))
                                            .map(rule -> rule.endsWith("]")
                                                    ? rule.substring(0, rule.length() - 1)
                                                    : rule)
                                            .toList(),
                                    (oldVal, newVal) -> newVal, // just for the 4th parameter of the toMap method
                                    LinkedHashMap::new));

                    if (!commentMentionsAndRules.keySet().containsAll(newMentionsWithRules.keySet()) ||
                            commentMentionsAndRules.size() != newMentionsWithRules.size()) {

                        // We preserve order of already mentioned people and append new people
                        newMentionsWithRules
                                .sequencedEntrySet()
                                .forEach(entry -> commentMentionsAndRules.merge(entry.getKey(), entry.getValue(),
                                        (oldMention, newMention) -> newMention));

                        String updatedBody = createCCMentionComment(commentMentionsAndRules);
                        if (wildFlyBotConfig.isDryRun()) {
                            LOG.infof(RuntimeConstants.DRY_RUN_PREPEND.formatted("Update comment %s to %s"), comment.getBody(),
                                    updatedBody);
                        } else {
                            comment.update(updatedBody);
                        }
                    } // else nothing to update, as we have all mentions in the comment
                }
                return;
            }
        }

        if (newMentionsWithRules.isEmpty()) {
            return;
        }

        String updatedBody = createCCMentionComment(newMentionsWithRules);
        if (wildFlyBotConfig.isDryRun()) {
            LOG.infof(RuntimeConstants.DRY_RUN_PREPEND.formatted("Add new comment %s"), updatedBody);
        } else {
            pullRequest.comment(updatedBody);
        }
    }

    public void createLabelsIfMissing(GHRepository repository, Collection<String> labels) throws IOException {
        PagedIterable<GHLabel> repoLabels = repository.listLabels();
        List<String> inRepoLabels = repoLabels.toList().stream()
                .map(GHLabel::getName)
                .filter(labels::contains)
                .toList();
        List<String> missingLabels = labels.stream()
                .filter(s -> !inRepoLabels.contains(s))
                .toList();

        if (!missingLabels.isEmpty()) {
            LOG.debugf("The following labels will be created as they are not in the repository [%s] %s.", repository.getName(),
                    missingLabels);
            for (String name : missingLabels) {
                String color = String.format("%06x", new Random().nextInt(0xffffff + 1));
                repository.createLabel(name, color);
            }
        }
    }

    public void sendEmail(String subject, String body, List<String> emails) {
        if (username.isPresent() && emails != null && !emails.isEmpty()) {
            LOG.infof("Sending email to the following emails [%s].", String.join(", ", emails));
            mailer.send(
                    new Mail()
                            .setSubject(subject)
                            .setText(body)
                            .setTo(emails));
        } else {
            LOG.debug("No emails setup to receive warnings or no email address setup to send emails from.");
        }
    }

    public String skipPullRequest(GHPullRequest pullRequest) throws IOException {
        String body = pullRequest.getBody();
        if (body != null && SKIP_FORMAT_COMMAND.matcher(body).find()) {
            return "skip format command found";
        }

        if (pullRequest.isDraft()) {
            return "pull request being a draft";
        }

        return null;
    }

    public String skipPullRequest(GHPullRequest pullRequest, WildFlyConfigFile wildFlyConfigFile) throws IOException {
        if (wildFlyConfigFile == null) {
            return "no configuration file found";
        }

        return skipPullRequest(pullRequest);
    }

    /**
     * This method might get executed in parallel, thus we prepend Pull Request specific info
     * instead of relying on the {@code org.wildfly.bot.util.PullRequestLogger#setPullRequest}
     * being called. The logger class was not designed for parallel execution, so this is somewhat
     * of a workaround compensating this short-coming.
     */
    public void updateLabels(GHPullRequest pullRequest, List<String> labelsToAdd, List<String> labelsToRemove)
            throws IOException {
        List<String> currentLabels = pullRequest.getLabels().stream()
                .map(GHLabel::getName)
                .toList();

        labelsToAdd.removeIf(currentLabels::contains);
        labelsToRemove.removeIf(label -> !currentLabels.contains(label));

        if (!labelsToAdd.isEmpty()) {
            String logMessage = "Adding the following labels: %s".formatted(labelsToAdd);
            if (!LOG.isPullRequestSet()) {
                logMessage = "Pull Request [#%d] - %s".formatted(pullRequest.getNumber(), logMessage);
            }
            LOG.info(logMessage);
            if (wildFlyBotConfig.isDryRun()) {
                LOG.infof(RuntimeConstants.DRY_RUN_PREPEND.formatted("Added the following labels: %s"), labelsToAdd);
            } else {
                pullRequest.addLabels(labelsToAdd.toArray(String[]::new));
            }
        }

        if (!labelsToRemove.isEmpty()) {
            String logMessage = "Removing the following labels: %s".formatted(labelsToRemove);
            if (!LOG.isPullRequestSet()) {
                logMessage = "Pull Request [#%d] - %s".formatted(pullRequest.getNumber(), logMessage);
            }
            LOG.info(logMessage);
            if (wildFlyBotConfig.isDryRun()) {
                LOG.infof(RuntimeConstants.DRY_RUN_PREPEND.formatted("Removed the following labels: %s"), labelsToRemove);
            } else {
                pullRequest.removeLabels(labelsToRemove.toArray(String[]::new));
            }
        }
    }

    public void deleteFormatComment(GHPullRequest pullRequest, String commentBody) throws IOException {
        formatComment(pullRequest, commentBody, null);
    }

    public void formatComment(GHPullRequest pullRequest, String commentBody, Collection<String> errors) throws IOException {
        boolean update = false;
        String firstLine = commentBody.split("\n")[0];
        for (GHIssueComment comment : pullRequest.listComments()) {
            if (comment.getUser().getLogin().equals(botContextProvider.getBotName())
                    && comment.getBody().startsWith(firstLine)) {
                if (errors == null || errors.isEmpty()) {
                    if (wildFlyBotConfig.isDryRun()) {
                        LOG.infof(RuntimeConstants.DRY_RUN_PREPEND.formatted("Delete comment %s"), comment);
                    } else {
                        comment.delete();
                    }
                    update = true;
                    break;
                }

                String updatedBody = commentBody.formatted(errors.stream()
                        .map("- %s"::formatted)
                        .collect(Collectors.joining("\n\n")));

                if (wildFlyBotConfig.isDryRun()) {
                    LOG.infof(RuntimeConstants.DRY_RUN_PREPEND.formatted("Update comment \"%s\" to \"%s\""), comment.getBody(),
                            updatedBody);
                } else {
                    comment.update(updatedBody);
                }
                update = true;
                break;
            }
        }

        if (!update && errors != null && !errors.isEmpty()) {
            String updatedBody = commentBody.formatted(errors.stream()
                    .map("- %s"::formatted)
                    .collect(Collectors.joining("\n\n")));
            if (wildFlyBotConfig.isDryRun()) {
                LOG.infof(RuntimeConstants.DRY_RUN_PREPEND.formatted("Add new comment %s"), updatedBody);
            } else {
                pullRequest.comment(updatedBody);
            }
        }
    }

    private String createCCMentionComment(SequencedMap<String, List<String>> ccMentionsWithRules) {
        return "/cc @" + String.join(", @", ccMentionsWithRules.sequencedEntrySet().stream()
                .map(entry -> entry.getKey()
                        + ((!entry.getValue().stream().allMatch(Objects::isNull))
                                ? entry.getValue().stream().filter(Objects::nonNull)
                                        .collect(Collectors.joining(", ", " [", "]"))
                                : "") // if id (rule) was not provided
                )
                .toList());
    }

    public boolean hasDuplicateCommitInBase(GHPullRequest pullRequest, GHRepository repository)
            throws IOException {
        String baseBranch = pullRequest.getBase().getRef();
        GHCommitQueryBuilder commitQuery = repository.queryCommits();
        if (commitQuery == null) {
            return false;
        }

        Set<String> baseCommitSHAs = commitQuery
                .from(baseBranch)
                .pageSize(100)
                .list()
                .toSet()
                .stream()
                .map(GHCommit::getSHA1)
                .collect(Collectors.toSet());

        for (GHPullRequestCommitDetail prCommit : pullRequest.listCommits()) {
            String prSha = prCommit.getSha();
            if (baseCommitSHAs.contains(prSha)) {
                LOG.infof("Skipping rules due to incorrect rebase detected: commit %s is already in the base branch %s",
                        prSha,
                        baseBranch);
                return true;
            }
        }
        return false;
    }
}
