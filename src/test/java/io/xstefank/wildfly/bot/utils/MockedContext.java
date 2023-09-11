package io.xstefank.wildfly.bot.utils;

import io.quarkiverse.githubapp.testing.GitHubAppMockito;
import io.quarkiverse.githubapp.testing.dsl.GitHubMockContext;
import io.smallrye.mutiny.tuples.Tuple2;
import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHFileNotFoundException;
import org.kohsuke.github.GHIssueComment;
import org.kohsuke.github.GHLabel;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHPullRequestCommitDetail;
import org.kohsuke.github.GHPullRequestFileDetail;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.HttpException;
import org.kohsuke.github.PagedIterable;
import org.kohsuke.github.PagedSearchIterable;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static io.xstefank.wildfly.bot.utils.TestConstants.INSTALLATION_ID;

public class MockedContext {

    private final long pullRequest;
    private Set<String> prFiles = new LinkedHashSet<>();
    private final List<Tuple2<String, String>> comments = new ArrayList<>();
    private Set<String> users = new LinkedHashSet<>();
    private Set<String> reviewers = new LinkedHashSet<>();
    private Set<String> labels = new LinkedHashSet<>();
    private final List<MockedCommit> commits = new ArrayList<>();
    private final Set<String> repositoryDirectories = new LinkedHashSet<>();
    private final Set<String> repositoryFiles = new LinkedHashSet<>();
    private String repository = "xstefank/wildfly";
    private Boolean mergeable = Boolean.TRUE;
    private boolean isDraft = false;

    private MockedContext(long pullRequest) {
        this.pullRequest = pullRequest;
    }

    public static MockedContext builder(long pullRequest) {
        return new MockedContext(pullRequest);
    }

    public MockedContext prFiles(String... filenames) {
        prFiles.addAll(new HashSet<>(Arrays.asList(filenames)));
        return this;
    }

    public MockedContext comment(String comment, String author) {
        Tuple2<String, String> commentTuple = Tuple2.of(comment, author);
        this.comments.add(commentTuple);
        return this;
    }

    public MockedContext commit(MockedCommit commit) {
        this.commits.add(commit);
        return this;
    }

    public MockedContext commit(String commitMessage) {
        return commit(MockedCommit.commit(commitMessage));
    }

    /**
     * Creates List with one empty GHContent as response to the specified `directories`
     * Otherwise an `GHFileNotFoundException` is thrown. To override the default
     * behavior you have to mock it before calling `mock` method.
     */
    public MockedContext repoDirectories(String... directories) {
        this.repositoryDirectories.addAll(Arrays.asList(directories));
        return this;
    }

    public MockedContext repoFiles(String... files) {
        this.repositoryFiles.addAll(Arrays.asList(files));
        return this;
    }

    public MockedContext users(String... users) {
        this.users.addAll(Arrays.asList(users));
        return this;
    }

    public MockedContext reviewers(String... reviewers) {
        this.reviewers.addAll(Arrays.asList(reviewers));
        return this;
    }

    public MockedContext labels(Set<String> labels) {
        this.labels = labels;
        return this;
    }

    public MockedContext mergeable(Boolean mergeable) {
        this.mergeable = mergeable;
        return this;
    }

    public MockedContext repository(String repository) {
        this.repository = repository;
        return this;
    }

    public MockedContext draft() {
        this.isDraft = true;
        return this;
    }

    public void mock(GitHubMockContext mocks) throws IOException {
        AtomicLong id = new AtomicLong(0L);

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
            GHUser user = mocks.ghObject(GHUser.class, id.incrementAndGet());

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
            GHUser user = mocks.ghObject(GHUser.class, id.incrementAndGet());

            Mockito.when(mockCommit.getMessage()).thenReturn(commit.message);
            Mockito.when(mockCommitDetail.getSha()).thenReturn(commit.sha);
            Mockito.when(user.getLogin()).thenReturn(commit.author);
            mockedCommits.add(mockCommitDetail);
        }

        PagedSearchIterable<GHPullRequestCommitDetail> commitDetails = GitHubAppMockito
                .mockPagedIterable(mockedCommits.toArray(GHPullRequestCommitDetail[]::new));
        Mockito.when(pullRequest.listCommits()).thenReturn(commitDetails);

        GHRepository repository = mocks.repository(this.repository);

        Mockito.when(repository.getCollaboratorNames()).thenReturn(this.users);

        for (String collaborator : users) {
            GHUser user = mocks.ghObject(GHUser.class, id.incrementAndGet());
            Mockito.when(mocks.installationClient(INSTALLATION_ID).getUser(collaborator)).thenReturn(user);
            Mockito.when(user.getLogin()).thenReturn(collaborator);
        }

        List<GHUser> requestedReviewers = new ArrayList<>();
        for (String reviewer : reviewers) {
            GHUser user = mocks.ghObject(GHUser.class, id.incrementAndGet());
            Mockito.when(user.getLogin()).thenReturn(reviewer);
            requestedReviewers.add(user);
        }
        Mockito.when(pullRequest.getRequestedReviewers()).thenReturn(requestedReviewers);

        // we are mocking the PagedIterable#toList method here
        List<GHLabel> ghLabels = new ArrayList<>();
        PagedIterable<GHLabel> allGHLabels = Mockito.mock(PagedIterable.class);
        Mockito.when(repository.listLabels()).thenReturn(allGHLabels);
        for (String label : labels) {
            GHLabel ghLabel = Mockito.mock(GHLabel.class);
            Mockito.when(ghLabel.getName()).thenReturn(label);
            ghLabels.add(ghLabel);
        }
        Mockito.when(allGHLabels.toList()).thenReturn(ghLabels);

        Mockito.when(pullRequest.getMergeable()).thenReturn(mergeable);

        Mockito.when(repository.getDirectoryContent(ArgumentMatchers.anyString())).thenAnswer(this::getDirectoryContentMock);

        Mockito.when(pullRequest.isDraft()).thenReturn(isDraft);
    }

    private List<GHContent> getDirectoryContentMock(InvocationOnMock invocationOnMock)
            throws HttpException, GHFileNotFoundException {
        if (repositoryDirectories.contains((String) invocationOnMock.getArgument(0))) {
            return List.of(new GHContent());
        } else if (repositoryFiles.contains((String) invocationOnMock.getArgument(0))) {
            throw new HttpException(200, null, "https://api.github.com/repos/xyz", null);
        }
        throw new GHFileNotFoundException();
    }
}
