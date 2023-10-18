package io.xstefank.wildfly.bot;

import io.quarkiverse.githubapp.event.PullRequestReview;
import io.xstefank.wildfly.bot.config.WildFlyBotConfig;
import io.xstefank.wildfly.bot.model.RuntimeConstants;
import io.xstefank.wildfly.bot.util.GithubProcessor;
import io.xstefank.wildfly.bot.util.PullRequestLogger;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHPullRequestReview;

import java.io.IOException;

import static io.xstefank.wildfly.bot.model.RuntimeConstants.LABEL_FIX_ME;
import static org.kohsuke.github.GHPullRequestReviewState.CHANGES_REQUESTED;

@RequestScoped
public class PullRequestReviewProcessor {

    private static final Logger LOG_DELEGATE = Logger.getLogger(PullRequestReviewProcessor.class);

    private final PullRequestLogger LOG = new PullRequestLogger(LOG_DELEGATE);

    @Inject
    GithubProcessor githubProcessor;

    @Inject
    WildFlyBotConfig wildFlyBotConfig;

    void pullRequestReviewCheck(
            @PullRequestReview.Submitted GHEventPayload.PullRequestReview pullRequestPayload)
            throws IOException {
        GHPullRequestReview pullRequestReview = pullRequestPayload.getReview();
        GHPullRequest pullRequest = pullRequestPayload.getPullRequest();
        LOG.setPullRequest(pullRequest);
        githubProcessor.LOG.setPullRequest(pullRequest);

        String message = githubProcessor.skipPullRequest(pullRequest);
        if (message != null) {
            LOG.infof("Skipping format due to %s", message);
            return;
        }

        if (pullRequestReview.getState() == CHANGES_REQUESTED) {
            if (pullRequest.getLabels().stream().noneMatch(ghLabel -> ghLabel.getName().equals(LABEL_FIX_ME))) {
                LOG.infof("Changes requested, applying following labels: %s.", LABEL_FIX_ME);
                if (wildFlyBotConfig.isDryRun()) {
                    LOG.infof(RuntimeConstants.DRY_RUN_PREPEND.formatted("The following labels have been applied: %s"),
                            LABEL_FIX_ME);
                } else {
                    pullRequest.addLabels(LABEL_FIX_ME);
                }
            }
        }
    }
}
