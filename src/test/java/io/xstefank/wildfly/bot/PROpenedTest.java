package io.xstefank.wildfly.bot;

import io.quarkiverse.githubapp.testing.GitHubAppMockito;
import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHCommitState;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GHPullRequestFileDetail;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.PagedSearchIterable;
import org.mockito.Mockito;

import java.io.IOException;

import static io.quarkiverse.githubapp.testing.GitHubAppTesting.given;
import static io.xstefank.wildfly.bot.helper.MockedGHPullRequestFileDetailProcessor.mockEmptyFileDetails;
import static io.xstefank.wildfly.bot.helper.MockedGHPullRequestFileDetailProcessor.mockFileDetails;

@QuarkusTest
@GitHubAppTest
public class PROpenedTest {

    @Test
    void testMentionsCCComment() throws IOException {
        given().github(mocks -> {
                    mocks.configFileFromString(
                            "wildfly-bot.yml", """
                                    wildfly:
                                      rules:
                                        - id: "Test"
                                          title: "Test"
                                          notify: [7125767235,0979986727]
                                    """);

                    PagedSearchIterable<GHPullRequestFileDetail> fileDetails = GitHubAppMockito.mockPagedIterable(mockEmptyFileDetails());
                    Mockito.when(mocks.pullRequest(1371642823).listFiles()).thenReturn(fileDetails);
                })
            .when().payloadFromClasspath("/pr-opened.json")
            .event(GHEvent.PULL_REQUEST)
            .then().github(mocks -> {
                Mockito.verify(mocks.pullRequest(1371642823))
                    .comment("/cc @0979986727, @7125767235");
                GHRepository repo = mocks.repository("xstefank/wildfly");
                Mockito.verify(repo).createCommitStatus("5db0f8e923d84fe05a60658ed5bb95f7aa23b66f",
                        GHCommitState.ERROR, "", "title-check: Wrong content of the title!", "Format");
                Mockito.verify(mocks.pullRequest(1371642823)).listFiles();
                Mockito.verifyNoMoreInteractions(mocks.ghObjects());
            });
    }

    @Test
    void testTitleBodyCheckForTitle() throws IOException {
        given().github(mocks -> {
                    mocks.configFileFromString(
                            "wildfly-bot.yml", """
                                    wildfly:
                                      rules:
                                        - id: "Hello Test"
                                          title: "Hello"
                                          body: "there"
                                          titleBody: "test"
                                          notify: [7125767235]
                                    """);
                    PagedSearchIterable<GHPullRequestFileDetail> fileDetails = GitHubAppMockito.mockPagedIterable(mockEmptyFileDetails());
                    Mockito.when(mocks.pullRequest(1371642823).listFiles()).thenReturn(fileDetails);
                })
            .when().payloadFromClasspath("/pr-opened.json")
            .event(GHEvent.PULL_REQUEST)
            .then().github(mocks -> {
                Mockito.verify(mocks.pullRequest(1371642823))
                    .comment("/cc @7125767235");
                GHRepository repo = mocks.repository("xstefank/wildfly");
                Mockito.verify(repo).createCommitStatus("5db0f8e923d84fe05a60658ed5bb95f7aa23b66f",
                        GHCommitState.SUCCESS, "", "Valid", "Format");
                Mockito.verify(mocks.pullRequest(1371642823)).listFiles();
                Mockito.verifyNoMoreInteractions(mocks.ghObjects());
            });
    }

    @Test
    void testTitleBodyCheckForBody() throws IOException {
        given().github(mocks -> {
                    mocks.configFileFromString(
                            "wildfly-bot.yml", """
                                    wildfly:
                                      rules:
                                        - id: "Hello Test"
                                          title: "Hello"
                                          body: "there"
                                          titleBody: "foobar"
                                          notify: [7125767235]
                                    """);

                    PagedSearchIterable<GHPullRequestFileDetail> fileDetails = GitHubAppMockito.mockPagedIterable(mockEmptyFileDetails());
                    Mockito.when(mocks.pullRequest(1371642823).listFiles()).thenReturn(fileDetails);
                })
            .when().payloadFromClasspath("/pr-opened.json")
            .event(GHEvent.PULL_REQUEST)
            .then().github(mocks -> {
                Mockito.verify(mocks.pullRequest(1371642823))
                    .comment("/cc @7125767235");
                GHRepository repo = mocks.repository("xstefank/wildfly");
                Mockito.verify(repo).createCommitStatus("5db0f8e923d84fe05a60658ed5bb95f7aa23b66f",
                        GHCommitState.SUCCESS, "", "Valid", "Format");
                Mockito.verify(mocks.pullRequest(1371642823)).listFiles();
                Mockito.verifyNoMoreInteractions(mocks.ghObjects());
            });
    }

    @Test
    void testTitleBodyCheckForTitleBodyCaseInsensitive() throws IOException {
        given().github(mocks -> {
                    mocks.configFileFromString(
                            "wildfly-bot.yml", """
                                    wildfly:
                                      rules:
                                        - id: "Hello Test"
                                          titleBody: "FoObAr"
                                          notify: [7125767235]
                                    """);

                    PagedSearchIterable<GHPullRequestFileDetail> fileDetails = GitHubAppMockito.mockPagedIterable(mockEmptyFileDetails());
                    Mockito.when(mocks.pullRequest(1371642823).listFiles()).thenReturn(fileDetails);
                })
                .when().payloadFromClasspath("/pr-opened.json")
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    Mockito.verify(mocks.pullRequest(1371642823))
                            .comment("/cc @7125767235");
                });
    }

    @Test
    void testTitleBodyCheckForTitleCaseInsensitive() throws IOException {
        given().github(mocks -> {
                    mocks.configFileFromString(
                            "wildfly-bot.yml", """
                                    wildfly:
                                      rules:
                                        - id: "Hello Test"
                                          title: "TeSt"
                                          notify: [7125767235]
                                    """);

                    PagedSearchIterable<GHPullRequestFileDetail> fileDetails = GitHubAppMockito.mockPagedIterable(mockEmptyFileDetails());
                    Mockito.when(mocks.pullRequest(1371642823).listFiles()).thenReturn(fileDetails);
                })
                .when().payloadFromClasspath("/pr-opened.json")
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    Mockito.verify(mocks.pullRequest(1371642823))
                            .comment("/cc @7125767235");
                });
    }

    @Test
    void testTitleBodyCheckForBodyCaseInsensitive() throws IOException {
        given().github(mocks -> {
                    mocks.configFileFromString(
                            "wildfly-bot.yml", """
                                    wildfly:
                                      rules:
                                        - id: "Hello Test"
                                          body: "FoObAr"
                                          notify: [7125767235]
                                    """);

                    PagedSearchIterable<GHPullRequestFileDetail> fileDetails = GitHubAppMockito.mockPagedIterable(mockEmptyFileDetails());
                    Mockito.when(mocks.pullRequest(1371642823).listFiles()).thenReturn(fileDetails);
                })
                .when().payloadFromClasspath("/pr-opened.json")
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    Mockito.verify(mocks.pullRequest(1371642823))
                            .comment("/cc @7125767235");
                });
    }

    @Test
    void testFailedTitleBodyCheck() throws IOException {
        given().github(mocks -> {
                    mocks.configFileFromString(
                            "wildfly-bot.yml", """
                                    wildfly:
                                      rules:
                                        - id: "Hello Test"
                                          title: "Hello"
                                          body: "there"
                                          titleBody: "General Kenobi"
                                          notify: [7125767235]
                                    """);

                    PagedSearchIterable<GHPullRequestFileDetail> fileDetails = GitHubAppMockito.mockPagedIterable(mockEmptyFileDetails());
                    Mockito.when(mocks.pullRequest(1371642823).listFiles()).thenReturn(fileDetails);
                })
            .when().payloadFromClasspath("/pr-opened.json")
            .event(GHEvent.PULL_REQUEST)
            .then().github(mocks -> {
                Mockito.verify(mocks.pullRequest(1371642823), Mockito.never()).comment("/cc @7125767235");
                GHRepository repo = mocks.repository("xstefank/wildfly");
                Mockito.verify(repo).createCommitStatus("5db0f8e923d84fe05a60658ed5bb95f7aa23b66f",
                        GHCommitState.SUCCESS, "", "Valid", "Format");
                Mockito.verify(mocks.pullRequest(1371642823)).listFiles();
                Mockito.verifyNoMoreInteractions(mocks.ghObjects());
            });
    }

    @Test
    void testDirectoriesMentionsNewFileInDiff() throws IOException {
        given().github(mocks -> {
                mocks.configFileFromString(
                    "wildfly-bot.yml", """
                        wildfly:
                          rules:
                            - id: "Directory Test"
                              directories:
                               - appclient
                              notify: [7125767235]
                        """);

                PagedSearchIterable<GHPullRequestFileDetail> fileDetails = GitHubAppMockito.mockPagedIterable(mockFileDetails());
                Mockito.when(mocks.pullRequest(1371642823).listFiles()).thenReturn(fileDetails);

            })
            .when().payloadFromClasspath("/pr-opened.json")
            .event(GHEvent.PULL_REQUEST)
            .then().github(mocks -> {
                Mockito.verify(mocks.pullRequest(1371642823), Mockito.times(2)).listFiles();
                Mockito.verify(mocks.pullRequest(1371642823)).comment("/cc @7125767235");
                Mockito.verifyNoMoreInteractions(mocks.pullRequest(1371642823));
            });
    }

    @Test
    void testDirectoriesMentionsChangeInSubdirectoryDiff() throws IOException {
        given().github(mocks -> {
                mocks.configFileFromString(
                    "wildfly-bot.yml", """
                        wildfly:
                          rules:
                            - id: "Directory Test"
                              directories:
                               - microprofile/health-smallrye
                              notify: [7125767235]
                        """);

                PagedSearchIterable<GHPullRequestFileDetail> fileDetails = GitHubAppMockito.mockPagedIterable(mockFileDetails());
                Mockito.when(mocks.pullRequest(1371642823).listFiles()).thenReturn(fileDetails);

            })
            .when().payloadFromClasspath("/pr-opened.json")
            .event(GHEvent.PULL_REQUEST)
            .then().github(mocks -> {
                Mockito.verify(mocks.pullRequest(1371642823), Mockito.times(2)).listFiles();
                Mockito.verify(mocks.pullRequest(1371642823)).comment("/cc @7125767235");
                Mockito.verifyNoMoreInteractions(mocks.pullRequest(1371642823));
            });
    }

    @Test
    void testDirectoriesMentionsChangeInSubdirectoryOfConfiguredDirectoryDiff() throws IOException {
        given().github(mocks -> {
                mocks.configFileFromString(
                    "wildfly-bot.yml", """
                        wildfly:
                          rules:
                            - id: "Directory Test"
                              directories:
                               - testsuite/integration
                              notify: [7125767235]
                        """);

                PagedSearchIterable<GHPullRequestFileDetail> fileDetails = GitHubAppMockito.mockPagedIterable(mockFileDetails());
                Mockito.when(mocks.pullRequest(1371642823).listFiles()).thenReturn(fileDetails);

            })
            .when().payloadFromClasspath("/pr-opened.json")
            .event(GHEvent.PULL_REQUEST)
            .then().github(mocks -> {
                Mockito.verify(mocks.pullRequest(1371642823), Mockito.times(2)).listFiles();
                Mockito.verify(mocks.pullRequest(1371642823)).comment("/cc @7125767235");
                Mockito.verifyNoMoreInteractions(mocks.pullRequest(1371642823));
            });
    }

    @Test
    void testDirectoriesMentionsNoHitInDiff() throws IOException {
        given().github(mocks -> {
                mocks.configFileFromString(
                    "wildfly-bot.yml", """
                        wildfly:
                          rules:
                            - id: "Directory Test"
                              directories:
                               - transactions
                              notify: [7125767235]
                        """);

                PagedSearchIterable<GHPullRequestFileDetail> fileDetails = GitHubAppMockito.mockPagedIterable(mockFileDetails());
                Mockito.when(mocks.pullRequest(1371642823).listFiles()).thenReturn(fileDetails);

            })
            .when().payloadFromClasspath("/pr-opened.json")
            .event(GHEvent.PULL_REQUEST)
            .then().github(mocks -> {
                Mockito.verify(mocks.pullRequest(1371642823), Mockito.times(2)).listFiles();
                Mockito.verifyNoMoreInteractions(mocks.pullRequest(1371642823));
            });
    }

    @Test
    void testPullRequestFormatTitleCheckOnOpen() throws IOException {
        given().github(mocks -> {
                    mocks.configFileFromString("wildfly-bot.yml",
                            """
                                    wildfly:
                                      rules:
                                        - title: "Hello"
                                        - body: "there"
                                          notify: [7125767235]
                                      format:
                                        title-check:
                                          pattern: "\\\\[WFLY-\\\\d+\\\\]\\\\s+.*|WFLY-\\\\d+\\\\s+.*"
                                          message: "Wrong content of the title!"
                                    """);

                    PagedSearchIterable<GHPullRequestFileDetail> fileDetails = GitHubAppMockito.mockPagedIterable(mockEmptyFileDetails());
                    Mockito.when(mocks.pullRequest(1371642823).listFiles()).thenReturn(fileDetails);
                })
                .when().payloadFromClasspath("/pr-opened.json")
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository("xstefank/wildfly");
                    Mockito.verify(repo).createCommitStatus("5db0f8e923d84fe05a60658ed5bb95f7aa23b66f",
                            GHCommitState.ERROR, "", "title-check: Wrong content of the title!", "Format");
                });
    }
}
