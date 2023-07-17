package io.xstefank.wildfly.bot;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;
import io.xstefank.wildfly.bot.helper.MockedGHPullRequestProcessor;
import io.xstefank.wildfly.bot.utils.Action;
import io.xstefank.wildfly.bot.utils.GitHubJson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHCommitState;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GHRepository;
import org.mockito.Mockito;

import java.io.IOException;

import static io.quarkiverse.githubapp.testing.GitHubAppTesting.given;

@QuarkusTest
@GitHubAppTest
public class PRFormatOverrideProjectKey {

    private String wildflyConfigFile;

    @BeforeEach
    void setUp() {
        wildflyConfigFile = """
                wildfly:
                  projectKey: WFCORE
                  format:
                    commit:
                      enabled: false
                """;
    }

    @Test
    public void testOverridingProjectKeyCorrectTitle() throws IOException {
        GitHubJson githubJson = GitHubJson.builder("pr-template.json")
                .action(Action.EDITED)
                .title("WFCORE-00000 title")
                .build();

        given()
                .github(mocks -> {
                    mocks.configFileFromString("wildfly-bot.yml", wildflyConfigFile);
                    MockedGHPullRequestProcessor.processEmptyPullRequestMock(mocks.pullRequest(1371642823));
                })
                .when().payloadFromString(githubJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository("xstefank/wildfly");
                    Mockito.verify(repo).createCommitStatus("5db0f8e923d84fe05a60658ed5bb95f7aa23b66f",
                            GHCommitState.SUCCESS, "", "Valid", "Format");
                });
    }

    @Test
    public void testOverridingProjectKeyIncorrectTitle() throws IOException {
        GitHubJson githubJson = GitHubJson.builder("pr-template.json")
                .action(Action.EDITED)
                .title("WFLY-00000 title")
                .build();

        given()
                .github(mocks -> {
                    mocks.configFileFromString("wildfly-bot.yml", wildflyConfigFile);
                    MockedGHPullRequestProcessor.processEmptyPullRequestMock(mocks.pullRequest(1371642823));
                })
                .when().payloadFromString(githubJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository("xstefank/wildfly");
                    Mockito.verify(repo).createCommitStatus("5db0f8e923d84fe05a60658ed5bb95f7aa23b66f",
                            GHCommitState.ERROR, "", "Failed checks: title", "Format");
                    Mockito.verify(mocks.pullRequest(1371642823)).comment(PullRequestFormatProcessor.FAILED_FORMAT_COMMENT
                            .formatted("- Wrong content of the title"));
                });
    }
}
