package org.wildfly.bot.webhooks;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHEvent;
import org.wildfly.bot.utils.TestConstants;
import org.wildfly.bot.utils.WildflyGitHubBotTesting;
import org.wildfly.bot.utils.mocking.Mockable;
import org.wildfly.bot.utils.mocking.MockedGHPullRequest;
import org.wildfly.bot.utils.model.SsePullRequestPayload;

import java.io.IOException;

import static io.quarkiverse.githubapp.testing.GitHubAppTesting.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@QuarkusTest
@GitHubAppTest
public class PRSkipPullRequestTest {

    private static SsePullRequestPayload ssePullRequestPayload;
    private Mockable mockedContext;
    private static final String wildflyConfigFile = """
            wildfly:
              format:
                title:
                  enabled: true
                commit:
                  enabled: true
            """;

    @Test
    void testSkippingFormatCheck() throws IOException {
        ssePullRequestPayload = SsePullRequestPayload.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .title(TestConstants.INVALID_TITLE)
                .description("""
                        Hi


                        line start @wildfly-bot[bot] skip format random things

                        to pass on my local env @wildfly-github-bot-fork[bot] skip format thanks

                        finished""")
                .build();
        mockedContext = MockedGHPullRequest.builder(ssePullRequestPayload.id()).commit(TestConstants.INVALID_COMMIT_MESSAGE);
        given().github(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, ssePullRequestPayload, mockedContext))
                .when().payloadFromString(ssePullRequestPayload.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    verify(mocks.pullRequest(ssePullRequestPayload.id()), times(2)).getBody();
                    verify(mocks.pullRequest(ssePullRequestPayload.id())).listFiles();
                    verify(mocks.pullRequest(ssePullRequestPayload.id())).listComments();
                    // Following invocations are used for logging
                    verify(mocks.pullRequest(ssePullRequestPayload.id()), times(2)).getNumber();
                    // commit status should not be set
                    verifyNoMoreInteractions(mocks.pullRequest(ssePullRequestPayload.id()));
                });
    }

    @Test
    void testSkippingFormatCheckOnDraft() throws IOException {
        ssePullRequestPayload = SsePullRequestPayload.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .title(TestConstants.INVALID_TITLE)
                .build();
        mockedContext = MockedGHPullRequest.builder(ssePullRequestPayload.id()).draft();
        given().github(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, ssePullRequestPayload, mockedContext))
                .when().payloadFromString(ssePullRequestPayload.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    verify(mocks.pullRequest(ssePullRequestPayload.id()), times(2)).getBody();
                    verify(mocks.pullRequest(ssePullRequestPayload.id())).listFiles();
                    verify(mocks.pullRequest(ssePullRequestPayload.id()), times(2)).isDraft();
                    verify(mocks.pullRequest(ssePullRequestPayload.id())).listComments();
                    // commit status should not be set
                    verifyNoMoreInteractions(mocks.pullRequest(ssePullRequestPayload.id()));
                });
    }
}
