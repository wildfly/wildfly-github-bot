package org.wildfly.bot;

import io.quarkiverse.githubapp.GitHubClientProvider;
import io.quarkiverse.githubapp.testing.GitHubAppMockito;
import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkiverse.githubapp.testing.dsl.GitHubMockSetup;
import io.quarkiverse.githubapp.testing.dsl.GitHubMockSetupContext;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.MockMailbox;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.test.InMemoryLogHandler;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestExtension;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import org.jboss.logmanager.Level;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.kohsuke.github.GHApp;
import org.kohsuke.github.GHAppInstallation;
import org.kohsuke.github.GHAuthenticatedAppInstallation;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.HttpException;
import org.kohsuke.github.PagedIterable;
import org.kohsuke.github.PagedIterator;
import org.kohsuke.github.PagedSearchIterable;
import org.wildfly.bot.model.RuntimeConstants;
import org.wildfly.bot.utils.TestConstants;
import org.wildfly.bot.utils.mocking.Mockable;
import org.wildfly.bot.utils.mocking.MockedGHPullRequest;
import org.wildfly.bot.utils.mocking.MockedGHRepository;
import org.wildfly.bot.utils.model.SsePullRequestPayload;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.LogManager;

import static io.quarkiverse.githubapp.testing.GitHubAppTesting.given;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.wildfly.bot.model.RuntimeConstants.LABEL_FIX_ME;
import static org.wildfly.bot.model.RuntimeConstants.LABEL_NEEDS_REBASE;

/**
 * Tests for the startup of the application.
 */
@QuarkusTest
@GitHubAppTest
public class StartupEventTest {

    @Inject
    Event<StartupEvent> startupEvent;

    // This injection causes problems with replaying events, thus resulting
    //  in usage of GHEvent.STAR event. However, the fired event is irrelevant
    @InjectMock
    GitHubClientProvider clientProvider;

    @Inject
    MockMailbox mailbox;

    private static SsePullRequestPayload ssePullRequestPayload;

    private static final java.util.logging.Logger rootLogger = LogManager.getLogManager().getLogger("org.wildfly.bot");
    private static final InMemoryLogHandler inMemoryLogHandler = new InMemoryLogHandler(
            record -> record.getLevel().intValue() >= Level.ALL.intValue());

    static {
        rootLogger.addHandler(inMemoryLogHandler);
    }

    private Mockable mockedContext;

    @RegisterExtension
    static QuarkusTestExtension TEST = new QuarkusTestExtension();

    @BeforeEach
    void setup() {
        mockedContext = MockedGHPullRequest.builder(1371642823);
    }

    private class CustomGithubMockSetup implements GitHubMockSetup {

        private final String configFile;
        private final Consumer<GitHubMockSetupContext> additionalMocking;

        private CustomGithubMockSetup(String configFile) {
            this.configFile = configFile;
            this.additionalMocking = null;
        }

        private CustomGithubMockSetup(String configFile, Consumer<GitHubMockSetupContext> additionalMocking) {
            this.configFile = configFile;
            this.additionalMocking = additionalMocking;
        }

        @Override
        public void setup(GitHubMockSetupContext mocks) throws Throwable {
            GitHub mockGitHub = mock(GitHub.class);
            GHApp mockGHApp = mock(GHApp.class);
            PagedIterable<GHAppInstallation> mockGHAppInstallations = GitHubAppMockito.mockPagedIterable(
                    mocks.ghObject(GHAppInstallation.class, 0L));
            GHAppInstallation mockGHAppInstallation = mock(GHAppInstallation.class);
            GHAuthenticatedAppInstallation mockGHAuthenticatedAppInstallation = mock(GHAuthenticatedAppInstallation.class);
            PagedSearchIterable<GHRepository> mockGHRepositories = mock(PagedSearchIterable.class);
            PagedIterator<GHRepository> mockIterator = mock(PagedIterator.class);
            mocks.configFile(RuntimeConstants.CONFIG_FILE_NAME).fromString(configFile);

            GHRepository repo = mocks.repository(TestConstants.TEST_REPO);

            when(clientProvider.getApplicationClient()).thenReturn(mockGitHub);
            when(mockGitHub.getApp()).thenReturn(mockGHApp);
            when(mockGHApp.listInstallations()).thenReturn(mockGHAppInstallations);
            when(mockGHAppInstallation.getId()).thenReturn(0L);

            if (additionalMocking != null) {
                additionalMocking.accept(mocks);
            } else {
                when(clientProvider.getInstallationClient(anyLong())).thenReturn(mockGitHub);
            }
            when(mockGitHub.getInstallation()).thenReturn(mockGHAuthenticatedAppInstallation);
            when(mockGHAuthenticatedAppInstallation.listRepositories()).thenReturn(mockGHRepositories);

            when(mockGHRepositories._iterator(anyInt())).thenReturn(mockIterator);
            when(mockIterator.next()).thenReturn(repo);
            when(mockIterator.hasNext()).thenReturn(true).thenReturn(false);

            mockedContext.mock(mocks);

            startupEvent.fire(new StartupEvent());
        }
    }

    @BeforeAll
    static void setUpGitHubJson() throws IOException {
        ssePullRequestPayload = SsePullRequestPayload.builder(TestConstants.VALID_PR_TEMPLATE_JSON).build();
    }

    @BeforeEach
    void init() {
        mailbox.clear();
    }

    @Test
    public void testMissingRuleId() throws IOException {
        given().github(new CustomGithubMockSetup("""
                wildfly:
                  rules:
                    - title: "Test"
                      notify: [Tadpole,Duke]
                  emails:
                    - foo@bar.baz
                """))
                .when().payloadFromString(ssePullRequestPayload.jsonString())
                .event(GHEvent.STAR)
                .then().github(mocks -> Assertions.assertTrue(inMemoryLogHandler.getRecords().stream().anyMatch(
                        logRecord -> logRecord.getMessage().equals(
                                "The configuration file from the repository %s was not parsed successfully due to following problems: %s"))));
    }

    @Test
    public void testSendEmailsOnInvalidRule() throws IOException {
        given().github(new CustomGithubMockSetup("""
                wildfly:
                  rules:
                    - title: "Test"
                      notify: [Tadpole,Duke]
                  emails:
                    - foo@bar.baz
                """))
                .when().payloadFromString(ssePullRequestPayload.jsonString())
                .event(GHEvent.STAR)
                .then().github(mocks -> {
                    GHRepository repository = mocks.repository(TestConstants.TEST_REPO);

                    List<Mail> sent = mailbox.getMailsSentTo("foo@bar.baz");
                    Assertions.assertEquals(sent.size(), 1);
                    Assertions.assertEquals(sent.get(0).getSubject(), LifecycleProcessor.EMAIL_SUBJECT);
                    Assertions.assertEquals(sent.get(0).getText(), LifecycleProcessor.EMAIL_TEXT.formatted(
                            RuntimeConstants.CONFIG_FILE_NAME, repository.getHttpTransportUrl(),
                            "- [WARN] - Rule [title=Test, notify=[Tadpole, Duke]] is missing an id"));
                });
    }

    @Test
    public void testWithOneValidRule() {
        given().github(new CustomGithubMockSetup("""
                wildfly:
                  rules:
                    - id: Test
                      title: "Test"
                      notify: [Tadpole,Duke]
                  emails:
                    - foo@bar.baz
                """))
                .when().payloadFromString(ssePullRequestPayload.jsonString())
                .event(GHEvent.STAR)
                .then().github(mocks -> Assertions.assertTrue(inMemoryLogHandler.getRecords().stream().anyMatch(
                        logRecord -> logRecord.getMessage().equals(
                                "The configuration file from the repository %s was parsed successfully."))));
    }

    @Test
    public void testSendEmailsOnMultipleInvalidRules() {
        given().github(new CustomGithubMockSetup("""
                wildfly:
                  rules:
                    - id: Test
                      title: "Test"
                    - id: Test
                      body: "Test"
                  emails:
                    - foo@bar.baz
                """))
                .when().payloadFromString(ssePullRequestPayload.jsonString())
                .event(GHEvent.STAR)
                .then().github(mocks -> {
                    GHRepository repository = mocks.repository(TestConstants.TEST_REPO);

                    Assertions.assertTrue(inMemoryLogHandler.getRecords().stream().anyMatch(logRecord -> logRecord.getMessage()
                            .equals("The configuration file from the repository %s was not parsed successfully due to following problems: %s")));

                    List<Mail> sent = mailbox.getMailsSentTo("foo@bar.baz");
                    Assertions.assertEquals(sent.size(), 1);
                    Assertions.assertEquals(sent.get(0).getSubject(), LifecycleProcessor.EMAIL_SUBJECT);
                    Assertions.assertEquals(sent.get(0).getText(), LifecycleProcessor.EMAIL_TEXT.formatted(
                            RuntimeConstants.CONFIG_FILE_NAME, repository.getHttpTransportUrl(),
                            "- Rule [id=Test, title=Test] and [id=Test, body=Test] have the same id"));
                });
    }

    @Test
    public void testWithMultipleValidRules() {
        given().github(new CustomGithubMockSetup("""
                wildfly:
                  rules:
                    - id: Test
                      title: "Test"
                    - id: Test-2
                      body: "Test"
                  emails:
                    - foo@bar.baz
                """))
                .when().payloadFromString(ssePullRequestPayload.jsonString())
                .event(GHEvent.STAR)
                .then()
                .github(mocks -> Assertions.assertTrue(inMemoryLogHandler.getRecords().stream().anyMatch(logRecord -> logRecord
                        .getMessage().equals("The configuration file from the repository %s was parsed successfully."))));
    }

    @Test
    public void testStartingSuspendedApp() throws IOException {
        given().github(new CustomGithubMockSetup("""
                wildfly:
                  rules:
                    - title: "Test"
                      notify: [Tadpole,Duke]
                  emails:
                    - foo@bar.baz
                """, (mocks) -> {
            when(clientProvider.getInstallationClient(anyLong())).thenAnswer(invocationOnMock -> {
                throw new IllegalStateException(new HttpException(403, "This installation has been suspended",
                        "https://docs.github.com/rest/reference/apps#create-an-installation-access-token-for-an-app", null));
            });
        }));
        Assertions.assertTrue(inMemoryLogHandler.getRecords().stream().anyMatch(
                logRecord -> logRecord.getMessage().equals(
                        "Your installation has been suspended. No events will be received until you unsuspend the github app installation.")));
        Assertions.assertTrue(inMemoryLogHandler.getRecords().stream().anyMatch(
                logRecord -> logRecord.getMessage().equals(
                        "Unable to correctly start %s for following installation id [%d]")));
    }

    @Test
    public void testCreateMissingLabelsLabel() throws IOException {
        given().github(new CustomGithubMockSetup("""
                wildfly:
                  rules:
                    - title: "Test"
                      id: "test"
                """))
                .when().payloadFromString(ssePullRequestPayload.jsonString())
                .event(GHEvent.STAR)
                .then().github(mocks -> {
                    GHRepository repository = mocks.repository(TestConstants.TEST_REPO);
                    verify(repository).createLabel(eq(LABEL_NEEDS_REBASE), anyString());
                    verify(repository).createLabel(eq(LABEL_FIX_ME), anyString());
                });
    }

    @Test
    public void testCreateOneMissingLabelsLabel() throws IOException {
        mockedContext = mockedContext.mockNext(MockedGHRepository.builder()).labels(Set.of(LABEL_FIX_ME));
        given().github(new CustomGithubMockSetup("""
                wildfly:
                  rules:
                    - title: "Test"
                      id: "test"
                """))
                .when().payloadFromString(ssePullRequestPayload.jsonString())
                .event(GHEvent.STAR)
                .then().github(mocks -> {
                    GHRepository repository = mocks.repository(TestConstants.TEST_REPO);
                    verify(repository).createLabel(eq(LABEL_NEEDS_REBASE), anyString());
                    verify(repository, never()).createLabel(eq(LABEL_FIX_ME), anyString());
                });
    }
}