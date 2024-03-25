package io.xstefank.wildfly.bot.utils;

import io.quarkiverse.githubapp.testing.GitHubAppMockito;
import io.quarkiverse.githubapp.testing.dsl.GitHubMockContext;
import io.smallrye.mutiny.tuples.Tuple2;
import org.kohsuke.github.GHIssueComment;
import org.kohsuke.github.GHLabel;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHPullRequestCommitDetail;
import org.kohsuke.github.GHPullRequestFileDetail;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.PagedSearchIterable;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

public class MockedGHPullRequest extends Mockable {

    private final long pullRequest;
    private Set<String> prFiles = new LinkedHashSet<>();
    private String description;
    private final List<Tuple2<String, String>> comments = new ArrayList<>();
    private Set<String> reviewers = new LinkedHashSet<>();
    private Set<String> prLabels = new LinkedHashSet<>();
    private final List<MockedCommit> commits = new ArrayList<>();
    private Boolean mergeable = Boolean.TRUE;
    private boolean isDraft = false;

    private MockedGHPullRequest(long pullRequest) {
        this.pullRequest = pullRequest;
    }

    public static MockedGHPullRequest builder(long pullRequest) {
        return new MockedGHPullRequest(pullRequest);
    }

    public MockedGHPullRequest files(String... filenames) {
        prFiles.addAll(new HashSet<>(Arrays.asList(filenames)));
        return this;
    }

    public MockedGHPullRequest comment(String comment, String author) {
        Tuple2<String, String> commentTuple = Tuple2.of(comment, author);
        this.comments.add(commentTuple);
        return this;
    }

    public MockedGHPullRequest describtion(String description) {
        this.description = description;
        return this;
    }

    public MockedGHPullRequest commit(MockedCommit commit) {
        this.commits.add(commit);
        return this;
    }

    public MockedGHPullRequest commit(String commitMessage) {
        return commit(MockedCommit.commit(commitMessage));
    }

    public MockedGHPullRequest reviewers(String... reviewers) {
        this.reviewers.addAll(Arrays.asList(reviewers));
        return this;
    }

    public MockedGHPullRequest labels(String... labels) {
        this.prLabels.addAll(Arrays.asList(labels));
        return this;
    }

    public MockedGHPullRequest mergeable(Boolean mergeable) {
        this.mergeable = mergeable;
        return this;
    }

    public MockedGHPullRequest draft() {
        this.isDraft = true;
        return this;
    }

    public AtomicLong mock(GitHubMockContext mocks, AtomicLong idGenerator) throws IOException {
        GHPullRequest pullRequest = mocks.pullRequest(this.pullRequest);

        List<GHPullRequestFileDetail> mockedFileDetails = new ArrayList<>();
        for (String filename : prFiles) {
            GHPullRequestFileDetail mocked = Mockito.mock(GHPullRequestFileDetail.class);
            Mockito.when(mocked.getFilename()).thenReturn(filename);
            mockedFileDetails.add(mocked);
        }
        PagedSearchIterable<GHPullRequestFileDetail> fileDetails = GitHubAppMockito
                .mockPagedIterable(mockedFileDetails.toArray(GHPullRequestFileDetail[]::new));
        Mockito.when(pullRequest.listFiles()).thenReturn(fileDetails);

        List<GHIssueComment> mockedComments = new ArrayList<>();
        for (int i = 0; i < comments.size(); i++) {
            Tuple2<String, String> commentTuple = comments.get(i);

            GHIssueComment comment = mocks.issueComment(i);
            GHUser user = mocks.ghObject(GHUser.class, idGenerator.incrementAndGet());

            Mockito.when(user.getLogin()).thenReturn(commentTuple.getItem2());
            Mockito.when(comment.getBody()).thenReturn(commentTuple.getItem1());
            Mockito.when(comment.getUser()).thenReturn(user);

            mockedComments.add(comment);
        }
        PagedSearchIterable<GHIssueComment> comments = GitHubAppMockito
                .mockPagedIterable(mockedComments.toArray(GHIssueComment[]::new));
        Mockito.when(pullRequest.listComments()).thenReturn(comments);

        List<GHPullRequestCommitDetail> mockedCommits = new ArrayList<>();
        for (MockedCommit commit : commits) {
            GHPullRequestCommitDetail mockCommitDetail = Mockito.mock(GHPullRequestCommitDetail.class);
            GHPullRequestCommitDetail.Commit mockCommit = Mockito.mock(GHPullRequestCommitDetail.Commit.class);
            Mockito.when(mockCommitDetail.getCommit()).thenReturn(mockCommit);
            GHUser user = mocks.ghObject(GHUser.class, idGenerator.incrementAndGet());

            Mockito.when(mockCommit.getMessage()).thenReturn(commit.message);
            Mockito.when(mockCommitDetail.getSha()).thenReturn(commit.sha);
            Mockito.when(user.getLogin()).thenReturn(commit.author);
            mockedCommits.add(mockCommitDetail);
        }

        PagedSearchIterable<GHPullRequestCommitDetail> commitDetails = GitHubAppMockito
                .mockPagedIterable(mockedCommits.toArray(GHPullRequestCommitDetail[]::new));
        Mockito.when(pullRequest.listCommits()).thenReturn(commitDetails);

        List<GHUser> requestedReviewers = new ArrayList<>();
        for (String reviewer : reviewers) {
            GHUser user = mocks.ghObject(GHUser.class, idGenerator.incrementAndGet());
            Mockito.when(user.getLogin()).thenReturn(reviewer);
            requestedReviewers.add(user);
        }
        Mockito.when(pullRequest.getRequestedReviewers()).thenReturn(requestedReviewers);

        Collection<GHLabel> pullRequestLabels = new ArrayList<>();
        for (String label : this.prLabels) {
            GHLabel ghLabel = Mockito.mock(GHLabel.class);
            Mockito.when(ghLabel.getName()).thenReturn(label);
            pullRequestLabels.add(ghLabel);
        }
        Mockito.when(pullRequest.getLabels()).thenReturn(pullRequestLabels);

        Mockito.when(pullRequest.getMergeable()).thenReturn(mergeable);

        Mockito.when(pullRequest.isDraft()).thenReturn(isDraft);

        if (description != null) {
            Mockito.when(pullRequest.getBody()).thenReturn(description);
        }

        return idGenerator;
    }
}
