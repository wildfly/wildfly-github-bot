package io.xstefank.wildfly.bot;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;
import io.xstefank.wildfly.bot.model.MockedGHPullRequestFileDetail;
import io.xstefank.wildfly.bot.model.RuntimeConstants;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHCommitState;
import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHPullRequestFileDetail;
import org.kohsuke.github.GHRepository;
import org.mockito.Mockito;

import java.io.IOException;

import static io.quarkiverse.githubapp.testing.GitHubAppTesting.given;
import static io.xstefank.wildfly.bot.helper.MockedGHPullRequestProcessor.mockEmptyComments;
import static io.xstefank.wildfly.bot.helper.MockedGHPullRequestProcessor.processPullRequestMock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@QuarkusTest
@GitHubAppTest
public class PRConfigFileChangeTest {

    @Test
    void testUpdateIncorrectlyIndentedFile() throws IOException {
        given().github(mocks -> {
                GHRepository repo = mocks.repository("The-Huginn/wildfly-github-bot");
                GHContent mockGHContent = mock(GHContent.class);
                when(repo.getFileContent(".github/" + RuntimeConstants.CONFIG_FILE_NAME, "d4cb8c40c8d1d568bd12126c6eb1f541e06fee4b")).thenReturn(mockGHContent);
                when(mockGHContent.read()).thenReturn(IOUtils.toInputStream("""
                        wildfly:
                          rules:
                            - title: "test"
                            - body: "test"
                               notify: [The-non-existing-user]
                          format:
                            description:
                              regexes:
                                - pattern: "\\\\s+https://"
                                  message: "The PR description must contain a link."
                            commits-quantity:
                              quantity: "1-3"
                              message: "Too many commits in PR!"
                          emails:
                            - foo@bar.baz
                            - address@email.com""",
                    "UTF-8"));

                mockPullRequestContents(mocks.pullRequest(1407732498));
            })
            .when().payloadFromClasspath("/pr-config-file.json")
            .event(GHEvent.PULL_REQUEST)
            .then().github(mocks -> {
                verify(mocks.pullRequest(1407732498)).listFiles();
                GHRepository repo = mocks.repository("The-Huginn/wildfly-github-bot");
                verify(repo).getFileContent(".github/" + RuntimeConstants.CONFIG_FILE_NAME, "d4cb8c40c8d1d568bd12126c6eb1f541e06fee4b");
                Mockito.verify(repo).createCommitStatus("d4cb8c40c8d1d568bd12126c6eb1f541e06fee4b",
                    GHCommitState.ERROR, "", "Unable to parse the configuration file. " +
                        "Make sure it can be loaded to model at https://github.com/xstefank/wildfly-github-bot/blob/main/CONFIGURATION.yml",
                    "Configuration File");

                verifyNoMoreInteractions(mocks.pullRequest(1407732498));
            });
    }

    @Test
    void testUpdateToCorrectFile() throws IOException {
        given().github(mocks -> {
                GHRepository repo = mocks.repository("The-Huginn/wildfly-github-bot");
                GHContent mockGHContent = mock(GHContent.class);
                when(repo.getFileContent(".github/" + RuntimeConstants.CONFIG_FILE_NAME, "d4cb8c40c8d1d568bd12126c6eb1f541e06fee4b")).thenReturn(mockGHContent);
                when(mockGHContent.read()).thenReturn(IOUtils.toInputStream("""
                        wildfly:
                          rules:
                            - title: "test"
                              id: "some test id"
                            - id: "another great id"
                              body: "test"
                              notify: [The-non-existing-user]
                          format:
                            title-check:
                              pattern: "\\\\[TITLE-\\\\d+\\\\]\\\\s+.*|TITLE-\\\\d+\\\\s+.*"
                              message: "Wrong content of the title"
                            description:
                              regexes:
                                - pattern: "\\\\s+https://"
                                  message: "The PR description must contain a link."
                            commits-quantity:
                              quantity: "1-3"
                              message: "Too many commits in PR!"
                          emails:
                            - foo@bar.baz
                            - address@email.com""",
                    "UTF-8"));

                mockPullRequestContents(mocks.pullRequest(1407732498));
            })
            .when().payloadFromClasspath("/pr-config-file.json")
            .event(GHEvent.PULL_REQUEST)
            .then().github(mocks -> {
                verify(mocks.pullRequest(1407732498)).listFiles();
                GHRepository repo = mocks.repository("The-Huginn/wildfly-github-bot");
                verify(repo).getFileContent(".github/" + RuntimeConstants.CONFIG_FILE_NAME, "d4cb8c40c8d1d568bd12126c6eb1f541e06fee4b");
                Mockito.verify(repo).createCommitStatus("d4cb8c40c8d1d568bd12126c6eb1f541e06fee4b",
                    GHCommitState.SUCCESS, "", "Valid", "Configuration File");

                verifyNoMoreInteractions(mocks.pullRequest(1407732498));
            });
    }

    private static void mockPullRequestContents(GHPullRequest pullRequestMock) throws IOException {
        GHPullRequestFileDetail[] fileDetails = {
            new MockedGHPullRequestFileDetail("62ca1d70d69efbf5ab79d46512292c29df72a9b1",
                ".github/wildfly-bot.yml", "modified", 3, 2, 5,
                "https://github.com/The-Huginn/wildfly-github-bot/blob/b167e4aa848bcdd4b3dacdd6edb4032bf045b9df/.github%2Fwildfly-bot.yml",
                "https://raw.githubusercontent.com/The-Huginn/wildfly-github-bot/b167e4aa848bcdd4b3dacdd6edb4032bf045b9df/.github/wildfly-bot.yml",
                "https://api.github.com/repos/The-Huginn/wildfly-github-bot/contents/.github%2Fwildfly-bot.yml?ref=b167e4aa848bcdd4b3dacdd6edb4032bf045b9df",
                ".. omitted ..",
                null)
        };

        processPullRequestMock(pullRequestMock, fileDetails, mockEmptyComments());
    }
}
