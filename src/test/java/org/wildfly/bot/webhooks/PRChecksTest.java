package org.wildfly.bot.webhooks;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.mockito.Mockito;
import org.wildfly.bot.utils.TestConstants;
import org.wildfly.bot.utils.WildflyGitHubBotTesting;
import org.wildfly.bot.utils.mocking.Mockable;
import org.wildfly.bot.utils.mocking.MockedGHPullRequest;
import org.wildfly.bot.utils.model.SsePullRequestPayload;

import java.io.IOException;

import static io.quarkiverse.githubapp.testing.GitHubAppTesting.given;

/**
 * Tests containing multiple/all checks at the same time.
 */
@QuarkusTest
@GitHubAppTest
public class PRChecksTest {

    private static String wildflyConfigFile;
    private static SsePullRequestPayload ssePullRequestPayload;
    private Mockable mockedContext;

    @Test
    void testNoConfigFile() throws IOException {
        wildflyConfigFile = """
                wildfly:
                  format:
                    title:
                      enabled: false
                    commit:
                      enabled: false
                """;
        ssePullRequestPayload = SsePullRequestPayload.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .title(TestConstants.INVALID_TITLE)
                .description(TestConstants.INVALID_DESCRIPTION)
                .build();
        mockedContext = MockedGHPullRequest.builder(ssePullRequestPayload.id())
                .commit(TestConstants.INVALID_COMMIT_MESSAGE);

        given().github(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, ssePullRequestPayload, mockedContext))
                .when().payloadFromString(ssePullRequestPayload.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    WildflyGitHubBotTesting.verifyFormatSuccess(repo, ssePullRequestPayload);
                });
    }

    @Test
    void testAllChecksPass() throws IOException {
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - id: "Title"
                      title: "Title"
                      notify: [Tadpole]
                    - id: "Description"
                      body: "issues.redhat.com"
                      notify: [Butterfly]
                  format:
                    description:
                         regexes:
                           - pattern: "JIRA:\\\\s+https://issues.redhat.com/browse/WFLY-\\\\d+|https://issues.redhat.com/browse/WFLY-\\\\d+"
                             message: "The PR description must contain a link to the JIRA issue"
                """;
        ssePullRequestPayload = SsePullRequestPayload.builder(TestConstants.VALID_PR_TEMPLATE_JSON).build();

        given().github(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, ssePullRequestPayload))
                .when().payloadFromString(ssePullRequestPayload.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    WildflyGitHubBotTesting.verifyFormatSuccess(repo, ssePullRequestPayload);
                    GHPullRequest mockedPR = mocks.pullRequest(ssePullRequestPayload.id());
                    Mockito.verify(mockedPR).comment("/cc @Butterfly, @Tadpole");
                });
    }

    @Test
    void testAllChecksFail() throws IOException {
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - id: "Title"
                      title: "Title"
                      notify: [Tadpole]
                    - id: "Description"
                      body: "JIRA"
                      notify: [Butterfly]
                  format:
                    description:
                         regexes:
                           - pattern: "JIRA:\\\\s+https://issues.redhat.com/browse/WFLY-\\\\d+|https://issues.redhat.com/browse/WFLY-\\\\d+"
                             message: "The PR description must contain a link to the JIRA issue"
                """;
        ssePullRequestPayload = SsePullRequestPayload.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .title(TestConstants.INVALID_TITLE)
                .description(TestConstants.INVALID_DESCRIPTION)
                .build();
        mockedContext = MockedGHPullRequest.builder(ssePullRequestPayload.id())
                .commit(TestConstants.INVALID_COMMIT_MESSAGE);

        given().github(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, ssePullRequestPayload, mockedContext))
                .when().payloadFromString(ssePullRequestPayload.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    WildflyGitHubBotTesting.verifyFormatFailure(repo, ssePullRequestPayload, "commit, description, title");
                    GHPullRequest mockedPR = mocks.pullRequest(ssePullRequestPayload.id());
                    Mockito.verify(mockedPR, Mockito.never()).comment("/cc @Butterfly, @Tadpole");
                });
    }
}
