package io.xstefank.wildfly.bot;

import io.quarkiverse.githubapp.testing.GitHubAppMockito;
import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;
import io.xstefank.wildfly.bot.model.MockedGHPullRequestFileDetail;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GHPullRequestFileDetail;
import org.kohsuke.github.PagedSearchIterable;
import org.mockito.Mockito;

import java.io.IOException;

import static io.quarkiverse.githubapp.testing.GitHubAppTesting.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@QuarkusTest
@GitHubAppTest
public class PROpenedTest {

    @Test
    void testMentionsCCComment() throws IOException {
        given().github(mocks -> mocks.configFileFromString(
                "wildfly-bot.yml", """
                    wildfly:
                      rules:
                        - title: "Test"
                          notify: [7125767235,0979986727]
                    """))
            .when().payloadFromClasspath("/pr-opened.json")
            .event(GHEvent.PULL_REQUEST)
            .then().github(mocks -> {
                verify(mocks.pullRequest(1371642823))
                    .comment("/cc @0979986727, @7125767235");
                verifyNoMoreInteractions(mocks.ghObjects());
            });
    }

    @Test
    void testTitleBodyCheckForTitle() throws IOException {
        given().github(mocks -> mocks.configFileFromString(
                "wildfly-bot.yml", """
                    wildfly:
                      rules:
                        - title: "Hello"
                        - body: "there"
                        - titleBody: "test"
                          notify: [7125767235]
                    """))
            .when().payloadFromClasspath("/pr-opened.json")
            .event(GHEvent.PULL_REQUEST)
            .then().github(mocks -> {
                verify(mocks.pullRequest(1371642823))
                    .comment("/cc @7125767235");
                verifyNoMoreInteractions(mocks.ghObjects());
            });
    }

    @Test
    void testTitleBodyCheckForBody() throws IOException {
        given().github(mocks -> mocks.configFileFromString(
                "wildfly-bot.yml", """
                    wildfly:
                      rules:
                        - title: "Hello"
                        - body: "there"
                        - titleBody: "foobar"
                          notify: [7125767235]
                    """))
            .when().payloadFromClasspath("/pr-opened.json")
            .event(GHEvent.PULL_REQUEST)
            .then().github(mocks -> {
                verify(mocks.pullRequest(1371642823))
                    .comment("/cc @7125767235");
                verifyNoMoreInteractions(mocks.ghObjects());
            });
    }

    @Test
    void testFailedTitleBodyCheck() throws IOException {
        given().github(mocks -> mocks.configFileFromString(
                "wildfly-bot.yml", """
                    wildfly:
                      rules:
                        - title: "Hello"
                        - body: "there"
                        - titleBody: "General Kenobi"
                          notify: [7125767235]
                    """))
            .when().payloadFromClasspath("/pr-opened.json")
            .event(GHEvent.PULL_REQUEST)
            .then().github(mocks -> {
                verify(mocks.pullRequest(1371642823), never()).comment("/cc @7125767235");
                verifyNoMoreInteractions(mocks.ghObjects());
            });
    }

    @Test
    void testDirectoriesMentionsNewFileInDiff() throws IOException {
        given().github(mocks -> {
                mocks.configFileFromString(
                    "wildfly-bot.yml", """
                        wildfly:
                          rules:
                            - directories:
                               - appclient
                              notify: [7125767235]
                        """);

                PagedSearchIterable<GHPullRequestFileDetail> fileDetails = GitHubAppMockito.mockPagedIterable(mockFileDetails());
                Mockito.when(mocks.pullRequest(1371642823).listFiles()).thenReturn(fileDetails);

            })
            .when().payloadFromClasspath("/pr-opened.json")
            .event(GHEvent.PULL_REQUEST)
            .then().github(mocks -> {
                verify(mocks.pullRequest(1371642823)).listFiles();
                verify(mocks.pullRequest(1371642823)).comment("/cc @7125767235");
                verifyNoMoreInteractions(mocks.pullRequest(1371642823));
            });
    }

    @Test
    void testDirectoriesMentionsChangeInSubdirectoryDiff() throws IOException {
        given().github(mocks -> {
                mocks.configFileFromString(
                    "wildfly-bot.yml", """
                        wildfly:
                          rules:
                            - directories:
                               - microprofile/health-smallrye
                              notify: [7125767235]
                        """);

                PagedSearchIterable<GHPullRequestFileDetail> fileDetails = GitHubAppMockito.mockPagedIterable(mockFileDetails());
                Mockito.when(mocks.pullRequest(1371642823).listFiles()).thenReturn(fileDetails);

            })
            .when().payloadFromClasspath("/pr-opened.json")
            .event(GHEvent.PULL_REQUEST)
            .then().github(mocks -> {
                verify(mocks.pullRequest(1371642823)).listFiles();
                verify(mocks.pullRequest(1371642823)).comment("/cc @7125767235");
                verifyNoMoreInteractions(mocks.pullRequest(1371642823));
            });
    }

    @Test
    void testDirectoriesMentionsChangeInSubdirectoryOfConfiguredDirectoryDiff() throws IOException {
        given().github(mocks -> {
                mocks.configFileFromString(
                    "wildfly-bot.yml", """
                        wildfly:
                          rules:
                            - directories:
                               - testsuite/integration
                              notify: [7125767235]
                        """);

                PagedSearchIterable<GHPullRequestFileDetail> fileDetails = GitHubAppMockito.mockPagedIterable(mockFileDetails());
                Mockito.when(mocks.pullRequest(1371642823).listFiles()).thenReturn(fileDetails);

            })
            .when().payloadFromClasspath("/pr-opened.json")
            .event(GHEvent.PULL_REQUEST)
            .then().github(mocks -> {
                verify(mocks.pullRequest(1371642823)).listFiles();
                verify(mocks.pullRequest(1371642823)).comment("/cc @7125767235");
                verifyNoMoreInteractions(mocks.pullRequest(1371642823));
            });
    }

    @Test
    void testDirectoriesMentionsNoHitInDiff() throws IOException {
        given().github(mocks -> {
                mocks.configFileFromString(
                    "wildfly-bot.yml", """
                        wildfly:
                          rules:
                            - directories:
                               - transactions
                              notify: [7125767235]
                        """);

                PagedSearchIterable<GHPullRequestFileDetail> fileDetails = GitHubAppMockito.mockPagedIterable(mockFileDetails());
                Mockito.when(mocks.pullRequest(1371642823).listFiles()).thenReturn(fileDetails);

            })
            .when().payloadFromClasspath("/pr-opened.json")
            .event(GHEvent.PULL_REQUEST)
            .then().github(mocks -> {
                verify(mocks.pullRequest(1371642823)).listFiles();
                verifyNoMoreInteractions(mocks.pullRequest(1371642823));
            });
    }

    private GHPullRequestFileDetail[] mockFileDetails() {
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
