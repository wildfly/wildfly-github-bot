package io.xstefank.wildfly.bot;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;
import io.xstefank.wildfly.bot.model.RuntimeConstants;
import io.xstefank.wildfly.bot.utils.GitHubJson;
import io.xstefank.wildfly.bot.utils.Util;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GHPullRequest;
import org.mockito.Mockito;

import java.io.IOException;

import static io.quarkiverse.githubapp.testing.GitHubAppTesting.given;
import static io.xstefank.wildfly.bot.utils.Action.SYNCHRONIZE;
import static io.xstefank.wildfly.bot.utils.TestConstants.VALID_PR_TEMPLATE_JSON;

/**
 * Tests for the PRs created by Dependabot.
 */
@QuarkusTest
@GitHubAppTest
public class PRDependabotTest {

    private String wildflyConfigFile;
    private GitHubJson gitHubJson;

    @Test
    void testDependabotOpenedPR() throws IOException {
        wildflyConfigFile = """
                wildfly:
                      format:
                        description:
                          regexes:
                            - pattern: "https://issues.redhat.com/browse/WFLY-\\\\d+"
                """;
        gitHubJson = GitHubJson.builder(VALID_PR_TEMPLATE_JSON)
                .userLogin(RuntimeConstants.DEPENDABOT)
                .title("Bump some version from x.y to x.y+1")
                .description("Very detailed description of this upgrade.")
                .build();
        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, gitHubJson))
                .when().payloadFromString(gitHubJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHPullRequest mockedPR = mocks.pullRequest(gitHubJson.id());
                    Mockito.verify(mockedPR).comment("WildFly Bot recognized this PR as dependabot dependency update. " +
                            "Please create a WFLY issue and add its ID to the title and its link to the description.");
                    Mockito.verify(mockedPR).listFiles();
                    Mockito.verify(mockedPR, Mockito.times(2)).getBody();
                    Mockito.verify(mockedPR, Mockito.times(2)).isDraft();
                    Mockito.verify(mockedPR, Mockito.times(3)).getUser();
                    Mockito.verifyNoMoreInteractions(mockedPR);
                });
    }

    @Test
    void testDependabotPRAddedCommit() throws IOException {
        wildflyConfigFile = """
                wildfly:
                      format:
                        description:
                          regexes:
                            - pattern: "https://issues.redhat.com/browse/WFLY-\\\\d+"
                """;
        gitHubJson = GitHubJson.builder(VALID_PR_TEMPLATE_JSON)
                .userLogin(RuntimeConstants.DEPENDABOT)
                .action(SYNCHRONIZE)
                .title("Bump some version from x.y to x.y+1")
                .description("Very detailed description of this upgrade.")
                .build();
        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, gitHubJson))
                .when().payloadFromString(gitHubJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHPullRequest mockedPR = mocks.pullRequest(gitHubJson.id());
                    Mockito.verify(mockedPR).listFiles();
                    Mockito.verify(mockedPR, Mockito.times(2)).getBody();
                    Mockito.verify(mockedPR, Mockito.times(2)).isDraft();
                    Mockito.verify(mockedPR, Mockito.times(2)).getUser();
                    Mockito.verifyNoMoreInteractions(mockedPR);
                });
    }
}
