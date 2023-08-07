package io.xstefank.wildfly.bot;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;
import io.xstefank.wildfly.bot.format.TitleCheck;
import io.xstefank.wildfly.bot.model.RegexDefinition;
import io.xstefank.wildfly.bot.utils.GitHubJson;
import io.xstefank.wildfly.bot.utils.MockedContext;
import io.xstefank.wildfly.bot.utils.Util;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GHRepository;

import java.io.IOException;

import static io.quarkiverse.githubapp.testing.GitHubAppTesting.given;
import static io.xstefank.wildfly.bot.model.RuntimeConstants.DEFAULT_TITLE_MESSAGE;
import static io.xstefank.wildfly.bot.model.RuntimeConstants.PROJECT_PATTERN_REGEX;
import static io.xstefank.wildfly.bot.utils.TestConstants.INVALID_TITLE;
import static io.xstefank.wildfly.bot.utils.TestConstants.TEST_REPO;
import static io.xstefank.wildfly.bot.utils.TestConstants.VALID_PR_TEMPLATE_JSON;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for the Wildfly -> Format -> Title checks.
 */
@QuarkusTest
@GitHubAppTest
public class PRTitleCheckTest {

    private static String wildflyConfigFile;
    private static GitHubJson gitHubJson;
    private MockedContext mockedContext;

    @Test
    void configFileNullTest() {
        RegexDefinition regexDefinition = new RegexDefinition();

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
            () -> new TitleCheck(regexDefinition));
        Assertions.assertEquals("Input argument cannot be null", thrown.getMessage());
    }

    @Test
    void incorrectTitleCheckFailTest() throws IOException {
        wildflyConfigFile = """
            wildfly:
              format:
                title:
                  enabled: true
            """;
        gitHubJson = GitHubJson.builder(VALID_PR_TEMPLATE_JSON)
                .title(INVALID_TITLE)
                .build();

        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, gitHubJson))
            .when().payloadFromString(gitHubJson.jsonString())
            .event(GHEvent.PULL_REQUEST)
            .then().github(mocks -> {
                GHRepository repo = mocks.repository(TEST_REPO);
                Util.verifyFormatFailure(repo, gitHubJson, "title");
                Util.verifyFailedFormatComment(mocks, gitHubJson, "- " + String.format(DEFAULT_TITLE_MESSAGE,
                    PROJECT_PATTERN_REGEX.formatted("WFLY")));
            });
    }

    @Test
    void incorrectTitleCheckFailFormatConfigMissingTest() throws IOException {
        gitHubJson = GitHubJson.builder(VALID_PR_TEMPLATE_JSON)
                .title(INVALID_TITLE)
                .build();
        wildflyConfigFile = """
                wildfly:
                """;

        given()
                .github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, gitHubJson))
                .when().payloadFromString(gitHubJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository(TEST_REPO);
                    Util.verifyFormatFailure(repo, gitHubJson, "title");
                    Util.verifyFailedFormatComment(mocks, gitHubJson, "- " + String.format(DEFAULT_TITLE_MESSAGE,
                            PROJECT_PATTERN_REGEX.formatted("WFLY", "WFLY")));
                });
    }

    @Test
    void correctTitleCheckFailFormatConfigMissingTest() throws IOException {
        gitHubJson = GitHubJson.builder(VALID_PR_TEMPLATE_JSON)
                .build();
        wildflyConfigFile = """
                wildfly:
                """;

        given()
                .github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, gitHubJson))
                .when().payloadFromString(gitHubJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository(TEST_REPO);
                    Util.verifyFormatSuccess(repo, gitHubJson);
                });
    }

    @Test
    void incorrectTitleCheckFailFormatConfigNullTest() throws IOException {
        gitHubJson = GitHubJson.builder(VALID_PR_TEMPLATE_JSON)
                .title(INVALID_TITLE)
                .build();
        wildflyConfigFile = """
                wildfly:
                  format:
                """;

        given()
                .github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, gitHubJson))
                .when().payloadFromString(gitHubJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository(TEST_REPO);
                    Util.verifyFormatFailure(repo, gitHubJson, "title");
                    Util.verifyFailedFormatComment(mocks, gitHubJson, "- " + String.format(DEFAULT_TITLE_MESSAGE,
                            PROJECT_PATTERN_REGEX.formatted("WFLY", "WFLY")));
                });
    }

    @Test
    void correctTitleCheckFailFormatConfigNullTest() throws IOException {
        gitHubJson = GitHubJson.builder(VALID_PR_TEMPLATE_JSON)
                .build();
        wildflyConfigFile = """
                wildfly:
                  format:
                """;

        given()
                .github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, gitHubJson))
                .when().payloadFromString(gitHubJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository(TEST_REPO);
                    Util.verifyFormatSuccess(repo, gitHubJson);
                });
    }

    @Test
    void incorrectTitleCheckFailFormatTitleConfigNullTest() throws IOException {
        gitHubJson = GitHubJson.builder(VALID_PR_TEMPLATE_JSON)
                .title(INVALID_TITLE)
                .build();
        wildflyConfigFile = """
                wildfly:
                  format:
                    title:
                """;

        given()
                .github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, gitHubJson))
                .when().payloadFromString(gitHubJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository(TEST_REPO);
                    Util.verifyFormatFailure(repo, gitHubJson, "title");
                    Util.verifyFailedFormatComment(mocks, gitHubJson, "- " + String.format(DEFAULT_TITLE_MESSAGE,
                            PROJECT_PATTERN_REGEX.formatted("WFLY", "WFLY")));
                });
    }

    @Test
    void correctTitleCheckFailFormatTitleConfigNullTest() throws IOException {
        gitHubJson = GitHubJson.builder(VALID_PR_TEMPLATE_JSON)
                .build();
        wildflyConfigFile = """
                wildfly:
                  format:
                    title:
                """;

        given()
                .github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, gitHubJson))
                .when().payloadFromString(gitHubJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository(TEST_REPO);
                    Util.verifyFormatSuccess(repo, gitHubJson);
                });
    }

    @Test
    void correctTitleCheckSuccessTest() throws IOException {
        wildflyConfigFile = """
            wildfly:
              format:
                title:
                  enabled: true
            """;
        gitHubJson = GitHubJson.builder(VALID_PR_TEMPLATE_JSON).build();

        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, gitHubJson))
            .when().payloadFromString(gitHubJson.jsonString())
            .event(GHEvent.PULL_REQUEST)
            .then().github(mocks -> {
                GHRepository repo = mocks.repository(TEST_REPO);
                Util.verifyFormatSuccess(repo, gitHubJson);
            });
    }

    @Test
    void testDisabledTitleCheck() throws IOException {
        wildflyConfigFile = """
            wildfly:
              format:
                title:
                  enabled: false
            """;
        gitHubJson = GitHubJson.builder(VALID_PR_TEMPLATE_JSON)
                .title(INVALID_TITLE)
                .build();

        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, gitHubJson))
                .when().payloadFromString(gitHubJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository(TEST_REPO);
                    Util.verifyFormatSuccess(repo, gitHubJson);
                });
    }

    @Test
    public void testOverridingTitleMessage() throws IOException {
        wildflyConfigFile = """
                wildfly:
                  format:
                    title:
                      message: "Custom title message"
                """;
        gitHubJson = GitHubJson.builder(VALID_PR_TEMPLATE_JSON)
                .title(INVALID_TITLE)
                .build();
        mockedContext = MockedContext.builder(gitHubJson.id())
                .commit("WFLY-00000 commit");

        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, gitHubJson, mockedContext))
                .when().payloadFromString(gitHubJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository(TEST_REPO);
                    Util.verifyFormatFailure(repo, gitHubJson, "title");
                    Util.verifyFailedFormatComment(mocks, gitHubJson, "- Custom title message");
                });
    }
}
