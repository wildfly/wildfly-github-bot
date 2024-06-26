package org.wildfly.bot.utils.testing.internal;

import io.quarkiverse.githubapp.testing.dsl.EventHandlingResponse;
import io.quarkiverse.githubapp.testing.dsl.EventSenderOptions;
import io.quarkiverse.githubapp.testing.dsl.GitHubMockSetup;
import org.kohsuke.github.GHEvent;
import org.wildfly.bot.utils.testing.IOExceptionFunction;
import org.wildfly.bot.utils.testing.PullRequestJson;

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
     */
    public final SseEventOptions sseEventOptions(IOExceptionFunction<EventSenderOptions, EventHandlingResponse> trigger) {
        return new SseEventOptions(gitHubMockSetup, trigger);
    }

    public PollingEventOptions pullRequestEvent(PullRequestJson pullRequestJson) {
        return new PollingEventOptions(gitHubMockSetup,
                eventSenderOptions -> eventSenderOptions.payloadFromString(pullRequestJson.jsonString())
                        .event(GHEvent.PULL_REQUEST),
                eventSenderOptions -> eventSenderOptions.eventFromPayload(pullRequestJson.jsonString()));
    }
}
