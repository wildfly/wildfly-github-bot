package org.wildfly.bot.webhooks;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.mockito.Mockito;
import org.wildfly.bot.utils.TestConstants;
import org.wildfly.bot.utils.WildflyGitHubBotTesting;
import org.wildfly.bot.utils.model.SsePullRequestPayload;
import org.wildfly.bot.utils.testing.PullRequestJson;
import org.wildfly.bot.utils.testing.internal.TestModel;
import org.wildfly.bot.utils.testing.model.PullRequestGitHubEventPayload;

/**
 * Tests for the Wildfly -> Rules -> Body checks.
 */
@QuarkusTest
@GitHubAppTest
public class PRRuleDescriptionCheckTest {

    private static String wildflyConfigFile;
    private static PullRequestJson pullRequestJson;

    @BeforeAll
    static void setPullRequestJson() {
        TestModel.setAllCallables(
                () -> SsePullRequestPayload.builder(TestConstants.VALID_PR_TEMPLATE_JSON),
                PullRequestGitHubEventPayload::new);
    }

    @Test
    void testSuccessfulBodyCheck() throws Throwable {
        pullRequestJson = TestModel.getPullRequestJson();
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - id: "Description"
                      body: "issues.redhat.com"
                      notify: [Tadpole]
                """;

        TestModel.given(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson))
                .sseEventOptions(eventSenderOptions -> eventSenderOptions.payloadFromString(pullRequestJson.jsonString())
                        .event(GHEvent.PULL_REQUEST))
                .pollingEventOptions(eventSenderOptions -> eventSenderOptions.eventFromPayload(pullRequestJson.jsonString()))
                .then(mocks -> {
                    GHPullRequest mockedPR = mocks.pullRequest(pullRequestJson.id());
                    Mockito.verify(mockedPR).comment("/cc @Tadpole");
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    WildflyGitHubBotTesting.verifyFormatSuccess(repo, pullRequestJson);
                })
                .run();
    }

    @Test
    void testFailedBodyCheck() throws Throwable {
        pullRequestJson = TestModel.setPullRequestJsonBuilderBuild(
                pullRequestJsonBuilder -> pullRequestJsonBuilder.description(TestConstants.INVALID_DESCRIPTION));
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - id: "Description"
                      body: "issues.redhat.com"
                      notify: [Tadpole]
                """;

        TestModel.given(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson))
                .sseEventOptions(eventSenderOptions -> eventSenderOptions.payloadFromString(pullRequestJson.jsonString())
                        .event(GHEvent.PULL_REQUEST))
                .pollingEventOptions(eventSenderOptions -> eventSenderOptions.eventFromPayload(pullRequestJson.jsonString()))
                .then(mocks -> {
                    GHPullRequest mockedPR = mocks.pullRequest(pullRequestJson.id());
                    Mockito.verify(mockedPR, Mockito.never()).comment("/cc @Tadpole");
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    WildflyGitHubBotTesting.verifyFormatSuccess(repo, pullRequestJson);
                })
                .run();
    }

    @Test
    void testTitleBodyCheckForBodyCaseInsensitive() throws Throwable {
        pullRequestJson = TestModel.getPullRequestJson();
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - id: "Description"
                      body: "ISSUES.REDHAT.COM"
                      notify: [Tadpole]
                """;

        TestModel.given(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson))
                .sseEventOptions(eventSenderOptions -> eventSenderOptions.payloadFromString(pullRequestJson.jsonString())
                        .event(GHEvent.PULL_REQUEST))
                .pollingEventOptions(eventSenderOptions -> eventSenderOptions.eventFromPayload(pullRequestJson.jsonString()))
                .then(mocks -> {
                    GHPullRequest mockedPR = mocks.pullRequest(pullRequestJson.id());
                    Mockito.verify(mockedPR).comment("/cc @Tadpole");
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    WildflyGitHubBotTesting.verifyFormatSuccess(repo, pullRequestJson);
                })
                .run();
    }
}
