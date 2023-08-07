package io.xstefank.wildfly.bot;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;
import io.xstefank.wildfly.bot.model.RuntimeConstants;
import io.xstefank.wildfly.bot.utils.GitHubJson;
import io.xstefank.wildfly.bot.utils.MockedContext;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHCommitState;
import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GHRepository;
import org.mockito.Mockito;

import java.io.IOException;

import static io.quarkiverse.githubapp.testing.GitHubAppTesting.given;
import static io.xstefank.wildfly.bot.utils.TestConstants.TEST_REPO;
import static io.xstefank.wildfly.bot.utils.TestConstants.VALID_PR_TEMPLATE_JSON;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests for PRs changing the configuration file.
 */
@QuarkusTest
@GitHubAppTest
public class PRConfigFileChangeTest {

    private static GitHubJson gitHubJson;

    @BeforeAll
    static void setUpGitHubJson() throws IOException {
        gitHubJson = GitHubJson.builder(VALID_PR_TEMPLATE_JSON).build();
    }

    @Test
    void testUpdateIncorrectlyIndentedFile() throws IOException {
        given().github(mocks -> {
                GHRepository repo = mocks.repository(TEST_REPO);
                GHContent mockGHContent = mock(GHContent.class);
                when(repo.getFileContent(".github/" + RuntimeConstants.CONFIG_FILE_NAME, gitHubJson.commitSHA())).thenReturn(mockGHContent);
                when(mockGHContent.read()).thenReturn(IOUtils.toInputStream("""
                        wildfly:
                          rules:
                            - title: "test"
                            - body: "test"
                               notify: [The-non-existing-user]
                          emails:
                            - foo@bar.baz
                            - address@email.com""",
                    "UTF-8"));

                    MockedContext.builder(gitHubJson.id())
                            .prFiles(".github/wildfly-bot.yml")
                            .mock(mocks);
            })
            .when().payloadFromString(gitHubJson.jsonString())
            .event(GHEvent.PULL_REQUEST)
            .then().github(mocks -> {
                verify(mocks.pullRequest(gitHubJson.id())).listFiles();
                GHRepository repo = mocks.repository(TEST_REPO);
                verify(repo).getFileContent(".github/" + RuntimeConstants.CONFIG_FILE_NAME, gitHubJson.commitSHA());
                Mockito.verify(repo).createCommitStatus(gitHubJson.commitSHA(),
                    GHCommitState.ERROR, "", "Unable to parse the configuration file. " +
                        "Make sure it can be loaded to model at https://github.com/xstefank/wildfly-github-bot/blob/main/CONFIGURATION.yml",
                    "Configuration File");

                verifyNoMoreInteractions(mocks.pullRequest(gitHubJson.id()));
            });
    }

    @Test
    void testUpdateToCorrectFile() throws IOException {
        given().github(mocks -> {
                GHRepository repo = mocks.repository(TEST_REPO);
                GHContent mockGHContent = mock(GHContent.class);
                when(repo.getFileContent(".github/" + RuntimeConstants.CONFIG_FILE_NAME, gitHubJson.commitSHA())).thenReturn(mockGHContent);
                when(mockGHContent.read()).thenReturn(IOUtils.toInputStream("""
                        wildfly:
                          rules:
                            - title: "test"
                              id: "some test id"
                            - id: "another great id"
                              body: "test"
                              notify: [The-non-existing-user]
                          emails:
                            - foo@bar.baz
                            - address@email.com""",
                    "UTF-8"));

                MockedContext.builder(gitHubJson.id())
                        .prFiles(".github/wildfly-bot.yml")
                        .mock(mocks);
            })
            .when().payloadFromString(gitHubJson.jsonString())
            .event(GHEvent.PULL_REQUEST)
            .then().github(mocks -> {
                verify(mocks.pullRequest(gitHubJson.id())).listFiles();
                GHRepository repo = mocks.repository(TEST_REPO);
                verify(repo).getFileContent(".github/" + RuntimeConstants.CONFIG_FILE_NAME, gitHubJson.commitSHA());
                Mockito.verify(repo).createCommitStatus(gitHubJson.commitSHA(),
                    GHCommitState.SUCCESS, "", "Valid", "Configuration File");

                verifyNoMoreInteractions(mocks.pullRequest(gitHubJson.id()));
            });
    }
}
