package io.xstefank.wildfly.bot;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;
import io.xstefank.wildfly.bot.helper.MockedGHPullRequestProcessor;
import io.xstefank.wildfly.bot.model.MockedGHPullRequestFileDetail;
import io.xstefank.wildfly.bot.utils.GitHubJson;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHPullRequestFileDetail;
import org.mockito.Mockito;

import java.io.IOException;

import static io.quarkiverse.githubapp.testing.GitHubAppTesting.given;
import static io.xstefank.wildfly.bot.model.RuntimeConstants.CONFIG_FILE_NAME;
import static io.xstefank.wildfly.bot.utils.TestConstants.VALID_PR_TEMPLATE_JSON;

/**
 * Tests for the Wildfly -> Rules -> Directories checks.
 */
@QuarkusTest
@GitHubAppTest
public class PRRuleDirectoryHitTest {

    private static String wildflyConfigFile;
    private static GitHubJson gitHubJson;

    @BeforeAll
    static void setUpGitHubJson() throws IOException {
        gitHubJson = GitHubJson.builder(VALID_PR_TEMPLATE_JSON).build();
    }

    @Test
    void testDirectoriesMentionsNewFileInDiff() throws IOException {
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

        given().github(mocks -> {
                mocks.configFile(CONFIG_FILE_NAME).fromString(wildflyConfigFile);
                MockedGHPullRequestProcessor.processPullRequestMock(mocks.pullRequest(gitHubJson.id()),
                    mockFileDetails(), MockedGHPullRequestProcessor.mockEmptyComments());
            })
            .when().payloadFromString(gitHubJson.jsonString())
            .event(GHEvent.PULL_REQUEST)
            .then().github(mocks -> {
                GHPullRequest mockedPR = mocks.pullRequest(gitHubJson.id());
                Mockito.verify(mockedPR, Mockito.times(2)).listFiles();
                Mockito.verify(mockedPR).comment("/cc @7125767235");
                Mockito.verify(mockedPR, Mockito.times(1)).listComments();
                Mockito.verifyNoMoreInteractions(mockedPR);
            });
    }

    @Test
    void testDirectoriesMentionsChangeInSubdirectoryDiff() throws IOException {
        wildflyConfigFile = """
            wildfly:
              rules:
                - id: "Directory Test"
                  directories:
                   - microprofile/health-smallrye
                  notify: [7125767235]
            """;

        given().github(mocks -> {
                mocks.configFile(CONFIG_FILE_NAME).fromString(wildflyConfigFile);
                MockedGHPullRequestProcessor.processPullRequestMock(mocks.pullRequest(gitHubJson.id()),
                    mockFileDetails(), MockedGHPullRequestProcessor.mockEmptyComments());
            })
            .when().payloadFromString(gitHubJson.jsonString())
            .event(GHEvent.PULL_REQUEST)
            .then().github(mocks -> {
                GHPullRequest mockedPR = mocks.pullRequest(gitHubJson.id());
                Mockito.verify(mockedPR, Mockito.times(2)).listFiles();
                Mockito.verify(mockedPR, Mockito.times(1)).listComments();
                Mockito.verify(mockedPR).comment("/cc @7125767235");
            });
    }

    @Test
    void testDirectoriesMentionsChangeInSubdirectoryOfConfiguredDirectoryDiff() throws IOException {
        wildflyConfigFile = """
            wildfly:
              rules:
                - id: "Directory Test"
                  directories:
                   - testsuite/integration
                  notify: [7125767235]
            """;

        given().github(mocks -> {
                mocks.configFile(CONFIG_FILE_NAME).fromString(wildflyConfigFile);
                MockedGHPullRequestProcessor.processPullRequestMock(mocks.pullRequest(gitHubJson.id()),
                    mockFileDetails(), MockedGHPullRequestProcessor.mockEmptyComments());
            })
            .when().payloadFromString(gitHubJson.jsonString())
            .event(GHEvent.PULL_REQUEST)
            .then().github(mocks -> {
                GHPullRequest mockedPR = mocks.pullRequest(gitHubJson.id());
                Mockito.verify(mockedPR, Mockito.times(2)).listFiles();
                Mockito.verify(mockedPR, Mockito.times(1)).listComments();
                Mockito.verify(mockedPR).comment("/cc @7125767235");
            });
    }

    @Test
    void testDirectoriesMentionsNoHitInDiff() throws IOException {
        wildflyConfigFile = """
            wildfly:
              rules:
                - id: "Directory Test"
                  directories:
                   - transactions
                  notify: [7125767235]
            """;

        given().github(mocks -> {
                mocks.configFile(CONFIG_FILE_NAME).fromString(wildflyConfigFile);
                MockedGHPullRequestProcessor.processPullRequestMock(mocks.pullRequest(gitHubJson.id()),
                    mockFileDetails(), MockedGHPullRequestProcessor.mockEmptyComments());
            })
            .when().payloadFromString(gitHubJson.jsonString())
            .event(GHEvent.PULL_REQUEST)
            .then().github(mocks -> {
                GHPullRequest mockedPR = mocks.pullRequest(gitHubJson.id());
                Mockito.verify(mockedPR, Mockito.times(2)).listFiles();
                Mockito.verify(mockedPR, Mockito.times(1)).listComments();
                Mockito.verify(mockedPR, Mockito.never()).comment("/cc @7125767235");
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
                  notify: [7125767235]
            """;

        given().github(mocks -> {
                mocks.configFile(CONFIG_FILE_NAME).fromString(wildflyConfigFile);
                MockedGHPullRequestProcessor.processPullRequestMock(mocks.pullRequest(gitHubJson.id()),
                    mockFileDetails(), MockedGHPullRequestProcessor.mockEmptyComments());
            })
            .when().payloadFromString(gitHubJson.jsonString())
            .event(GHEvent.PULL_REQUEST)
            .then().github(mocks -> {
                GHPullRequest mockedPR = mocks.pullRequest(gitHubJson.id());
                Mockito.verify(mockedPR, Mockito.times(2)).listFiles();
                Mockito.verify(mockedPR, Mockito.times(1)).listComments();
                Mockito.verify(mockedPR, Mockito.never()).comment("/cc @7125767235");
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
