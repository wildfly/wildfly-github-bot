package org.wildfly.bot.utils.testing.model;

import org.kohsuke.github.GHEvent;
import org.wildfly.bot.utils.model.ReviewState;

import java.io.IOException;

public class PullRequestReviewGitHubEventPayload extends PullRequestGitHubEventPayload {

    private static final String REVIEW = "review";
    private static final String STATE = "state";
    private static final String fileName = "events/raw_pr_review_template.json";

    public PullRequestReviewGitHubEventPayload(String repository, long eventId) throws IOException {
        super(fileName, repository, GHEvent.PULL_REQUEST_REVIEW, eventId);
    }

    @SuppressWarnings("unchecked")
    public <T extends PullRequestReviewGitHubEventPayload> T state(ReviewState state) {
        getPayload(REVIEW).put(STATE, state.toString());
        return (T) this;
    }
}
