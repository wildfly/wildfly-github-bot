package org.wildfly.bot;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHCommitQueryBuilder;
import org.kohsuke.github.GHPullRequestCommitDetail;
import org.kohsuke.github.PagedIterable;
import org.kohsuke.github.PagedIterator;
import org.wildfly.bot.utils.TestConstants;
import org.wildfly.bot.utils.WildflyGitHubBotTesting;
import org.wildfly.bot.utils.mocking.Mockable;
import org.wildfly.bot.utils.mocking.MockedGHPullRequest;
import org.wildfly.bot.utils.testing.PullRequestJson;
import org.wildfly.bot.utils.testing.internal.TestModel;

import java.util.Set;

import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@QuarkusTest
@GitHubAppTest
public class PRSkipPullRequestTest {

    private static PullRequestJson pullRequestJson;
    private Mockable mockedContext;
    private static final String wildflyConfigFile = """
            wildfly:
              format:
                title:
                  enabled: true
                commit:
                  enabled: true
            """;

    @BeforeAll
    static void setPullRequestJson() throws Exception {
        TestModel.defaultBeforeEachJsons();
    }

    @Test
    void testSkippingFormatCheck() throws Throwable {
        pullRequestJson = TestModel.setPullRequestJsonBuilder(pullRequestJsonBuilder -> pullRequestJsonBuilder
                .title(TestConstants.INVALID_TITLE)
                .description("""
                        Hi


                        line start @wildfly-bot[bot] skip format random things

                        to pass on my local env @wildfly-github-bot-fork[bot] skip format thanks

                        finished"""));
        mockedContext = MockedGHPullRequest.builder(pullRequestJson.id()).commit(TestConstants.INVALID_COMMIT_MESSAGE);

        TestModel.given(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson, mockedContext))
                .pullRequestEvent(pullRequestJson)
                .then(mocks -> {
                    verify(mocks.pullRequest(pullRequestJson.id()), times(2)).getBody();
                    verify(mocks.pullRequest(pullRequestJson.id())).listFiles();
                    verify(mocks.pullRequest(pullRequestJson.id())).listComments();
                    // Following invocations are used for logging
                    verify(mocks.pullRequest(pullRequestJson.id()), times(2)).getNumber();
                    // commit status should not be set
                    verifyNoMoreInteractions(mocks.pullRequest(pullRequestJson.id()));
                });
    }

    @Test
    void testSkippingFormatCheckOnDraft() throws Throwable {
        pullRequestJson = TestModel.setPullRequestJsonBuilder(
                pullRequestJsonBuilder -> pullRequestJsonBuilder.title(TestConstants.INVALID_TITLE));
        mockedContext = MockedGHPullRequest.builder(pullRequestJson.id()).draft();

        TestModel.given(
                mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson, mockedContext))
                .pullRequestEvent(pullRequestJson)
                .then(mocks -> {
                    verify(mocks.pullRequest(pullRequestJson.id()), times(2)).getBody();
                    verify(mocks.pullRequest(pullRequestJson.id())).listFiles();
                    verify(mocks.pullRequest(pullRequestJson.id()), times(2)).isDraft();
                    verify(mocks.pullRequest(pullRequestJson.id())).listComments();
                    // commit status should not be set
                    verifyNoMoreInteractions(mocks.pullRequest(pullRequestJson.id()));
                });
    }

    @Test
    void testSkippingRulesOnIncorrectRebase() throws Throwable {
        final String duplicateSHA = "sha1";
        final String baseBranch = "main";
        pullRequestJson = TestModel.setPullRequestJsonBuilder(pullRequestJsonBuilder -> pullRequestJsonBuilder);
        mockedContext = MockedGHPullRequest.builder(pullRequestJson.id());
        TestModel.given(
                mocks -> {
                    WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson, mockedContext);
                    GHCommit commit1 = mock(GHCommit.class);
                    GHCommit commit2 = mock(GHCommit.class);

                    when(commit1.getSHA1()).thenReturn(duplicateSHA);
                    when(commit2.getSHA1()).thenReturn("sha2");

                    // Create a fake PagedIterable for repository commits.
                    Set<GHCommit> fakeRepoCommitSet = Set.of(commit1, commit2);
                    PagedIterable<GHCommit> fakeRepoCommits = mock(PagedIterable.class);
                    GHCommitQueryBuilder mockedQueryBuilder = mock(GHCommitQueryBuilder.class);

                    when(mocks.repository(TestConstants.TEST_REPO).queryCommits()).thenReturn(mockedQueryBuilder);
                    when(mockedQueryBuilder.from(eq(baseBranch))).thenReturn(mockedQueryBuilder);
                    when(mockedQueryBuilder.pageSize(anyInt())).thenReturn(mockedQueryBuilder);
                    when(mockedQueryBuilder.list()).thenReturn(fakeRepoCommits);
                    when(fakeRepoCommits.toSet()).thenReturn(fakeRepoCommitSet);

                    GHPullRequestCommitDetail fakePRCommit = mock(GHPullRequestCommitDetail.class);
                    PagedIterator<GHPullRequestCommitDetail> mockIterator = mock(PagedIterator.class);
                    PagedIterable<GHPullRequestCommitDetail> fakePRCommits = mock(PagedIterable.class);

                    when(fakePRCommit.getSha()).thenReturn(duplicateSHA);
                    when(mockIterator.hasNext()).thenReturn(true, false);
                    when(mockIterator.next()).thenReturn(fakePRCommit);
                    when(mocks.pullRequest(pullRequestJson.id()).listCommits()).thenReturn(fakePRCommits);
                    when(fakePRCommits._iterator(anyInt())).thenReturn(mockIterator);
                })
                .pullRequestEvent(pullRequestJson)
                .then(mocks -> {
                    verify(mocks.pullRequest(pullRequestJson.id())).getBase();
                    verify(mocks.repository(TestConstants.TEST_REPO)).queryCommits();
                    verify(mocks.pullRequest(pullRequestJson.id()), never()).addLabels(nullable(String[].class));
                    verify(mocks.pullRequest(pullRequestJson.id()), never()).requestReviewers(any());
                });
    }
}