package org.wildfly.bot.webhooks;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHRepository;
import org.mockito.Mockito;
import org.wildfly.bot.format.TitleCheck;
import org.wildfly.bot.model.RegexDefinition;
import org.wildfly.bot.utils.TestConstants;
import org.wildfly.bot.utils.WildflyGitHubBotTesting;
import org.wildfly.bot.utils.mocking.Mockable;
import org.wildfly.bot.utils.mocking.MockedGHPullRequest;
import org.wildfly.bot.utils.model.Action;
import org.wildfly.bot.utils.testing.PullRequestJson;
import org.wildfly.bot.utils.testing.internal.TestModel;

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
    private static PullRequestJson pullRequestJson;
    private Mockable mockedContext;

    @BeforeAll
    static void setPullRequestJson() throws Exception {
        TestModel.defaultBeforeEachJsons();
    }

    @Test
    void configFileNullTest() {
        RegexDefinition regexDefinition = new RegexDefinition();

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> new TitleCheck(regexDefinition));
        Assertions.assertEquals("Input argument cannot be null", thrown.getMessage());
    }

    @Test
    void incorrectTitleCheckFailTest() throws Throwable {
        wildflyConfigFile = """
                wildfly:
                  format:
                    title:
                      enabled: true
                """;
        pullRequestJson = TestModel.setPullRequestJsonBuilder(
                pullRequestJsonBuilder -> pullRequestJsonBuilder.title(TestConstants.INVALID_TITLE));

        TestModel.given(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson))
                .pullRequestEvent(pullRequestJson)
                .then(mocks -> {
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    WildflyGitHubBotTesting.verifyFormatFailure(repo, pullRequestJson, "title");
                    WildflyGitHubBotTesting.verifyFailedFormatComment(mocks, pullRequestJson,
                            "- " + String.format(DEFAULT_TITLE_MESSAGE,
                                    PROJECT_PATTERN_REGEX.formatted("WFLY")));
                });
    }

    @Test
    void incorrectTitleCheckFailFormatConfigMissingTest() throws Throwable {
        pullRequestJson = TestModel.setPullRequestJsonBuilder(
                pullRequestJsonBuilder -> pullRequestJsonBuilder.title(TestConstants.INVALID_TITLE));
        wildflyConfigFile = """
                wildfly:
                """;

        TestModel.given(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson))
                .pullRequestEvent(pullRequestJson)
                .then(mocks -> {
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    WildflyGitHubBotTesting.verifyFormatFailure(repo, pullRequestJson, "title");
                    WildflyGitHubBotTesting.verifyFailedFormatComment(mocks, pullRequestJson,
                            "- " + String.format(DEFAULT_TITLE_MESSAGE,
                                    PROJECT_PATTERN_REGEX.formatted("WFLY", "WFLY")));
                });
    }

    @Test
    void correctTitleCheckFailFormatConfigMissingTest() throws Throwable {
        pullRequestJson = TestModel.getPullRequestJson();
        wildflyConfigFile = """
                wildfly:
                """;

        TestModel.given(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson))
                .pullRequestEvent(pullRequestJson)
                .then(mocks -> {
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    WildflyGitHubBotTesting.verifyFormatSuccess(repo, pullRequestJson);
                });
    }

    @Test
    void incorrectTitleCheckFailFormatConfigNullTest() throws Throwable {
        pullRequestJson = TestModel.setPullRequestJsonBuilder(
                pullRequestJsonBuilder -> pullRequestJsonBuilder.title(TestConstants.INVALID_TITLE));
        wildflyConfigFile = """
                wildfly:
                  format:
                """;

        TestModel.given(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson))
                .pullRequestEvent(pullRequestJson)
                .then(mocks -> {
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    WildflyGitHubBotTesting.verifyFormatFailure(repo, pullRequestJson, "title");
                    WildflyGitHubBotTesting.verifyFailedFormatComment(mocks, pullRequestJson,
                            "- " + String.format(DEFAULT_TITLE_MESSAGE,
                                    PROJECT_PATTERN_REGEX.formatted("WFLY", "WFLY")));
                });
    }

    @Test
    void correctTitleCheckFailFormatConfigNullTest() throws Throwable {
        pullRequestJson = TestModel.getPullRequestJson();
        wildflyConfigFile = """
                wildfly:
                  format:
                """;

        TestModel.given(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson))
                .pullRequestEvent(pullRequestJson)
                .then(mocks -> {
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    WildflyGitHubBotTesting.verifyFormatSuccess(repo, pullRequestJson);
                });
    }

    @Test
    void incorrectTitleCheckFailFormatTitleConfigNullTest() throws Throwable {
        pullRequestJson = TestModel.setPullRequestJsonBuilder(
                pullRequestJsonBuilder -> pullRequestJsonBuilder.title(TestConstants.INVALID_TITLE));
        wildflyConfigFile = """
                wildfly:
                  format:
                    title:
                """;

        TestModel.given(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson))
                .pullRequestEvent(pullRequestJson)
                .then(mocks -> {
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    WildflyGitHubBotTesting.verifyFormatFailure(repo, pullRequestJson, "title");
                    WildflyGitHubBotTesting.verifyFailedFormatComment(mocks, pullRequestJson,
                            "- " + String.format(DEFAULT_TITLE_MESSAGE,
                                    PROJECT_PATTERN_REGEX.formatted("WFLY", "WFLY")));
                });
    }

    @Test
    void correctTitleCheckFailFormatTitleConfigNullTest() throws Throwable {
        pullRequestJson = TestModel.getPullRequestJson();
        wildflyConfigFile = """
                wildfly:
                  format:
                    title:
                """;

        TestModel.given(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson))
                .pullRequestEvent(pullRequestJson)
                .then(mocks -> {
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    WildflyGitHubBotTesting.verifyFormatSuccess(repo, pullRequestJson);
                });
    }

    @Test
    void correctTitleCheckSuccessMultipleJirasWithTextTest() throws Throwable {
        wildflyConfigFile = """
                wildfly:
                  format:
                    commit:
                      enabled: false
                """;
        pullRequestJson = TestModel
                .setPullRequestJsonBuilder(pullRequestJsonBuilder -> pullRequestJsonBuilder.action(Action.EDITED)
                        .title("WFLY-00001 jira,WFLY-00002 title"));

        TestModel.given(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson))
                .pullRequestEvent(pullRequestJson)
                .then(mocks -> {
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    WildflyGitHubBotTesting.verifyFormatSuccess(repo, pullRequestJson);
                });
    }

    @Test
    void correctTitleCheckFailMultipleJirasBracketsMissingBracketTest() throws Throwable {
        wildflyConfigFile = """
                wildfly:
                  format:
                    commit:
                      enabled: false
                """;
        pullRequestJson = TestModel
                .setPullRequestJsonBuilder(pullRequestJsonBuilder -> pullRequestJsonBuilder.action(Action.EDITED)
                        .title("[WFLY-00001,WFLY-00002 title"));

        TestModel.given(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson))
                .pullRequestEvent(pullRequestJson)
                .then(mocks -> {
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    WildflyGitHubBotTesting.verifyFormatSuccess(repo, pullRequestJson);
                });
    }

    @Test
    void correctTitleCheckSuccessMultipleJirasBracketsIncorrectIssuePlacementTest() throws Throwable {
        wildflyConfigFile = """
                wildfly:
                  format:
                    commit:
                      enabled: false
                """;
        pullRequestJson = TestModel
                .setPullRequestJsonBuilder(pullRequestJsonBuilder -> pullRequestJsonBuilder.action(Action.EDITED)
                        .title("[WFLY-00001 jira,WFLY-00002] title"));

        TestModel.given(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson))
                .pullRequestEvent(pullRequestJson)
                .then(mocks -> {
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    WildflyGitHubBotTesting.verifyFormatSuccess(repo, pullRequestJson);
                });
    }

    @Test
    void correctTitleCheckSuccessMultipleJirasBracketsIncorrectEndingBracketPlacementTest() throws Throwable {
        wildflyConfigFile = """
                wildfly:
                  format:
                    commit:
                      enabled: false
                """;
        pullRequestJson = TestModel
                .setPullRequestJsonBuilder(pullRequestJsonBuilder -> pullRequestJsonBuilder.action(Action.EDITED)
                        .title("[WFLY-00001, WFLY-00002 jira] title"));

        TestModel.given(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson))
                .pullRequestEvent(pullRequestJson)
                .then(mocks -> {
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    WildflyGitHubBotTesting.verifyFormatSuccess(repo, pullRequestJson);
                });
    }

    @Test
    void correctTitleCheckSuccessTest() throws Throwable {
        wildflyConfigFile = """
                wildfly:
                  format:
                    title:
                      enabled: true
                """;
        pullRequestJson = TestModel.getPullRequestJson();

        TestModel.given(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson))
                .pullRequestEvent(pullRequestJson)
                .then(mocks -> {
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    WildflyGitHubBotTesting.verifyFormatSuccess(repo, pullRequestJson);
                });
    }

    @Test
    void testDisabledTitleCheck() throws Throwable {
        wildflyConfigFile = """
                wildfly:
                  format:
                    title:
                      enabled: false
                """;
        pullRequestJson = TestModel.setPullRequestJsonBuilder(
                pullRequestJsonBuilder -> pullRequestJsonBuilder.title(TestConstants.INVALID_TITLE));

        TestModel.given(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson))
                .pullRequestEvent(pullRequestJson)
                .then(mocks -> {
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    WildflyGitHubBotTesting.verifyFormatSuccess(repo, pullRequestJson);
                });
    }

    @Test
    public void testOverridingTitleMessage() throws Throwable {
        wildflyConfigFile = """
                wildfly:
                  format:
                    title:
                      message: "Custom title message"
                """;
        pullRequestJson = TestModel.setPullRequestJsonBuilder(
                pullRequestJsonBuilder -> pullRequestJsonBuilder.title(TestConstants.INVALID_TITLE));
        mockedContext = MockedGHPullRequest.builder(pullRequestJson.id())
                .commit("WFLY-00000 commit");

        TestModel.given(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson, mockedContext))
                .pullRequestEvent(pullRequestJson)
                .then(mocks -> {
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    WildflyGitHubBotTesting.verifyFormatFailure(repo, pullRequestJson, "title");
                    WildflyGitHubBotTesting.verifyFailedFormatComment(mocks, pullRequestJson, "- Custom title message");
                });
    }

    @Test
    public void testDisableGlobalFormatCheck() throws Throwable {
        wildflyConfigFile = """
                wildfly:
                  format:
                    enabled: false
                """;

        pullRequestJson = TestModel.setPullRequestJsonBuilder(
                pullRequestJsonBuilder -> pullRequestJsonBuilder.title(TestConstants.INVALID_TITLE));

        TestModel.given(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson))
                .pullRequestEvent(pullRequestJson)
                .then(mocks -> {
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    Mockito.verify(repo, never()).createCommitStatus(anyString(), any(), anyString(), anyString());
                    Mockito.verify(repo, never()).createCommitStatus(anyString(), any(), anyString(), anyString(), anyString());
                });
    }

    @Test
    void correctTitleCheckSuccessSingleJiraBracketsTest() throws Throwable {
        wildflyConfigFile = """
                wildfly:
                  format:
                    commit:
                      enabled: false
                """;

        pullRequestJson = TestModel
                .setPullRequestJsonBuilder(pullRequestJsonBuilder -> pullRequestJsonBuilder.action(Action.EDITED)
                        .title("[WFLY-00001] title"));

        TestModel.given(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson))
                .pullRequestEvent(pullRequestJson)
                .then(mocks -> {
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    WildflyGitHubBotTesting.verifyFormatSuccess(repo, pullRequestJson);
                });
    }

    @Test
    void correctTitleCheckSuccessMultipleJirasTest() throws Throwable {
        wildflyConfigFile = """
                wildfly:
                  format:
                    commit:
                      enabled: false
                """;

        pullRequestJson = TestModel
                .setPullRequestJsonBuilder(pullRequestJsonBuilder -> pullRequestJsonBuilder.action(Action.EDITED)
                        .title("WFLY-00001,WFLY-00002 title"));

        TestModel.given(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson))
                .pullRequestEvent(pullRequestJson)
                .then(mocks -> {
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    WildflyGitHubBotTesting.verifyFormatSuccess(repo, pullRequestJson);
                });
    }

    @Test
    void correctTitleCheckSuccessMultipleJirasWithSpacesTest() throws Throwable {
        wildflyConfigFile = """
                wildfly:
                  format:
                    commit:
                      enabled: false
                """;

        pullRequestJson = TestModel
                .setPullRequestJsonBuilder(pullRequestJsonBuilder -> pullRequestJsonBuilder.action(Action.EDITED)
                        .title("WFLY-00001, WFLY-00002 title"));

        TestModel.given(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson))
                .pullRequestEvent(pullRequestJson)
                .then(mocks -> {
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    WildflyGitHubBotTesting.verifyFormatSuccess(repo, pullRequestJson);
                });
    }

    @Test
    void correctTitleCheckSuccessMultipleJirasBracketsTest() throws Throwable {
        wildflyConfigFile = """
                wildfly:
                  format:
                    commit:
                      enabled: false
                """;

        pullRequestJson = TestModel
                .setPullRequestJsonBuilder(pullRequestJsonBuilder -> pullRequestJsonBuilder.action(Action.EDITED)
                        .title("[WFLY-00001,WFLY-00002] title"));

        TestModel.given(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson))
                .pullRequestEvent(pullRequestJson)
                .then(mocks -> {
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    WildflyGitHubBotTesting.verifyFormatSuccess(repo, pullRequestJson);
                });
    }

    @Test
    void correctTitleCheckSuccessMultipleJirasBracketsWithSpacesTest() throws Throwable {
        wildflyConfigFile = """
                wildfly:
                  format:
                    commit:
                      enabled: false
                """;

        pullRequestJson = TestModel
                .setPullRequestJsonBuilder(pullRequestJsonBuilder -> pullRequestJsonBuilder.action(Action.EDITED)
                        .title("[WFLY-00001, WFLY-00002] title"));

        TestModel.given(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson))
                .pullRequestEvent(pullRequestJson)
                .then(mocks -> {
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    WildflyGitHubBotTesting.verifyFormatSuccess(repo, pullRequestJson);
                });
    }
}
