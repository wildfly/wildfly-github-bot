package org.wildfly.bot.webhooks;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHPerson;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHUser;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.wildfly.bot.utils.WildflyGitHubBotTesting;
import org.wildfly.bot.utils.mocking.Mockable;
import org.wildfly.bot.utils.mocking.MockedGHPullRequest;
import org.wildfly.bot.utils.mocking.MockedGHRepository;
import org.wildfly.bot.utils.testing.PullRequestJson;
import org.wildfly.bot.utils.testing.internal.TestModel;

import java.util.List;

/**
 * Tests for the WildFly -> Rules -> Directories checks.
 */
@QuarkusTest
@GitHubAppTest
public class PRRuleDirectoryHitTest {

    private static String wildflyConfigFile;
    private static PullRequestJson pullRequestJson;
    private Mockable mockedContext;

    @BeforeAll
    static void setPullRequestJson() throws Exception {
        pullRequestJson = TestModel.defaultBeforeEachJsons();
    }

    @Test
    void testDirectoriesNotifyNewFileInDiff() throws Throwable {
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - id: "Directory Test"
                      directories:
                       - appclient
                      notify: [Tadpole]
                  format:
                    commit:
                      enabled: false
                    title:
                      enabled: false
                """;
        mockedContext = MockedGHPullRequest.builder(pullRequestJson.id())
                .files("appclient/test.txt",
                        "microprofile/health-smallrye/pom.xml",
                        "testsuite/integration/basic/pom.xml")
                .mockNext(MockedGHRepository.builder())
                .users("Tadpole");

        TestModel.given(
                mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson, mockedContext))
                .pullRequestEvent(pullRequestJson)
                .then(mocks -> {
                    GHPullRequest mockedPR = mocks.pullRequest(pullRequestJson.id());
                    Mockito.verify(mockedPR, Mockito.times(2)).listFiles();
                    Mockito.verify(mocks.pullRequest(pullRequestJson.id())).requestReviewers(ArgumentMatchers.anyList());
                    Mockito.verify(mockedPR, Mockito.times(2)).listComments();
                });
    }

    @Test
    void testDirectoriesNotifyChangeInSubdirectoryDiff() throws Throwable {
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - id: "Directory Test"
                      directories:
                       - microprofile/health-smallrye
                      notify: [Tadpole]
                """;
        mockedContext = MockedGHPullRequest.builder(pullRequestJson.id())
                .files("appclient/test.txt",
                        "microprofile/health-smallrye/pom.xml",
                        "testsuite/integration/basic/pom.xml")
                .mockNext(MockedGHRepository.builder())
                .users("Tadpole");

        TestModel.given(
                mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson, mockedContext))
                .pullRequestEvent(pullRequestJson)
                .then(mocks -> {
                    GHPullRequest mockedPR = mocks.pullRequest(pullRequestJson.id());
                    Mockito.verify(mockedPR, Mockito.times(2)).listFiles();
                    ArgumentCaptor<List<GHUser>> captor = ArgumentCaptor.forClass(List.class);
                    Mockito.verify(mocks.pullRequest(pullRequestJson.id())).requestReviewers(captor.capture());
                    Assertions.assertEquals(captor.getValue().size(), 1);
                    MatcherAssert.assertThat(captor.getValue().stream()
                            .map(GHPerson::getLogin)
                            .toList(), Matchers.containsInAnyOrder("Tadpole"));
                    Mockito.verify(mockedPR, Mockito.times(2)).listComments();
                });
    }

    @Test
    void testDirectoriesNotifyChangeInSubdirectoryOfConfiguredDirectoryDiff() throws Throwable {
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - id: "Directory Test"
                      directories:
                       - testsuite/integration
                      notify: [Tadpole]
                """;
        mockedContext = MockedGHPullRequest.builder(pullRequestJson.id())
                .files("appclient/test.txt",
                        "microprofile/health-smallrye/pom.xml",
                        "testsuite/integration/basic/pom.xml")
                .mockNext(MockedGHRepository.builder())
                .users("Tadpole");

        TestModel.given(
                mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson, mockedContext))
                .pullRequestEvent(pullRequestJson)
                .then(mocks -> {
                    GHPullRequest mockedPR = mocks.pullRequest(pullRequestJson.id());
                    Mockito.verify(mockedPR, Mockito.times(2)).listFiles();
                    ArgumentCaptor<List<GHUser>> captor = ArgumentCaptor.forClass(List.class);
                    Mockito.verify(mocks.pullRequest(pullRequestJson.id())).requestReviewers(captor.capture());
                    Assertions.assertEquals(captor.getValue().size(), 1);
                    MatcherAssert.assertThat(captor.getValue().stream()
                            .map(GHPerson::getLogin)
                            .toList(), Matchers.containsInAnyOrder("Tadpole"));
                    Mockito.verify(mockedPR, Mockito.times(2)).listComments();
                });
    }

    @Test
    void testDirectoriesNotifyNoHitInDiff() throws Throwable {
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - id: "Directory Test"
                      directories:
                       - transactions
                      notify: [Tadpole]
                """;
        mockedContext = MockedGHPullRequest.builder(pullRequestJson.id())
                .files("appclient/test.txt",
                        "microprofile/health-smallrye/pom.xml",
                        "testsuite/integration/basic/pom.xml");

        TestModel.given(
                mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson, mockedContext))
                .pullRequestEvent(pullRequestJson)
                .then(mocks -> {
                    GHPullRequest mockedPR = mocks.pullRequest(pullRequestJson.id());
                    Mockito.verify(mockedPR, Mockito.times(2)).listFiles();
                    Mockito.verify(mocks.pullRequest(pullRequestJson.id()), Mockito.never())
                            .requestReviewers(ArgumentMatchers.any());
                    Mockito.verify(mockedPR, Mockito.times(2)).listComments();
                });
    }

    @Test
    void testDirectoriesNoHitInDiffIfSubstring() throws Throwable {
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - id: "Directory Test"
                      directories:
                       - app
                      notify: [Tadpole]
                """;
        mockedContext = MockedGHPullRequest.builder(pullRequestJson.id())
                .files("appclient/test.txt",
                        "microprofile/health-smallrye/pom.xml",
                        "testsuite/integration/basic/pom.xml");

        TestModel.given(
                mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson, mockedContext))
                .pullRequestEvent(pullRequestJson)
                .then(mocks -> {
                    GHPullRequest mockedPR = mocks.pullRequest(pullRequestJson.id());
                    Mockito.verify(mockedPR, Mockito.times(2)).listFiles();
                    Mockito.verify(mockedPR, Mockito.times(2)).listComments();
                    Mockito.verify(mockedPR, Mockito.never()).comment("/cc @Tadpole [Directory Test]");
                });
    }
}
