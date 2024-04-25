package org.wildfly.bot.webhooks;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GHRepository;
import org.mockito.Mockito;
import org.wildfly.bot.format.DescriptionCheck;
import org.wildfly.bot.model.Description;
import org.wildfly.bot.utils.TestConstants;
import org.wildfly.bot.utils.WildflyGitHubBotTesting;
import org.wildfly.bot.utils.model.SsePullRequestPayload;

import java.io.IOException;

import static io.quarkiverse.githubapp.testing.GitHubAppTesting.given;
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
    private static SsePullRequestPayload ssePullRequestPayload;

    @Test
    void configFileNullTest() {
        Description description = new Description();

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> new DescriptionCheck(description));
        assertEquals("Input argument cannot be null", thrown.getMessage());
    }

    @Test
    void testDescriptionCheckFail() throws IOException {
        wildflyConfigFile = """
                wildfly:
                  format:
                    description:
                      message: Default fail message
                      regexes:
                        - pattern: "https://issues.redhat.com/browse/WFLY-\\\\d+"
                          message: "The PR description must contain a link to the JIRA issue"
                """;
        ssePullRequestPayload = SsePullRequestPayload.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .description(TestConstants.INVALID_DESCRIPTION)
                .build();

        given().github(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, ssePullRequestPayload))
                .when().payloadFromString(ssePullRequestPayload.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    WildflyGitHubBotTesting.verifyFormatFailure(repo, ssePullRequestPayload, "description");
                    WildflyGitHubBotTesting.verifyFailedFormatComment(mocks, ssePullRequestPayload,
                            "- The PR description must contain a link to the JIRA issue");
                });
    }

    @Test
    void testNoMessageInConfigFile() throws IOException {
        wildflyConfigFile = """
                wildfly:
                  format:
                    description:
                      regexes:
                        - pattern: "https://issues.redhat.com/browse/WFLY-\\\\d+"
                """;
        ssePullRequestPayload = SsePullRequestPayload.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .description(TestConstants.INVALID_DESCRIPTION)
                .build();

        given().github(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, ssePullRequestPayload))
                .when().payloadFromString(ssePullRequestPayload.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    WildflyGitHubBotTesting.verifyFormatFailure(repo, ssePullRequestPayload, "description");
                    WildflyGitHubBotTesting.verifyFailedFormatComment(mocks, ssePullRequestPayload, "- Invalid description content");
                });
    }

    @Test
    void testDescriptionCheckSuccess() throws IOException {
        wildflyConfigFile = """
                wildfly:
                  format:
                    description:
                      message: Default fail message
                      regexes:
                        - pattern: "https://issues.redhat.com/browse/WFLY-\\\\d+"
                          message: "The PR description must contain a link to the JIRA issue"
                """;
        ssePullRequestPayload = SsePullRequestPayload.builder(TestConstants.VALID_PR_TEMPLATE_JSON).build();

        given().github(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, ssePullRequestPayload))
                .when().payloadFromString(ssePullRequestPayload.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    WildflyGitHubBotTesting.verifyFormatSuccess(repo, ssePullRequestPayload);
                });
    }

    @Test
    void testDescriptionCheckSuccessDescriptionConfigNull() throws IOException {
        wildflyConfigFile = """
                wildfly:
                  format:
                    description:
                """;
        ssePullRequestPayload = SsePullRequestPayload.builder(TestConstants.VALID_PR_TEMPLATE_JSON).build();

        given().github(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, ssePullRequestPayload))
                .when().payloadFromString(ssePullRequestPayload.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    WildflyGitHubBotTesting.verifyFormatSuccess(repo, ssePullRequestPayload);
                });
    }

    @Test
    void testMultipleLineDescription() throws IOException {
        wildflyConfigFile = """
                wildfly:
                  format:
                    description:
                      message: Default fail message
                      regexes:
                        - pattern: "https://issues.redhat.com/browse/WFLY-\\\\d+"
                          message: "The PR description must contain a link to the JIRA issue"
                """;
        ssePullRequestPayload = SsePullRequestPayload.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .description("""
                        First line of description
                        Additional line of description - JIRA: https://issues.redhat.com/browse/WFLY-666
                        Another line with no JIRA link
                        """)
                .build();

        given().github(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, ssePullRequestPayload))
                .when().payloadFromString(ssePullRequestPayload.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    WildflyGitHubBotTesting.verifyFormatSuccess(repo, ssePullRequestPayload);
                });
    }

    @Test
    void testMultipleRegexesFirstPatternHit() throws IOException {
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
        ssePullRequestPayload = SsePullRequestPayload.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .description("JIRA")
                .build();

        given().github(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, ssePullRequestPayload))
                .when().payloadFromString(ssePullRequestPayload.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    WildflyGitHubBotTesting.verifyFormatFailure(repo, ssePullRequestPayload, "description");
                    WildflyGitHubBotTesting.verifyFailedFormatComment(mocks, ssePullRequestPayload,
                            "- The PR description must contain a link to the JIRA issue");
                });
    }

    @Test
    void testMultipleRegexesSecondPatternHit() throws IOException {
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
        ssePullRequestPayload = SsePullRequestPayload.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .description("https://issues.redhat.com/browse/WFLY-123")
                .build();

        given().github(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, ssePullRequestPayload))
                .when().payloadFromString(ssePullRequestPayload.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    WildflyGitHubBotTesting.verifyFormatFailure(repo, ssePullRequestPayload, "description");
                    WildflyGitHubBotTesting.verifyFailedFormatComment(mocks, ssePullRequestPayload, "- Lorem ipsum dolor sit amet");
                });
    }

    @Test
    void testMultipleRegexesAllPatternsHit() throws IOException {
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
        ssePullRequestPayload = SsePullRequestPayload.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .description(TestConstants.INVALID_DESCRIPTION)
                .build();

        given().github(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, ssePullRequestPayload))
                .when().payloadFromString(ssePullRequestPayload.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    WildflyGitHubBotTesting.verifyFormatFailure(repo, ssePullRequestPayload, "description");
                    WildflyGitHubBotTesting.verifyFailedFormatComment(mocks, ssePullRequestPayload,
                            "- The PR description must contain a link to the JIRA issue");
                });
    }

    @Test
    void testMultipleRegexesNoPatternsHit() throws IOException {
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
        ssePullRequestPayload = SsePullRequestPayload.builder(TestConstants.VALID_PR_TEMPLATE_JSON).build();

        given().github(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, ssePullRequestPayload))
                .when().payloadFromString(ssePullRequestPayload.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    WildflyGitHubBotTesting.verifyFormatSuccess(repo, ssePullRequestPayload);
                });
    }

    @Test
    public void testDisableGlobalFormatCheck() throws IOException {
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
        ssePullRequestPayload = SsePullRequestPayload.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .title(TestConstants.INVALID_TITLE)
                .build();
        given().github(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, ssePullRequestPayload))
                .when().payloadFromString(ssePullRequestPayload.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    Mockito.verify(repo, never()).createCommitStatus(anyString(), any(), anyString(), anyString());
                    Mockito.verify(repo, never()).createCommitStatus(anyString(), any(), anyString(), anyString(), anyString());
                });
    }
}
