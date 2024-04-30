package org.wildfly.bot.webhooks;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHEvent;
import org.wildfly.bot.utils.TestConstants;
import org.wildfly.bot.utils.WildflyGitHubBotTesting;
import org.wildfly.bot.utils.mocking.Mockable;
import org.wildfly.bot.utils.mocking.MockedGHPullRequest;
import org.wildfly.bot.utils.model.SsePullRequestPayload;
import org.wildfly.bot.utils.testing.PullRequestJson;
import org.wildfly.bot.utils.testing.internal.TestModel;
import org.wildfly.bot.utils.testing.model.PullRequestGitHubEventPayload;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@QuarkusTest
@GitHubAppTest
public class PRSkipPullRequestTest {

    private static PullRequestJson pullRequestJson;
    private Mockable mockedContext;
    private static final String wildflyConfigFile = """
            wildfly:
              format:
                title:
                  enabled: true
                commit:
                  enabled: true
            """;

    @BeforeAll
    static void setPullRequestJson() {
        TestModel.setAllCallables(
                () -> SsePullRequestPayload.builder(TestConstants.VALID_PR_TEMPLATE_JSON),
                PullRequestGitHubEventPayload::new);
    }

    @Test
    void testSkippingFormatCheck() throws Throwable {
        pullRequestJson = TestModel.setPullRequestJsonBuilderBuild(pullRequestJsonBuilder -> pullRequestJsonBuilder
                .title(TestConstants.INVALID_TITLE)
                .description("""
                        Hi


                        line start @wildfly-bot[bot] skip format random things

                        to pass on my local env @wildfly-github-bot-fork[bot] skip format thanks

                        finished"""));
        mockedContext = MockedGHPullRequest.builder(pullRequestJson.id()).commit(TestConstants.INVALID_COMMIT_MESSAGE);

        TestModel.given(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson, mockedContext))
                .sseEventOptions(eventSenderOptions -> eventSenderOptions.payloadFromString(pullRequestJson.jsonString())
                        .event(GHEvent.PULL_REQUEST))
                .pollingEventOptions(eventSenderOptions -> eventSenderOptions.eventFromPayload(pullRequestJson.jsonString()))
                .then(mocks -> {
                    verify(mocks.pullRequest(pullRequestJson.id()), times(2)).getBody();
                    verify(mocks.pullRequest(pullRequestJson.id())).listFiles();
                    verify(mocks.pullRequest(pullRequestJson.id())).listComments();
                    // Following invocations are used for logging
                    verify(mocks.pullRequest(pullRequestJson.id()), times(2)).getNumber();
                    // commit status should not be set
                    verifyNoMoreInteractions(mocks.pullRequest(pullRequestJson.id()));
                })
                .run();
    }

    @Test
    void testSkippingFormatCheckOnDraft() throws Throwable {
        pullRequestJson = TestModel.setPullRequestJsonBuilderBuild(
                pullRequestJsonBuilder -> pullRequestJsonBuilder.title(TestConstants.INVALID_TITLE));
        mockedContext = MockedGHPullRequest.builder(pullRequestJson.id()).draft();

        TestModel.given(
                mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson, mockedContext))
                .sseEventOptions(eventSenderOptions -> eventSenderOptions.payloadFromString(pullRequestJson.jsonString())
                        .event(GHEvent.PULL_REQUEST))
                .pollingEventOptions(eventSenderOptions -> eventSenderOptions.eventFromPayload(pullRequestJson.jsonString()))
                .then(mocks -> {
                    verify(mocks.pullRequest(pullRequestJson.id()), times(2)).getBody();
                    verify(mocks.pullRequest(pullRequestJson.id())).listFiles();
                    verify(mocks.pullRequest(pullRequestJson.id()), times(2)).isDraft();
                    verify(mocks.pullRequest(pullRequestJson.id())).listComments();
                    // commit status should not be set
                    verifyNoMoreInteractions(mocks.pullRequest(pullRequestJson.id()));
                })
                .run();
    }
}
