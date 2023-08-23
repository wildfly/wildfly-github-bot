package io.xstefank.wildfly.bot;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;
import io.xstefank.wildfly.bot.utils.GitHubJson;
import io.xstefank.wildfly.bot.utils.MockedContext;
import io.xstefank.wildfly.bot.utils.Util;
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

import java.io.IOException;
import java.util.List;

import static io.quarkiverse.githubapp.testing.GitHubAppTesting.given;
import static io.xstefank.wildfly.bot.utils.TestConstants.VALID_PR_TEMPLATE_JSON;

/**
 * Tests for the Wildfly -> Rules -> Directories checks.
 */
@QuarkusTest
@GitHubAppTest
public class PRRuleDirectoryHitTest {

    private static String wildflyConfigFile;
    private static GitHubJson gitHubJson;
    private MockedContext mockedContext;

    @BeforeAll
    static void setUpGitHubJson() throws IOException {
        gitHubJson = GitHubJson.builder(VALID_PR_TEMPLATE_JSON).build();
    }

    @Test
    void testDirectoriesNotifyNewFileInDiff() throws IOException {
        wildflyConfigFile = """
            wildfly:
              rules:
                - id: "Directory Test"
                  directories:
                   - appclient
                  notify: [7125767235]
              format:
                commit:
                  enabled: false
                title:
                  enabled: false
            """;
        mockedContext = MockedContext.builder(gitHubJson.id())
                .collaborators("7125767235")
                .prFiles("appclient/test.txt",
                        "microprofile/health-smallrye/pom.xml",
                        "testsuite/integration/basic/pom.xml");

        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, gitHubJson, mockedContext))
                .when().payloadFromString(gitHubJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHPullRequest mockedPR = mocks.pullRequest(gitHubJson.id());
                    Mockito.verify(mockedPR, Mockito.times(2)).listFiles();
                    Mockito.verify(mocks.pullRequest(gitHubJson.id())).requestReviewers(ArgumentMatchers.anyList());
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
                  notify: [7125767235]
            """;
        mockedContext = MockedContext.builder(gitHubJson.id())
                .collaborators("7125767235")
                .prFiles("appclient/test.txt",
                        "microprofile/health-smallrye/pom.xml",
                        "testsuite/integration/basic/pom.xml");

        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, gitHubJson, mockedContext))
                .when().payloadFromString(gitHubJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHPullRequest mockedPR = mocks.pullRequest(gitHubJson.id());
                    Mockito.verify(mockedPR, Mockito.times(2)).listFiles();
                    ArgumentCaptor<List<GHUser>> captor = ArgumentCaptor.forClass(List.class);
                    Mockito.verify(mocks.pullRequest(gitHubJson.id())).requestReviewers(captor.capture());
                    Assertions.assertEquals(captor.getValue().size(), 1);
                    MatcherAssert.assertThat(captor.getValue().stream()
                            .map(GHPerson::getLogin)
                            .toList(), Matchers.containsInAnyOrder("7125767235"));
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
                  notify: [7125767235]
            """;
        mockedContext = MockedContext.builder(gitHubJson.id())
                .collaborators("7125767235")
                .prFiles("appclient/test.txt",
                        "microprofile/health-smallrye/pom.xml",
                        "testsuite/integration/basic/pom.xml");

        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, gitHubJson, mockedContext))
                .when().payloadFromString(gitHubJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHPullRequest mockedPR = mocks.pullRequest(gitHubJson.id());
                    Mockito.verify(mockedPR, Mockito.times(2)).listFiles();
                    ArgumentCaptor<List<GHUser>> captor = ArgumentCaptor.forClass(List.class);
                    Mockito.verify(mocks.pullRequest(gitHubJson.id())).requestReviewers(captor.capture());
                    Assertions.assertEquals(captor.getValue().size(), 1);
                    MatcherAssert.assertThat(captor.getValue().stream()
                            .map(GHPerson::getLogin)
                            .toList(), Matchers.containsInAnyOrder("7125767235"));
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
                  notify: [7125767235]
            """;
        mockedContext = MockedContext.builder(gitHubJson.id())
                .prFiles("appclient/test.txt",
                        "microprofile/health-smallrye/pom.xml",
                        "testsuite/integration/basic/pom.xml");

        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, gitHubJson, mockedContext))
                .when().payloadFromString(gitHubJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHPullRequest mockedPR = mocks.pullRequest(gitHubJson.id());
                    Mockito.verify(mockedPR, Mockito.times(2)).listFiles();
                    Mockito.verify(mocks.pullRequest(gitHubJson.id()), Mockito.never()).requestReviewers(ArgumentMatchers.any());
                    Mockito.verify(mockedPR, Mockito.times(2)).listComments();
                });
    }
}
