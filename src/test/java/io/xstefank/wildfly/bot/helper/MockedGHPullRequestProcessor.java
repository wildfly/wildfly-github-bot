package io.xstefank.wildfly.bot.helper;

import io.quarkiverse.githubapp.testing.GitHubAppMockito;
import org.kohsuke.github.GHIssueComment;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHPullRequestFileDetail;
import org.kohsuke.github.PagedSearchIterable;
import org.mockito.Mockito;

import java.io.IOException;

public class MockedGHPullRequestProcessor {

    public static void processEmptyPullRequestMock(GHPullRequest pullRequest) throws IOException {
        processPullRequestMock(pullRequest, mockEmptyFileDetails(), mockEmptyComments());
    }

    public static void processPullRequestMock(GHPullRequest pullRequest,
                                              GHPullRequestFileDetail[] fileDetails,
                                              GHIssueComment[] comments) throws IOException {
        PagedSearchIterable<GHPullRequestFileDetail> fileDetails1 = GitHubAppMockito.mockPagedIterable(fileDetails);
        Mockito.when(pullRequest.listFiles()).thenReturn(fileDetails1);
        PagedSearchIterable<GHIssueComment> comments1 = GitHubAppMockito.mockPagedIterable(comments);
        Mockito.when(pullRequest.listComments()).thenReturn(comments1);

    }

    public static GHPullRequestFileDetail[] mockEmptyFileDetails() {
        return new GHPullRequestFileDetail[]{};
    }

    public static GHIssueComment[] mockEmptyComments() {
        return new GHIssueComment[]{};
    }
}
