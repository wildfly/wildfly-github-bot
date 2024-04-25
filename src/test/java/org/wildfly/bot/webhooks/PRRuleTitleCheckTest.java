package org.wildfly.bot.webhooks;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.mockito.Mockito;
import org.wildfly.bot.utils.TestConstants;
import org.wildfly.bot.utils.WildflyGitHubBotTesting;
import org.wildfly.bot.utils.model.PullRequestJson;

import java.io.IOException;

import static io.quarkiverse.githubapp.testing.GitHubAppTesting.given;

/**
 * Tests for the Wildfly -> Rules -> Title checks.
 */
@QuarkusTest
@GitHubAppTest
public class PRRuleTitleCheckTest {

    private static String wildflyConfigFile;
    private static PullRequestJson pullRequestJson;

    @BeforeEach
    void setUpGitHubJson() throws IOException {
        pullRequestJson = PullRequestJson.builder(TestConstants.VALID_PR_TEMPLATE_JSON).build();
    }

    @Test
    void testSuccessfulTitleCheck() throws IOException {
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - id: "Title"
                      title: "Title"
                      notify: [Tadpole]
                """;
        given().github(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson))
                .when().payloadFromString(pullRequestJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHPullRequest mockedPR = mocks.pullRequest(pullRequestJson.id());
                    Mockito.verify(mockedPR).comment("/cc @Tadpole");
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    WildflyGitHubBotTesting.verifyFormatSuccess(repo, pullRequestJson);
                });
    }

    @Test
    void testFailedTitleCheck() throws IOException {
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - id: "Title"
                      title: "NonValidTitle"
                      notify: [Tadpole]
                """;
        given().github(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson))
                .when().payloadFromString(pullRequestJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHPullRequest mockedPR = mocks.pullRequest(pullRequestJson.id());
                    Mockito.verify(mockedPR, Mockito.never()).comment("/cc @Tadpole");
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    WildflyGitHubBotTesting.verifyFormatSuccess(repo, pullRequestJson);
                });
    }

    @Test
    void testTitleBodyCheckForTitleCaseInsensitive() throws IOException {
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - id: "Title"
                      title: "TiTle"
                      notify: [Tadpole]
                """;

        given().github(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson))
                .when().payloadFromString(pullRequestJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHPullRequest mockedPR = mocks.pullRequest(pullRequestJson.id());
                    Mockito.verify(mockedPR).comment("/cc @Tadpole");
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    WildflyGitHubBotTesting.verifyFormatSuccess(repo, pullRequestJson);
                });
    }

}
