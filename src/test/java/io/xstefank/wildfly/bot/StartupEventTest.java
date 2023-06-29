package io.xstefank.wildfly.bot;

import io.quarkiverse.githubapp.GitHubClientProvider;
import io.quarkiverse.githubapp.testing.GitHubAppMockito;
import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.test.InMemoryLogHandler;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestExtension;
import io.quarkus.test.junit.mockito.InjectMock;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import org.jboss.logmanager.Level;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.kohsuke.github.GHApp;
import org.kohsuke.github.GHAppInstallation;
import org.kohsuke.github.GHAuthenticatedAppInstallation;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.PagedIterable;
import org.kohsuke.github.PagedIterator;
import org.kohsuke.github.PagedSearchIterable;

import java.io.IOException;
import java.util.logging.LogManager;

import static io.quarkiverse.githubapp.testing.GitHubAppTesting.given;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@QuarkusTest
@GitHubAppTest
public class StartupEventTest {

    @Inject
    Event<StartupEvent> startupEvent;

    private static final java.util.logging.Logger rootLogger = LogManager.getLogManager().getLogger("io.xstefank.wildlfy.bot");
    private static final InMemoryLogHandler inMemoryLogHandler = new InMemoryLogHandler(
            record -> record.getLevel().intValue() >= Level.ERROR.intValue());

    static {
        rootLogger.addHandler(inMemoryLogHandler);
    }

    @RegisterExtension
    static QuarkusTestExtension TEST = new QuarkusTestExtension();

    @InjectMock
    GitHubClientProvider clientProvider;

    @Test
    public void testMissingRuleId() throws IOException {
        given().github(mocks -> {
                    GitHub mockGitHub = mock(GitHub.class);
                    GHApp mockGHApp = mock(GHApp.class);
                    PagedIterable<GHAppInstallation> mockGHAppInstallations = GitHubAppMockito.mockPagedIterable(mocks.ghObject(GHAppInstallation.class, 0L));
                    GHAppInstallation mockGHAppInstallation = mock(GHAppInstallation.class);
                    GHAuthenticatedAppInstallation mockGHAuthenticatedAppInstallation = mock(GHAuthenticatedAppInstallation.class);
                    PagedSearchIterable<GHRepository> mockGHRepositories = mock(PagedSearchIterable.class);
                    PagedIterator<GHRepository> mockIterator = mock(PagedIterator.class);
                    mocks.configFile("wildfly-bot.yml").fromString( """
                    wildfly:
                      rules:
                        - title: "Test"
                          notify: [7125767235,0979986727]
                    """);

                    GHRepository repo = mocks.repository("xstefank/wildfly");

                    when(clientProvider.getApplicationClient()).thenReturn(mockGitHub);
                    when(mockGitHub.getApp()).thenReturn(mockGHApp);
                    when(mockGHApp.listInstallations()).thenReturn(mockGHAppInstallations);
                    when(mockGHAppInstallation.getId()).thenReturn(0L);
                    when(clientProvider.getInstallationClient(anyLong())).thenReturn(mockGitHub);
                    when(mockGitHub.getInstallation()).thenReturn(mockGHAuthenticatedAppInstallation);
                    when(mockGHAuthenticatedAppInstallation.listRepositories()).thenReturn(mockGHRepositories);

                    when(mockGHRepositories._iterator(anyInt())).thenReturn(mockIterator);
                    when(mockIterator.next()).thenReturn(repo);
                    when(mockIterator.hasNext()).thenReturn(true).thenReturn(false);

                    startupEvent.fire(new StartupEvent());

                })
                .when().payloadFromClasspath("/pr-opened.json")
                .event(GHEvent.STAR)
                .then().github(mocks -> Assertions.assertTrue(inMemoryLogHandler.getRecords().stream().anyMatch(logRecord -> logRecord.getMessage().equals("In repository %s the following rules are missing ids. [%s]"))));
    }
}