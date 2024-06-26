package org.wildfly.bot.utils.testing.model;

import org.kohsuke.github.GHEvent;
import org.wildfly.bot.utils.PullRequestReviewJsonBuildable;
import org.wildfly.bot.utils.model.ReviewState;
import org.wildfly.bot.utils.testing.PullRequestJson;

import java.io.IOException;

public class PullRequestReviewGitHubEventPayload extends PullRequestGitHubEventPayload
        implements PullRequestReviewJsonBuildable, PullRequestJson {

    private static final String REVIEW = "review";
    private static final String STATE = "state";
    private static final String fileName = "events/raw_pr_review_template.json";

    public PullRequestReviewGitHubEventPayload() throws IOException {
        super(fileName, GHEvent.PULL_REQUEST_REVIEW);
    }

    public PullRequestReviewGitHubEventPayload(String repository, long eventId) throws IOException {
        super(fileName, repository, GHEvent.PULL_REQUEST_REVIEW, eventId);
    }

    public PullRequestReviewGitHubEventPayload state(ReviewState state) {
        getPayload(REVIEW).put(STATE, state.toString());
        return this;
    }

    @Override
    public PullRequestReviewGitHubEventPayload build() {
        return this;
    }
}
