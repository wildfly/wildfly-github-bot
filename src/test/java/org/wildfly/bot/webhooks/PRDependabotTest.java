package org.wildfly.bot.webhooks;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;
import org.wildfly.bot.model.RuntimeConstants;
import org.wildfly.bot.utils.model.PullRequestJson;
import org.wildfly.bot.utils.WildflyGitHubBotTesting;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.mockito.Mockito;
import org.wildfly.bot.utils.TestConstants;

import java.io.IOException;

import static io.quarkiverse.githubapp.testing.GitHubAppTesting.given;
import static org.wildfly.bot.model.RuntimeConstants.PROJECT_PATTERN_REGEX;

/**
 * Tests for the PRs created by Dependabot.
 */
@QuarkusTest
@GitHubAppTest
public class PRDependabotTest {

    private String wildflyConfigFile;
    private PullRequestJson pullRequestJson;

    @Test
    void testDependabotPR() throws IOException {
        wildflyConfigFile = """
                wildfly:
                      format:
                        description:
                          regexes:
                            - pattern: "https://issues.redhat.com/browse/WFLY-\\\\d+"
                """;
        pullRequestJson = PullRequestJson.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .userLogin(RuntimeConstants.DEPENDABOT)
                .title("Bump some version from x.y to x.y+1")
                .description("Very detailed description of this upgrade.")
                .build();
        given().github(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson))
                .when().payloadFromString(pullRequestJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHPullRequest mockedPR = mocks.pullRequest(pullRequestJson.id());
                    Mockito.verify(mockedPR).comment("WildFly Bot recognized this PR as dependabot dependency update. " +
                            "Please create a WFLY issue and add new comment containing this JIRA link please.");
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    WildflyGitHubBotTesting.verifyFormatFailure(repo, pullRequestJson, "description, title");
                    WildflyGitHubBotTesting.verifyFailedFormatComment(mocks, pullRequestJson, String.format("""
                            - Invalid description content

                            - %s""", String.format(RuntimeConstants.DEFAULT_TITLE_MESSAGE,
                            PROJECT_PATTERN_REGEX.formatted("WFLY"))));
                });
    }

    @Test
    void testDependabotPREmptyBody() throws IOException {
        wildflyConfigFile = """
                wildfly:
                      format:
                        description:
                          regexes:
                            - pattern: "https://issues.redhat.com/browse/WFLY-\\\\d+"
                """;
        pullRequestJson = PullRequestJson.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .userLogin(RuntimeConstants.DEPENDABOT)
                .title("Bump some version from x.y to x.y+1")
                .description(null)
                .build();
        given().github(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson))
                .when().payloadFromString(pullRequestJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHPullRequest mockedPR = mocks.pullRequest(pullRequestJson.id());
                    Mockito.verify(mockedPR).comment("WildFly Bot recognized this PR as dependabot dependency update. " +
                            "Please create a WFLY issue and add new comment containing this JIRA link please.");
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    WildflyGitHubBotTesting.verifyFormatFailure(repo, pullRequestJson, "description, title");
                    WildflyGitHubBotTesting.verifyFailedFormatComment(mocks, pullRequestJson, String.format("""
                            - Invalid description content

                            - %s""", String.format(RuntimeConstants.DEFAULT_TITLE_MESSAGE,
                            PROJECT_PATTERN_REGEX.formatted("WFLY"))));
                });
    }
}
