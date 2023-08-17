package io.xstefank.wildfly.bot;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;
import io.xstefank.wildfly.bot.utils.GitHubJson;
import io.xstefank.wildfly.bot.utils.Util;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GHPullRequest;
import org.mockito.Mockito;

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
        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, gitHubJson, INVALID_COMMIT_MESSAGE))
                .when().payloadFromString(gitHubJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    verify(mocks.pullRequest(gitHubJson.id())).getBody();
                    verify(mocks.pullRequest(gitHubJson.id())).listFiles();
                    // Following invocations are used for logging
                    verify(mocks.pullRequest(gitHubJson.id())).getNumber();
                    verify(mocks.pullRequest(gitHubJson.id())).getTitle();
                    verifyNoMoreInteractions(mocks.pullRequest(gitHubJson.id()));
                });
    }

    @Test
    void testSkippingFormatCheckOnDraft() throws IOException {
        gitHubJson = GitHubJson.builder(VALID_PR_TEMPLATE_JSON)
                .title(INVALID_TITLE)
                .build();
        given().github(mocks -> {
            // TODO update with `add-labels` branch merged
            GHPullRequest pullRequest = mocks.pullRequest(gitHubJson.id());
            Mockito.when(pullRequest.isDraft()).thenReturn(true);
            Util.mockRepo(mocks, wildflyConfigFile, gitHubJson, null);
        })
                .when().payloadFromString(gitHubJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    verify(mocks.pullRequest(gitHubJson.id())).getBody();
                    verify(mocks.pullRequest(gitHubJson.id())).listFiles();
                    verify(mocks.pullRequest(gitHubJson.id()), times(2)).isDraft();
                    verifyNoMoreInteractions(mocks.pullRequest(gitHubJson.id()));
                });
    }
}
