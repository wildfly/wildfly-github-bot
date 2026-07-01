package org.wildfly.bot;

import io.quarkiverse.githubapp.event.PullRequest;
import org.wildfly.bot.util.GithubProcessor;
import org.wildfly.bot.util.PullRequestLogger;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.GHPullRequest;
import org.wildfly.bot.model.RuntimeConstants;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RequestScoped
public class PullRequestLabelProcessor {

    private final PullRequestLogger logger = PullRequestLogger.getLogger(PullRequestLabelProcessor.class);

    @Inject
    GithubProcessor githubProcessor;

    void pullRequestLabelCheck(@PullRequest.Synchronize @PullRequest.Reopened GHEventPayload.PullRequest pullRequestPayload)
            throws IOException {
        GHPullRequest pullRequest = pullRequestPayload.getPullRequest();
        logger.setPullRequest(pullRequest);
        githubProcessor.logger.setPullRequest(pullRequest);

        if (shouldSkipLabelCheck(pullRequest)) {
            return;
        }

        List<String> labelsToAdd = new ArrayList<>();
        List<String> labelsToRemove = new ArrayList<>(List.of(RuntimeConstants.LABEL_FIX_ME));

        // By default, if we do not know the state, consider it mergeable.
        if (!Optional.ofNullable(pullRequest.getMergeable()).orElse(true)) {
            labelsToAdd.add(RuntimeConstants.LABEL_NEEDS_REBASE);
        } else {
            labelsToRemove.add(RuntimeConstants.LABEL_NEEDS_REBASE);
        }

        githubProcessor.updateLabels(pullRequest, labelsToAdd, labelsToRemove);
    }

    private boolean shouldSkipLabelCheck(GHPullRequest pullRequest) throws IOException {
        if (pullRequest.isDraft()) {
            logger.info("Skipping label check due to pull request being a draft");
            return true;
        }
        return false;
    }
}
