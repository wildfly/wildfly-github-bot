package io.xstefank.wildfly.bot.util;

import io.xstefank.wildfly.bot.config.WildFlyBotConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.kohsuke.github.GHCommitState;
import org.kohsuke.github.GHIssueComment;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.HttpException;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@ApplicationScoped
public class GithubProcessor {

    private static final Logger LOG = Logger.getLogger(GithubProcessor.class);

    @Inject
    WildFlyBotConfig wildFlyBotConfig;

    public void commitStatusSuccess(GHPullRequest pullRequest, String checkName, String description) throws IOException {
        String sha = pullRequest.getHead().getSha();

        if (wildFlyBotConfig.isDryRun()) {
            LOG.infof("Pull request #%d - Commit status success {%s, %s, %s}", pullRequest.getNumber(), sha, checkName, description);
        } else {
            pullRequest.getRepository().createCommitStatus(sha, GHCommitState.SUCCESS, "", description, checkName);
        }
    }

    public void commitStatusError(GHPullRequest pullRequest, String checkName, String description) throws IOException {
        String sha = pullRequest.getHead().getSha();

        if (wildFlyBotConfig.isDryRun()) {
            LOG.infof("Pull request #%d - Commit status failure {%s, %s, %s}", pullRequest.getNumber(), sha, checkName, description);
        } else {
            pullRequest.getRepository().createCommitStatus(sha, GHCommitState.ERROR, "", description, checkName);
        }
    }

    public void processMentions(GHPullRequest pullRequest, GitHub gitHub, List<String> mentions) throws IOException {
        if (mentions.isEmpty()) {
            return;
        }

        Set<String> collaborators = pullRequest.getRepository().getCollaboratorNames();
        List<String> ccMentions = mentions.stream()
                .filter(s -> !collaborators.contains(s))
                .toList();

        if (!ccMentions.isEmpty()) {
            LOG.debugf("Removing following people from pull request review as they are not collaborators in this repository [%s]. %s",
                    pullRequest.getRepository().getName(), ccMentions);
            mentions.removeAll(ccMentions);
        }

        List<String> currentReviewers = pullRequest.getRequestedReviewers()
                .stream()
                .map(ghUser -> ghUser.getLogin())
                .toList();

        mentions.removeAll(currentReviewers);

        updateCCMentions(pullRequest, ccMentions);

        if (!mentions.isEmpty()) {
            if (wildFlyBotConfig.isDryRun()) {
                LOG.infof("Pull request #%d - PR review requested from \"%s\"", pullRequest.getNumber(), String.join(",", mentions));
            } else {
                List<GHUser> reviewers = mentions.stream()
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
                try {
                    pullRequest.requestReviewers(reviewers);
                } catch (HttpException e) {
                    LOG.errorf("The request has failed due to %s", e.getMessage());
                }
            }
        }
    }

    private void updateCCMentions(GHPullRequest pullRequest, List<String> newMentions) throws IOException {
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
                    List<String> currentMentions = Arrays.stream(comment.getBody().split(" @"))
                            .skip(1)
                            .map(s -> s.endsWith(",")
                                    ? s.substring(0, s.length() - 1)
                                    : s)
                            .toList();

                    if (!currentMentions.containsAll(newMentions) ||
                        currentMentions.size() != newMentions.size()) {

                        String updatedBody = "/cc @" + String.join(", @", newMentions);
                        if (wildFlyBotConfig.isDryRun()) {
                            LOG.infof("Pull request %d - Update comment %s to %s", pullRequest.getNumber(), comment.getBody(), updatedBody);
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
}
