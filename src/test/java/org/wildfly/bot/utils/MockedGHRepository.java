package org.wildfly.bot.utils;

import io.quarkiverse.githubapp.testing.GitHubAppMockito;
import io.quarkiverse.githubapp.testing.dsl.GitHubMockContext;
import org.kohsuke.github.GHCommitStatus;
import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHFileNotFoundException;
import org.kohsuke.github.GHLabel;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.HttpException;
import org.kohsuke.github.PagedIterable;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.mockito.ArgumentMatchers.anyString;

public class MockedGHRepository extends Mockable {
    private final Set<String> users = new LinkedHashSet<>();
    private Set<String> labels = new LinkedHashSet<>();
    private final Set<String> directories = new LinkedHashSet<>();
    private final Set<String> files = new LinkedHashSet<>();
    private final Set<String> commitStatuses = new LinkedHashSet<>();
    private String repository = TestConstants.TEST_REPO;

    private MockedGHRepository() {
    }

    private MockedGHRepository(String repository) {
        this.repository = repository;
    }

    public static MockedGHRepository builder() {
        return new MockedGHRepository();
    }

    public static MockedGHRepository builder(String repository) {
        return new MockedGHRepository(repository);
    }

    public MockedGHRepository repository(String repository) {
        this.repository = repository;
        return this;
    }

    /**
     * Creates List with one empty GHContent as response to the specified `directories`
     * Otherwise an `GHFileNotFoundException` is thrown. To override the default
     * behavior you have to mock it before calling `mock` method.
     */
    public MockedGHRepository directories(String... directories) {
        this.directories.addAll(Arrays.asList(directories));
        return this;
    }

    public MockedGHRepository files(String... files) {
        this.files.addAll(Arrays.asList(files));
        return this;
    }

    public MockedGHRepository users(String... users) {
        this.users.addAll(Arrays.asList(users));
        return this;
    }

    public MockedGHRepository labels(Set<String> labels) {
        this.labels = labels;
        return this;
    }

    public MockedGHRepository commitStatuses(String... commitStatuses) {
        this.commitStatuses.addAll(Arrays.asList(commitStatuses));
        return this;
    }

    @Override
    public AtomicLong mock(GitHubMockContext mocks, AtomicLong idGenerator) throws IOException {
        GHRepository repository = mocks.repository(this.repository);

        List<GHCommitStatus> mockedCommitStatuses = new ArrayList<>();
        for (String commitStatus : commitStatuses) {
            GHCommitStatus mockedCommitStatus = Mockito.mock(GHCommitStatus.class);
            Mockito.when(mockedCommitStatus.getContext()).thenReturn(commitStatus);
            mockedCommitStatuses.add(mockedCommitStatus);
        }

        PagedIterable<GHCommitStatus> commitStatusesIterable = GitHubAppMockito
                .mockPagedIterable(mockedCommitStatuses.toArray(GHCommitStatus[]::new));
        Mockito.when(repository.listCommitStatuses(anyString())).thenReturn(commitStatusesIterable);

        Mockito.when(repository.getCollaboratorNames()).thenReturn(this.users);

        for (String collaborator : users) {
            GHUser user = mocks.ghObject(GHUser.class, idGenerator.incrementAndGet());
            Mockito.when(mocks.installationClient(TestConstants.INSTALLATION_ID).getUser(collaborator)).thenReturn(user);
            Mockito.when(user.getLogin()).thenReturn(collaborator);
        }

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

        Mockito.when(repository.getDirectoryContent(ArgumentMatchers.anyString())).thenAnswer(this::getDirectoryContentMock);

        return idGenerator;
    }

    private List<GHContent> getDirectoryContentMock(InvocationOnMock invocationOnMock)
            throws HttpException, GHFileNotFoundException {
        if (directories.contains((String) invocationOnMock.getArgument(0))) {
            return List.of(new GHContent());
        } else if (files.contains((String) invocationOnMock.getArgument(0))) {
            throw new HttpException(200, null, "https://api.github.com/repos/xyz", null);
        }
        throw new GHFileNotFoundException();
    }
}
