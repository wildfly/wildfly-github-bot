package io.xstefank.wildfly.bot;

import io.quarkiverse.githubapp.testing.GitHubAppMockito;
import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;
import io.xstefank.wildfly.bot.config.RuntimeConstants;
import io.xstefank.wildfly.bot.model.MockedGHPullRequestFileDetail;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHCommitState;
import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GHPullRequestFileDetail;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.PagedSearchIterable;
import org.mockito.Mockito;

import java.io.IOException;

import static io.quarkiverse.githubapp.testing.GitHubAppTesting.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@QuarkusTest
@GitHubAppTest
public class PRConfigFileChangeTest {

    @Test
    void testUpdateToIncorrectFile() throws IOException {
        given().github(mocks -> {
            GHRepository repo = mocks.repository("The-Huginn/wildfly-github-bot");
            GHContent mockGHContent = mock(GHContent.class);
            when(repo.getFileContent(".github/" + RuntimeConstants.CONFIG_FILE_NAME, "d4cb8c40c8d1d568bd12126c6eb1f541e06fee4b")).thenReturn(mockGHContent);
            when(mockGHContent.read()).thenReturn(IOUtils.toInputStream( "wildfly:\n" +
                    "  rules:\n" +
                    "    - title: \"test\"\n" +
                    "    - body: \"test\"\n" +
                    "      notify: [The-non-existing-user]\n" +
                    "  format:\n" +
                    "    title-check:\n" +
                    "      pattern: \"\\\\[TITLE-\\\\d+\\\\]\\\\s+.*|TITLE-\\\\d+\\\\s+.*\"\n" +
                    "      message: \"Wrong content of the title!\"\n" +
                    "    description:\n" +
                    "      regexes:\n" +
                    "        - pattern: \"\\\\s+https://\"\n" +
                    "          message: \"The PR description must contain a link.\"\n" +
                    "    commits-quantity:\n" +
                    "      quantity: \"1-3\"\n" +
                    "      message: \"Too many commits in PR!\"\n" +
                    "  emails:\n" +
                    "    - foo@bar.baz\n" +
                    "    - address@email.com",
                    "UTF-8"));

                    PagedSearchIterable<GHPullRequestFileDetail> fileDetails = GitHubAppMockito.mockPagedIterable(
                            new GHPullRequestFileDetail[]{
                                    new MockedGHPullRequestFileDetail("62ca1d70d69efbf5ab79d46512292c29df72a9b1",
                                            ".github/wildfly-bot.yml", "modified", 3, 2, 5,
                                            "https://github.com/The-Huginn/wildfly-github-bot/blob/b167e4aa848bcdd4b3dacdd6edb4032bf045b9df/.github%2Fwildfly-bot.yml",
                                            "https://raw.githubusercontent.com/The-Huginn/wildfly-github-bot/b167e4aa848bcdd4b3dacdd6edb4032bf045b9df/.github/wildfly-bot.yml",
                                            "https://api.github.com/repos/The-Huginn/wildfly-github-bot/contents/.github%2Fwildfly-bot.yml?ref=b167e4aa848bcdd4b3dacdd6edb4032bf045b9df",
                                            ".. omitted ..",
                                            null)
                            }
                    );
                    Mockito.when(mocks.pullRequest(1407732498).listFiles()).thenReturn(fileDetails);

                })
                .when().payloadFromClasspath("/pr-config-file.json")
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    verify(mocks.pullRequest(1407732498)).listFiles();
                    GHRepository repo = mocks.repository("The-Huginn/wildfly-github-bot");
                    verify(repo).getFileContent(".github/" + RuntimeConstants.CONFIG_FILE_NAME, "d4cb8c40c8d1d568bd12126c6eb1f541e06fee4b");
                    Mockito.verify(repo).createCommitStatus("d4cb8c40c8d1d568bd12126c6eb1f541e06fee4b",
                            GHCommitState.ERROR, "", "\u274C Invalid rule: id=null title=test body=null titleBody=null, Invalid rule: id=null title=null body=test titleBody=null", "Configuration File");

                    verifyNoMoreInteractions(mocks.pullRequest(1407732498));
                });
    }

    @Test
    void testUpdateToCorrectFile() throws IOException {
        given().github(mocks -> {
                    GHRepository repo = mocks.repository("The-Huginn/wildfly-github-bot");
                    GHContent mockGHContent = mock(GHContent.class);
                    when(repo.getFileContent(".github/" + RuntimeConstants.CONFIG_FILE_NAME, "d4cb8c40c8d1d568bd12126c6eb1f541e06fee4b")).thenReturn(mockGHContent);
                    when(mockGHContent.read()).thenReturn(IOUtils.toInputStream( "wildfly:\n" +
                                    "  rules:\n" +
                                    "    - title: \"test\"\n" +
                                    "      id: \"some test id\"\n" +
                                    "    - id: \"another great id\"\n" +
                                    "      body: \"test\"\n" +
                                    "      notify: [The-non-existing-user]\n" +
                                    "  format:\n" +
                                    "    title-check:\n" +
                                    "      pattern: \"\\\\[TITLE-\\\\d+\\\\]\\\\s+.*|TITLE-\\\\d+\\\\s+.*\"\n" +
                                    "      message: \"Wrong content of the title!\"\n" +
                                    "    description:\n" +
                                    "      regexes:\n" +
                                    "        - pattern: \"\\\\s+https://\"\n" +
                                    "          message: \"The PR description must contain a link.\"\n" +
                                    "    commits-quantity:\n" +
                                    "      quantity: \"1-3\"\n" +
                                    "      message: \"Too many commits in PR!\"\n" +
                                    "  emails:\n" +
                                    "    - foo@bar.baz\n" +
                                    "    - address@email.com",
                            "UTF-8"));

                    PagedSearchIterable<GHPullRequestFileDetail> fileDetails = GitHubAppMockito.mockPagedIterable(
                            new GHPullRequestFileDetail[]{
                                    new MockedGHPullRequestFileDetail("62ca1d70d69efbf5ab79d46512292c29df72a9b1",
                                            ".github/wildfly-bot.yml", "modified", 3, 2, 5,
                                            "https://github.com/The-Huginn/wildfly-github-bot/blob/b167e4aa848bcdd4b3dacdd6edb4032bf045b9df/.github%2Fwildfly-bot.yml",
                                            "https://raw.githubusercontent.com/The-Huginn/wildfly-github-bot/b167e4aa848bcdd4b3dacdd6edb4032bf045b9df/.github/wildfly-bot.yml",
                                            "https://api.github.com/repos/The-Huginn/wildfly-github-bot/contents/.github%2Fwildfly-bot.yml?ref=b167e4aa848bcdd4b3dacdd6edb4032bf045b9df",
                                            ".. omitted ..",
                                            null)
                            }
                    );
                    Mockito.when(mocks.pullRequest(1407732498).listFiles()).thenReturn(fileDetails);

                })
                .when().payloadFromClasspath("/pr-config-file.json")
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    verify(mocks.pullRequest(1407732498)).listFiles();
                    GHRepository repo = mocks.repository("The-Huginn/wildfly-github-bot");
                    verify(repo).getFileContent(".github/" + RuntimeConstants.CONFIG_FILE_NAME, "d4cb8c40c8d1d568bd12126c6eb1f541e06fee4b");
                    Mockito.verify(repo).createCommitStatus("d4cb8c40c8d1d568bd12126c6eb1f541e06fee4b",
                            GHCommitState.SUCCESS, "", "\u2705", "Configuration File");

                    verifyNoMoreInteractions(mocks.pullRequest(1407732498));
                });
    }
}
