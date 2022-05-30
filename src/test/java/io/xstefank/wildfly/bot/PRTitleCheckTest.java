package io.xstefank.wildfly.bot;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkiverse.githubapp.testing.GitHubAppTesting;
import io.quarkus.test.junit.QuarkusTest;
import io.xstefank.wildlfy.bot.config.RegexDefinition;
import io.xstefank.wildlfy.bot.format.checks.TitleCheck;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHCommitState;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GHRepository;
import org.mockito.Mockito;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertThrows;

@QuarkusTest
@GitHubAppTest
public class PRTitleCheckTest {

    private String wildflyConfigFile;

    @BeforeEach
    void setUp() {
        wildflyConfigFile =
                "wildfly:\n" +
                "  format:\n" +
                "    title-check:\n" +
                "      pattern: \"\\\\[WFLY-\\\\d+\\\\]\\\\s+.*|WFLY-\\\\d+\\\\s+.*\"\n" +
                "      message: \"Wrong content of the title!\"";
    }

    @Test
    void configFileNullTest() {
        RegexDefinition regexDefinition = new RegexDefinition();

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> new TitleCheck(regexDefinition));
        Assertions.assertEquals("Input argument cannot be null", thrown.getMessage());
    }

    @Test
    void incorrectTitleCheckFailTest() throws IOException {
        GitHubAppTesting.given()
                .github(mocks -> mocks.configFileFromString("wildfly-bot.yml", wildflyConfigFile))
                .when().payloadFromClasspath("/pr-fail-checks.json")
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository("tutorial-quarkus-github-app");
                    Mockito.verify(repo).createCommitStatus("3de181159483ae3f2103e258cd2a6e7f9bafb3b8",
                            GHCommitState.ERROR, "", "\u274C title-check: Wrong content of the title!", "Format");
                });
    }

    @Test
    void correctTitleCheckSuccessTest() throws IOException {
        GitHubAppTesting.given()
                .github(mocks -> mocks.configFileFromString("wildfly-bot.yml", wildflyConfigFile))
                .when().payloadFromClasspath("/pr-success-checks.json")
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository("tutorial-quarkus-github-app");
                    Mockito.verify(repo).createCommitStatus("e97a94bcedda66f652c196c2b0e6b01db4f1c0ce",
                            GHCommitState.SUCCESS, "", "\u2705 Correct", "Format");
                });
    }
}
