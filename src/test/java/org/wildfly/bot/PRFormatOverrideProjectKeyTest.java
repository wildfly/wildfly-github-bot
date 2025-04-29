package org.wildfly.bot;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHRepository;
import org.wildfly.bot.utils.TestConstants;
import org.wildfly.bot.utils.WildflyGitHubBotTesting;
import org.wildfly.bot.utils.mocking.MockedGHPullRequest;
import org.wildfly.bot.utils.model.Action;
import org.wildfly.bot.utils.testing.PullRequestJson;
import org.wildfly.bot.utils.testing.internal.TestModel;

import static org.wildfly.bot.model.RuntimeConstants.DEFAULT_TITLE_MESSAGE;
import static org.wildfly.bot.model.RuntimeConstants.PROJECT_PATTERN_REGEX;

/**
 * Tests for the WildFly -> ProjectKey function.
 */
@QuarkusTest
@GitHubAppTest
public class PRFormatOverrideProjectKeyTest {

    private static final String wildflyConfigFile = """
            wildfly:
              projectKey: WFCORE
            """;
    private static PullRequestJson pullRequestJson;
    private static MockedGHPullRequest mockedContext;

    @BeforeAll
    static void setPullRequestJson() throws Exception {
        TestModel.defaultBeforeEachJsons();
    }

    @Test
    public void testOverridingProjectKeyCorrectTitle() throws Throwable {
        pullRequestJson = TestModel.setPullRequestJsonBuilder(pullRequestJsonBuilder -> pullRequestJsonBuilder
                .action(Action.EDITED)
                .title("WFCORE-00000 title"));

        mockedContext = MockedGHPullRequest.builder(pullRequestJson.id())
                .commit("[WFCORE-123] Valid commit message");

        TestModel.given(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson,
                mockedContext))
                .pullRequestEvent(pullRequestJson)
                .then(mocks -> {
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    WildflyGitHubBotTesting.verifyFormatSuccess(repo, pullRequestJson);
                });
    }

    @Test
    public void testOverridingProjectKeyIncorrectTitle() throws Throwable {
        pullRequestJson = TestModel
                .setPullRequestJsonBuilder(pullRequestJsonBuilder -> pullRequestJsonBuilder.action(Action.EDITED));

        mockedContext = MockedGHPullRequest.builder(pullRequestJson.id())
                .commit("[WFCORE-123] Valid commit message");

        TestModel.given(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson,
                mockedContext))
                .pullRequestEvent(pullRequestJson)
                .then(mocks -> {
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    WildflyGitHubBotTesting.verifyFormatFailure(repo, pullRequestJson, "title");
                    WildflyGitHubBotTesting.verifyFailedFormatComment(mocks, pullRequestJson,
                            "- " + String.format(DEFAULT_TITLE_MESSAGE,
                                    PROJECT_PATTERN_REGEX.formatted("WFCORE")));
                });
    }
}
