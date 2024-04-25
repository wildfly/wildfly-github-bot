package org.wildfly.bot.webhooks;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GHPerson;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHUser;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.wildfly.bot.utils.TestConstants;
import org.wildfly.bot.utils.WildflyGitHubBotTesting;
import org.wildfly.bot.utils.mocking.Mockable;
import org.wildfly.bot.utils.mocking.MockedGHPullRequest;
import org.wildfly.bot.utils.mocking.MockedGHRepository;
import org.wildfly.bot.utils.model.SsePullRequestPayload;

import java.io.IOException;
import java.util.List;

import static io.quarkiverse.githubapp.testing.GitHubAppTesting.given;

/**
 * Tests for the Wildfly -> Rules -> Directories checks.
 */
@QuarkusTest
@GitHubAppTest
public class PRRuleDirectoryHitTest {

    private static String wildflyConfigFile;
    private static SsePullRequestPayload ssePullRequestPayload;
    private Mockable mockedContext;

    @BeforeAll
    static void setUpGitHubJson() throws IOException {
        ssePullRequestPayload = SsePullRequestPayload.builder(TestConstants.VALID_PR_TEMPLATE_JSON).build();
    }

    @Test
    void testDirectoriesNotifyNewFileInDiff() throws IOException {
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
        mockedContext = MockedGHPullRequest.builder(ssePullRequestPayload.id())
                .files("appclient/test.txt",
                        "microprofile/health-smallrye/pom.xml",
                        "testsuite/integration/basic/pom.xml")
                .mockNext(MockedGHRepository.builder())
                .users("Tadpole");

        given().github(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, ssePullRequestPayload, mockedContext))
                .when().payloadFromString(ssePullRequestPayload.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHPullRequest mockedPR = mocks.pullRequest(ssePullRequestPayload.id());
                    Mockito.verify(mockedPR, Mockito.times(2)).listFiles();
                    Mockito.verify(mocks.pullRequest(ssePullRequestPayload.id())).requestReviewers(ArgumentMatchers.anyList());
                    Mockito.verify(mockedPR, Mockito.times(2)).listComments();
                });
    }

    @Test
    void testDirectoriesNotifyChangeInSubdirectoryDiff() throws IOException {
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - id: "Directory Test"
                      directories:
                       - microprofile/health-smallrye
                      notify: [Tadpole]
                """;
        mockedContext = MockedGHPullRequest.builder(ssePullRequestPayload.id())
                .files("appclient/test.txt",
                        "microprofile/health-smallrye/pom.xml",
                        "testsuite/integration/basic/pom.xml")
                .mockNext(MockedGHRepository.builder())
                .users("Tadpole");

        given().github(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, ssePullRequestPayload, mockedContext))
                .when().payloadFromString(ssePullRequestPayload.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHPullRequest mockedPR = mocks.pullRequest(ssePullRequestPayload.id());
                    Mockito.verify(mockedPR, Mockito.times(2)).listFiles();
                    ArgumentCaptor<List<GHUser>> captor = ArgumentCaptor.forClass(List.class);
                    Mockito.verify(mocks.pullRequest(ssePullRequestPayload.id())).requestReviewers(captor.capture());
                    Assertions.assertEquals(captor.getValue().size(), 1);
                    MatcherAssert.assertThat(captor.getValue().stream()
                            .map(GHPerson::getLogin)
                            .toList(), Matchers.containsInAnyOrder("Tadpole"));
                    Mockito.verify(mockedPR, Mockito.times(2)).listComments();
                });
    }

    @Test
    void testDirectoriesNotifyChangeInSubdirectoryOfConfiguredDirectoryDiff() throws IOException {
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - id: "Directory Test"
                      directories:
                       - testsuite/integration
                      notify: [Tadpole]
                """;
        mockedContext = MockedGHPullRequest.builder(ssePullRequestPayload.id())
                .files("appclient/test.txt",
                        "microprofile/health-smallrye/pom.xml",
                        "testsuite/integration/basic/pom.xml")
                .mockNext(MockedGHRepository.builder())
                .users("Tadpole");

        given().github(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, ssePullRequestPayload, mockedContext))
                .when().payloadFromString(ssePullRequestPayload.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHPullRequest mockedPR = mocks.pullRequest(ssePullRequestPayload.id());
                    Mockito.verify(mockedPR, Mockito.times(2)).listFiles();
                    ArgumentCaptor<List<GHUser>> captor = ArgumentCaptor.forClass(List.class);
                    Mockito.verify(mocks.pullRequest(ssePullRequestPayload.id())).requestReviewers(captor.capture());
                    Assertions.assertEquals(captor.getValue().size(), 1);
                    MatcherAssert.assertThat(captor.getValue().stream()
                            .map(GHPerson::getLogin)
                            .toList(), Matchers.containsInAnyOrder("Tadpole"));
                    Mockito.verify(mockedPR, Mockito.times(2)).listComments();
                });
    }

    @Test
    void testDirectoriesNotifyNoHitInDiff() throws IOException {
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - id: "Directory Test"
                      directories:
                       - transactions
                      notify: [Tadpole]
                """;
        mockedContext = MockedGHPullRequest.builder(ssePullRequestPayload.id())
                .files("appclient/test.txt",
                        "microprofile/health-smallrye/pom.xml",
                        "testsuite/integration/basic/pom.xml");

        given().github(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, ssePullRequestPayload, mockedContext))
                .when().payloadFromString(ssePullRequestPayload.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHPullRequest mockedPR = mocks.pullRequest(ssePullRequestPayload.id());
                    Mockito.verify(mockedPR, Mockito.times(2)).listFiles();
                    Mockito.verify(mocks.pullRequest(ssePullRequestPayload.id()), Mockito.never())
                            .requestReviewers(ArgumentMatchers.any());
                    Mockito.verify(mockedPR, Mockito.times(2)).listComments();
                });
    }

    @Test
    void testDirectoriesNoHitInDiffIfSubstring() throws IOException {
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - id: "Directory Test"
                      directories:
                       - app
                      notify: [Tadpole]
                """;
        mockedContext = MockedGHPullRequest.builder(ssePullRequestPayload.id())
                .files("appclient/test.txt",
                        "microprofile/health-smallrye/pom.xml",
                        "testsuite/integration/basic/pom.xml");

        given().github(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, ssePullRequestPayload, mockedContext))
                .when().payloadFromString(ssePullRequestPayload.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHPullRequest mockedPR = mocks.pullRequest(ssePullRequestPayload.id());
                    Mockito.verify(mockedPR, Mockito.times(2)).listFiles();
                    Mockito.verify(mockedPR, Mockito.times(2)).listComments();
                    Mockito.verify(mockedPR, Mockito.never()).comment("/cc @Tadpole");
                });
    }
}
