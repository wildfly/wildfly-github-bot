package io.xstefank.wildfly.bot.utils;

import io.quarkiverse.githubapp.testing.dsl.GitHubMockSetupContext;
import io.quarkiverse.githubapp.testing.dsl.GitHubMockVerificationContext;
import io.xstefank.wildfly.bot.PullRequestFormatProcessor;
import io.xstefank.wildfly.bot.model.RuntimeConstants;
import org.kohsuke.github.GHCommitState;
import org.kohsuke.github.GHRepository;
import org.mockito.Mockito;

import java.io.IOException;

/**
 * Class containing utility and helper methods for tests
 */
public class Util {

    public static void mockRepo(GitHubMockSetupContext mocks, String wildflyConfigFile, GitHubJson gitHubJson) throws IOException {
        mocks.configFile(RuntimeConstants.CONFIG_FILE_NAME).fromString(wildflyConfigFile);
        MockedContext context = MockedContext.builder(gitHubJson.id())
                .commit("[WFLY-123] Valid commit message");
        context.mock(mocks);
    }

    public static void mockRepo(GitHubMockSetupContext mocks, String wildflyConfigFile, GitHubJson gitHubJson, MockedContext context) throws IOException {
        mocks.configFile(RuntimeConstants.CONFIG_FILE_NAME).fromString(wildflyConfigFile);
        context.mock(mocks);
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
