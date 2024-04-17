package org.wildfly.bot.webhooks;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;
import org.wildfly.bot.utils.mocking.Mockable;
import org.wildfly.bot.utils.mocking.MockedGHPullRequest;
import org.wildfly.bot.utils.model.PullRequestJson;
import org.wildfly.bot.utils.WildflyGitHubBotTesting;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GHRepository;
import org.mockito.Mockito;
import org.wildfly.bot.utils.TestConstants;

import java.io.IOException;

import static io.quarkiverse.githubapp.testing.GitHubAppTesting.given;
import static org.wildfly.bot.model.RuntimeConstants.DEFAULT_COMMIT_MESSAGE;
import static org.wildfly.bot.model.RuntimeConstants.PROJECT_PATTERN_REGEX;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;

/**
 * Tests for the Wildfly -> Format -> Commit checks.
 */
@QuarkusTest
@GitHubAppTest
public class PRCommitCheckTest {

    private static String wildflyConfigFile;
    private static PullRequestJson pullRequestJson;
    private Mockable mockedContext;

    @BeforeAll
    static void setUpGitHubJson() throws IOException {
        pullRequestJson = PullRequestJson.builder(TestConstants.VALID_PR_TEMPLATE_JSON).build();
    }

    @Test
    void testFailedCommitCheck() throws IOException {
        wildflyConfigFile = """
                wildfly:
                  format:
                    commit:
                      enabled: true
                """;
        mockedContext = MockedGHPullRequest.builder(pullRequestJson.id())
                .commit(TestConstants.INVALID_COMMIT_MESSAGE);
        given().github(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson, mockedContext))
                .when().payloadFromString(pullRequestJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    WildflyGitHubBotTesting.verifyFormatFailure(repo, pullRequestJson, "commit");
                    WildflyGitHubBotTesting.verifyFailedFormatComment(mocks, pullRequestJson,
                            "- " + String.format(DEFAULT_COMMIT_MESSAGE,
                                    PROJECT_PATTERN_REGEX.formatted("WFLY")));
                });
    }

    @Test
    void testFailedCommitCheckCommitConfigNull() throws IOException {
        wildflyConfigFile = """
                wildfly:
                  format:
                    commit:
                """;
        mockedContext = MockedGHPullRequest.builder(pullRequestJson.id())
                .commit(TestConstants.INVALID_COMMIT_MESSAGE);

        given().github(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson, mockedContext))
                .when().payloadFromString(pullRequestJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    WildflyGitHubBotTesting.verifyFormatFailure(repo, pullRequestJson, "commit");
                    WildflyGitHubBotTesting.verifyFailedFormatComment(mocks, pullRequestJson,
                            "- " + String.format(DEFAULT_COMMIT_MESSAGE,
                                    PROJECT_PATTERN_REGEX.formatted("WFLY", "WFLY")));
                });
    }

    @Test
    void testSuccessfulCommitCheck() throws IOException {
        wildflyConfigFile = """
                wildfly:
                  format:
                    commit:
                      enabled: true
                """;

        given()
                .github(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson))
                .when().payloadFromString(pullRequestJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    WildflyGitHubBotTesting.verifyFormatSuccess(repo, pullRequestJson);
                });
    }

    /**
     * Issue detected on the following PR with a multiline commit https://github.com/wildfly/wildfly/pull/17092
     */
    @Test
    void testSuccessfulMultilinedCommitCheck() throws IOException {
        wildflyConfigFile = """
                wildfly:
                  format:
                    commit:
                      enabled: true
                """;

        mockedContext = MockedGHPullRequest.builder(pullRequestJson.id())
                .commit("""
                        WFLY-18341 Restore incorrectly updated copyright dates in Jipijapa

                        This reverts incorrect changes from
                        096a516e745663a99fce0062ff4bb93a4ca1066f:
                        Upgrade to Hibernate Search 6.2.0.CR1""");
        given()
                .github(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson, mockedContext))
                .when().payloadFromString(pullRequestJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    WildflyGitHubBotTesting.verifyFormatSuccess(repo, pullRequestJson);
                });
    }

    @Test
    void testSuccessfulCommitCheckCommitConfigNull() throws IOException {
        wildflyConfigFile = """
                wildfly:
                  format:
                    commit:
                """;

        given()
                .github(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson))
                .when().payloadFromString(pullRequestJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    WildflyGitHubBotTesting.verifyFormatSuccess(repo, pullRequestJson);
                });
    }

    @Test
    void testDisabledCommitCheck() throws IOException {
        wildflyConfigFile = """
                wildfly:
                  format:
                    commit:
                      enabled: false
                """;
        mockedContext = MockedGHPullRequest.builder(pullRequestJson.id())
                .commit(TestConstants.INVALID_COMMIT_MESSAGE);

        given().github(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson, mockedContext))
                .when().payloadFromString(pullRequestJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    WildflyGitHubBotTesting.verifyFormatSuccess(repo, pullRequestJson);
                });
    }

    @Test
    public void testOverridingCommitMessage() throws IOException {
        wildflyConfigFile = """
                wildfly:
                  format:
                    commit:
                      message: "Lorem ipsum dolor sit amet"
                """;

        mockedContext = MockedGHPullRequest.builder(pullRequestJson.id())
                .commit(TestConstants.INVALID_COMMIT_MESSAGE);

        given().github(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson, mockedContext))
                .when().payloadFromString(pullRequestJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    WildflyGitHubBotTesting.verifyFormatFailure(repo, pullRequestJson, "commit");
                    WildflyGitHubBotTesting.verifyFailedFormatComment(mocks, pullRequestJson, "- Lorem ipsum dolor sit amet");
                });
    }

    @Test
    public void testDisableGlobalFormatCheck() throws IOException {
        wildflyConfigFile = """
                wildfly:
                  format:
                    enabled: false
                """;
        mockedContext = MockedGHPullRequest.builder(pullRequestJson.id())
                .commit(TestConstants.INVALID_COMMIT_MESSAGE);
        given().github(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson))
                .when().payloadFromString(pullRequestJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    Mockito.verify(repo, never()).createCommitStatus(anyString(), any(), anyString(), anyString());
                    Mockito.verify(repo, never()).createCommitStatus(anyString(), any(), anyString(), anyString(), anyString());
                });
    }
}
