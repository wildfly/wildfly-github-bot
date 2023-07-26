package io.xstefank.wildfly.bot;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;
import io.xstefank.wildfly.bot.helper.MockedGHPullRequestProcessor;
import io.xstefank.wildfly.bot.model.MockedGHPullRequestFileDetail;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHCommitState;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHPullRequestFileDetail;
import org.kohsuke.github.GHRepository;
import org.mockito.Mockito;

import java.io.IOException;

import static io.quarkiverse.githubapp.testing.GitHubAppTesting.given;

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
                            - id: "Best"
                              title: "Best"
                              notify: [3251142365,4533458845]
                          format:
                            commit:
                              enabled: false
                        """);

                MockedGHPullRequestProcessor.processEmptyPullRequestMock(mocks.pullRequest(1371642823));
            })
            .when().payloadFromClasspath("/pr-opened.json")
            .event(GHEvent.PULL_REQUEST)
            .then().github(mocks -> {
                GHPullRequest mockedPR = mocks.pullRequest(1371642823);
                Mockito.verify(mockedPR).comment("/cc @7125767235, @0979986727");
                GHRepository repo = mocks.repository("xstefank/wildfly");
                // TODO The expected state should be SUCCESS. Fix this test as part of the issue #59.
                Mockito.verify(repo).createCommitStatus("5db0f8e923d84fe05a60658ed5bb95f7aa23b66f",
                    GHCommitState.ERROR, "", "Failed checks: title", "Format");
                Mockito.verify(mockedPR).comment(PullRequestFormatProcessor.FAILED_FORMAT_COMMENT
                    .formatted("- Wrong content of the title"));
                Mockito.verify(mockedPR).listFiles();
                Mockito.verify(mockedPR).listComments();
                Mockito.verifyNoMoreInteractions(mocks.ghObjects());
            });
    }

    @Test
    void testMentionsCCCommentForDuplicateMentions() throws IOException {
        given().github(mocks -> {
                mocks.configFileFromString(
                    "wildfly-bot.yml", """
                        wildfly:
                          rules:
                            - id: "Test"
                              title: "Test"
                              notify: [7125767235,0979986727]
                            - id: "Best"
                              title: "Commit"
                              notify: [7125767235,4533458845]
                          format:
                            commit:
                              enabled: false
                        """);

                MockedGHPullRequestProcessor.processEmptyPullRequestMock(mocks.pullRequest(1371642823));
            })
            .when().payloadFromClasspath("/pr-opened.json")
            .event(GHEvent.PULL_REQUEST)
            .then().github(mocks -> {
                GHPullRequest mockedPR = mocks.pullRequest(1371642823);
                Mockito.verify(mockedPR)
                    .comment("/cc @7125767235, @0979986727, @4533458845");
                GHRepository repo = mocks.repository("xstefank/wildfly");

                // Even though the test is the same as testMentionsCCComment, the expected status is different.
                // There is a problem with test isolation (see issue #59), but the important check is if there are
                // only 3 mentioned users.
                Mockito.verify(repo).createCommitStatus("5db0f8e923d84fe05a60658ed5bb95f7aa23b66f",
                        GHCommitState.ERROR, "", "Failed checks: title", "Format");
                Mockito.verify(mockedPR).comment(PullRequestFormatProcessor.FAILED_FORMAT_COMMENT
                        .formatted("- Wrong content of the title"));
                Mockito.verify(mockedPR).listFiles();
                Mockito.verify(mockedPR).listComments();
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
                          format:
                            commit:
                              enabled: false
                        """);
                MockedGHPullRequestProcessor.processEmptyPullRequestMock(mocks.pullRequest(1371642823));
            })
            .when().payloadFromClasspath("/pr-opened.json")
            .event(GHEvent.PULL_REQUEST)
            .then().github(mocks -> {
                GHPullRequest mockedPR = mocks.pullRequest(1371642823);
                Mockito.verify(mockedPR)
                    .comment("/cc @7125767235");
                GHRepository repo = mocks.repository("xstefank/wildfly");
                Mockito.verify(repo).createCommitStatus("5db0f8e923d84fe05a60658ed5bb95f7aa23b66f",
                        GHCommitState.ERROR, "", "Failed checks: title", "Format");
                Mockito.verify(mockedPR).comment(PullRequestFormatProcessor.FAILED_FORMAT_COMMENT
                        .formatted("- Wrong content of the title"));
                Mockito.verify(mockedPR).listFiles();
                Mockito.verify(mockedPR).listComments();
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
                          format:
                            commit:
                              enabled: false
                        """);
                MockedGHPullRequestProcessor.processEmptyPullRequestMock(mocks.pullRequest(1371642823));
            })
            .when().payloadFromClasspath("/pr-opened.json")
            .event(GHEvent.PULL_REQUEST)
            .then().github(mocks -> {
                GHPullRequest mockedPR = mocks.pullRequest(1371642823);
                Mockito.verify(mockedPR)
                    .comment("/cc @7125767235");
                GHRepository repo = mocks.repository("xstefank/wildfly");
                Mockito.verify(repo).createCommitStatus("5db0f8e923d84fe05a60658ed5bb95f7aa23b66f",
                        GHCommitState.ERROR, "", "Failed checks: title", "Format");
                Mockito.verify(mockedPR).comment(PullRequestFormatProcessor.FAILED_FORMAT_COMMENT
                        .formatted("- Wrong content of the title"));
                Mockito.verify(mockedPR).listFiles();
                Mockito.verify(mockedPR).listComments();
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
                          format:
                            commit:
                              enabled: false
                        """);
                MockedGHPullRequestProcessor.processEmptyPullRequestMock(mocks.pullRequest(1371642823));
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
                          format:
                            commit:
                              enabled: false
                        """);
                MockedGHPullRequestProcessor.processEmptyPullRequestMock(mocks.pullRequest(1371642823));
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
                          format:
                            commit:
                              enabled: false
                        """);
                MockedGHPullRequestProcessor.processEmptyPullRequestMock(mocks.pullRequest(1371642823));
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
                          format:
                            commit:
                              enabled: false
                        """);
                MockedGHPullRequestProcessor.processEmptyPullRequestMock(mocks.pullRequest(1371642823));
            })
            .when().payloadFromClasspath("/pr-opened.json")
            .event(GHEvent.PULL_REQUEST)
            .then().github(mocks -> {
                GHPullRequest mockedPR = mocks.pullRequest(1371642823);
                Mockito.verify(mockedPR, Mockito.never()).comment("/cc @7125767235");
                GHRepository repo = mocks.repository("xstefank/wildfly");
                    Mockito.verify(repo).createCommitStatus("5db0f8e923d84fe05a60658ed5bb95f7aa23b66f",
                            GHCommitState.ERROR, "", "Failed checks: title", "Format");
                    Mockito.verify(mockedPR).comment(PullRequestFormatProcessor.FAILED_FORMAT_COMMENT
                            .formatted("- Wrong content of the title"));
                Mockito.verify(mockedPR).listFiles();
                Mockito.verify(mockedPR).listComments();
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
                          format:
                            commit:
                              enabled: false
                        """);
                MockedGHPullRequestProcessor.processPullRequestMock(mocks.pullRequest(1371642823),
                    mockFileDetails(), MockedGHPullRequestProcessor.mockEmptyComments());
            })
            .when().payloadFromClasspath("/pr-opened.json")
            .event(GHEvent.PULL_REQUEST)
            .then().github(mocks -> {
                GHPullRequest mockedPR = mocks.pullRequest(1371642823);
                Mockito.verify(mockedPR, Mockito.times(2)).listFiles();
                Mockito.verify(mockedPR).comment("/cc @7125767235");
                Mockito.verify(mockedPR, Mockito.times(1)).listComments();
                Mockito.verify(mockedPR).comment(PullRequestFormatProcessor.FAILED_FORMAT_COMMENT
                    .formatted("- Wrong content of the title"));
                Mockito.verifyNoMoreInteractions(mockedPR);
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
                          format:
                            commit:
                              enabled: false
                        """);
                MockedGHPullRequestProcessor.processPullRequestMock(mocks.pullRequest(1371642823),
                    mockFileDetails(), MockedGHPullRequestProcessor.mockEmptyComments());
            })
            .when().payloadFromClasspath("/pr-opened.json")
            .event(GHEvent.PULL_REQUEST)
            .then().github(mocks -> {
                GHPullRequest mockedPR = mocks.pullRequest(1371642823);
                Mockito.verify(mockedPR, Mockito.times(2)).listFiles();
                Mockito.verify(mockedPR, Mockito.times(1)).listComments();
                Mockito.verify(mockedPR).comment(PullRequestFormatProcessor.FAILED_FORMAT_COMMENT
                    .formatted("- Wrong content of the title"));
                Mockito.verify(mockedPR).comment("/cc @7125767235");
                Mockito.verifyNoMoreInteractions(mockedPR);
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
                          format:
                            commit:
                              enabled: false
                        """);
                MockedGHPullRequestProcessor.processPullRequestMock(mocks.pullRequest(1371642823),
                    mockFileDetails(), MockedGHPullRequestProcessor.mockEmptyComments());
            })
            .when().payloadFromClasspath("/pr-opened.json")
            .event(GHEvent.PULL_REQUEST)
            .then().github(mocks -> {
                GHPullRequest mockedPR = mocks.pullRequest(1371642823);
                Mockito.verify(mockedPR, Mockito.times(2)).listFiles();
                Mockito.verify(mockedPR, Mockito.times(1)).listComments();
                Mockito.verify(mockedPR).comment(PullRequestFormatProcessor.FAILED_FORMAT_COMMENT
                        .formatted("- Wrong content of the title"));
                Mockito.verify(mockedPR).comment("/cc @7125767235");
                Mockito.verifyNoMoreInteractions(mockedPR);
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
                          format:
                            commit:
                              enabled: false
                        """);
                MockedGHPullRequestProcessor.processPullRequestMock(mocks.pullRequest(1371642823),
                    mockFileDetails(), MockedGHPullRequestProcessor.mockEmptyComments());
            })
            .when().payloadFromClasspath("/pr-opened.json")
            .event(GHEvent.PULL_REQUEST)
            .then().github(mocks -> {
                GHPullRequest mockedPR = mocks.pullRequest(1371642823);
                Mockito.verify(mockedPR, Mockito.times(2)).listFiles();
                Mockito.verify(mockedPR, Mockito.times(1)).listComments();
                Mockito.verify(mockedPR).comment(PullRequestFormatProcessor.FAILED_FORMAT_COMMENT
                    .formatted("- Wrong content of the title"));
                Mockito.verifyNoMoreInteractions(mockedPR);
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
                            commit:
                              enabled: false
                        """);
                MockedGHPullRequestProcessor.processEmptyPullRequestMock(mocks.pullRequest(1371642823));
            })
            .when().payloadFromClasspath("/pr-opened.json")
            .event(GHEvent.PULL_REQUEST)
            .then().github(mocks -> {
                GHRepository repo = mocks.repository("xstefank/wildfly");
                Mockito.verify(repo).createCommitStatus("5db0f8e923d84fe05a60658ed5bb95f7aa23b66f",
                    GHCommitState.ERROR, "", "Failed checks: title", "Format");
                Mockito.verify(mocks.pullRequest(1371642823)).comment(PullRequestFormatProcessor.FAILED_FORMAT_COMMENT
                    .formatted("- Wrong content of the title"));
            });
    }

    private static GHPullRequestFileDetail[] mockFileDetails() {
        return new GHPullRequestFileDetail[]{
            new MockedGHPullRequestFileDetail("9daeafb9864cf43055ae93beb0afd6c7d144bfa4",
                "appclient/test.txt", "added", 1, 0, 1,
                "https://github.com/xstefank/wildfly/blob/5db0f8e923d84fe05a60658ed5bb95f7aa23b66f/appclient%2Ftest.txt",
                "https://github.com/xstefank/wildfly/raw/5db0f8e923d84fe05a60658ed5bb95f7aa23b66f/appclient%2Ftest.txt",
                "https://api.github.com/repos/xstefank/wildfly/contents/appclient%2Ftest.txt?ref=5db0f8e923d84fe05a60658ed5bb95f7aa23b66f",
                "@@ -0,0 +1 @@\\n+test", null),
            new MockedGHPullRequestFileDetail("3b125a455e091e58db3acd8e8460019aaef2d3f4",
                "microprofile/health-smallrye/pom.xml", "modified", 4, 0, 4,
                "https://github.com/xstefank/wildfly/blob/5db0f8e923d84fe05a60658ed5bb95f7aa23b66f/microprofile%2Fhealth-smallrye%2Fpom.xml",
                "https://github.com/xstefank/wildfly/raw/5db0f8e923d84fe05a60658ed5bb95f7aa23b66f/microprofile%2Fhealth-smallrye%2Fpom.xml",
                "https://api.github.com/repos/xstefank/wildfly/contents/microprofile%2Fhealth-smallrye%2Fpom.xml?ref=5db0f8e923d84fe05a60658ed5bb95f7aa23b66f",
                "@@ -36,6 +36,10 @@\\n \\n     <name>WildFly: MicroProfile Health Extension With SmallRye</name>\\n \\n+    <properties>\\n+      <test>test</test>\\n+    </properties>\\n+\\n     <dependencyManagement>\\n         <dependencies>\\n             <dependency>",
                null),
            new MockedGHPullRequestFileDetail("62ca1d70d69efbf5ab79d46512292c29df72a9b1",
                "testsuite/integration/basic/pom.xml", "modified", 1, 0, 1,
                "https://github.com/xstefank/wildfly/blob/5db0f8e923d84fe05a60658ed5bb95f7aa23b66f/testsuite%2Fintegration%2Fbasic%2Fpom.xml",
                "https://github.com/xstefank/wildfly/raw/5db0f8e923d84fe05a60658ed5bb95f7aa23b66f/testsuite%2Fintegration%2Fbasic%2Fpom.xml",
                "https://api.github.com/repos/xstefank/wildfly/contents/testsuite%2Fintegration%2Fbasic%2Fpom.xml?ref=5db0f8e923d84fe05a60658ed5bb95f7aa23b66f",
                "@@ -22,6 +22,7 @@\\n     <name>WildFly Test Suite: Integration - Basic</name>\\n \\n     <properties>\\n+        <test>test</test>\\n         <jbossas.ts.integ.dir>${basedir}/..</jbossas.ts.integ.dir>\\n         <jbossas.ts.dir>${jbossas.ts.integ.dir}/..</jbossas.ts.dir>\\n         <jbossas.project.dir>${jbossas.ts.dir}/..</jbossas.project.dir>",
                null)
        };
    }
}
