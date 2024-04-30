package org.wildfly.bot.utils;

import org.wildfly.bot.utils.testing.PullRequestJson;
import org.wildfly.bot.utils.testing.PullRequestJsonBuilder;

public interface PullRequestJsonBuildable extends PullRequestJsonBuilder {

    PullRequestJson build();
}
