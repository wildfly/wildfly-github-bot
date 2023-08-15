package io.xstefank.wildfly.bot;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;
import io.xstefank.wildfly.bot.model.WildFlyConfigFile;
import io.xstefank.wildfly.bot.utils.Action;
import io.xstefank.wildfly.bot.utils.GitHubJson;
import io.xstefank.wildfly.bot.utils.Util;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GHRepository;

import java.io.IOException;

import static io.quarkiverse.githubapp.testing.GitHubAppTesting.given;
import static io.xstefank.wildfly.bot.model.RuntimeConstants.DEFAULT_TITLE_MESSAGE;
import static io.xstefank.wildfly.bot.utils.TestConstants.VALID_PR_TEMPLATE_JSON;
import static io.xstefank.wildfly.bot.utils.TestConstants.TEST_REPO;

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
    private static GitHubJson gitHubJson;


    @Test
    public void testOverridingProjectKeyCorrectTitle() throws IOException {
        gitHubJson = GitHubJson.builder(VALID_PR_TEMPLATE_JSON)
                .action(Action.EDITED)
                .title("WFCORE-00000 title")
                .build();
        String commitMessage = "[WFCORE-123] Valid commit message";

        given()
                .github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, gitHubJson, commitMessage))
                .when().payloadFromString(gitHubJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository(TEST_REPO);
                    Util.verifyFormatSuccess(repo, gitHubJson);
                });
    }

    @Test
    public void testOverridingProjectKeyIncorrectTitle() throws IOException {
        gitHubJson = GitHubJson.builder(VALID_PR_TEMPLATE_JSON)
                .action(Action.EDITED)
                .title("WFLY-00000 title")
                .build();
        String commitMessage = "[WFCORE-123] Valid commit message";

        given()
                .github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, gitHubJson, commitMessage))
                .when().payloadFromString(gitHubJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository(TEST_REPO);
                    Util.verifyFormatFailure(repo, gitHubJson, "title");
                    Util.verifyFailedFormatComment(mocks, gitHubJson, "- " + String.format(DEFAULT_TITLE_MESSAGE,
                        WildFlyConfigFile.PROJECT_PATTERN_REGEX_PREFIXED.formatted("WFCORE", "WFCORE")));
                });
    }
}
