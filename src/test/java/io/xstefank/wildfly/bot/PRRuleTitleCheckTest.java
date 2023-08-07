package io.xstefank.wildfly.bot;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;
import io.xstefank.wildfly.bot.utils.GitHubJson;
import io.xstefank.wildfly.bot.utils.Util;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.mockito.Mockito;

import java.io.IOException;

import static io.quarkiverse.githubapp.testing.GitHubAppTesting.given;
import static io.xstefank.wildfly.bot.utils.TestConstants.TEST_REPO;
import static io.xstefank.wildfly.bot.utils.TestConstants.VALID_PR_TEMPLATE_JSON;

/**
 * Tests for the Wildfly -> Rules -> Title checks.
 */
@QuarkusTest
@GitHubAppTest
public class PRRuleTitleCheckTest {

    private static String wildflyConfigFile;
    private static GitHubJson gitHubJson;

    @BeforeEach
    void setUpGitHubJson() throws IOException {
        gitHubJson = GitHubJson.builder(VALID_PR_TEMPLATE_JSON).build();
    }

    @Test
    void testSuccessfulTitleCheck() throws IOException {
        wildflyConfigFile = """
            wildfly:
              rules:
                - id: "Title"
                  title: "Title"
                  notify: [7125767235]
            """;
        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, gitHubJson, null))
                .when().payloadFromString(gitHubJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHPullRequest mockedPR = mocks.pullRequest(gitHubJson.id());
                    Mockito.verify(mockedPR, Mockito.times(2)).comment("/cc @7125767235");
                    GHRepository repo = mocks.repository(TEST_REPO);
                    Util.verifyFormatSuccess(repo, gitHubJson);
                });
    }

    @Test
    void testFailedTitleCheck() throws IOException {
        wildflyConfigFile = """
            wildfly:
              rules:
                - id: "Title"
                  title: "NonValidTitle"
                  notify: [7125767235]
            """;
        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, gitHubJson, null))
                .when().payloadFromString(gitHubJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHPullRequest mockedPR = mocks.pullRequest(gitHubJson.id());
                    Mockito.verify(mockedPR, Mockito.never()).comment("/cc @7125767235");
                    GHRepository repo = mocks.repository(TEST_REPO);
                    Util.verifyFormatSuccess(repo, gitHubJson);
                });
    }

    @Test
    void testTitleBodyCheckForTitleCaseInsensitive() throws IOException {
        wildflyConfigFile = """
            wildfly:
              rules:
                - id: "Title"
                  title: "TiTle"
                  notify: [7125767235]
            """;

        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, gitHubJson, null))
                .when().payloadFromString(gitHubJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHPullRequest mockedPR = mocks.pullRequest(gitHubJson.id());
                    Mockito.verify(mockedPR, Mockito.times(2)).comment("/cc @7125767235");
                    GHRepository repo = mocks.repository(TEST_REPO);
                    Util.verifyFormatSuccess(repo, gitHubJson);
                });
    }




}
