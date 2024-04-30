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
 * Tests for the Wildfly -> Rules -> Notify function.
 */
@QuarkusTest
@GitHubAppTest
public class PRRuleNotifyTest {

    private static String wildflyConfigFile;
    private static PullRequestJson pullRequestJson;

    @BeforeAll
    static void setUpGitHubJson() throws Exception {
        TestModel.setAllCallables(
                () -> SsePullRequestPayload.builder(TestConstants.VALID_PR_TEMPLATE_JSON),
                PullRequestGitHubEventPayload::new);

        pullRequestJson = TestModel.getPullRequestJson();
    }

    @Test
    void testMentionsCCComment() throws Throwable {
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - id: "Title"
                      title: "Title"
                      notify: [Tadpole,Duke]
                    - id: "Description"
                      title: "Description"
                      notify: [Butterfly,Doggo]
                """;

        TestModel.given(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson))
                .sseEventOptions(eventSenderOptions -> eventSenderOptions.payloadFromString(pullRequestJson.jsonString())
                        .event(GHEvent.PULL_REQUEST))
                .pollingEventOptions(eventSenderOptions -> eventSenderOptions.eventFromPayload(pullRequestJson.jsonString()))
                .then(mocks -> {
                    GHPullRequest mockedPR = mocks.pullRequest(pullRequestJson.id());
                    Mockito.verify(mockedPR).comment("/cc @Tadpole, @Duke");
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    WildflyGitHubBotTesting.verifyFormatSuccess(repo, pullRequestJson);
                })
                .run();
    }

    @Test
    void testMentionsCCCommentSeveralHits() throws Throwable {
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - id: "WFLY"
                      title: "WFLY"
                      notify: [Tadpole,Duke]
                    - id: "Title"
                      title: "Title"
                      notify: [Butterfly,Doggo]
                """;

        TestModel.given(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson))
                .sseEventOptions(eventSenderOptions -> eventSenderOptions.payloadFromString(pullRequestJson.jsonString())
                        .event(GHEvent.PULL_REQUEST))
                .pollingEventOptions(eventSenderOptions -> eventSenderOptions.eventFromPayload(pullRequestJson.jsonString()))
                .then(mocks -> {
                    GHPullRequest mockedPR = mocks.pullRequest(pullRequestJson.id());
                    Mockito.verify(mockedPR).comment("/cc @Butterfly, @Doggo, @Tadpole, @Duke");
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    WildflyGitHubBotTesting.verifyFormatSuccess(repo, pullRequestJson);
                })
                .run();
    }

    @Test
    void testMentionsCCCommentForDuplicateMentions() throws Throwable {
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - id: "Title"
                      title: "Title"
                      notify: [Tadpole,Duke]
                    - id: "Description"
                      body: "issues.redhat.com"
                      notify: [Tadpole,Doggo]
                """;

        TestModel.given(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson))
                .sseEventOptions(eventSenderOptions -> eventSenderOptions.payloadFromString(pullRequestJson.jsonString())
                        .event(GHEvent.PULL_REQUEST))
                .pollingEventOptions(eventSenderOptions -> eventSenderOptions.eventFromPayload(pullRequestJson.jsonString()))
                .then(mocks -> {
                    GHPullRequest mockedPR = mocks.pullRequest(pullRequestJson.id());
                    Mockito.verify(mockedPR).comment("/cc @Doggo, @Tadpole, @Duke");
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    WildflyGitHubBotTesting.verifyFormatSuccess(repo, pullRequestJson);
                })
                .run();
    }
}
