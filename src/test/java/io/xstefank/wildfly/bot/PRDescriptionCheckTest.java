package io.xstefank.wildfly.bot;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkiverse.githubapp.testing.GitHubAppTesting;
import io.quarkus.test.junit.QuarkusTest;
import io.xstefank.wildfly.bot.format.DescriptionCheck;
import io.xstefank.wildfly.bot.helper.MockedGHPullRequestProcessor;
import io.xstefank.wildfly.bot.model.Description;
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
public class PRDescriptionCheckTest {

    private String wildflyConfigFile;

    @BeforeEach
    void setUp() {
        wildflyConfigFile = """
                wildfly:
                  format:
                    description:
                      message: Default fail message
                      regexes:
                        - pattern: "https://issues.redhat.com/browse/WFLY-\\\\d+"
                          message: "The PR description must contain a link to the JIRA issue"
                        - pattern: "JIRA:\\\\s+https://issues.redhat.com/browse/WFLY-\\\\d+"
                """;
    }

    @Test
    void configFileNullTest() {
        Description description = new Description();

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
            () -> new DescriptionCheck(description));
        Assertions.assertEquals("Input argument cannot be null", thrown.getMessage());
    }

    @Test
    void noLinkCheckFailTest() throws IOException {
        GitHubAppTesting.given()
            .github(mocks -> {
                mocks.configFileFromString("wildfly-bot.yml", wildflyConfigFile);
                MockedGHPullRequestProcessor.processEmptyPullRequestMock(mocks.pullRequest(1352150111));
            })
            .when().payloadFromClasspath("/pr-fail-checks.json")
            .event(GHEvent.PULL_REQUEST)
            .then().github(mocks -> {
                GHRepository repo = mocks.repository("xstefank/wildfly");
                Mockito.verify(repo).createCommitStatus("860035425072e50c290561191e90edc90254f900",
                    GHCommitState.ERROR, "", "Failed checks: description", "Format");
                Mockito.verify(mocks.pullRequest(1352150111)).comment(PullRequestFormatProcessor.FAILED_FORMAT_COMMENT
                    .formatted("- The PR description must contain a link to the JIRA issue"));
            });
    }

    @Test
    void correctLinkCheckSuccessTest() throws IOException {
        GitHubAppTesting.given()
            .github(mocks -> {
                mocks.configFileFromString("wildfly-bot.yml", wildflyConfigFile);
                MockedGHPullRequestProcessor.processEmptyPullRequestMock(mocks.pullRequest(1352150111));
            })
            .when().payloadFromClasspath("/pr-success-checks.json")
            .event(GHEvent.PULL_REQUEST)
            .then().github(mocks -> {
                GHRepository repo = mocks.repository("xstefank/wildfly");
                Mockito.verify(repo).createCommitStatus("40dbbdde147294cd8b29df16d79fe874247d8053",
                    GHCommitState.SUCCESS, "", "Valid", "Format");
            });
    }

    @Test
    void multipleLineDescription() throws IOException {
        GitHubAppTesting.given()
                .github(mocks -> {
                    mocks.configFileFromString("wildfly-bot.yml", wildflyConfigFile);
                    MockedGHPullRequestProcessor.processEmptyPullRequestMock(mocks.pullRequest(1352150111));
                })
                .when().payloadFromClasspath("/pr-success-checks-multiline-description.json")
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository("xstefank/wildfly");
                    Mockito.verify(repo).createCommitStatus("65fdbdde133f94cy6b29df16d79fe874247d513",
                            GHCommitState.SUCCESS, "", "Valid", "Format");
                });
    }
}
