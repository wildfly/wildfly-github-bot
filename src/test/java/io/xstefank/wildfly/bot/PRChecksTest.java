package io.xstefank.wildfly.bot;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;
import io.xstefank.wildfly.bot.utils.MockedContext;
import io.xstefank.wildfly.bot.utils.PullRequestJson;
import io.xstefank.wildfly.bot.utils.Util;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.mockito.Mockito;

import java.io.IOException;

import static io.quarkiverse.githubapp.testing.GitHubAppTesting.given;
import static io.xstefank.wildfly.bot.utils.TestConstants.INVALID_COMMIT_MESSAGE;
import static io.xstefank.wildfly.bot.utils.TestConstants.INVALID_DESCRIPTION;
import static io.xstefank.wildfly.bot.utils.TestConstants.INVALID_TITLE;
import static io.xstefank.wildfly.bot.utils.TestConstants.TEST_REPO;
import static io.xstefank.wildfly.bot.utils.TestConstants.VALID_PR_TEMPLATE_JSON;

/**
 * Tests containing multiple/all checks at the same time.
 */
@QuarkusTest
@GitHubAppTest
public class PRChecksTest {

    private static String wildflyConfigFile;
    private static PullRequestJson pullRequestJson;
    private MockedContext mockedContext;

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
        pullRequestJson = PullRequestJson.builder(VALID_PR_TEMPLATE_JSON)
                .title(INVALID_TITLE)
                .description(INVALID_DESCRIPTION)
                .build();
        mockedContext = MockedContext.builder(pullRequestJson.id())
                .commit(INVALID_COMMIT_MESSAGE);

        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, pullRequestJson, mockedContext))
                .when().payloadFromString(pullRequestJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository(TEST_REPO);
                    Util.verifyFormatSuccess(repo, pullRequestJson);
                });
    }

    @Test
    void testAllChecksPass() throws IOException {
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - id: "Title"
                      title: "Title"
                      notify: [7125767235]
                    - id: "Description"
                      body: "issues.redhat.com"
                      notify: [3251142365]
                  format:
                    description:
                         regexes:
                           - pattern: "JIRA:\\\\s+https://issues.redhat.com/browse/WFLY-\\\\d+|https://issues.redhat.com/browse/WFLY-\\\\d+"
                             message: "The PR description must contain a link to the JIRA issue"
                """;
        pullRequestJson = PullRequestJson.builder(VALID_PR_TEMPLATE_JSON).build();

        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, pullRequestJson))
                .when().payloadFromString(pullRequestJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository(TEST_REPO);
                    Util.verifyFormatSuccess(repo, pullRequestJson);
                    GHPullRequest mockedPR = mocks.pullRequest(pullRequestJson.id());
                    Mockito.verify(mockedPR).comment("/cc @7125767235, @3251142365");
                });
    }

    @Test
    void testAllChecksFail() throws IOException {
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - id: "Title"
                      title: "Title"
                      notify: [7125767235]
                    - id: "Description"
                      body: "JIRA"
                      notify: [3251142365]
                  format:
                    description:
                         regexes:
                           - pattern: "JIRA:\\\\s+https://issues.redhat.com/browse/WFLY-\\\\d+|https://issues.redhat.com/browse/WFLY-\\\\d+"
                             message: "The PR description must contain a link to the JIRA issue"
                """;
        pullRequestJson = PullRequestJson.builder(VALID_PR_TEMPLATE_JSON)
                .title(INVALID_TITLE)
                .description(INVALID_DESCRIPTION)
                .build();
        mockedContext = MockedContext.builder(pullRequestJson.id())
                .commit(INVALID_COMMIT_MESSAGE);

        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, pullRequestJson, mockedContext))
                .when().payloadFromString(pullRequestJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository(TEST_REPO);
                    Util.verifyFormatFailure(repo, pullRequestJson, "commit, description, title");
                    GHPullRequest mockedPR = mocks.pullRequest(pullRequestJson.id());
                    Mockito.verify(mockedPR, Mockito.never()).comment("/cc @7125767235, @3251142365");
                });
    }
}
