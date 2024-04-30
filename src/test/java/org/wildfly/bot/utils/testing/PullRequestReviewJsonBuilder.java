package org.wildfly.bot.utils.testing;

import org.wildfly.bot.utils.model.ReviewState;

public interface PullRequestReviewJsonBuilder extends PullRequestJsonBuilder {

    PullRequestReviewJsonBuilder state(ReviewState state);
}
