package io.xstefank.wildfly.bot;

import io.quarkiverse.githubapp.testing.GitHubAppMockito;
import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkiverse.githubapp.testing.GitHubAppTesting;
import io.quarkus.test.junit.QuarkusTest;
import io.xstefank.wildfly.bot.model.RuntimeConstants;
import io.xstefank.wildfly.bot.utils.GitHubJson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHCommitState;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GHPullRequestCommitDetail;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.PagedSearchIterable;
import org.mockito.Mockito;

import java.io.IOException;

import static io.xstefank.wildfly.bot.helper.MockedGHPullRequestProcessor.processEmptyPullRequestMock;

@QuarkusTest
@GitHubAppTest
public class PRFormatOverrideMessage {

    private String wildflyConfigFile;

    @BeforeEach
    void setUp() {
        wildflyConfigFile = """
                wildfly:
                  format:
                    title:
                      message: "Custom title message"
                    commit:
                      message: "Custom commit message"
                """;
    }

    @Test
    public void testOverridingTitleMessage() throws IOException {
        String commitMessage = "WFLY-00000 commit";
        GitHubAppTesting.given()
                .github(mocks -> {
                    mocks.configFile(RuntimeConstants.CONFIG_FILE_NAME).fromString(wildflyConfigFile);

                    GHPullRequestCommitDetail mockCommitDetail = Mockito.mock(GHPullRequestCommitDetail.class);
                    PagedSearchIterable<GHPullRequestCommitDetail> commitDetails = GitHubAppMockito.mockPagedIterable(mockCommitDetail);
                    Mockito.when(mocks.pullRequest(1352150111).listCommits()).thenReturn(commitDetails);

                    GHPullRequestCommitDetail.Commit mockCommit = Mockito.mock(GHPullRequestCommitDetail.Commit.class);
                    Mockito.when(mockCommitDetail.getCommit()).thenReturn(mockCommit);
                    Mockito.when(mockCommit.getMessage()).thenReturn(commitMessage);

                    processEmptyPullRequestMock(mocks.pullRequest(1352150111));
                })
                .when().payloadFromClasspath("/pr-fail-checks.json")
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository("xstefank/wildfly");
                    Mockito.verify(repo).createCommitStatus("860035425072e50c290561191e90edc90254f900",
                            GHCommitState.ERROR, "", "Failed checks: title", "Format");
                    Mockito.verify(mocks.pullRequest(1352150111)).comment(PullRequestFormatProcessor.FAILED_FORMAT_COMMENT
                            .formatted("- Custom title message"));
                });
    }

    @Test
    public void testOverridingCommitMessage() throws IOException {
        String commitMessage = "commit message";
        GitHubJson gitHubJson = GitHubJson.builder("pr-success-checks.json").build();
        GitHubAppTesting.given()
                .github(mocks -> {
                    mocks.configFile(RuntimeConstants.CONFIG_FILE_NAME).fromString(wildflyConfigFile);

                    GHPullRequestCommitDetail mockCommitDetail = Mockito.mock(GHPullRequestCommitDetail.class);
                    Mockito.when(mockCommitDetail.getSha()).thenReturn(gitHubJson.commitSHA());
                    PagedSearchIterable<GHPullRequestCommitDetail> commitDetails = GitHubAppMockito.mockPagedIterable(mockCommitDetail);
                    Mockito.when(mocks.pullRequest(1352150111).listCommits()).thenReturn(commitDetails);

                    GHPullRequestCommitDetail.Commit mockCommit = Mockito.mock(GHPullRequestCommitDetail.Commit.class);
                    Mockito.when(mockCommitDetail.getCommit()).thenReturn(mockCommit);
                    Mockito.when(mockCommit.getMessage()).thenReturn(commitMessage);

                    processEmptyPullRequestMock(mocks.pullRequest(1352150111));
                })
                .when().payloadFromClasspath("/pr-success-checks.json")
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository("xstefank/wildfly");
                    Mockito.verify(repo).createCommitStatus("40dbbdde147294cd8b29df16d79fe874247d8053",
                            GHCommitState.ERROR, "", "Failed checks: commit", "Format");

                    Mockito.verify(mocks.pullRequest(1352150111)).comment(PullRequestFormatProcessor.FAILED_FORMAT_COMMENT
                            .formatted(String.format("- For commit: \"%s\" (%s) - Custom commit message", commitMessage, gitHubJson.commitSHA())));
                });
    }
}
