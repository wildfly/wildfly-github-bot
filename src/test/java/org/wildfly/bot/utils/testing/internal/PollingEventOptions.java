package org.wildfly.bot.utils.testing.internal;

import io.quarkiverse.githubapp.testing.dsl.EventHandlingResponse;
import io.quarkiverse.githubapp.testing.dsl.GitHubMockSetup;
import io.quarkiverse.githubapp.testing.dsl.GitHubMockVerification;
import org.wildfly.bot.utils.testing.CheckedFunction;
import org.wildfly.bot.utils.testing.dsl.EventSenderOptions;

public class PollingEventOptions {
    private final GitHubMockSetup<? extends Throwable> gitHubMockSetup;
    private final CheckedFunction<io.quarkiverse.githubapp.testing.dsl.EventSenderOptions, EventHandlingResponse> sseTrigger;
    private final CheckedFunction<EventSenderOptions, EventHandlingResponse> pollingTrigger;

    PollingEventOptions(GitHubMockSetup<? extends Throwable> gitHubMockSetup,
            CheckedFunction<io.quarkiverse.githubapp.testing.dsl.EventSenderOptions, EventHandlingResponse> sseTrigger,
            CheckedFunction<EventSenderOptions, EventHandlingResponse> pollingTrigger) {
        this.gitHubMockSetup = gitHubMockSetup;
        this.sseTrigger = sseTrigger;
        this.pollingTrigger = pollingTrigger;
    }

    public ThenTest then(GitHubMockVerification<? extends Throwable> verification) {
        return new ThenTest(gitHubMockSetup, sseTrigger, pollingTrigger, verification);
    }
}
