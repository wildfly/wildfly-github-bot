package io.xstefank.wildfly.bot;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;
import io.xstefank.wildfly.bot.utils.GitHubJson;
import io.xstefank.wildfly.bot.utils.Util;
import org.junit.jupiter.api.BeforeAll;
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
 * Tests for the Wildfly -> Rules -> Notify function.
 */
@QuarkusTest
@GitHubAppTest
public class PRRuleNotifyTest {

    private static String wildflyConfigFile;
    private static GitHubJson gitHubJson;

    @BeforeAll
    static void setUpGitHubJson() throws IOException {
        gitHubJson = GitHubJson.builder(VALID_PR_TEMPLATE_JSON).build();
    }

    @Test
    void testMentionsCCComment() throws IOException {
        wildflyConfigFile = """
            wildfly:
              rules:
                - id: "Title"
                  title: "Title"
                  notify: [7125767235,0979986727]
                - id: "Description"
                  title: "Description"
                  notify: [3251142365,4533458845]
            """;
        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, gitHubJson))
                .when().payloadFromString(gitHubJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHPullRequest mockedPR = mocks.pullRequest(gitHubJson.id());
                    Mockito.verify(mockedPR).comment("/cc @0979986727, @7125767235");
                    GHRepository repo = mocks.repository(TEST_REPO);
                    Util.verifyFormatSuccess(repo, gitHubJson);
                });
    }

    @Test
    void testMentionsCCCommentSeveralHits() throws IOException {
        wildflyConfigFile = """
            wildfly:
              rules:
                - id: "WFLY"
                  title: "WFLY"
                  notify: [7125767235,0979986727]
                - id: "Title"
                  title: "Title"
                  notify: [3251142365,4533458845]
            """;
        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, gitHubJson))
                .when().payloadFromString(gitHubJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHPullRequest mockedPR = mocks.pullRequest(gitHubJson.id());
                    Mockito.verify(mockedPR).comment("/cc @4533458845, @0979986727, @7125767235, @3251142365");
                    GHRepository repo = mocks.repository(TEST_REPO);
                    Util.verifyFormatSuccess(repo, gitHubJson);
                });
    }

    @Test
    void testMentionsCCCommentForDuplicateMentions() throws IOException {
        wildflyConfigFile = """
            wildfly:
              rules:
                - id: "Title"
                  title: "Title"
                  notify: [7125767235,0979986727]
                - id: "Description"
                  body: "issues.redhat.com"
                  notify: [7125767235,4533458845]
            """;
        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, gitHubJson))
                .when().payloadFromString(gitHubJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHPullRequest mockedPR = mocks.pullRequest(gitHubJson.id());
                    Mockito.verify(mockedPR).comment("/cc @4533458845, @0979986727, @7125767235");
                    GHRepository repo = mocks.repository(TEST_REPO);
                    Util.verifyFormatSuccess(repo, gitHubJson);
                });
    }
}
