package org.wildfly.bot.webhooks;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GHRepository;
import org.mockito.Mockito;
import org.wildfly.bot.format.DescriptionCheck;
import org.wildfly.bot.model.Description;
import org.wildfly.bot.utils.TestConstants;
import org.wildfly.bot.utils.WildflyGitHubBotTesting;
import org.wildfly.bot.utils.model.SsePullRequestPayload;
import org.wildfly.bot.utils.testing.PullRequestJson;
import org.wildfly.bot.utils.testing.internal.TestModel;
import org.wildfly.bot.utils.testing.model.PullRequestGitHubEventPayload;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;

/**
 * Tests for the Wildfly -> Format -> Description checks.
 */
@QuarkusTest
@GitHubAppTest
public class PRDescriptionCheckTest {

    private static String wildflyConfigFile;
    private static PullRequestJson pullRequestJson;

    @BeforeAll
    static void setPullRequestJson() {
        TestModel.setAllCallables(
                () -> SsePullRequestPayload.builder(TestConstants.VALID_PR_TEMPLATE_JSON),
                PullRequestGitHubEventPayload::new);
    }

    @Test
    void configFileNullTest() {
        Description description = new Description();

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> new DescriptionCheck(description));
        assertEquals("Input argument cannot be null", thrown.getMessage());
    }

    @Test
    void testDescriptionCheckFail() throws Throwable {
        wildflyConfigFile = """
                wildfly:
                  format:
                    description:
                      message: Default fail message
                      regexes:
                        - pattern: "https://issues.redhat.com/browse/WFLY-\\\\d+"
                          message: "The PR description must contain a link to the JIRA issue"
                """;

        pullRequestJson = TestModel.setPullRequestJsonBuilderBuild(
                pullRequestJsonBuilder -> pullRequestJsonBuilder.description(TestConstants.INVALID_DESCRIPTION));

        TestModel.given(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson))
                .sseEventOptions(eventSenderOptions -> eventSenderOptions.payloadFromString(pullRequestJson.jsonString())
                        .event(GHEvent.PULL_REQUEST))
                .pollingEventOptions(eventSenderOptions -> eventSenderOptions.eventFromPayload(pullRequestJson.jsonString()))
                .then(mocks -> {
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    WildflyGitHubBotTesting.verifyFormatFailure(repo, pullRequestJson, "description");
                    WildflyGitHubBotTesting.verifyFailedFormatComment(mocks, pullRequestJson,
                            "- The PR description must contain a link to the JIRA issue");
                })
                .run();
    }

    @Test
    void testNoMessageInConfigFile() throws Throwable {
        wildflyConfigFile = """
                wildfly:
                  format:
                    description:
                      regexes:
                        - pattern: "https://issues.redhat.com/browse/WFLY-\\\\d+"
                """;

        pullRequestJson = TestModel.setPullRequestJsonBuilderBuild(
                pullRequestJsonBuilder -> pullRequestJsonBuilder.description(TestConstants.INVALID_DESCRIPTION));

        TestModel.given(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson))
                .sseEventOptions(eventSenderOptions -> eventSenderOptions.payloadFromString(pullRequestJson.jsonString())
                        .event(GHEvent.PULL_REQUEST))
                .pollingEventOptions(eventSenderOptions -> eventSenderOptions.eventFromPayload(pullRequestJson.jsonString()))
                .then(mocks -> {
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    WildflyGitHubBotTesting.verifyFormatFailure(repo, pullRequestJson, "description");
                    WildflyGitHubBotTesting.verifyFailedFormatComment(mocks, pullRequestJson,
                            "- Invalid description content");
                })
                .run();
    }

    @Test
    void testDescriptionCheckSuccess() throws Throwable {
        wildflyConfigFile = """
                wildfly:
                  format:
                    description:
                      message: Default fail message
                      regexes:
                        - pattern: "https://issues.redhat.com/browse/WFLY-\\\\d+"
                          message: "The PR description must contain a link to the JIRA issue"
                """;

        pullRequestJson = TestModel.getPullRequestJson();

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
    void testDescriptionCheckSuccessDescriptionConfigNull() throws Throwable {
        wildflyConfigFile = """
                wildfly:
                  format:
                    description:
                """;

        pullRequestJson = TestModel.getPullRequestJson();

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
    void testMultipleLineDescription() throws Throwable {
        wildflyConfigFile = """
                wildfly:
                  format:
                    description:
                      message: Default fail message
                      regexes:
                        - pattern: "https://issues.redhat.com/browse/WFLY-\\\\d+"
                          message: "The PR description must contain a link to the JIRA issue"
                """;

        pullRequestJson = TestModel.setPullRequestJsonBuilderBuild(pullRequestJsonBuilder -> pullRequestJsonBuilder
                .description("""
                        First line of description
                        Additional line of description - JIRA: https://issues.redhat.com/browse/WFLY-666
                        Another line with no JIRA link
                        """));

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
    void testMultipleRegexesFirstPatternHit() throws Throwable {
        wildflyConfigFile = """
                wildfly:
                  format:
                    description:
                      message: Default fail message
                      regexes:
                        - pattern: "https://issues.redhat.com/browse/WFLY-\\\\d+"
                          message: "The PR description must contain a link to the JIRA issue"
                        - pattern: "JIRA"
                """;

        pullRequestJson = TestModel
                .setPullRequestJsonBuilderBuild(pullRequestJsonBuilder -> pullRequestJsonBuilder.description("JIRA"));

        TestModel.given(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson))
                .sseEventOptions(eventSenderOptions -> eventSenderOptions.payloadFromString(pullRequestJson.jsonString())
                        .event(GHEvent.PULL_REQUEST))
                .pollingEventOptions(eventSenderOptions -> eventSenderOptions.eventFromPayload(pullRequestJson.jsonString()))
                .then(mocks -> {
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    WildflyGitHubBotTesting.verifyFormatFailure(repo, pullRequestJson, "description");
                    WildflyGitHubBotTesting.verifyFailedFormatComment(mocks, pullRequestJson,
                            "- The PR description must contain a link to the JIRA issue");
                })
                .run();
    }

    @Test
    void testMultipleRegexesSecondPatternHit() throws Throwable {
        wildflyConfigFile = """
                wildfly:
                  format:
                    description:
                      message: Lorem ipsum dolor sit amet
                      regexes:
                        - pattern: "https://issues.redhat.com/browse/WFLY-\\\\d+"
                          message: "The PR description must contain a link to the JIRA issue"
                        - pattern: "JIRA"
                """;

        pullRequestJson = TestModel.setPullRequestJsonBuilderBuild(
                pullRequestJsonBuilder -> pullRequestJsonBuilder.description("https://issues.redhat.com/browse/WFLY-123"));

        TestModel.given(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson))
                .sseEventOptions(eventSenderOptions -> eventSenderOptions.payloadFromString(pullRequestJson.jsonString())
                        .event(GHEvent.PULL_REQUEST))
                .pollingEventOptions(eventSenderOptions -> eventSenderOptions.eventFromPayload(pullRequestJson.jsonString()))
                .then(mocks -> {
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    WildflyGitHubBotTesting.verifyFormatFailure(repo, pullRequestJson, "description");
                    WildflyGitHubBotTesting.verifyFailedFormatComment(mocks, pullRequestJson,
                            "- Lorem ipsum dolor sit amet");
                })
                .run();
    }

    @Test
    void testMultipleRegexesAllPatternsHit() throws Throwable {
        wildflyConfigFile = """
                wildfly:
                  format:
                    description:
                      message: Default fail message
                      regexes:
                        - pattern: "https://issues.redhat.com/browse/WFLY-\\\\d+"
                          message: "The PR description must contain a link to the JIRA issue"
                        - pattern: "JIRA"
                """;

        pullRequestJson = TestModel.setPullRequestJsonBuilderBuild(
                pullRequestJsonBuilder -> pullRequestJsonBuilder.description(TestConstants.INVALID_DESCRIPTION));

        TestModel.given(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson))
                .sseEventOptions(eventSenderOptions -> eventSenderOptions.payloadFromString(pullRequestJson.jsonString())
                        .event(GHEvent.PULL_REQUEST))
                .pollingEventOptions(eventSenderOptions -> eventSenderOptions.eventFromPayload(pullRequestJson.jsonString()))
                .then(mocks -> {
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    WildflyGitHubBotTesting.verifyFormatFailure(repo, pullRequestJson, "description");
                    WildflyGitHubBotTesting.verifyFailedFormatComment(mocks, pullRequestJson,
                            "- The PR description must contain a link to the JIRA issue");
                })
                .run();
    }

    @Test
    void testMultipleRegexesNoPatternsHit() throws Throwable {
        wildflyConfigFile = """
                wildfly:
                  format:
                    description:
                      message: Default fail message
                      regexes:
                        - pattern: "https://issues.redhat.com/browse/WFLY-\\\\d+"
                          message: "The PR description must contain a link to the JIRA issue"
                        - pattern: "JIRA"
                """;

        pullRequestJson = TestModel.getPullRequestJson();

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
    public void testDisableGlobalFormatCheck() throws Throwable {
        wildflyConfigFile = """
                wildfly:
                  format:
                    enabled: false
                    description:
                      message: Default fail message
                      regexes:
                        - pattern: "https://issues.redhat.com/browse/WFLY-\\\\d+"
                          message: "The PR description must contain a link to the JIRA issue"
                        - pattern: "JIRA"
                """;

        pullRequestJson = TestModel.setPullRequestJsonBuilderBuild(
                pullRequestJsonBuilder -> pullRequestJsonBuilder.title(TestConstants.INVALID_TITLE));

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
