package io.xstefank.wildfly.bot;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;
import io.xstefank.wildfly.bot.utils.GitHubJson;
import io.xstefank.wildfly.bot.utils.Util;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.io.IOException;

import static io.quarkiverse.githubapp.testing.GitHubAppTesting.given;
import static io.xstefank.wildfly.bot.utils.TestConstants.INVALID_DESCRIPTION;
import static io.xstefank.wildfly.bot.utils.TestConstants.INVALID_TITLE;
import static io.xstefank.wildfly.bot.utils.TestConstants.TEST_REPO;
import static io.xstefank.wildfly.bot.utils.TestConstants.VALID_PR_TEMPLATE_JSON;

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
        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, gitHubJson))
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
        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, gitHubJson))
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
        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, gitHubJson))
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
        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, gitHubJson))
                .when().payloadFromString(gitHubJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHPullRequest mockedPR = mocks.pullRequest(gitHubJson.id());
                    Mockito.verify(mockedPR, Mockito.never()).comment("/cc @7125767235");
                    GHRepository repo = mocks.repository(TEST_REPO);
                    Util.verifyFormatFailure(repo, gitHubJson, "title");
                });
    }

    @Test
    void testTitleBodyCheckTitleRegex() throws IOException {
        gitHubJson = GitHubJson.builder(VALID_PR_TEMPLATE_JSON)
                .title("[WFLY-00000] Metrics PR")
                .build();
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - titleBody: "metrics|micrometer"
                      notify: [7125767235]
                """;
        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, gitHubJson))
                .when().payloadFromString(gitHubJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHPullRequest mockedPR = mocks.pullRequest(gitHubJson.id());
                    Mockito.verify(mockedPR).comment("/cc @7125767235");
                });
    }

    @Test
    void testTitleBodyCheckBodyRegex() throws IOException {
        gitHubJson = GitHubJson.builder(VALID_PR_TEMPLATE_JSON)
                .description("[WFLY-00000] Micrometer commit")
                .build();
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - titleBody: "metrics|micrometer"
                      notify: [7125767235]
                """;
        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, gitHubJson))
                .when().payloadFromString(gitHubJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHPullRequest mockedPR = mocks.pullRequest(gitHubJson.id());
                    Mockito.verify(mockedPR).comment("/cc @7125767235");
                });
    }

    @Test
    void testTitleBodyCheckBodyRegexNoHit() throws IOException {
        gitHubJson = GitHubJson.builder(VALID_PR_TEMPLATE_JSON).build();
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - titleBody: "not|valid"
                      notify: [7125767235]
                """;
        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, gitHubJson))
                .when().payloadFromString(gitHubJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHPullRequest mockedPR = mocks.pullRequest(gitHubJson.id());
                    Mockito.verify(mockedPR, Mockito.never()).comment(ArgumentMatchers.anyString());
                });
    }

    @Test
    void testTitleBodyCheckTitleSubstringNoHit() throws IOException {
        gitHubJson = GitHubJson.builder(VALID_PR_TEMPLATE_JSON)
                .title("See the deep no hit").build();
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - titleBody: "ee"
                      notify: [7125767235]
                  format:
                    title:
                      enabled: false
                """;
        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, gitHubJson))
                .when().payloadFromString(gitHubJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHPullRequest mockedPR = mocks.pullRequest(gitHubJson.id());
                    Mockito.verify(mockedPR, Mockito.never()).comment(ArgumentMatchers.anyString());
                });
    }

    @Test
    void testTitleBodyCheckTitleRegexSubstringNoHit() throws IOException {
        gitHubJson = GitHubJson.builder(VALID_PR_TEMPLATE_JSON)
                .title("See the deep no hit nor originated").build();
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - titleBody: "ee|or"
                      notify: [7125767235]
                  format:
                    title:
                      enabled: false
                """;
        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, gitHubJson))
                .when().payloadFromString(gitHubJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHPullRequest mockedPR = mocks.pullRequest(gitHubJson.id());
                    Mockito.verify(mockedPR, Mockito.never()).comment(ArgumentMatchers.anyString());
                });
    }

    @Test
    void testTitleBodyCheckForTitleWithPrefix() throws IOException {
        gitHubJson = GitHubJson.builder(VALID_PR_TEMPLATE_JSON)
                .title("(30.x) WFLY-123 Test title").build();
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - titleBody: "ee"
                """;
        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, gitHubJson))
                .when().payloadFromString(gitHubJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHPullRequest mockedPR = mocks.pullRequest(gitHubJson.id());
                    Mockito.verify(mockedPR, Mockito.never()).comment(Mockito.anyString());
                    GHRepository repo = mocks.repository(TEST_REPO);
                    Util.verifyFormatSuccess(repo, gitHubJson);
                });
    }
}
