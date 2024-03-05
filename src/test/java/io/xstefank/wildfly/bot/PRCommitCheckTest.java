package io.xstefank.wildfly.bot;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;
import io.xstefank.wildfly.bot.utils.Mockable;
import io.xstefank.wildfly.bot.utils.MockedGHPullRequest;
import io.xstefank.wildfly.bot.utils.PullRequestJson;
import io.xstefank.wildfly.bot.utils.Util;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GHRepository;
import org.mockito.Mockito;

import java.io.IOException;

import static io.quarkiverse.githubapp.testing.GitHubAppTesting.given;
import static io.xstefank.wildfly.bot.model.RuntimeConstants.DEFAULT_COMMIT_MESSAGE;
import static io.xstefank.wildfly.bot.model.RuntimeConstants.PROJECT_PATTERN_REGEX;
import static io.xstefank.wildfly.bot.utils.TestConstants.INVALID_COMMIT_MESSAGE;
import static io.xstefank.wildfly.bot.utils.TestConstants.TEST_REPO;
import static io.xstefank.wildfly.bot.utils.TestConstants.VALID_PR_TEMPLATE_JSON;
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
        pullRequestJson = PullRequestJson.builder(VALID_PR_TEMPLATE_JSON).build();
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
                .commit(INVALID_COMMIT_MESSAGE);
        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, pullRequestJson, mockedContext))
                .when().payloadFromString(pullRequestJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository(TEST_REPO);
                    Util.verifyFormatFailure(repo, pullRequestJson, "commit");
                    Util.verifyFailedFormatComment(mocks, pullRequestJson, "- " + String.format(DEFAULT_COMMIT_MESSAGE,
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
                .commit(INVALID_COMMIT_MESSAGE);

        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, pullRequestJson, mockedContext))
                .when().payloadFromString(pullRequestJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository(TEST_REPO);
                    Util.verifyFormatFailure(repo, pullRequestJson, "commit");
                    Util.verifyFailedFormatComment(mocks, pullRequestJson, "- " + String.format(DEFAULT_COMMIT_MESSAGE,
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
                .github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, pullRequestJson))
                .when().payloadFromString(pullRequestJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository(TEST_REPO);
                    Util.verifyFormatSuccess(repo, pullRequestJson);
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
                .github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, pullRequestJson, mockedContext))
                .when().payloadFromString(pullRequestJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository(TEST_REPO);
                    Util.verifyFormatSuccess(repo, pullRequestJson);
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
                .github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, pullRequestJson))
                .when().payloadFromString(pullRequestJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository(TEST_REPO);
                    Util.verifyFormatSuccess(repo, pullRequestJson);
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
                .commit(INVALID_COMMIT_MESSAGE);

        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, pullRequestJson, mockedContext))
                .when().payloadFromString(pullRequestJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository(TEST_REPO);
                    Util.verifyFormatSuccess(repo, pullRequestJson);
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
                .commit(INVALID_COMMIT_MESSAGE);

        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, pullRequestJson, mockedContext))
                .when().payloadFromString(pullRequestJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository(TEST_REPO);
                    Util.verifyFormatFailure(repo, pullRequestJson, "commit");
                    Util.verifyFailedFormatComment(mocks, pullRequestJson, "- Lorem ipsum dolor sit amet");
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
                .commit(INVALID_COMMIT_MESSAGE);
        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, pullRequestJson))
                .when().payloadFromString(pullRequestJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository(TEST_REPO);
                    Mockito.verify(repo, never()).createCommitStatus(anyString(), any(), anyString(), anyString());
                    Mockito.verify(repo, never()).createCommitStatus(anyString(), any(), anyString(), anyString(), anyString());
                });
    }
}
