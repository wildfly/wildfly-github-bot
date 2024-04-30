package org.wildfly.bot.webhooks;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GHRepository;
import org.mockito.Mockito;
import org.wildfly.bot.utils.TestConstants;
import org.wildfly.bot.utils.WildflyGitHubBotTesting;
import org.wildfly.bot.utils.mocking.Mockable;
import org.wildfly.bot.utils.mocking.MockedGHPullRequest;
import org.wildfly.bot.utils.model.SsePullRequestPayload;
import org.wildfly.bot.utils.testing.PullRequestJson;
import org.wildfly.bot.utils.testing.internal.TestModel;
import org.wildfly.bot.utils.testing.model.PullRequestGitHubEventPayload;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.wildfly.bot.model.RuntimeConstants.DEFAULT_COMMIT_MESSAGE;
import static org.wildfly.bot.model.RuntimeConstants.PROJECT_PATTERN_REGEX;

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
    static void setUpGitHubJson() throws Exception {
        TestModel.setAllCallables(
                () -> SsePullRequestPayload.builder(TestConstants.VALID_PR_TEMPLATE_JSON),
                PullRequestGitHubEventPayload::new);

        pullRequestJson = TestModel.getPullRequestJson();
    }

    @Test
    void testFailedCommitCheck() throws Throwable {
        wildflyConfigFile = """
                wildfly:
                  format:
                    commit:
                      enabled: true
                """;
        mockedContext = MockedGHPullRequest.builder(pullRequestJson.id())
                .commit(TestConstants.INVALID_COMMIT_MESSAGE);

        TestModel.given(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson, mockedContext))
                .sseEventOptions(eventSenderOptions -> eventSenderOptions.payloadFromString(pullRequestJson.jsonString())
                        .event(GHEvent.PULL_REQUEST))
                .pollingEventOptions(eventSenderOptions -> eventSenderOptions.eventFromPayload(pullRequestJson.jsonString()))
                .then(mocks -> {
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    WildflyGitHubBotTesting.verifyFormatFailure(repo, pullRequestJson, "commit");
                    WildflyGitHubBotTesting.verifyFailedFormatComment(mocks, pullRequestJson,
                            "- " + String.format(DEFAULT_COMMIT_MESSAGE,
                                    PROJECT_PATTERN_REGEX.formatted("WFLY")));
                })
                .run();
    }

    @Test
    void testFailedCommitCheckCommitConfigNull() throws Throwable {
        wildflyConfigFile = """
                wildfly:
                  format:
                    commit:
                """;
        mockedContext = MockedGHPullRequest.builder(pullRequestJson.id())
                .commit(TestConstants.INVALID_COMMIT_MESSAGE);

        TestModel.given(
                mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson, mockedContext))
                .sseEventOptions(eventSenderOptions -> eventSenderOptions.payloadFromString(pullRequestJson.jsonString())
                        .event(GHEvent.PULL_REQUEST))
                .pollingEventOptions(eventSenderOptions -> eventSenderOptions.eventFromPayload(pullRequestJson.jsonString()))
                .then(mocks -> {
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    WildflyGitHubBotTesting.verifyFormatFailure(repo, pullRequestJson, "commit");
                    WildflyGitHubBotTesting.verifyFailedFormatComment(mocks, pullRequestJson,
                            "- " + String.format(DEFAULT_COMMIT_MESSAGE,
                                    PROJECT_PATTERN_REGEX.formatted("WFLY", "WFLY")));
                })
                .run();
    }

    @Test
    void testSuccessfulCommitCheck() throws Throwable {
        wildflyConfigFile = """
                wildfly:
                  format:
                    commit:
                      enabled: true
                """;

        TestModel.given(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson))
                .sseEventOptions(eventSenderOptions -> eventSenderOptions.payloadFromString(pullRequestJson.jsonString())
                        .event(GHEvent.PULL_REQUEST))
                .pollingEventOptions(eventSenderOptions -> eventSenderOptions.eventFromPayload(pullRequestJson.jsonString()))
                .then(mocks -> {
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    WildflyGitHubBotTesting.verifyFormatSuccess(repo, pullRequestJson);
                })
                .run();
    }

    /**
     * Issue detected on the following PR with a multiline commit https://github.com/wildfly/wildfly/pull/17092
     */
    @Test
    void testSuccessfulMultilinedCommitCheck() throws Throwable {
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

        TestModel.given(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson,
                mockedContext))
                .sseEventOptions(eventSenderOptions -> eventSenderOptions.payloadFromString(pullRequestJson.jsonString())
                        .event(GHEvent.PULL_REQUEST))
                .pollingEventOptions(eventSenderOptions -> eventSenderOptions.eventFromPayload(pullRequestJson.jsonString()))
                .then(mocks -> {
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    WildflyGitHubBotTesting.verifyFormatSuccess(repo, pullRequestJson);
                })
                .run();
    }

    @Test
    void testSuccessfulCommitCheckCommitConfigNull() throws Throwable {
        wildflyConfigFile = """
                wildfly:
                  format:
                    commit:
                """;

        TestModel.given(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson))
                .sseEventOptions(eventSenderOptions -> eventSenderOptions.payloadFromString(pullRequestJson.jsonString())
                        .event(GHEvent.PULL_REQUEST))
                .pollingEventOptions(eventSenderOptions -> eventSenderOptions.eventFromPayload(pullRequestJson.jsonString()))
                .then(mocks -> {
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    WildflyGitHubBotTesting.verifyFormatSuccess(repo, pullRequestJson);
                })
                .run();
    }

    @Test
    void testDisabledCommitCheck() throws Throwable {
        wildflyConfigFile = """
                wildfly:
                  format:
                    commit:
                      enabled: false
                """;
        mockedContext = MockedGHPullRequest.builder(pullRequestJson.id())
                .commit(TestConstants.INVALID_COMMIT_MESSAGE);

        TestModel.given(
                mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson, mockedContext))
                .sseEventOptions(eventSenderOptions -> eventSenderOptions.payloadFromString(pullRequestJson.jsonString())
                        .event(GHEvent.PULL_REQUEST))
                .pollingEventOptions(eventSenderOptions -> eventSenderOptions.eventFromPayload(pullRequestJson.jsonString()))
                .then(mocks -> {
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    WildflyGitHubBotTesting.verifyFormatSuccess(repo, pullRequestJson);
                })
                .run();
    }

    @Test
    public void testOverridingCommitMessage() throws Throwable {
        wildflyConfigFile = """
                wildfly:
                  format:
                    commit:
                      message: "Lorem ipsum dolor sit amet"
                """;

        mockedContext = MockedGHPullRequest.builder(pullRequestJson.id())
                .commit(TestConstants.INVALID_COMMIT_MESSAGE);

        TestModel.given(
                mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson, mockedContext))
                .sseEventOptions(eventSenderOptions -> eventSenderOptions.payloadFromString(pullRequestJson.jsonString())
                        .event(GHEvent.PULL_REQUEST))
                .pollingEventOptions(eventSenderOptions -> eventSenderOptions.eventFromPayload(pullRequestJson.jsonString()))
                .then(mocks -> {
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    WildflyGitHubBotTesting.verifyFormatFailure(repo, pullRequestJson, "commit");
                    WildflyGitHubBotTesting.verifyFailedFormatComment(mocks, pullRequestJson,
                            "- Lorem ipsum dolor sit amet");
                })
                .run();
    }

    @Test
    public void testDisableGlobalFormatCheck() throws Throwable {
        wildflyConfigFile = """
                wildfly:
                  format:
                    enabled: false
                """;
        mockedContext = MockedGHPullRequest.builder(pullRequestJson.id())
                .commit(TestConstants.INVALID_COMMIT_MESSAGE);

        TestModel.given(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson))
                .sseEventOptions(eventSenderOptions -> eventSenderOptions.payloadFromString(pullRequestJson.jsonString())
                        .event(GHEvent.PULL_REQUEST))
                .pollingEventOptions(eventSenderOptions -> eventSenderOptions.eventFromPayload(pullRequestJson.jsonString()))
                .then(mocks -> {
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    Mockito.verify(repo, never()).createCommitStatus(anyString(), any(), anyString(), anyString());
                    Mockito.verify(repo, never()).createCommitStatus(anyString(), any(), anyString(), anyString(), anyString());
                })
                .run();
    }
}
