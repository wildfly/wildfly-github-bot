package io.xstefank.wildfly.bot;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkiverse.githubapp.testing.GitHubAppTesting;
import io.quarkus.test.junit.QuarkusTest;
import io.xstefank.wildlfy.bot.config.CommitsQuantity;
import io.xstefank.wildlfy.bot.format.checks.CommitsQuantityCheck;
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
public class PRCommitsQuantityCheckTest {

    private String wildflyConfigFile;

    @BeforeEach
    void setUp() {
        wildflyConfigFile =
                "wildfly:\n" +
                "  format:\n" +
                "    commits-quantity:\n" +
                "      quantity: \"1-2\"\n" +
                "      message: \"Too many commits in PR!\"";
    }

    @Test
    void configFileNullTest() {
        CommitsQuantity commitsQuantity = new CommitsQuantity();

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> new CommitsQuantityCheck(commitsQuantity));
        Assertions.assertEquals("Quantity was not set", thrown.getMessage());
    }

    @Test
    void tooManyCommitsCheckFailTest() throws IOException {
        GitHubAppTesting.given()
                .github(mocks -> mocks.configFile("wildfly-bot.yml").fromString(wildflyConfigFile))
                .when().payloadFromClasspath("/pr-fail-checks.json")
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository("xstefank/wildfly");
                    Mockito.verify(repo).createCommitStatus("860035425072e50c290561191e90edc90254f900",
                            GHCommitState.ERROR, "", "\u274C commits-quantity: Too many commits in PR!", "Format");
                });
    }

    @Test
    void correctAmountOfCommitsCheckSuccessTest() throws IOException {
        GitHubAppTesting.given()
                .github(mocks -> mocks.configFile("wildfly-bot.yml").fromString(wildflyConfigFile))
                .when().payloadFromClasspath("/pr-success-checks.json")
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository("xstefank/wildfly");
                    Mockito.verify(repo).createCommitStatus("40dbbdde147294cd8b29df16d79fe874247d8053",
                            GHCommitState.SUCCESS, "", "\u2705 Correct", "Format");
                });
    }
}
