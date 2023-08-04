package io.xstefank.wildfly.bot;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;
import io.xstefank.wildfly.bot.format.DescriptionCheck;
import io.xstefank.wildfly.bot.model.Description;
import io.xstefank.wildfly.bot.utils.GitHubJson;
import io.xstefank.wildfly.bot.utils.Util;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GHRepository;

import java.io.IOException;

import static io.quarkiverse.githubapp.testing.GitHubAppTesting.given;
import static io.xstefank.wildfly.bot.utils.TestConstants.INVALID_DESCRIPTION;
import static io.xstefank.wildfly.bot.utils.TestConstants.VALID_PR_TEMPLATE_JSON;
import static io.xstefank.wildfly.bot.utils.TestConstants.TEST_REPO;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for the Wildfly -> Format -> Description checks.
 */
@QuarkusTest
@GitHubAppTest
public class PRDescriptionCheckTest {

    private static String wildflyConfigFile;
    private static GitHubJson gitHubJson;

    @Test
    void configFileNullTest() {
        Description description = new Description();

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
            () -> new DescriptionCheck(description));
        Assertions.assertEquals("Input argument cannot be null", thrown.getMessage());
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
        gitHubJson = GitHubJson.builder(VALID_PR_TEMPLATE_JSON)
                .description(INVALID_DESCRIPTION)
                .build();

        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, gitHubJson, null))
            .when().payloadFromString(gitHubJson.jsonString())
            .event(GHEvent.PULL_REQUEST)
            .then().github(mocks -> {
                GHRepository repo = mocks.repository(TEST_REPO);
                Util.verifyFormatFailure(repo, gitHubJson, "description");
                Util.verifyFailedFormatComment(mocks, gitHubJson, "- The PR description must contain a link to the JIRA issue");
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
        gitHubJson = GitHubJson.builder(VALID_PR_TEMPLATE_JSON)
                .description(INVALID_DESCRIPTION)
                .build();

        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, gitHubJson, null))
                .when().payloadFromString(gitHubJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository(TEST_REPO);
                    Util.verifyFormatFailure(repo, gitHubJson, "description");
                    Util.verifyFailedFormatComment(mocks, gitHubJson, "- Invalid description content");
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
        gitHubJson = GitHubJson.builder(VALID_PR_TEMPLATE_JSON).build();

        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, gitHubJson, null))
            .when().payloadFromString(gitHubJson.jsonString())
            .event(GHEvent.PULL_REQUEST)
            .then().github(mocks -> {
                GHRepository repo = mocks.repository(TEST_REPO);
                Util.verifyFormatSuccess(repo, gitHubJson);
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
        gitHubJson = GitHubJson.builder(VALID_PR_TEMPLATE_JSON)
                .description("""
                        First line of description
                        Additional line of description - JIRA: https://issues.redhat.com/browse/WFLY-666
                        Another line with no JIRA link
                        """)
                .build();

        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, gitHubJson, null))
                .when().payloadFromString(gitHubJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository(TEST_REPO);
                    Util.verifyFormatSuccess(repo, gitHubJson);
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
        gitHubJson = GitHubJson.builder(VALID_PR_TEMPLATE_JSON)
                .description("JIRA")
                .build();

        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, gitHubJson, null))
                .when().payloadFromString(gitHubJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository(TEST_REPO);
                    Util.verifyFormatFailure(repo, gitHubJson, "description");
                    Util.verifyFailedFormatComment(mocks, gitHubJson, "- The PR description must contain a link to the JIRA issue");
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
        gitHubJson = GitHubJson.builder(VALID_PR_TEMPLATE_JSON)
                .description("https://issues.redhat.com/browse/WFLY-123")
                .build();

        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, gitHubJson, null))
                .when().payloadFromString(gitHubJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository(TEST_REPO);
                    Util.verifyFormatFailure(repo, gitHubJson, "description");
                    Util.verifyFailedFormatComment(mocks, gitHubJson, "- Lorem ipsum dolor sit amet");
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
        gitHubJson = GitHubJson.builder(VALID_PR_TEMPLATE_JSON)
                .description(INVALID_DESCRIPTION)
                .build();

        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, gitHubJson, null))
                .when().payloadFromString(gitHubJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository(TEST_REPO);
                    Util.verifyFormatFailure(repo, gitHubJson, "description");
                    Util.verifyFailedFormatComment(mocks, gitHubJson, "- The PR description must contain a link to the JIRA issue");
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
        gitHubJson = GitHubJson.builder(VALID_PR_TEMPLATE_JSON).build();

        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, gitHubJson, null))
                .when().payloadFromString(gitHubJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository(TEST_REPO);
                    Util.verifyFormatSuccess(repo, gitHubJson);
                });
    }
}
