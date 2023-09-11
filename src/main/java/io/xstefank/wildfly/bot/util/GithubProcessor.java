package io.xstefank.wildfly.bot.util;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import io.xstefank.wildfly.bot.config.WildFlyBotConfig;
import io.xstefank.wildfly.bot.model.WildFlyConfigFile;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.kohsuke.github.GHCommitState;
import org.kohsuke.github.GHIssueComment;
import org.kohsuke.github.GHLabel;
import org.kohsuke.github.GHPerson;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.HttpException;
import org.kohsuke.github.PagedIterable;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
    Mailer mailer;

    @ConfigProperty(name = "quarkus.mailer.username")
    Optional<String> username;

    @PostConstruct
    void construct() {
        SKIP_FORMAT_COMMAND = Pattern.compile("@%s skip format".formatted(wildFlyBotConfig.githubName()),
                Pattern.DOTALL | Pattern.LITERAL);
    }

    public void commitStatusSuccess(GHPullRequest pullRequest, String checkName, String description) throws IOException {
        String sha = pullRequest.getHead().getSha();

        if (wildFlyBotConfig.isDryRun()) {
            LOG.infof("Pull request #%d - Commit status success {%s, %s, %s}", pullRequest.getNumber(), sha, checkName,
                    description);
        } else {
            pullRequest.getRepository().createCommitStatus(sha, GHCommitState.SUCCESS, "", description, checkName);
        }
    }

    public void commitStatusError(GHPullRequest pullRequest, String checkName, String description) throws IOException {
        String sha = pullRequest.getHead().getSha();

        if (wildFlyBotConfig.isDryRun()) {
            LOG.infof("Pull request #%d - Commit status failure {%s, %s, %s}", pullRequest.getNumber(), sha, checkName,
                    description);
        } else {
            pullRequest.getRepository().createCommitStatus(sha, GHCommitState.ERROR, "", description, checkName);
        }
    }

    public void processNotifies(GHPullRequest pullRequest, GitHub gitHub, Set<String> ccMentions, Set<String> reviewers,
            List<String> emails) throws IOException {
        if (ccMentions.isEmpty() && reviewers.isEmpty()) {
            updateCCMentions(pullRequest, Collections.emptySet());
            return;
        }

        ccMentions.removeAll(reviewers);

        Set<String> collaborators = pullRequest.getRepository().getCollaboratorNames();
        List<String> notCollaborators = reviewers.stream()
                .filter(s -> !collaborators.contains(s))
                .toList();

        if (!notCollaborators.isEmpty()) {
            LOG.infof(
                    "Following people are not collaborators in this repository [%s] and can not be requested for PR review: %s",
                    pullRequest.getRepository().getName(), notCollaborators);
            GHRepository repository = pullRequest.getRepository();
            sendEmail(
                    COLLABORATOR_MISSING_SUBJECT.formatted(repository.getFullName()),
                    COLLABORATOR_MISSING_BODY.formatted(repository.getFullName(), pullRequest.getNumber(), notCollaborators),
                    emails);
            reviewers.removeAll(notCollaborators);
        }

        List<String> currentReviewers = pullRequest.getRequestedReviewers()
                .stream()
                .map(GHPerson::getLogin)
                .toList();

        reviewers.removeAll(currentReviewers);

        updateCCMentions(pullRequest, ccMentions);

        if (!reviewers.isEmpty()) {
            if (wildFlyBotConfig.isDryRun()) {
                LOG.infof("Pull request #%d - PR review requested from \"%s\"", pullRequest.getNumber(),
                        String.join(",", reviewers));
            } else {
                try {
                    List<GHUser> ghReviewers = reviewers.stream()
                            .map(user -> {
                                try {
                                    return gitHub.getUser(user);
                                } catch (IOException e) {
                                    // This should not be thrown as these are valid people - Collaborators
                                    LOG.errorf(e, "User %s was not found", user);
                                    throw new RuntimeException("Unable to find user " + user, e);
                                }
                            })
                            .toList();
                    pullRequest.requestReviewers(ghReviewers);
                } catch (HttpException | RuntimeException e) {
                    LOG.errorf("The request has failed due to %s", e.getMessage());
                }
            }
        }
    }

    private void updateCCMentions(GHPullRequest pullRequest, Set<String> newMentions) throws IOException {
        for (GHIssueComment comment : pullRequest.listComments()) {
            if (comment.getUser().getLogin().equals(wildFlyBotConfig.githubName())
                    && comment.getBody().startsWith("/cc")) {
                if (newMentions.isEmpty()) {
                    if (wildFlyBotConfig.isDryRun()) {
                        LOG.infof("Pull request #%d - Delete comment %s", pullRequest.getNumber(), comment);
                    } else {
                        comment.delete();
                    }
                } else {
                    List<String> commentMentions = Arrays.stream(comment.getBody().split(" @"))
                            .skip(1)
                            .map(s -> s.endsWith(",")
                                    ? s.substring(0, s.length() - 1)
                                    : s)
                            .collect(Collectors.toList());

                    if (!new HashSet<>(commentMentions).containsAll(newMentions) ||
                            commentMentions.size() != newMentions.size()) {

                        // We preserve order of already mentioned people and append new people
                        commentMentions.removeIf(s -> !newMentions.contains(s));
                        newMentions.removeAll(commentMentions);
                        commentMentions.addAll(newMentions);

                        String updatedBody = "/cc @" + String.join(", @", commentMentions);
                        if (wildFlyBotConfig.isDryRun()) {
                            LOG.infof("Pull request %d - Update comment %s to %s", pullRequest.getNumber(), comment.getBody(),
                                    updatedBody);
                        } else {
                            comment.update(updatedBody);
                        }
                    } // else nothing to update, as we have all mentions in the comment
                }
                return;
            }
        }

        if (newMentions.isEmpty()) {
            return;
        }

        String updatedBody = "/cc @" + String.join(", @", newMentions);
        if (wildFlyBotConfig.isDryRun()) {
            LOG.infof("Pull request %d - Add new comment %s", pullRequest.getNumber(), updatedBody);
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

    public String skipPullRequest(GHPullRequest pullRequest, WildFlyConfigFile wildFlyConfigFile) throws IOException {
        if (pullRequest.getBody() != null && SKIP_FORMAT_COMMAND.matcher(pullRequest.getBody()).find()) {
            return "skip format command found";
        }

        if (wildFlyConfigFile == null) {
            return "no configuration file found";
        }

        if (pullRequest.isDraft()) {
            return "pull request being a draft";
        }

        return null;
    }
}
