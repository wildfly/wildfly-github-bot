package io.xstefank.wildfly.bot;

import io.quarkiverse.githubapp.testing.GitHubAppMockito;
import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkiverse.githubapp.testing.GitHubAppTesting;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHCommitState;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GHPullRequestFileDetail;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.PagedSearchIterable;
import org.mockito.Mockito;

import java.io.IOException;

import static io.xstefank.wildfly.bot.helper.MockedGHPullRequestFileDetailProcessor.mockEmptyFileDetails;

@QuarkusTest
@GitHubAppTest
public class PREditTest {

    private String wildflyConfigFile;

    @BeforeEach
    void setUp() {
        wildflyConfigFile = """
            wildfly:
              format:
                title-check:
                  pattern: "\\\\[WFLY-\\\\d+\\\\]\\\\s+.*|WFLY-\\\\d+\\\\s+.*"
                  message: "Wrong content of the title!"
                description:
                  regexes:
                    - pattern: "JIRA:\\\\s+https://issues.redhat.com/browse/WFLY-\\\\d+|https://issues.redhat.com/browse/WFLY-\\\\d+"
                      message: "The PR description must contain a link to the JIRA issue"
                commits-quantity:
                  quantity: "1-2"
                  message: "Too many commits in PR!"
            """;
    }

    @Test
    void configFileNullTest() throws IOException {
        wildflyConfigFile =
            "wildfly:\n" +
                "  rules:\n" +
                "    - id: \"test\"\n" +
                "      title: \"test\"\n" +
                "      body: \"test\"\n" +
                "      notify: [xstefank,petrberan]\n" +
                "  format:";

        GitHubAppTesting.given()
            .github(mocks -> {
                mocks.configFileFromString("wildfly-bot.yml", wildflyConfigFile);

                PagedSearchIterable<GHPullRequestFileDetail> fileDetails = GitHubAppMockito.mockPagedIterable(mockEmptyFileDetails());
                Mockito.when(mocks.pullRequest(1352150111).listFiles()).thenReturn(fileDetails);
            })
            .when().payloadFromClasspath("/pr-success-checks.json")
            .event(GHEvent.PULL_REQUEST)
            .then().github(mocks -> {
                GHRepository repo = mocks.repository("xstefank/wildfly");
                Mockito.verify(repo).createCommitStatus("40dbbdde147294cd8b29df16d79fe874247d8053",
                    GHCommitState.SUCCESS, "", "Valid", "Format");
            });
    }

    @Test
    void incorrectPRFailTest() throws IOException {
        GitHubAppTesting.given()
            .github(mocks -> {
                mocks.configFileFromString("wildfly-bot.yml", wildflyConfigFile);

                PagedSearchIterable<GHPullRequestFileDetail> fileDetails = GitHubAppMockito.mockPagedIterable(mockEmptyFileDetails());
                Mockito.when(mocks.pullRequest(1352150111).listFiles()).thenReturn(fileDetails);
            })
            .when().payloadFromClasspath("/pr-fail-checks.json")
            .event(GHEvent.PULL_REQUEST)
            .then().github(mocks -> {
                GHRepository repo = mocks.repository("xstefank/wildfly");
                Mockito.verify(repo).createCommitStatus("860035425072e50c290561191e90edc90254f900",
                    GHCommitState.ERROR, "","title-check: Wrong content of the title!", "Format");
            });
    }

    @Test
    void correctPRSuccessTest() throws IOException {
        GitHubAppTesting.given()
            .github(mocks -> {
                mocks.configFileFromString("wildfly-bot.yml", wildflyConfigFile);

                PagedSearchIterable<GHPullRequestFileDetail> fileDetails = GitHubAppMockito.mockPagedIterable(mockEmptyFileDetails());
                Mockito.when(mocks.pullRequest(1352150111).listFiles()).thenReturn(fileDetails);
            })
            .when().payloadFromClasspath("/pr-success-checks.json")
            .event(GHEvent.PULL_REQUEST)
            .then().github(mocks -> {
                GHRepository repo = mocks.repository("xstefank/wildfly");
                Mockito.verify(repo).createCommitStatus("40dbbdde147294cd8b29df16d79fe874247d8053",
                    GHCommitState.SUCCESS, "", "Valid", "Format");
            });
    }
}
