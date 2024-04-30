package org.wildfly.bot.utils;

import org.wildfly.bot.utils.testing.PullRequestJson;
import org.wildfly.bot.utils.testing.PullRequestReviewJsonBuilder;

public interface PullRequestReviewJsonBuildable extends PullRequestReviewJsonBuilder {

    PullRequestJson build();
}
