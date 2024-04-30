package org.wildfly.bot.utils;

import io.quarkiverse.githubapp.testing.dsl.GitHubMockSetupContext;
import io.quarkiverse.githubapp.testing.dsl.GitHubMockVerificationContext;
import org.kohsuke.github.GHCommitState;
import org.kohsuke.github.GHRepository;
import org.mockito.Mockito;
import org.wildfly.bot.model.RuntimeConstants;
import org.wildfly.bot.utils.mocking.Mockable;
import org.wildfly.bot.utils.mocking.MockedGHPullRequest;
import org.wildfly.bot.utils.testing.EventContextSpecificationImpl;
import org.wildfly.bot.utils.testing.ExtendedGitHubAppTestingContext;
import org.wildfly.bot.utils.testing.PullRequestJson;
import org.wildfly.bot.utils.testing.dsl.EventContextSpecification;

import java.io.IOException;

/**
 * Class containing utility and helper methods for tests
 */
public class WildflyGitHubBotTesting {

    public static void mockRepo(GitHubMockSetupContext mocks, String wildflyConfigFile,
            PullRequestJson pullRequestJson)
            throws IOException {
        mocks.configFile(RuntimeConstants.CONFIG_FILE_NAME).fromString(wildflyConfigFile);
        MockedGHPullRequest context = MockedGHPullRequest.builder(pullRequestJson.id())
                .commit("[WFLY-123] Valid commit message");
        context.mock(mocks);
    }

    public static void mockRepo(GitHubMockSetupContext mocks, String wildflyConfigFile,
            PullRequestJson pullRequestJson,
            Mockable context) throws IOException {
        mocks.configFile(RuntimeConstants.CONFIG_FILE_NAME).fromString(wildflyConfigFile);
        context.mock(mocks);
    }

    public static void verifyFormatSuccess(GHRepository repo, PullRequestJson pullRequestJson) throws IOException {
        Mockito.verify(repo).createCommitStatus(pullRequestJson.commitSHA(),
                GHCommitState.SUCCESS, "", "Valid", "Format");
    }

    public static void verifyFormatSkipped(GHRepository repo, PullRequestJson pullRequestJson) throws IOException {
        Mockito.verify(repo).createCommitStatus(pullRequestJson.commitSHA(),
                GHCommitState.SUCCESS, "", "Valid [Skipped]", "Format");
    }

    public static void verifyFormatFailure(GHRepository repo, PullRequestJson pullRequestJson, String failedChecks)
            throws IOException {
        Mockito.verify(repo).createCommitStatus(pullRequestJson.commitSHA(),
                GHCommitState.ERROR, "", "Failed checks: " + failedChecks, "Format");
    }

    public static void verifyFailedFormatComment(GitHubMockVerificationContext mocks,
            PullRequestJson pullRequestJson,
            String comment)
            throws IOException {
        Mockito.verify(mocks.pullRequest(pullRequestJson.id()))
                .comment(RuntimeConstants.FAILED_FORMAT_COMMENT.formatted(comment));
    }

    public static EventContextSpecification given() {
        return new EventContextSpecificationImpl(new ExtendedGitHubAppTestingContext());
    }
}
