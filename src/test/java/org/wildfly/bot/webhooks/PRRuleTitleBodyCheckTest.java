package org.wildfly.bot.webhooks;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.wildfly.bot.utils.TestConstants;
import org.wildfly.bot.utils.WildflyGitHubBotTesting;
import org.wildfly.bot.utils.model.PullRequestJson;

import java.io.IOException;

import static io.quarkiverse.githubapp.testing.GitHubAppTesting.given;

/**
 * Tests for the Wildfly -> Rules -> TitleBody checks.
 */
@QuarkusTest
@GitHubAppTest
public class PRRuleTitleBodyCheckTest {

    private static String wildflyConfigFile;
    private static PullRequestJson pullRequestJson;

    @Test
    void testTitleBodyCheckForTitle() throws IOException {
        pullRequestJson = PullRequestJson.builder(TestConstants.VALID_PR_TEMPLATE_JSON).build();
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - id: "TitleBody"
                      title: "NonTitle"
                      body: "NonBody"
                      titleBody: "WFLY"
                      notify: [Tadpole]
                """;
        given().github(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson))
                .when().payloadFromString(pullRequestJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHPullRequest mockedPR = mocks.pullRequest(pullRequestJson.id());
                    Mockito.verify(mockedPR).comment("/cc @Tadpole");
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    WildflyGitHubBotTesting.verifyFormatSuccess(repo, pullRequestJson);
                });
    }

    @Test
    void testTitleBodyCheckForBody() throws IOException {
        pullRequestJson = PullRequestJson.builder(TestConstants.VALID_PR_TEMPLATE_JSON).build();
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - id: "TitleBody"
                      title: "NonTitle"
                      body: "NonBody"
                      titleBody: "issues.redhat.com"
                      notify: [Tadpole]
                """;
        given().github(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson))
                .when().payloadFromString(pullRequestJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHPullRequest mockedPR = mocks.pullRequest(pullRequestJson.id());
                    Mockito.verify(mockedPR).comment("/cc @Tadpole");
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    WildflyGitHubBotTesting.verifyFormatSuccess(repo, pullRequestJson);
                });
    }

    @Test
    void testTitleBodyCheckForTitleBodyCaseInsensitive() throws IOException {
        pullRequestJson = PullRequestJson.builder(TestConstants.VALID_PR_TEMPLATE_JSON).build();
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - id: "TitleBody"
                      title: "NonTitle"
                      body: "NonBody"
                      titleBody: "ISSUES.REDHAT.COM"
                      notify: [Tadpole]
                """;
        given().github(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson))
                .when().payloadFromString(pullRequestJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHPullRequest mockedPR = mocks.pullRequest(pullRequestJson.id());
                    Mockito.verify(mockedPR).comment("/cc @Tadpole");
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    WildflyGitHubBotTesting.verifyFormatSuccess(repo, pullRequestJson);
                });
    }

    @Test
    void testFailedTitleBodyCheck() throws IOException {
        pullRequestJson = PullRequestJson.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .title(TestConstants.INVALID_TITLE)
                .description(TestConstants.INVALID_DESCRIPTION)
                .build();
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - id: "TitleBody"
                      title: "NonTitle"
                      body: "NonBody"
                      titleBody: "NonValidTitleBody"
                      notify: [Tadpole]
                """;
        given().github(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson))
                .when().payloadFromString(pullRequestJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHPullRequest mockedPR = mocks.pullRequest(pullRequestJson.id());
                    Mockito.verify(mockedPR, Mockito.never()).comment("/cc @Tadpole");
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    WildflyGitHubBotTesting.verifyFormatFailure(repo, pullRequestJson, "title");
                });
    }

    @Test
    void testTitleBodyCheckTitleRegex() throws IOException {
        pullRequestJson = PullRequestJson.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .title("[WFLY-00000] Metrics PR")
                .build();
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - titleBody: "metrics|micrometer"
                      notify: [Tadpole]
                """;
        given().github(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson))
                .when().payloadFromString(pullRequestJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHPullRequest mockedPR = mocks.pullRequest(pullRequestJson.id());
                    Mockito.verify(mockedPR).comment("/cc @Tadpole");
                });
    }

    @Test
    void testTitleBodyCheckBodyRegex() throws IOException {
        pullRequestJson = PullRequestJson.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .description("[WFLY-00000] Micrometer commit")
                .build();
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - titleBody: "metrics|micrometer"
                      notify: [Tadpole]
                """;
        given().github(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson))
                .when().payloadFromString(pullRequestJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHPullRequest mockedPR = mocks.pullRequest(pullRequestJson.id());
                    Mockito.verify(mockedPR).comment("/cc @Tadpole");
                });
    }

    @Test
    void testTitleBodyCheckBodyRegexNoHit() throws IOException {
        pullRequestJson = PullRequestJson.builder(TestConstants.VALID_PR_TEMPLATE_JSON).build();
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - titleBody: "not|valid"
                      notify: [Tadpole]
                """;
        given().github(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson))
                .when().payloadFromString(pullRequestJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHPullRequest mockedPR = mocks.pullRequest(pullRequestJson.id());
                    Mockito.verify(mockedPR, Mockito.never()).comment(ArgumentMatchers.anyString());
                });
    }

    @Test
    void testTitleBodyCheckTitleSubstringNoHit() throws IOException {
        pullRequestJson = PullRequestJson.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .title("See the deep no hit").build();
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - titleBody: "ee"
                      notify: [Tadpole]
                  format:
                    title:
                      enabled: false
                """;
        given().github(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson))
                .when().payloadFromString(pullRequestJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHPullRequest mockedPR = mocks.pullRequest(pullRequestJson.id());
                    Mockito.verify(mockedPR, Mockito.never()).comment(ArgumentMatchers.anyString());
                });
    }

    @Test
    void testTitleBodyCheckTitleRegexSubstringNoHit() throws IOException {
        pullRequestJson = PullRequestJson.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .title("See the deep no hit nor originated").build();
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - titleBody: "ee|or"
                      notify: [Tadpole]
                  format:
                    title:
                      enabled: false
                """;
        given().github(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson))
                .when().payloadFromString(pullRequestJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHPullRequest mockedPR = mocks.pullRequest(pullRequestJson.id());
                    Mockito.verify(mockedPR, Mockito.never()).comment(ArgumentMatchers.anyString());
                });
    }

    @Test
    void testTitleBodyCheckForTitleWithPrefix() throws IOException {
        pullRequestJson = PullRequestJson.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .title("(30.x) WFLY-123 Test title").build();
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - titleBody: "ee"
                """;
        given().github(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson))
                .when().payloadFromString(pullRequestJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHPullRequest mockedPR = mocks.pullRequest(pullRequestJson.id());
                    Mockito.verify(mockedPR, Mockito.never()).comment(Mockito.anyString());
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    WildflyGitHubBotTesting.verifyFormatSuccess(repo, pullRequestJson);
                });
    }
}
