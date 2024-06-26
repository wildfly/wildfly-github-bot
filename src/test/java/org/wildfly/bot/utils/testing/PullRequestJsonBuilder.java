package org.wildfly.bot.utils.testing;

import org.wildfly.bot.utils.PullRequestJsonBuildable;
import org.wildfly.bot.utils.model.Action;

/**
 * This interface should be used in tests, where {@link PullRequestJsonBuildable#build()}
 * method is removed from this interface, so the user is not tempted to instantiate
 * new object. As this should be done internally via API calls in {@link org.wildfly.bot.utils.testing.internal.TestModel}
 */
public interface PullRequestJsonBuilder {

    PullRequestJsonBuilder action(Action action);

    PullRequestJsonBuilder title(String title);

    PullRequestJsonBuilder description(String description);

    PullRequestJsonBuilder userLogin(String login);
}
