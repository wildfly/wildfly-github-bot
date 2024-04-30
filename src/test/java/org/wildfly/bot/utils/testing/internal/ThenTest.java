package org.wildfly.bot.utils.testing.internal;

import io.quarkiverse.githubapp.testing.GitHubAppTesting;
import io.quarkiverse.githubapp.testing.dsl.EventHandlingResponse;
import io.quarkiverse.githubapp.testing.dsl.GitHubMockSetup;
import io.quarkiverse.githubapp.testing.dsl.GitHubMockVerification;
import org.wildfly.bot.utils.WildflyGitHubBotTesting;
import org.wildfly.bot.utils.testing.CheckedFunction;
import org.wildfly.bot.utils.testing.dsl.EventSenderOptions;

public class ThenTest {
    private final GitHubMockSetup<? extends Throwable> gitHubMockSetup;
    private final CheckedFunction<io.quarkiverse.githubapp.testing.dsl.EventSenderOptions, EventHandlingResponse> sseTrigger;
    private final CheckedFunction<EventSenderOptions, EventHandlingResponse> pollingTrigger;
    private final GitHubMockVerification<? extends Throwable> verification;

    ThenTest(GitHubMockSetup<? extends Throwable> gitHubMockSetup,
            CheckedFunction<io.quarkiverse.githubapp.testing.dsl.EventSenderOptions, EventHandlingResponse> sseTrigger,
            CheckedFunction<EventSenderOptions, EventHandlingResponse> pollingTrigger,
            GitHubMockVerification<? extends Throwable> verification) {
        this.gitHubMockSetup = gitHubMockSetup;
        this.sseTrigger = sseTrigger;
        this.pollingTrigger = pollingTrigger;
        this.verification = verification;
    }

    public final void run() throws Throwable {
        EventHandlingResponse afterTrigger;

        if (TestModel.pollingProfile()) { // test with polling test framework given().github()...
            afterTrigger = pollingTrigger.apply(WildflyGitHubBotTesting.given().github(gitHubMockSetup::setup).when());
        } else { // default test with quarkus-github-app given().github()...
            afterTrigger = sseTrigger.apply(GitHubAppTesting.given().github(gitHubMockSetup::setup).when());
        }

        afterTrigger.then().github(verification);
    }
}
