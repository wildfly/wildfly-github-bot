package org.wildfly.bot.utils.testing;

import io.quarkiverse.githubapp.testing.dsl.EventHandlingResponse;
import io.quarkiverse.githubapp.testing.dsl.ValidatableEventHandling;
import io.quarkiverse.githubapp.testing.internal.GitHubAppTestingContext;

public class EventHandlingResponseImpl implements EventHandlingResponse {
    private final GitHubAppTestingContext testingContext;

    EventHandlingResponseImpl(GitHubAppTestingContext testingContext) {
        this.testingContext = testingContext;
    }

    @Override
    public int statusCode() {
        // Events are dispatched in-process, bypassing the HTTP layer, so there is no status code to report.
        return -1;
    }

    @Override
    public ValidatableEventHandling then() {
        return new ValidatableEventHandlingImpl(testingContext);
    }
}
