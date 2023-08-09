package io.xstefank.wildfly.bot.utils;

import io.quarkiverse.githubapp.testing.GitHubAppMockito;
import io.quarkiverse.githubapp.testing.dsl.GitHubMockSetupContext;
import io.quarkiverse.githubapp.testing.dsl.GitHubMockVerificationContext;
import io.xstefank.wildfly.bot.PullRequestFormatProcessor;
import io.xstefank.wildfly.bot.model.RuntimeConstants;
import org.kohsuke.github.GHCommitState;
import org.kohsuke.github.GHPullRequestCommitDetail;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.PagedSearchIterable;
import org.mockito.Mockito;

import java.io.IOException;

import static io.xstefank.wildfly.bot.helper.MockedGHPullRequestProcessor.processEmptyPullRequestMock;

/**
 * Class containing utility and helper methods for tests
 */
public class Util {

    public static void mockRepo(GitHubMockSetupContext mocks, String wildflyConfigFile, GitHubJson gitHubJson, String commitMessage) throws IOException {
        mocks.configFile(RuntimeConstants.CONFIG_FILE_NAME).fromString(wildflyConfigFile);
        GHPullRequestCommitDetail mockCommitDetail = Mockito.mock(GHPullRequestCommitDetail.class);
        Mockito.when(mockCommitDetail.getSha()).thenReturn(gitHubJson.commitSHA());
        PagedSearchIterable<GHPullRequestCommitDetail> commitDetails = GitHubAppMockito.mockPagedIterable(mockCommitDetail);
        Mockito.when(mocks.pullRequest(gitHubJson.id()).listCommits()).thenReturn(commitDetails);

        commitMessage = commitMessage != null ? commitMessage : "[WFLY-123] Valid commit message";
        GHPullRequestCommitDetail.Commit mockCommit = Mockito.mock(GHPullRequestCommitDetail.Commit.class);
        Mockito.when(mockCommitDetail.getCommit()).thenReturn(mockCommit);
        Mockito.when(mockCommit.getMessage()).thenReturn(commitMessage);

        processEmptyPullRequestMock(mocks.pullRequest(gitHubJson.id()));
    }

    public static void verifyFormatSuccess(GHRepository repo, GitHubJson gitHubJson) throws IOException {
        Mockito.verify(repo).createCommitStatus(gitHubJson.commitSHA(),
                GHCommitState.SUCCESS, "", "Valid", "Format");
    }

    public static void verifyFormatFailure(GHRepository repo, GitHubJson gitHubJson, String failedChecks) throws IOException {
        Mockito.verify(repo).createCommitStatus(gitHubJson.commitSHA(),
                GHCommitState.ERROR, "", "Failed checks: " + failedChecks, "Format");
    }

    public static void verifyFailedFormatComment(GitHubMockVerificationContext mocks, GitHubJson gitHubJson, String comment) throws IOException {
        Mockito.verify(mocks.pullRequest(gitHubJson.id())).comment(PullRequestFormatProcessor.FAILED_FORMAT_COMMENT.formatted(comment));
    }
}
