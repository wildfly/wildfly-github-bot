package org.wildfly.bot;

import io.quarkiverse.githubapp.event.PullRequestReview;
import org.wildfly.bot.config.WildFlyBotConfig;
import org.wildfly.bot.model.RuntimeConstants;
import org.wildfly.bot.util.PullRequestLogger;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHPullRequestReview;

import java.io.IOException;

import static org.kohsuke.github.GHPullRequestReviewState.CHANGES_REQUESTED;

@RequestScoped
public class PullRequestReviewProcessor {

    private final PullRequestLogger logger = PullRequestLogger.getLogger(PullRequestReviewProcessor.class);

    @Inject
    WildFlyBotConfig wildFlyBotConfig;

    void pullRequestReviewCheck(
            @PullRequestReview.Submitted GHEventPayload.PullRequestReview pullRequestPayload)
            throws IOException {
        GHPullRequestReview pullRequestReview = pullRequestPayload.getReview();
        GHPullRequest pullRequest = pullRequestPayload.getPullRequest();
        logger.setPullRequest(pullRequest);

        if (pullRequest.isDraft()) {
            logger.info("Skipping review handling due to pull request being a draft");
            return;
        }

        if (pullRequestReview.getState() == CHANGES_REQUESTED) {
            if (pullRequest.getLabels().stream()
                    .noneMatch(ghLabel -> ghLabel.getName().equals(RuntimeConstants.LABEL_FIX_ME))) {
                logger.infof("Changes requested, applying following labels: %s.", RuntimeConstants.LABEL_FIX_ME);
                if (wildFlyBotConfig.isDryRun()) {
                    logger.infof(
                            RuntimeConstants.DRY_RUN_PREPEND.formatted("The following labels have been applied: %s"),
                            RuntimeConstants.LABEL_FIX_ME);
                } else {
                    pullRequest.addLabels(RuntimeConstants.LABEL_FIX_ME);
                }
            }
        }
    }
}
