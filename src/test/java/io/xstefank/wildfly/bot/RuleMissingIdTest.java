package io.xstefank.wildfly.bot;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHCommitState;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GHRepository;
import org.mockito.Mockito;

import java.io.IOException;

import static io.quarkiverse.githubapp.testing.GitHubAppTesting.given;
import static org.mockito.Mockito.*;

@QuarkusTest
@GitHubAppTest
public class RuleMissingIdTest {

    @Test
    public void testMissingRuleId() throws IOException {
        given().github(mocks -> mocks.configFileFromString(
                "wildfly-bot.yml", """
                    wildfly:
                      rules:
                        - title: "Test"
                          notify: [7125767235,0979986727]
                    """))
        .when().payloadFromClasspath("/pr-opened.json")
        .event(GHEvent.PULL_REQUEST)
        .then().github(mocks -> {
            verify(mocks.pullRequest(1371642823), never())
                            .comment("/cc @7125767235");
            GHRepository repo = mocks.repository("xstefank/wildfly");
            Mockito.verify(repo).createCommitStatus("5db0f8e923d84fe05a60658ed5bb95f7aa23b66f",
                        GHCommitState.SUCCESS, "", "\u2705 Correct", "Format");
            verifyNoMoreInteractions(mocks.ghObjects());
        });

    }
}
