package org.wildfly.bot.webhooks;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GHRepository;
import org.wildfly.bot.utils.TestConstants;
import org.wildfly.bot.utils.WildflyGitHubBotTesting;
import org.wildfly.bot.utils.mocking.MockedGHPullRequest;
import org.wildfly.bot.utils.model.Action;
import org.wildfly.bot.utils.model.SsePullRequestPayload;
import org.wildfly.bot.utils.testing.PullRequestJson;
import org.wildfly.bot.utils.testing.internal.TestModel;
import org.wildfly.bot.utils.testing.model.PullRequestGitHubEventPayload;

import static org.wildfly.bot.model.RuntimeConstants.DEFAULT_TITLE_MESSAGE;
import static org.wildfly.bot.model.RuntimeConstants.PROJECT_PATTERN_REGEX;

/**
 * Tests for the Wildfly -> ProjectKey function.
 */
@QuarkusTest
@GitHubAppTest
public class PRFormatOverrideProjectKeyTest {

    private static final String wildflyConfigFile = """
            wildfly:
              projectKey: WFCORE
            """;
    private static PullRequestJson pullRequestJson;
    private static MockedGHPullRequest mockedContext;

    @BeforeAll
    static void setPullRequestJson() {
        TestModel.setAllCallables(
                () -> SsePullRequestPayload.builder(TestConstants.VALID_PR_TEMPLATE_JSON),
                PullRequestGitHubEventPayload::new);
    }

    @Test
    public void testOverridingProjectKeyCorrectTitle() throws Throwable {
        pullRequestJson = TestModel.setPullRequestJsonBuilderBuild(pullRequestJsonBuilder -> pullRequestJsonBuilder
                .action(Action.EDITED)
                .title("WFCORE-00000 title"));

        mockedContext = MockedGHPullRequest.builder(pullRequestJson.id())
                .commit("[WFCORE-123] Valid commit message");

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
    public void testOverridingProjectKeyIncorrectTitle() throws Throwable {
        pullRequestJson = TestModel
                .setPullRequestJsonBuilderBuild(pullRequestJsonBuilder -> pullRequestJsonBuilder.action(Action.EDITED));

        mockedContext = MockedGHPullRequest.builder(pullRequestJson.id())
                .commit("[WFCORE-123] Valid commit message");

        TestModel.given(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson,
                mockedContext))
                .sseEventOptions(eventSenderOptions -> eventSenderOptions.payloadFromString(pullRequestJson.jsonString())
                        .event(GHEvent.PULL_REQUEST))
                .pollingEventOptions(eventSenderOptions -> eventSenderOptions.eventFromPayload(pullRequestJson.jsonString()))
                .then(mocks -> {
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    WildflyGitHubBotTesting.verifyFormatFailure(repo, pullRequestJson, "title");
                    WildflyGitHubBotTesting.verifyFailedFormatComment(mocks, pullRequestJson,
                            "- " + String.format(DEFAULT_TITLE_MESSAGE,
                                    PROJECT_PATTERN_REGEX.formatted("WFCORE")));
                })
                .run();
    }
}
