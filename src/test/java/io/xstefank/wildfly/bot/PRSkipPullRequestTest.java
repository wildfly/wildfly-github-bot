package io.xstefank.wildfly.bot;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;
import io.xstefank.wildfly.bot.utils.GitHubJson;
import io.xstefank.wildfly.bot.utils.MockedContext;
import io.xstefank.wildfly.bot.utils.Util;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHEvent;

import java.io.IOException;

import static io.quarkiverse.githubapp.testing.GitHubAppTesting.given;
import static io.xstefank.wildfly.bot.utils.TestConstants.INVALID_COMMIT_MESSAGE;
import static io.xstefank.wildfly.bot.utils.TestConstants.INVALID_TITLE;
import static io.xstefank.wildfly.bot.utils.TestConstants.VALID_PR_TEMPLATE_JSON;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@QuarkusTest
@GitHubAppTest
public class PRSkipPullRequestTest {

    private static GitHubJson gitHubJson;
    private MockedContext mockedContext;
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
        gitHubJson = GitHubJson.builder(VALID_PR_TEMPLATE_JSON)
                .title(INVALID_TITLE)
                .description("""
                        Hi


                        line start @wildfly-bot[bot] skip format random things

                        to pass on my local env @wildfly-github-bot-fork[bot] skip format thanks

                        finished""")
                .build();
        mockedContext = MockedContext.builder(gitHubJson.id()).commit(INVALID_COMMIT_MESSAGE);
        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, gitHubJson, mockedContext))
                .when().payloadFromString(gitHubJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    verify(mocks.pullRequest(gitHubJson.id()), times(2)).getBody();
                    verify(mocks.pullRequest(gitHubJson.id())).listFiles();
                    // Following invocations are used for logging
                    verify(mocks.pullRequest(gitHubJson.id()), times(2)).getNumber();
                    verifyNoMoreInteractions(mocks.pullRequest(gitHubJson.id()));
                });
    }

    @Test
    void testSkippingFormatCheckOnDraft() throws IOException {
        gitHubJson = GitHubJson.builder(VALID_PR_TEMPLATE_JSON)
                .title(INVALID_TITLE)
                .build();
        mockedContext = MockedContext.builder(gitHubJson.id()).draft();
        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, gitHubJson, mockedContext))
                .when().payloadFromString(gitHubJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    verify(mocks.pullRequest(gitHubJson.id()), times(2)).getBody();
                    verify(mocks.pullRequest(gitHubJson.id())).listFiles();
                    verify(mocks.pullRequest(gitHubJson.id()), times(2)).isDraft();
                    verifyNoMoreInteractions(mocks.pullRequest(gitHubJson.id()));
                });
    }
}
