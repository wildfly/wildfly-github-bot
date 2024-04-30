package org.wildfly.bot.utils.mocking;

import com.thoughtworks.xstream.InitializationException;
import io.quarkiverse.githubapp.testing.dsl.GitHubMockContext;
import org.wildfly.bot.utils.TestConstants;
import org.wildfly.bot.utils.model.SsePullRequestPayload;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This class serves as base for grouping Mocking functionality
 * for certain logical domains, such as {@link org.kohsuke.github.GHRepository}
 * or {@link org.kohsuke.github.GHPullRequest}.
 * Where you would create a {@code Mockable} for your specific domain and can
 * chain other {@code Mockable}-s together, when mocking. See usage in tests.
 *
 * You can add your implementation of Mockable into {@code requiredMockables} Set.
 * After all registered {@code Mockable}-s (by {@code mockNext}) have been executed
 * and no instances from {@code requiredMockables} have been found, then default implementation
 * will be executed. This is the implementation you add to the {@code requiredMockables}.
 *
 * Note: Only one {@code Mockable} per domain is allowed to be mocked.
 *
 * Parallel execution is not supported as we are relaying static variables.
 */
public abstract class Mockable {

    protected Mockable previousMock;

    private static final Set<Mockable> requiredMockables;
    static {
        Set<Mockable> requiredMockableCollector = new HashSet<>();
        requiredMockableCollector.add(MockedGHRepository.builder());
        try {
            SsePullRequestPayload ssePullRequestPayload = SsePullRequestPayload.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                    .build();
            requiredMockableCollector.add(MockedGHPullRequest.builder(ssePullRequestPayload.id()));
        } catch (IOException e) {
            throw new InitializationException("Unable to initialize MockedGHPullRequest", e);
        }
        requiredMockables = Collections.unmodifiableSet(requiredMockableCollector);
    }
    private static final Set<Mockable> executedMockables = new HashSet<>();

    public final void mock(GitHubMockContext mocks) throws IOException {
        this.mockNext(mocks, new AtomicLong());
    }

    /**
     * Implement this method to mock your specific domain.
     *
     * @param mocks context passed into {@code io.quarkiverse.githubapp.testing.dsl.GitHubMockVerification} interface
     * @param idGenerator Common aggregate for mocked id generation, such that no duplicate id-s are created.
     * @return {code idGenerator} should be returned after it was used.
     * @throws IOException
     */
    abstract AtomicLong mock(GitHubMockContext mocks, AtomicLong idGenerator) throws IOException;

    public final <T extends Mockable> T mockNext(T mockable) {
        mockable.previousMock = this;
        return mockable;
    }

    /**
     * Runs chain of {@code Mockable}-s and checks for duplicates and executes defaults, if no overrides were found.
     */
    private void mockNext(GitHubMockContext mocks, AtomicLong idGenerator) throws IOException {
        idGenerator = this.mock(mocks, idGenerator);
        Class<?> clazz = this.getClass();
        if (executedMockables.stream().anyMatch(mockable -> mockable.getClass().equals(clazz))) {
            throw new RuntimeException("Two instances of the same Mockable detected for class %s".formatted(clazz));
        }
        executedMockables.add(this);

        if (previousMock != null) {
            previousMock.mockNext(mocks, idGenerator);
        } else {
            // executes default implementations if no overrides provided
            for (Mockable requiredMockable : requiredMockables) {
                Class<?> requiredMockableClazz = requiredMockable.getClass();
                if (executedMockables.stream().noneMatch(mockable -> mockable.getClass().equals(requiredMockableClazz))) {
                    idGenerator = requiredMockable.mock(mocks, idGenerator);
                }
            }
            executedMockables.clear();
        }
    }
}
