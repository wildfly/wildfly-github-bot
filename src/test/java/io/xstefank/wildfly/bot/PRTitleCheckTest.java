package io.xstefank.wildfly.bot;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;
import io.xstefank.wildfly.bot.format.TitleCheck;
import io.xstefank.wildfly.bot.model.RegexDefinition;
import io.xstefank.wildfly.bot.utils.Action;
import io.xstefank.wildfly.bot.utils.MockedContext;
import io.xstefank.wildfly.bot.utils.PullRequestJson;
import io.xstefank.wildfly.bot.utils.Util;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GHRepository;
import org.mockito.Mockito;

import java.io.IOException;

import static io.quarkiverse.githubapp.testing.GitHubAppTesting.given;
import static io.xstefank.wildfly.bot.model.RuntimeConstants.DEFAULT_TITLE_MESSAGE;
import static io.xstefank.wildfly.bot.model.RuntimeConstants.PROJECT_PATTERN_REGEX;
import static io.xstefank.wildfly.bot.utils.TestConstants.INVALID_TITLE;
import static io.xstefank.wildfly.bot.utils.TestConstants.TEST_REPO;
import static io.xstefank.wildfly.bot.utils.TestConstants.VALID_PR_TEMPLATE_JSON;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;

/**
 * Tests for the Wildfly -> Format -> Title checks.
 */
@QuarkusTest
@GitHubAppTest
public class PRTitleCheckTest {

    private static String wildflyConfigFile;
    private static PullRequestJson pullRequestJson;
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
        pullRequestJson = PullRequestJson.builder(VALID_PR_TEMPLATE_JSON)
                .title(INVALID_TITLE)
                .build();

        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, pullRequestJson))
                .when().payloadFromString(pullRequestJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository(TEST_REPO);
                    Util.verifyFormatFailure(repo, pullRequestJson, "title");
                    Util.verifyFailedFormatComment(mocks, pullRequestJson, "- " + String.format(DEFAULT_TITLE_MESSAGE,
                            PROJECT_PATTERN_REGEX.formatted("WFLY")));
                });
    }

    @Test
    void incorrectTitleCheckFailFormatConfigMissingTest() throws IOException {
        pullRequestJson = PullRequestJson.builder(VALID_PR_TEMPLATE_JSON)
                .title(INVALID_TITLE)
                .build();
        wildflyConfigFile = """
                wildfly:
                """;

        given()
                .github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, pullRequestJson))
                .when().payloadFromString(pullRequestJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository(TEST_REPO);
                    Util.verifyFormatFailure(repo, pullRequestJson, "title");
                    Util.verifyFailedFormatComment(mocks, pullRequestJson, "- " + String.format(DEFAULT_TITLE_MESSAGE,
                            PROJECT_PATTERN_REGEX.formatted("WFLY", "WFLY")));
                });
    }

    @Test
    void correctTitleCheckFailFormatConfigMissingTest() throws IOException {
        pullRequestJson = PullRequestJson.builder(VALID_PR_TEMPLATE_JSON)
                .build();
        wildflyConfigFile = """
                wildfly:
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
    void incorrectTitleCheckFailFormatConfigNullTest() throws IOException {
        pullRequestJson = PullRequestJson.builder(VALID_PR_TEMPLATE_JSON)
                .title(INVALID_TITLE)
                .build();
        wildflyConfigFile = """
                wildfly:
                  format:
                """;

        given()
                .github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, pullRequestJson))
                .when().payloadFromString(pullRequestJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository(TEST_REPO);
                    Util.verifyFormatFailure(repo, pullRequestJson, "title");
                    Util.verifyFailedFormatComment(mocks, pullRequestJson, "- " + String.format(DEFAULT_TITLE_MESSAGE,
                            PROJECT_PATTERN_REGEX.formatted("WFLY", "WFLY")));
                });
    }

    @Test
    void correctTitleCheckFailFormatConfigNullTest() throws IOException {
        pullRequestJson = PullRequestJson.builder(VALID_PR_TEMPLATE_JSON)
                .build();
        wildflyConfigFile = """
                wildfly:
                  format:
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
    void incorrectTitleCheckFailFormatTitleConfigNullTest() throws IOException {
        pullRequestJson = PullRequestJson.builder(VALID_PR_TEMPLATE_JSON)
                .title(INVALID_TITLE)
                .build();
        wildflyConfigFile = """
                wildfly:
                  format:
                    title:
                """;

        given()
                .github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, pullRequestJson))
                .when().payloadFromString(pullRequestJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository(TEST_REPO);
                    Util.verifyFormatFailure(repo, pullRequestJson, "title");
                    Util.verifyFailedFormatComment(mocks, pullRequestJson, "- " + String.format(DEFAULT_TITLE_MESSAGE,
                            PROJECT_PATTERN_REGEX.formatted("WFLY", "WFLY")));
                });
    }

    @Test
    void correctTitleCheckFailFormatTitleConfigNullTest() throws IOException {
        pullRequestJson = PullRequestJson.builder(VALID_PR_TEMPLATE_JSON)
                .build();
        wildflyConfigFile = """
                wildfly:
                  format:
                    title:
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
    void correctTitleCheckSuccessMultipleJirasWithTextTest() throws IOException {
        wildflyConfigFile = """
                wildfly:
                  format:
                    commit:
                      enabled: false
                """;
        pullRequestJson = PullRequestJson.builder(VALID_PR_TEMPLATE_JSON)
                .action(Action.EDITED)
                .title("WFLY-00001 jira,WFLY-00002 title")
                .build();

        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, pullRequestJson))
                .when().payloadFromString(pullRequestJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository(TEST_REPO);
                    Util.verifyFormatSuccess(repo, pullRequestJson);
                });
    }

    @Test
    void correctTitleCheckFailMultipleJirasBracketsMissingBracketTest() throws IOException {
        wildflyConfigFile = """
                wildfly:
                  format:
                    commit:
                      enabled: false
                """;
        pullRequestJson = PullRequestJson.builder(VALID_PR_TEMPLATE_JSON)
                .action(Action.EDITED)
                .title("[WFLY-00001,WFLY-00002 title")
                .build();

        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, pullRequestJson))
                .when().payloadFromString(pullRequestJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository(TEST_REPO);
                    Util.verifyFormatSuccess(repo, pullRequestJson);
                });
    }

    @Test
    void correctTitleCheckSuccessMultipleJirasBracketsIncorrectIssuePlacementTest() throws IOException {
        wildflyConfigFile = """
                wildfly:
                  format:
                    commit:
                      enabled: false
                """;
        pullRequestJson = PullRequestJson.builder(VALID_PR_TEMPLATE_JSON)
                .action(Action.EDITED)
                .title("[WFLY-00001 jira,WFLY-00002] title")
                .build();

        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, pullRequestJson))
                .when().payloadFromString(pullRequestJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository(TEST_REPO);
                    Util.verifyFormatSuccess(repo, pullRequestJson);
                });
    }

    @Test
    void correctTitleCheckSuccessMultipleJirasBracketsIncorrectEndingBracketPlacementTest() throws IOException {
        wildflyConfigFile = """
                wildfly:
                  format:
                    commit:
                      enabled: false
                """;
        pullRequestJson = PullRequestJson.builder(VALID_PR_TEMPLATE_JSON)
                .action(Action.EDITED)
                .title("[WFLY-00001, WFLY-00002 jira] title")
                .build();

        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, pullRequestJson))
                .when().payloadFromString(pullRequestJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository(TEST_REPO);
                    Util.verifyFormatSuccess(repo, pullRequestJson);
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
        pullRequestJson = PullRequestJson.builder(VALID_PR_TEMPLATE_JSON).build();

        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, pullRequestJson))
                .when().payloadFromString(pullRequestJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository(TEST_REPO);
                    Util.verifyFormatSuccess(repo, pullRequestJson);
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
        pullRequestJson = PullRequestJson.builder(VALID_PR_TEMPLATE_JSON)
                .title(INVALID_TITLE)
                .build();

        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, pullRequestJson))
                .when().payloadFromString(pullRequestJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository(TEST_REPO);
                    Util.verifyFormatSuccess(repo, pullRequestJson);
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
        pullRequestJson = PullRequestJson.builder(VALID_PR_TEMPLATE_JSON)
                .title(INVALID_TITLE)
                .build();
        mockedContext = MockedContext.builder(pullRequestJson.id())
                .commit("WFLY-00000 commit");

        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, pullRequestJson, mockedContext))
                .when().payloadFromString(pullRequestJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository(TEST_REPO);
                    Util.verifyFormatFailure(repo, pullRequestJson, "title");
                    Util.verifyFailedFormatComment(mocks, pullRequestJson, "- Custom title message");
                });
    }

    @Test
    public void testDisableGlobalFormatCheck() throws IOException {
        wildflyConfigFile = """
                wildfly:
                  format:
                    enabled: false
                """;
        pullRequestJson = PullRequestJson.builder(VALID_PR_TEMPLATE_JSON)
                .title(INVALID_TITLE)
                .build();
        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, pullRequestJson))
                .when().payloadFromString(pullRequestJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository(TEST_REPO);
                    Mockito.verify(repo, never()).createCommitStatus(anyString(), any(), anyString(), anyString());
                    Mockito.verify(repo, never()).createCommitStatus(anyString(), any(), anyString(), anyString(), anyString());
                });
    }

    @Test
    void correctTitleCheckSuccessSingleJiraBracketsTest() throws IOException {
        wildflyConfigFile = """
                wildfly:
                  format:
                    commit:
                      enabled: false
                """;
        pullRequestJson = PullRequestJson.builder(VALID_PR_TEMPLATE_JSON)
                .action(Action.EDITED)
                .title("[WFLY-00001] title")
                .build();

        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, pullRequestJson))
                .when().payloadFromString(pullRequestJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository(TEST_REPO);
                    Util.verifyFormatSuccess(repo, pullRequestJson);
                });
    }

    @Test
    void correctTitleCheckSuccessMultipleJirasTest() throws IOException {
        wildflyConfigFile = """
                wildfly:
                  format:
                    commit:
                      enabled: false
                """;
        pullRequestJson = PullRequestJson.builder(VALID_PR_TEMPLATE_JSON)
                .action(Action.EDITED)
                .title("WFLY-00001,WFLY-00002 title")
                .build();

        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, pullRequestJson))
                .when().payloadFromString(pullRequestJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository(TEST_REPO);
                    Util.verifyFormatSuccess(repo, pullRequestJson);
                });
    }

    @Test
    void correctTitleCheckSuccessMultipleJirasWithSpacesTest() throws IOException {
        wildflyConfigFile = """
                wildfly:
                  format:
                    commit:
                      enabled: false
                """;
        pullRequestJson = PullRequestJson.builder(VALID_PR_TEMPLATE_JSON)
                .action(Action.EDITED)
                .title("WFLY-00001, WFLY-00002 title")
                .build();

        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, pullRequestJson))
                .when().payloadFromString(pullRequestJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository(TEST_REPO);
                    Util.verifyFormatSuccess(repo, pullRequestJson);
                });
    }

    @Test
    void correctTitleCheckSuccessMultipleJirasBracketsTest() throws IOException {
        wildflyConfigFile = """
                wildfly:
                  format:
                    commit:
                      enabled: false
                """;
        pullRequestJson = PullRequestJson.builder(VALID_PR_TEMPLATE_JSON)
                .action(Action.EDITED)
                .title("[WFLY-00001,WFLY-00002] title")
                .build();

        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, pullRequestJson))
                .when().payloadFromString(pullRequestJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository(TEST_REPO);
                    Util.verifyFormatSuccess(repo, pullRequestJson);
                });
    }

    @Test
    void correctTitleCheckSuccessMultipleJirasBracketsWithSpacesTest() throws IOException {
        wildflyConfigFile = """
                wildfly:
                  format:
                    commit:
                      enabled: false
                """;
        pullRequestJson = PullRequestJson.builder(VALID_PR_TEMPLATE_JSON)
                .action(Action.EDITED)
                .title("[WFLY-00001, WFLY-00002] title")
                .build();

        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, pullRequestJson))
                .when().payloadFromString(pullRequestJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository(TEST_REPO);
                    Util.verifyFormatSuccess(repo, pullRequestJson);
                });
    }
}
