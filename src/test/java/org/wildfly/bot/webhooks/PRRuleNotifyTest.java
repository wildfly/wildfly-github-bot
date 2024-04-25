package org.wildfly.bot.webhooks;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.mockito.Mockito;
import org.wildfly.bot.utils.TestConstants;
import org.wildfly.bot.utils.WildflyGitHubBotTesting;
import org.wildfly.bot.utils.model.SsePullRequestPayload;

import java.io.IOException;

import static io.quarkiverse.githubapp.testing.GitHubAppTesting.given;

/**
 * Tests for the Wildfly -> Rules -> Notify function.
 */
@QuarkusTest
@GitHubAppTest
public class PRRuleNotifyTest {

    private static String wildflyConfigFile;
    private static SsePullRequestPayload ssePullRequestPayload;

    @BeforeAll
    static void setUpGitHubJson() throws IOException {
        ssePullRequestPayload = SsePullRequestPayload.builder(TestConstants.VALID_PR_TEMPLATE_JSON).build();
    }

    @Test
    void testMentionsCCComment() throws IOException {
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - id: "Title"
                      title: "Title"
                      notify: [Tadpole,Duke]
                    - id: "Description"
                      title: "Description"
                      notify: [Butterfly,Doggo]
                """;
        given().github(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, ssePullRequestPayload))
                .when().payloadFromString(ssePullRequestPayload.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHPullRequest mockedPR = mocks.pullRequest(ssePullRequestPayload.id());
                    Mockito.verify(mockedPR).comment("/cc @Tadpole, @Duke");
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    WildflyGitHubBotTesting.verifyFormatSuccess(repo, ssePullRequestPayload);
                });
    }

    @Test
    void testMentionsCCCommentSeveralHits() throws IOException {
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - id: "WFLY"
                      title: "WFLY"
                      notify: [Tadpole,Duke]
                    - id: "Title"
                      title: "Title"
                      notify: [Butterfly,Doggo]
                """;
        given().github(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, ssePullRequestPayload))
                .when().payloadFromString(ssePullRequestPayload.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHPullRequest mockedPR = mocks.pullRequest(ssePullRequestPayload.id());
                    Mockito.verify(mockedPR).comment("/cc @Butterfly, @Doggo, @Tadpole, @Duke");
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    WildflyGitHubBotTesting.verifyFormatSuccess(repo, ssePullRequestPayload);
                });
    }

    @Test
    void testMentionsCCCommentForDuplicateMentions() throws IOException {
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - id: "Title"
                      title: "Title"
                      notify: [Tadpole,Duke]
                    - id: "Description"
                      body: "issues.redhat.com"
                      notify: [Tadpole,Doggo]
                """;
        given().github(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, ssePullRequestPayload))
                .when().payloadFromString(ssePullRequestPayload.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHPullRequest mockedPR = mocks.pullRequest(ssePullRequestPayload.id());
                    Mockito.verify(mockedPR).comment("/cc @Doggo, @Tadpole, @Duke");
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    WildflyGitHubBotTesting.verifyFormatSuccess(repo, ssePullRequestPayload);
                });
    }
}
