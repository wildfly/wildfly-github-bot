package org.wildfly.bot.utils.testing.internal;

import io.quarkiverse.githubapp.testing.dsl.EventHandlingResponse;
import io.quarkiverse.githubapp.testing.dsl.GitHubMockSetup;
import org.wildfly.bot.utils.testing.CheckedFunction;
import org.wildfly.bot.utils.testing.dsl.EventSenderOptions;

public class SseEventOptions {
    private final GitHubMockSetup<? extends Throwable> gitHubMockSetup;
    private final CheckedFunction<io.quarkiverse.githubapp.testing.dsl.EventSenderOptions, EventHandlingResponse> sseTrigger;

    SseEventOptions(GitHubMockSetup<? extends Throwable> gitHubMockSetup,
            CheckedFunction<io.quarkiverse.githubapp.testing.dsl.EventSenderOptions, EventHandlingResponse> sseTrigger) {
        this.gitHubMockSetup = gitHubMockSetup;
        this.sseTrigger = sseTrigger;
    }

    /**
     * Function, for creating corresponding trigger in the test pipeline and setting options for pooling trigger.
     * <p>
     * For example:
     * given().github(..omitted..).when()
     * .eventFromPayload(..omitted..) <- this line
     * .then().github(..omitted..)
     * <p>
     * Would be converted into following lambda
     * eventSenderOptions -> eventSenderOptions.eventFromPayload(..omitted..)
     *
     * @param trigger
     */
    public PollingEventOptions pollingEventOptions(CheckedFunction<EventSenderOptions, EventHandlingResponse> trigger) {
        return new PollingEventOptions(gitHubMockSetup, sseTrigger, trigger);
    }
}
