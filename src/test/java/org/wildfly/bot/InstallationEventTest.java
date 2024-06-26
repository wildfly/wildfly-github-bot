package org.wildfly.bot;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.InMemoryLogHandler;
import io.quarkus.test.junit.QuarkusTest;
import org.jboss.logmanager.Level;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHEvent;

import java.io.IOException;
import java.util.logging.LogManager;

import static io.quarkiverse.githubapp.testing.GitHubAppTesting.given;

@QuarkusTest
@GitHubAppTest
public class InstallationEventTest {

    private static final java.util.logging.Logger rootLogger = LogManager.getLogManager().getLogger("org.wildfly.bot");
    private static final InMemoryLogHandler inMemoryLogHandler = new InMemoryLogHandler(
            record -> record.getLevel().intValue() >= Level.ALL.intValue());

    static {
        rootLogger.addHandler(inMemoryLogHandler);
    }

    @Test
    public void testUnsuspendingApp() throws IOException {
        given().when().payloadFromClasspath("/webhooks/installation.json")
                .event(GHEvent.INSTALLATION)
                .then().github(mocks -> Assertions.assertTrue(inMemoryLogHandler.getRecords().stream()
                        .anyMatch(logRecord -> logRecord.getMessage().equals(
                                "%s has been unsuspended for following installation id: %d and has started to listen for new incoming Events."))));
    }
}
