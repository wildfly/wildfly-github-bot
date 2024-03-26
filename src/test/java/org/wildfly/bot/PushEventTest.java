package org.wildfly.bot;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.InMemoryLogHandler;
import io.quarkus.test.junit.QuarkusTest;
import org.wildfly.bot.config.WildFlyBotConfig;
import org.wildfly.bot.model.RuntimeConstants;
import org.wildfly.bot.utils.MockedGHPullRequest;
import jakarta.inject.Inject;
import org.jboss.logmanager.Level;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GHFileNotFoundException;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHPullRequestQueryBuilder;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.PagedIterable;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.wildfly.bot.utils.TestConstants;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.LogManager;

import static io.quarkiverse.githubapp.testing.GitHubAppTesting.given;
import static org.wildfly.bot.model.RuntimeConstants.LABEL_NEEDS_REBASE;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@QuarkusTest
@GitHubAppTest
public class PushEventTest {

    private static final String PAYLOAD = "/push.json";
    private long sleepTimeMilis;

    private static final java.util.logging.Logger rootLogger = LogManager.getLogManager().getLogger("org.wildfly.bot");
    private static final InMemoryLogHandler inMemoryLogHandler = new InMemoryLogHandler(
            record -> record.getLevel().intValue() >= Level.ALL.intValue());

    static {
        rootLogger.addHandler(inMemoryLogHandler);
    }

    @Inject
    WildFlyBotConfig wildFlyBotConfig;

    @BeforeEach
    void setup() {
        inMemoryLogHandler.getRecords().clear();
        sleepTimeMilis = (long) (wildFlyBotConfig.timeout() + 1) * 1_000;
    }

    /**
     * First 2 Pull Requests have {@link RuntimeConstants#LABEL_NEEDS_REBASE} applied and
     * other 2 do not. Also, every even Pull Request is not mergable.
     * Note: We are not mocking PushCommit, that's why we have exception thrown in the log of the tests
     */
    @Test
    void applyRebaseThisLabelOnUnmergablePullRequestsTest() throws Exception {
        int pullRequestCount = 4;
        given().github(mocks -> {
            GHRepository repository = mocks.repository(TestConstants.TEST_REPO);
            List<GHPullRequest> pullRequests = new ArrayList<>();
            for (int i = 0; i < pullRequestCount; i++) {
                if (i < 2) {
                    // This happens as we mock repository content and on second mocking we invoke the mocked method...
                    try {
                        MockedGHPullRequest.builder(i)
                                .labels(LABEL_NEEDS_REBASE)
                                .mock(mocks);
                    } catch (GHFileNotFoundException ignored) {
                    }
                }
                GHPullRequest mockedPR = mocks.pullRequest(i);
                final int tmp = i;
                when(mockedPR.getMergeable()).thenReturn(null).thenAnswer(new Answer<>() {
                    private Instant validAfter;
                    private final int index = tmp;

                    @Override
                    public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                        if (validAfter == null) {
                            validAfter = Instant.now().plusSeconds(wildFlyBotConfig.timeout());
                            return null;
                        }

                        if (validAfter.isAfter(Instant.now())) {
                            if (index % 2 == 0) {
                                return Boolean.FALSE;
                            }
                            return Boolean.TRUE;
                        }
                        return null;
                    }
                });

                pullRequests.add(mockedPR);
            }
            GHPullRequestQueryBuilder mockedQueryBuilder = Mockito.mock(GHPullRequestQueryBuilder.class);
            PagedIterable<GHPullRequest> pullRequestsPagedIterable = Mockito.mock(PagedIterable.class);

            when(repository.queryPullRequests()).thenReturn(mockedQueryBuilder);

            when(mockedQueryBuilder.base(anyString())).thenReturn(mockedQueryBuilder);
            when(mockedQueryBuilder.state(GHIssueState.OPEN)).thenReturn(mockedQueryBuilder);
            when(mockedQueryBuilder.list()).thenReturn(pullRequestsPagedIterable);

            when(pullRequestsPagedIterable.toList()).thenReturn(pullRequests);
        })
                .when().payloadFromClasspath(PAYLOAD).event(GHEvent.PUSH)
                .then().github(mocks -> {
                    // we sleep in order to finish the asynchronous execution
                    Thread.sleep(sleepTimeMilis);
                    verify(mocks.repository(TestConstants.TEST_REPO)).queryPullRequests();
                    for (int i = 0; i < pullRequestCount; i++) {
                        Mockito.verify(mocks.pullRequest(i), times(4)).getMergeable();
                        verify(mocks.pullRequest(i)).getLabels();
                        if (i % 2 == 0 && i >= 2) {
                            verify(mocks.pullRequest(i)).addLabels(LABEL_NEEDS_REBASE);
                        }
                        if (i % 2 == 1 && i < 2) {
                            verify(mocks.pullRequest(i)).removeLabels(LABEL_NEEDS_REBASE);
                        }
                        verifyNoMoreInteractions(mocks.pullRequest(i));
                    }
                    // Same as above... This happens as we mock repository content and on second mocking we invoke the mocked method...
                    verify(mocks.repository(TestConstants.TEST_REPO)).getDirectoryContent(anyString());
                    Mockito.verifyNoMoreInteractions(mocks.repository(TestConstants.TEST_REPO));
                });
    }

    /**
     * We test that a queue is created and the Push Events are run reactively
     */
    @Test
    void creatingQueueOnPushEventTest() throws Exception {
        given().github(mocks -> {
            GHRepository repository = mocks.repository(TestConstants.TEST_REPO);
            List<GHPullRequest> pullRequests = new ArrayList<>();
            GHPullRequest mockedPR = mocks.pullRequest(123);
            when(mockedPR.getMergeable()).thenReturn(null).thenAnswer(new Answer<>() {
                private Instant validAfter;

                @Override
                public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                    if (validAfter == null) {
                        validAfter = Instant.now().plusSeconds(wildFlyBotConfig.timeout());
                        return null;
                    }

                    if (validAfter.isAfter(Instant.now())) {
                        return Boolean.TRUE;
                    }
                    return null;
                }
            });
            pullRequests.add(mockedPR);
            GHPullRequestQueryBuilder mockedQueryBuilder = Mockito.mock(GHPullRequestQueryBuilder.class);
            PagedIterable<GHPullRequest> pullRequestsPagedIterable = Mockito.mock(PagedIterable.class);

            when(repository.queryPullRequests()).thenReturn(mockedQueryBuilder);

            when(mockedQueryBuilder.base(anyString())).thenReturn(mockedQueryBuilder);
            when(mockedQueryBuilder.state(GHIssueState.OPEN)).thenReturn(mockedQueryBuilder);
            when(mockedQueryBuilder.list()).thenReturn(pullRequestsPagedIterable);

            when(pullRequestsPagedIterable.toList()).thenReturn(pullRequests);
        })
                .when().payloadFromClasspath(PAYLOAD).event(GHEvent.PUSH)
                .then();

        given().github(mocks -> {
            GHRepository repository = mocks.repository(TestConstants.TEST_REPO);
            List<GHPullRequest> pullRequests = new ArrayList<>();
            GHPullRequest mockedPR = mocks.pullRequest(123);
            when(mockedPR.getMergeable()).thenReturn(null).thenAnswer(new Answer<>() {
                private Instant validAfter;

                @Override
                public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                    if (validAfter == null) {
                        validAfter = Instant.now().plusSeconds(wildFlyBotConfig.timeout());
                        return null;
                    }

                    if (validAfter.isAfter(Instant.now())) {
                        return Boolean.TRUE;
                    }
                    return null;
                }
            });
            pullRequests.add(mockedPR);
            GHPullRequestQueryBuilder mockedQueryBuilder = Mockito.mock(GHPullRequestQueryBuilder.class);
            PagedIterable<GHPullRequest> pullRequestsPagedIterable = Mockito.mock(PagedIterable.class);

            when(repository.queryPullRequests()).thenReturn(mockedQueryBuilder);

            when(mockedQueryBuilder.base(anyString())).thenReturn(mockedQueryBuilder);
            when(mockedQueryBuilder.state(GHIssueState.OPEN)).thenReturn(mockedQueryBuilder);
            when(mockedQueryBuilder.list()).thenReturn(pullRequestsPagedIterable);

            when(pullRequestsPagedIterable.toList()).thenReturn(pullRequests);
        })
                .when().payloadFromClasspath(PAYLOAD).event(GHEvent.PUSH)
                .then().github(mocks -> {
                    Assertions.assertEquals(1, inMemoryLogHandler.getRecords().stream().filter(
                            logRecord -> logRecord.getMessage()
                                    .equals("Scheduling a mergable status update for open pull requests for new head [%s - \"%s\"]"))
                            .count());
                    Thread.sleep(sleepTimeMilis);
                    Assertions.assertEquals(2, inMemoryLogHandler.getRecords().stream().filter(
                            logRecord -> logRecord.getMessage()
                                    .equals("Scheduling a mergable status update for open pull requests for new head [%s - \"%s\"]"))
                            .count());
                });
    }
}
