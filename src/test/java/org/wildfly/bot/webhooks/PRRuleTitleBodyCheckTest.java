package org.wildfly.bot.webhooks;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.wildfly.bot.utils.TestConstants;
import org.wildfly.bot.utils.WildflyGitHubBotTesting;
import org.wildfly.bot.utils.model.SsePullRequestPayload;
import org.wildfly.bot.utils.testing.PullRequestJson;
import org.wildfly.bot.utils.testing.internal.TestModel;
import org.wildfly.bot.utils.testing.model.PullRequestGitHubEventPayload;

/**
 * Tests for the Wildfly -> Rules -> TitleBody checks.
 */
@QuarkusTest
@GitHubAppTest
public class PRRuleTitleBodyCheckTest {

    private static String wildflyConfigFile;
    private static PullRequestJson pullRequestJson;

    @BeforeAll
    static void setPullRequestJson() {
        TestModel.setAllCallables(
                () -> SsePullRequestPayload.builder(TestConstants.VALID_PR_TEMPLATE_JSON),
                PullRequestGitHubEventPayload::new);
    }

    @Test
    void testTitleBodyCheckForTitle() throws Throwable {
        pullRequestJson = TestModel.getPullRequestJson();
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - id: "TitleBody"
                      title: "NonTitle"
                      body: "NonBody"
                      titleBody: "WFLY"
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
    void testTitleBodyCheckForBody() throws Throwable {
        pullRequestJson = TestModel.getPullRequestJson();
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - id: "TitleBody"
                      title: "NonTitle"
                      body: "NonBody"
                      titleBody: "issues.redhat.com"
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
    void testTitleBodyCheckForTitleBodyCaseInsensitive() throws Throwable {
        pullRequestJson = TestModel.getPullRequestJson();
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - id: "TitleBody"
                      title: "NonTitle"
                      body: "NonBody"
                      titleBody: "ISSUES.REDHAT.COM"
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
    void testFailedTitleBodyCheck() throws Throwable {
        pullRequestJson = TestModel.setPullRequestJsonBuilderBuild(pullRequestJsonBuilder -> pullRequestJsonBuilder
                .title(TestConstants.INVALID_TITLE)
                .description(TestConstants.INVALID_DESCRIPTION));
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - id: "TitleBody"
                      title: "NonTitle"
                      body: "NonBody"
                      titleBody: "NonValidTitleBody"
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
                    WildflyGitHubBotTesting.verifyFormatFailure(repo, pullRequestJson, "title");
                })
                .run();
    }

    @Test
    void testTitleBodyCheckTitleRegex() throws Throwable {
        pullRequestJson = TestModel.setPullRequestJsonBuilderBuild(
                pullRequestJsonBuilder -> pullRequestJsonBuilder.title("[WFLY-00000] Metrics PR"));
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - titleBody: "metrics|micrometer"
                      notify: [Tadpole]
                """;

        TestModel.given(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson))
                .sseEventOptions(eventSenderOptions -> eventSenderOptions.payloadFromString(pullRequestJson.jsonString())
                        .event(GHEvent.PULL_REQUEST))
                .pollingEventOptions(eventSenderOptions -> eventSenderOptions.eventFromPayload(pullRequestJson.jsonString()))
                .then(mocks -> {
                    GHPullRequest mockedPR = mocks.pullRequest(pullRequestJson.id());
                    Mockito.verify(mockedPR).comment("/cc @Tadpole");
                })
                .run();
    }

    @Test
    void testTitleBodyCheckBodyRegex() throws Throwable {
        pullRequestJson = TestModel.setPullRequestJsonBuilderBuild(
                pullRequestJsonBuilder -> pullRequestJsonBuilder.description("[WFLY-00000] Micrometer commit"));
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - titleBody: "metrics|micrometer"
                      notify: [Tadpole]
                """;

        TestModel.given(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson))
                .sseEventOptions(eventSenderOptions -> eventSenderOptions.payloadFromString(pullRequestJson.jsonString())
                        .event(GHEvent.PULL_REQUEST))
                .pollingEventOptions(eventSenderOptions -> eventSenderOptions.eventFromPayload(pullRequestJson.jsonString()))
                .then(mocks -> {
                    GHPullRequest mockedPR = mocks.pullRequest(pullRequestJson.id());
                    Mockito.verify(mockedPR).comment("/cc @Tadpole");
                })
                .run();
    }

    @Test
    void testTitleBodyCheckBodyRegexNoHit() throws Throwable {
        pullRequestJson = TestModel.getPullRequestJson();
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - titleBody: "not|valid"
                      notify: [Tadpole]
                """;

        TestModel.given(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson))
                .sseEventOptions(eventSenderOptions -> eventSenderOptions.payloadFromString(pullRequestJson.jsonString())
                        .event(GHEvent.PULL_REQUEST))
                .pollingEventOptions(eventSenderOptions -> eventSenderOptions.eventFromPayload(pullRequestJson.jsonString()))
                .then(mocks -> {
                    GHPullRequest mockedPR = mocks.pullRequest(pullRequestJson.id());
                    Mockito.verify(mockedPR, Mockito.never()).comment(ArgumentMatchers.anyString());
                })
                .run();
    }

    @Test
    void testTitleBodyCheckTitleSubstringNoHit() throws Throwable {
        pullRequestJson = TestModel
                .setPullRequestJsonBuilderBuild(pullRequestJsonBuilder -> pullRequestJsonBuilder.title("See the deep no hit"));
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - titleBody: "ee"
                      notify: [Tadpole]
                  format:
                    title:
                      enabled: false
                """;

        TestModel.given(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson))
                .sseEventOptions(eventSenderOptions -> eventSenderOptions.payloadFromString(pullRequestJson.jsonString())
                        .event(GHEvent.PULL_REQUEST))
                .pollingEventOptions(eventSenderOptions -> eventSenderOptions.eventFromPayload(pullRequestJson.jsonString()))
                .then(mocks -> {
                    GHPullRequest mockedPR = mocks.pullRequest(pullRequestJson.id());
                    Mockito.verify(mockedPR, Mockito.never()).comment(ArgumentMatchers.anyString());
                })
                .run();
    }

    @Test
    void testTitleBodyCheckTitleRegexSubstringNoHit() throws Throwable {
        pullRequestJson = TestModel.setPullRequestJsonBuilderBuild(
                pullRequestJsonBuilder -> pullRequestJsonBuilder.title("See the deep no hit nor originated"));
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - titleBody: "ee|or"
                      notify: [Tadpole]
                  format:
                    title:
                      enabled: false
                """;

        TestModel.given(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson))
                .sseEventOptions(eventSenderOptions -> eventSenderOptions.payloadFromString(pullRequestJson.jsonString())
                        .event(GHEvent.PULL_REQUEST))
                .pollingEventOptions(eventSenderOptions -> eventSenderOptions.eventFromPayload(pullRequestJson.jsonString()))
                .then(mocks -> {
                    GHPullRequest mockedPR = mocks.pullRequest(pullRequestJson.id());
                    Mockito.verify(mockedPR, Mockito.never()).comment(ArgumentMatchers.anyString());
                })
                .run();
    }

    @Test
    void testTitleBodyCheckForTitleWithPrefix() throws Throwable {
        pullRequestJson = TestModel.setPullRequestJsonBuilderBuild(
                pullRequestJsonBuilder -> pullRequestJsonBuilder.title("(30.x) WFLY-123 Test title"));
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - titleBody: "ee"
                """;

        TestModel.given(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson))
                .sseEventOptions(eventSenderOptions -> eventSenderOptions.payloadFromString(pullRequestJson.jsonString())
                        .event(GHEvent.PULL_REQUEST))
                .pollingEventOptions(eventSenderOptions -> eventSenderOptions.eventFromPayload(pullRequestJson.jsonString()))
                .then(mocks -> {
                    GHPullRequest mockedPR = mocks.pullRequest(pullRequestJson.id());
                    Mockito.verify(mockedPR, Mockito.never()).comment(Mockito.anyString());
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    WildflyGitHubBotTesting.verifyFormatSuccess(repo, pullRequestJson);
                })
                .run();
    }
}
