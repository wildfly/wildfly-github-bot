package org.wildfly.bot.webhooks;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;
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
 * Tests for the Wildfly -> Rules -> Body checks.
 */
@QuarkusTest
@GitHubAppTest
public class PRRuleDescriptionCheckTest {

    private static String wildflyConfigFile;
    private static PullRequestJson pullRequestJson;

    @Test
    void testSuccessfulBodyCheck() throws IOException {
        pullRequestJson = PullRequestJson.builder(TestConstants.VALID_PR_TEMPLATE_JSON).build();
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - id: "Description"
                      body: "issues.redhat.com"
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
    void testFailedBodyCheck() throws IOException {
        pullRequestJson = PullRequestJson.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .description(TestConstants.INVALID_DESCRIPTION)
                .build();
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - id: "Description"
                      body: "issues.redhat.com"
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
    void testTitleBodyCheckForBodyCaseInsensitive() throws IOException {
        pullRequestJson = PullRequestJson.builder(TestConstants.VALID_PR_TEMPLATE_JSON).build();
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - id: "Description"
                      body: "ISSUES.REDHAT.COM"
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
