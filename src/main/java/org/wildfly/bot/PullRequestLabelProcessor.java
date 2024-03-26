package org.wildfly.bot;

import io.quarkiverse.githubapp.event.PullRequest;
import org.wildfly.bot.util.GithubProcessor;
import org.wildfly.bot.util.PullRequestLogger;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.GHPullRequest;
import org.wildfly.bot.model.RuntimeConstants;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PullRequestLabelProcessor {

    private static final Logger LOG_DELEGATE = Logger.getLogger(PullRequestFormatProcessor.class);
    private final PullRequestLogger LOG = new PullRequestLogger(LOG_DELEGATE);

    @Inject
    GithubProcessor githubProcessor;

    void pullRequestLabelCheck(@PullRequest.Synchronize @PullRequest.Reopened GHEventPayload.PullRequest pullRequestPayload)
            throws IOException {
        GHPullRequest pullRequest = pullRequestPayload.getPullRequest();
        LOG.setPullRequest(pullRequest);
        githubProcessor.LOG.setPullRequest(pullRequest);

        String message = githubProcessor.skipPullRequest(pullRequest);
        if (message != null) {
            LOG.infof("Skipping labelling due to %s", message);
            return;
        }

        List<String> labelsToAdd = new ArrayList<>();
        List<String> labelsToRemove = new ArrayList<>(List.of(RuntimeConstants.LABEL_FIX_ME));

        // By default, if we do not know the state, consider it mergable.
        if (!Optional.ofNullable(pullRequest.getMergeable()).orElse(true)) {
            labelsToAdd.add(RuntimeConstants.LABEL_NEEDS_REBASE);
        } else {
            labelsToRemove.add(RuntimeConstants.LABEL_NEEDS_REBASE);
        }

        githubProcessor.updateLabels(pullRequest, labelsToAdd, labelsToRemove);
    }
}
