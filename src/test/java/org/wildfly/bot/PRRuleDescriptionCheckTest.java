package org.wildfly.bot;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.mockito.Mockito;
import org.wildfly.bot.utils.TestConstants;
import org.wildfly.bot.utils.WildflyGitHubBotTesting;
import org.wildfly.bot.utils.testing.PullRequestJson;
import org.wildfly.bot.utils.testing.internal.TestModel;

/**
 * Tests for the WildFly -> Rules -> Body checks.
 */
@QuarkusTest
@GitHubAppTest
public class PRRuleDescriptionCheckTest {

    private static String wildflyConfigFile;
    private static PullRequestJson pullRequestJson;

    @BeforeAll
    static void setPullRequestJson() throws Exception {
        TestModel.defaultBeforeEachJsons();
    }

    @Test
    void testSuccessfulBodyCheck() throws Throwable {
        pullRequestJson = TestModel.getPullRequestJson();
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - id: "Description"
                      body: "redhat.atlassian.net"
                      notify: [Tadpole]
                """;

        TestModel.given(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson))
                .pullRequestEvent(pullRequestJson)
                .then(mocks -> {
                    GHPullRequest mockedPR = mocks.pullRequest(pullRequestJson.id());
                    Mockito.verify(mockedPR).comment("/cc @Tadpole [Description]");
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    WildflyGitHubBotTesting.verifyFormatSuccess(repo, pullRequestJson);
                });
    }

    @Test
    void testFailedBodyCheck() throws Throwable {
        pullRequestJson = TestModel.setPullRequestJsonBuilder(
                pullRequestJsonBuilder -> pullRequestJsonBuilder.description(TestConstants.INVALID_DESCRIPTION));
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - id: "Description"
                      body: "redhat.atlassian.net"
                      notify: [Tadpole]
                """;

        TestModel.given(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson))
                .pullRequestEvent(pullRequestJson)
                .then(mocks -> {
                    GHPullRequest mockedPR = mocks.pullRequest(pullRequestJson.id());
                    Mockito.verify(mockedPR, Mockito.never()).comment("/cc @Tadpole [Description]");
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    WildflyGitHubBotTesting.verifyFormatSuccess(repo, pullRequestJson);
                });
    }

    @Test
    void testTitleBodyCheckForBodyCaseInsensitive() throws Throwable {
        pullRequestJson = TestModel.getPullRequestJson();
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - id: "Description"
                      body: "REDHAT.ATLASSIAN.NET"
                      notify: [Tadpole]
                """;

        TestModel.given(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson))
                .pullRequestEvent(pullRequestJson)
                .then(mocks -> {
                    GHPullRequest mockedPR = mocks.pullRequest(pullRequestJson.id());
                    Mockito.verify(mockedPR).comment("/cc @Tadpole [Description]");
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    WildflyGitHubBotTesting.verifyFormatSuccess(repo, pullRequestJson);
                });
    }
}
