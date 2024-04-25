package org.wildfly.bot.utils;

import org.wildfly.bot.utils.model.Action;

public interface PullRequestJsonBuilder {

    PullRequestJsonBuilder action(Action action);

    PullRequestJsonBuilder title(String title);

    PullRequestJsonBuilder description(String description);

    PullRequestJsonBuilder userLogin(String login);

    PullRequestJson build();
}
