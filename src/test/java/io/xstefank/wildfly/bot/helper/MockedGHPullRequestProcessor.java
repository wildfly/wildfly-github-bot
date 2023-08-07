package io.xstefank.wildfly.bot.helper;

import io.quarkiverse.githubapp.testing.GitHubAppMockito;
import io.quarkiverse.githubapp.testing.dsl.GitHubMockContext;
import io.smallrye.mutiny.tuples.Tuple2;
import org.kohsuke.github.GHIssueComment;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHPullRequestFileDetail;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.PagedSearchIterable;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

public class MockedGHPullRequestProcessor {

    private final long pullRequest;
    private GHPullRequestFileDetail[] fileDetails = new GHPullRequestFileDetail[]{};
    private List<Tuple2<String, String>> comments = new ArrayList<>();
    private Set<String> collaborators = new HashSet<>();
    private Set<String> reviewers = new HashSet<>();
    private Set<String> labels = new HashSet<>();
    private String repository = "xstefank/wildfly";

    private MockedGHPullRequestProcessor(long pullRequest) {
        this.pullRequest = pullRequest;
    }

    public static MockedGHPullRequestProcessor builder(long pullRequest) {
        return new MockedGHPullRequestProcessor(pullRequest);
    }

    public MockedGHPullRequestProcessor fileDetails(GHPullRequestFileDetail[] fileDetails) {
        this.fileDetails = fileDetails;
        return this;
    }

    public MockedGHPullRequestProcessor comment(String comment, String author) {
        Tuple2<String, String> commentTuple = Tuple2.of(comment, author);
        this.comments.add(commentTuple);
        return this;
    }

    /**
     * Sets the collaborators for the Pull Request and creates `GHUser`s for retrieval
     * See GitHub#getUser()
     */
    public MockedGHPullRequestProcessor collaborators(Set<String> collaborators) {
        this.collaborators = collaborators;
        return this;
    }

    public MockedGHPullRequestProcessor reviewers(Set<String> reviewers) {
        this.reviewers = reviewers;
        return this;
    }

    public MockedGHPullRequestProcessor repository(String repository) {
        this.repository = repository;
        return this;
    }

    public void mock(GitHubMockContext mocks) throws IOException {
        AtomicLong id = new AtomicLong(0L);

        GHPullRequest pullRequest = mocks.pullRequest(this.pullRequest);
        PagedSearchIterable<GHPullRequestFileDetail> fileDetails = GitHubAppMockito.mockPagedIterable(this.fileDetails);
        Mockito.when(pullRequest.listFiles()).thenReturn(fileDetails);
        GHIssueComment[] mockedComments = comments.stream()
                .map(tuple2 -> {
                    GHIssueComment comment = mocks.ghObject(GHIssueComment.class, id.incrementAndGet());
                    GHUser user = mocks.ghObject(GHUser.class, id.incrementAndGet());
                    Mockito.when(user.getLogin()).thenReturn(tuple2.getItem2());

                    Mockito.when(comment.getBody()).thenReturn(tuple2.getItem1());
                    try {
                        Mockito.when(comment.getUser()).thenReturn(user);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    return comment;
                })
                .toArray(GHIssueComment[]::new);
        PagedSearchIterable<GHIssueComment> comments = GitHubAppMockito.mockPagedIterable(mockedComments);
        Mockito.when(pullRequest.listComments()).thenReturn(comments);
        GHRepository repository = mocks.repository(this.repository);
        Mockito.when(repository.getCollaboratorNames()).thenReturn(this.collaborators);

        for (String collaborator : collaborators) {
            GHUser user = mocks.ghObject(GHUser.class, id.incrementAndGet());
            // TODO update github json handler to retrieve installation id
            Mockito.when(mocks.installationClient(22950279).getUser(collaborator)).thenReturn(user);
            Mockito.when(user.getLogin()).thenReturn(collaborator);
        }

        List<GHUser> requestedReviewers = new ArrayList<>();
        for (String reviewer : reviewers) {
            GHUser user = mocks.ghObject(GHUser.class, id.incrementAndGet());
            Mockito.when(user.getLogin()).thenReturn(reviewer);
            requestedReviewers.add(user);
        }
        Mockito.when(pullRequest.getRequestedReviewers()).thenReturn(requestedReviewers);
    }
}
