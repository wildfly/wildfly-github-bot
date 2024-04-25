package org.wildfly.bot.webhooks;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GHRepository;
import org.mockito.Mockito;
import org.wildfly.bot.format.TitleCheck;
import org.wildfly.bot.model.RegexDefinition;
import org.wildfly.bot.utils.TestConstants;
import org.wildfly.bot.utils.WildflyGitHubBotTesting;
import org.wildfly.bot.utils.mocking.Mockable;
import org.wildfly.bot.utils.mocking.MockedGHPullRequest;
import org.wildfly.bot.utils.model.Action;
import org.wildfly.bot.utils.model.SsePullRequestPayload;

import java.io.IOException;

import static io.quarkiverse.githubapp.testing.GitHubAppTesting.given;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.wildfly.bot.model.RuntimeConstants.DEFAULT_TITLE_MESSAGE;
import static org.wildfly.bot.model.RuntimeConstants.PROJECT_PATTERN_REGEX;

/**
 * Tests for the Wildfly -> Format -> Title checks.
 */
@QuarkusTest
@GitHubAppTest
public class PRTitleCheckTest {

    private static String wildflyConfigFile;
    private static SsePullRequestPayload ssePullRequestPayload;
    private Mockable mockedContext;

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
        ssePullRequestPayload = SsePullRequestPayload.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .title(TestConstants.INVALID_TITLE)
                .build();

        given().github(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, ssePullRequestPayload))
                .when().payloadFromString(ssePullRequestPayload.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    WildflyGitHubBotTesting.verifyFormatFailure(repo, ssePullRequestPayload, "title");
                    WildflyGitHubBotTesting.verifyFailedFormatComment(mocks, ssePullRequestPayload,
                            "- " + String.format(DEFAULT_TITLE_MESSAGE,
                                    PROJECT_PATTERN_REGEX.formatted("WFLY")));
                });
    }

    @Test
    void incorrectTitleCheckFailFormatConfigMissingTest() throws IOException {
        ssePullRequestPayload = SsePullRequestPayload.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .title(TestConstants.INVALID_TITLE)
                .build();
        wildflyConfigFile = """
                wildfly:
                """;

        given()
                .github(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, ssePullRequestPayload))
                .when().payloadFromString(ssePullRequestPayload.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    WildflyGitHubBotTesting.verifyFormatFailure(repo, ssePullRequestPayload, "title");
                    WildflyGitHubBotTesting.verifyFailedFormatComment(mocks, ssePullRequestPayload,
                            "- " + String.format(DEFAULT_TITLE_MESSAGE,
                                    PROJECT_PATTERN_REGEX.formatted("WFLY", "WFLY")));
                });
    }

    @Test
    void correctTitleCheckFailFormatConfigMissingTest() throws IOException {
        ssePullRequestPayload = SsePullRequestPayload.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .build();
        wildflyConfigFile = """
                wildfly:
                """;

        given()
                .github(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, ssePullRequestPayload))
                .when().payloadFromString(ssePullRequestPayload.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    WildflyGitHubBotTesting.verifyFormatSuccess(repo, ssePullRequestPayload);
                });
    }

    @Test
    void incorrectTitleCheckFailFormatConfigNullTest() throws IOException {
        ssePullRequestPayload = SsePullRequestPayload.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .title(TestConstants.INVALID_TITLE)
                .build();
        wildflyConfigFile = """
                wildfly:
                  format:
                """;

        given()
                .github(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, ssePullRequestPayload))
                .when().payloadFromString(ssePullRequestPayload.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    WildflyGitHubBotTesting.verifyFormatFailure(repo, ssePullRequestPayload, "title");
                    WildflyGitHubBotTesting.verifyFailedFormatComment(mocks, ssePullRequestPayload,
                            "- " + String.format(DEFAULT_TITLE_MESSAGE,
                                    PROJECT_PATTERN_REGEX.formatted("WFLY", "WFLY")));
                });
    }

    @Test
    void correctTitleCheckFailFormatConfigNullTest() throws IOException {
        ssePullRequestPayload = SsePullRequestPayload.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .build();
        wildflyConfigFile = """
                wildfly:
                  format:
                """;

        given()
                .github(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, ssePullRequestPayload))
                .when().payloadFromString(ssePullRequestPayload.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    WildflyGitHubBotTesting.verifyFormatSuccess(repo, ssePullRequestPayload);
                });
    }

    @Test
    void incorrectTitleCheckFailFormatTitleConfigNullTest() throws IOException {
        ssePullRequestPayload = SsePullRequestPayload.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .title(TestConstants.INVALID_TITLE)
                .build();
        wildflyConfigFile = """
                wildfly:
                  format:
                    title:
                """;

        given()
                .github(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, ssePullRequestPayload))
                .when().payloadFromString(ssePullRequestPayload.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    WildflyGitHubBotTesting.verifyFormatFailure(repo, ssePullRequestPayload, "title");
                    WildflyGitHubBotTesting.verifyFailedFormatComment(mocks, ssePullRequestPayload,
                            "- " + String.format(DEFAULT_TITLE_MESSAGE,
                                    PROJECT_PATTERN_REGEX.formatted("WFLY", "WFLY")));
                });
    }

    @Test
    void correctTitleCheckFailFormatTitleConfigNullTest() throws IOException {
        ssePullRequestPayload = SsePullRequestPayload.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .build();
        wildflyConfigFile = """
                wildfly:
                  format:
                    title:
                """;

        given()
                .github(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, ssePullRequestPayload))
                .when().payloadFromString(ssePullRequestPayload.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    WildflyGitHubBotTesting.verifyFormatSuccess(repo, ssePullRequestPayload);
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
        ssePullRequestPayload = SsePullRequestPayload.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .action(Action.EDITED)
                .title("WFLY-00001 jira,WFLY-00002 title")
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
    void correctTitleCheckFailMultipleJirasBracketsMissingBracketTest() throws IOException {
        wildflyConfigFile = """
                wildfly:
                  format:
                    commit:
                      enabled: false
                """;
        ssePullRequestPayload = SsePullRequestPayload.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .action(Action.EDITED)
                .title("[WFLY-00001,WFLY-00002 title")
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
    void correctTitleCheckSuccessMultipleJirasBracketsIncorrectIssuePlacementTest() throws IOException {
        wildflyConfigFile = """
                wildfly:
                  format:
                    commit:
                      enabled: false
                """;
        ssePullRequestPayload = SsePullRequestPayload.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .action(Action.EDITED)
                .title("[WFLY-00001 jira,WFLY-00002] title")
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
    void correctTitleCheckSuccessMultipleJirasBracketsIncorrectEndingBracketPlacementTest() throws IOException {
        wildflyConfigFile = """
                wildfly:
                  format:
                    commit:
                      enabled: false
                """;
        ssePullRequestPayload = SsePullRequestPayload.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .action(Action.EDITED)
                .title("[WFLY-00001, WFLY-00002 jira] title")
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
    void correctTitleCheckSuccessTest() throws IOException {
        wildflyConfigFile = """
                wildfly:
                  format:
                    title:
                      enabled: true
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
    void testDisabledTitleCheck() throws IOException {
        wildflyConfigFile = """
                wildfly:
                  format:
                    title:
                      enabled: false
                """;
        ssePullRequestPayload = SsePullRequestPayload.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .title(TestConstants.INVALID_TITLE)
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
    public void testOverridingTitleMessage() throws IOException {
        wildflyConfigFile = """
                wildfly:
                  format:
                    title:
                      message: "Custom title message"
                """;
        ssePullRequestPayload = SsePullRequestPayload.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .title(TestConstants.INVALID_TITLE)
                .build();
        mockedContext = MockedGHPullRequest.builder(ssePullRequestPayload.id())
                .commit("WFLY-00000 commit");

        given().github(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, ssePullRequestPayload, mockedContext))
                .when().payloadFromString(ssePullRequestPayload.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    WildflyGitHubBotTesting.verifyFormatFailure(repo, ssePullRequestPayload, "title");
                    WildflyGitHubBotTesting.verifyFailedFormatComment(mocks, ssePullRequestPayload, "- Custom title message");
                });
    }

    @Test
    public void testDisableGlobalFormatCheck() throws IOException {
        wildflyConfigFile = """
                wildfly:
                  format:
                    enabled: false
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

    @Test
    void correctTitleCheckSuccessSingleJiraBracketsTest() throws IOException {
        wildflyConfigFile = """
                wildfly:
                  format:
                    commit:
                      enabled: false
                """;
        ssePullRequestPayload = SsePullRequestPayload.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .action(Action.EDITED)
                .title("[WFLY-00001] title")
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
    void correctTitleCheckSuccessMultipleJirasTest() throws IOException {
        wildflyConfigFile = """
                wildfly:
                  format:
                    commit:
                      enabled: false
                """;
        ssePullRequestPayload = SsePullRequestPayload.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .action(Action.EDITED)
                .title("WFLY-00001,WFLY-00002 title")
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
    void correctTitleCheckSuccessMultipleJirasWithSpacesTest() throws IOException {
        wildflyConfigFile = """
                wildfly:
                  format:
                    commit:
                      enabled: false
                """;
        ssePullRequestPayload = SsePullRequestPayload.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .action(Action.EDITED)
                .title("WFLY-00001, WFLY-00002 title")
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
    void correctTitleCheckSuccessMultipleJirasBracketsTest() throws IOException {
        wildflyConfigFile = """
                wildfly:
                  format:
                    commit:
                      enabled: false
                """;
        ssePullRequestPayload = SsePullRequestPayload.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .action(Action.EDITED)
                .title("[WFLY-00001,WFLY-00002] title")
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
    void correctTitleCheckSuccessMultipleJirasBracketsWithSpacesTest() throws IOException {
        wildflyConfigFile = """
                wildfly:
                  format:
                    commit:
                      enabled: false
                """;
        ssePullRequestPayload = SsePullRequestPayload.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .action(Action.EDITED)
                .title("[WFLY-00001, WFLY-00002] title")
                .build();

        given().github(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, ssePullRequestPayload))
                .when().payloadFromString(ssePullRequestPayload.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    WildflyGitHubBotTesting.verifyFormatSuccess(repo, ssePullRequestPayload);
                });
    }
}
