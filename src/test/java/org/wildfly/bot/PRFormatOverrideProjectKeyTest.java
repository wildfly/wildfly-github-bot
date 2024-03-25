package org.wildfly.bot;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;
import org.wildfly.bot.utils.Action;
import org.wildfly.bot.utils.MockedGHPullRequest;
import org.wildfly.bot.utils.PullRequestJson;
import org.wildfly.bot.utils.Util;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GHRepository;
import org.wildfly.bot.utils.TestConstants;

import java.io.IOException;

import static io.quarkiverse.githubapp.testing.GitHubAppTesting.given;
import static org.wildfly.bot.model.RuntimeConstants.DEFAULT_TITLE_MESSAGE;
import static org.wildfly.bot.model.RuntimeConstants.PROJECT_PATTERN_REGEX;

/**
 * Tests for the Wildfly -> ProjectKey function.
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

    @Test
    public void testOverridingProjectKeyCorrectTitle() throws IOException {
        pullRequestJson = PullRequestJson.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .action(Action.EDITED)
                .title("WFCORE-00000 title")
                .build();
        mockedContext = MockedGHPullRequest.builder(pullRequestJson.id())
                .commit("[WFCORE-123] Valid commit message");

        given()
                .github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, pullRequestJson, mockedContext))
                .when().payloadFromString(pullRequestJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    Util.verifyFormatSuccess(repo, pullRequestJson);
                });
    }

    @Test
    public void testOverridingProjectKeyIncorrectTitle() throws IOException {
        pullRequestJson = PullRequestJson.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .action(Action.EDITED)
                .build();
        mockedContext = MockedGHPullRequest.builder(pullRequestJson.id())
                .commit("[WFCORE-123] Valid commit message");

        given()
                .github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, pullRequestJson, mockedContext))
                .when().payloadFromString(pullRequestJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    Util.verifyFormatFailure(repo, pullRequestJson, "title");
                    Util.verifyFailedFormatComment(mocks, pullRequestJson, "- " + String.format(DEFAULT_TITLE_MESSAGE,
                            PROJECT_PATTERN_REGEX.formatted("WFCORE")));
                });
    }
}
