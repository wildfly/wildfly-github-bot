package io.xstefank.wildfly.bot;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;
import io.xstefank.wildfly.bot.utils.GitHubJson;
import io.xstefank.wildfly.bot.utils.Util;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.mockito.Mockito;

import java.io.IOException;

import static io.quarkiverse.githubapp.testing.GitHubAppTesting.given;
import static io.xstefank.wildfly.bot.utils.TestConstants.INVALID_DESCRIPTION;
import static io.xstefank.wildfly.bot.utils.TestConstants.INVALID_TITLE;
import static io.xstefank.wildfly.bot.utils.TestConstants.VALID_PR_TEMPLATE_JSON;
import static io.xstefank.wildfly.bot.utils.TestConstants.TEST_REPO;

/**
 * Tests for the Wildfly -> Rules -> TitleBody checks.
 */
@QuarkusTest
@GitHubAppTest
public class PRRuleTitleBodyCheckTest {

    private static String wildflyConfigFile;
    private static GitHubJson gitHubJson;

    @Test
    void testTitleBodyCheckForTitle() throws IOException {
        gitHubJson = GitHubJson.builder(VALID_PR_TEMPLATE_JSON).build();
        wildflyConfigFile = """
            wildfly:
              rules:
                - id: "TitleBody"
                  title: "NonTitle"
                  body: "NonBody"
                  titleBody: "WFLY"
                  notify: [7125767235]
            """;
        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, gitHubJson, null))
                .when().payloadFromString(gitHubJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHPullRequest mockedPR = mocks.pullRequest(gitHubJson.id());
                    Mockito.verify(mockedPR).comment("/cc @7125767235");
                    GHRepository repo = mocks.repository(TEST_REPO);
                    Util.verifyFormatSuccess(repo, gitHubJson);
                });
    }

    @Test
    void testTitleBodyCheckForBody() throws IOException {
        gitHubJson = GitHubJson.builder(VALID_PR_TEMPLATE_JSON).build();
        wildflyConfigFile = """
            wildfly:
              rules:
                - id: "TitleBody"
                  title: "NonTitle"
                  body: "NonBody"
                  titleBody: "issues.redhat.com"
                  notify: [7125767235]
            """;
        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, gitHubJson, null))
                .when().payloadFromString(gitHubJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHPullRequest mockedPR = mocks.pullRequest(gitHubJson.id());
                    Mockito.verify(mockedPR).comment("/cc @7125767235");
                    GHRepository repo = mocks.repository(TEST_REPO);
                    Util.verifyFormatSuccess(repo, gitHubJson);
                });
    }

    @Test
    void testTitleBodyCheckForTitleBodyCaseInsensitive() throws IOException {
        gitHubJson = GitHubJson.builder(VALID_PR_TEMPLATE_JSON).build();
        wildflyConfigFile = """
            wildfly:
              rules:
                - id: "TitleBody"
                  title: "NonTitle"
                  body: "NonBody"
                  titleBody: "ISSUES.REDHAT.COM"
                  notify: [7125767235]
            """;
        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, gitHubJson, null))
                .when().payloadFromString(gitHubJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHPullRequest mockedPR = mocks.pullRequest(gitHubJson.id());
                    Mockito.verify(mockedPR).comment("/cc @7125767235");
                    GHRepository repo = mocks.repository(TEST_REPO);
                    Util.verifyFormatSuccess(repo, gitHubJson);
                });
    }

    @Test
    void testFailedTitleBodyCheck() throws IOException {
        gitHubJson = GitHubJson.builder(VALID_PR_TEMPLATE_JSON)
                .title(INVALID_TITLE)
                .description(INVALID_DESCRIPTION)
                .build();
        wildflyConfigFile = """
            wildfly:
              rules:
                - id: "TitleBody"
                  title: "NonTitle"
                  body: "NonBody"
                  titleBody: "NonValidTitleBody"
                  notify: [7125767235]
            """;
        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, gitHubJson, null))
                .when().payloadFromString(gitHubJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHPullRequest mockedPR = mocks.pullRequest(gitHubJson.id());
                    Mockito.verify(mockedPR, Mockito.never()).comment("/cc @7125767235");
                    GHRepository repo = mocks.repository(TEST_REPO);
                    Util.verifyFormatFailure(repo, gitHubJson, "title");
                });
    }
}
