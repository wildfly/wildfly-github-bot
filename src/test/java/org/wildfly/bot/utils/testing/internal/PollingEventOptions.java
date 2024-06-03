package org.wildfly.bot.utils.testing.internal;

import io.quarkiverse.githubapp.testing.GitHubAppTesting;
import io.quarkiverse.githubapp.testing.dsl.EventHandlingResponse;
import io.quarkiverse.githubapp.testing.dsl.GitHubMockSetup;
import io.quarkiverse.githubapp.testing.dsl.GitHubMockVerification;
import org.wildfly.bot.utils.WildflyGitHubBotTesting;
import org.wildfly.bot.utils.testing.IOExceptionFunction;
import org.wildfly.bot.utils.testing.dsl.EventSenderOptions;

public class PollingEventOptions {
    private final GitHubMockSetup<? extends Throwable> gitHubMockSetup;
    private final IOExceptionFunction<io.quarkiverse.githubapp.testing.dsl.EventSenderOptions, EventHandlingResponse> sseTrigger;
    private final IOExceptionFunction<EventSenderOptions, EventHandlingResponse> pollingTrigger;

    PollingEventOptions(GitHubMockSetup<? extends Throwable> gitHubMockSetup,
            IOExceptionFunction<io.quarkiverse.githubapp.testing.dsl.EventSenderOptions, EventHandlingResponse> sseTrigger,
            IOExceptionFunction<EventSenderOptions, EventHandlingResponse> pollingTrigger) {
        this.gitHubMockSetup = gitHubMockSetup;
        this.sseTrigger = sseTrigger;
        this.pollingTrigger = pollingTrigger;
    }

    public void then(GitHubMockVerification<? extends Throwable> verification) throws Throwable {
        EventHandlingResponse afterTrigger;

        if (TestModel.pollingProfile()) { // test with polling test framework given().github()...
            afterTrigger = pollingTrigger.apply(WildflyGitHubBotTesting.given().github(gitHubMockSetup::setup).when());
        } else { // default test with quarkus-github-app given().github()...
            afterTrigger = sseTrigger.apply(GitHubAppTesting.given().github(gitHubMockSetup::setup).when());
        }

        afterTrigger.then().github(verification);
    }
}
