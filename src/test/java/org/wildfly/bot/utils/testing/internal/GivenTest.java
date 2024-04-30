package org.wildfly.bot.utils.testing.internal;

import io.quarkiverse.githubapp.testing.dsl.EventHandlingResponse;
import io.quarkiverse.githubapp.testing.dsl.EventSenderOptions;
import io.quarkiverse.githubapp.testing.dsl.GitHubMockSetup;
import org.wildfly.bot.utils.testing.CheckedFunction;

public class GivenTest {
    private final GitHubMockSetup<? extends Throwable> gitHubMockSetup;

    GivenTest(GitHubMockSetup<? extends Throwable> gitHubMockSetup) {
        this.gitHubMockSetup = gitHubMockSetup;
    }

    /**
     * Function, for creating corresponding trigger in the test pipeline and setting options for sse event.
     * <p>
     * For example:
     * given().github(..omitted..).when()
     * .payloadFromString(..omitted..).event(GHEvent.PULL_REQUEST) <- this line
     * .then().github(..omitted..)
     * <p>
     * Would be converted into following lambda
     * eventSenderOptions -> eventSenderOptions.payloadFromString(..omitted..).event(GHEvent.PULL_REQUEST)
     *
     * @param trigger
     */
    public final SseEventOptions sseEventOptions(CheckedFunction<EventSenderOptions, EventHandlingResponse> trigger) {
        return new SseEventOptions(gitHubMockSetup, trigger);
    }
}
