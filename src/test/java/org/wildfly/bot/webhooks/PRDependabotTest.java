package org.wildfly.bot.webhooks;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.mockito.Mockito;
import org.wildfly.bot.model.RuntimeConstants;
import org.wildfly.bot.utils.TestConstants;
import org.wildfly.bot.utils.WildflyGitHubBotTesting;
import org.wildfly.bot.utils.model.SsePullRequestPayload;
import org.wildfly.bot.utils.testing.PullRequestJson;
import org.wildfly.bot.utils.testing.internal.TestModel;
import org.wildfly.bot.utils.testing.model.PullRequestGitHubEventPayload;

import static org.wildfly.bot.model.RuntimeConstants.PROJECT_PATTERN_REGEX;

/**
 * Tests for the PRs created by Dependabot.
 */
@QuarkusTest
@GitHubAppTest
public class PRDependabotTest {

    private String wildflyConfigFile;
    private PullRequestJson pullRequestJson;

    @BeforeAll
    static void setPullRequestJson() {
        TestModel.setAllCallables(
                () -> SsePullRequestPayload.builder(TestConstants.VALID_PR_TEMPLATE_JSON),
                PullRequestGitHubEventPayload::new);
    }

    @Test
    void testDependabotPR() throws Throwable {
        wildflyConfigFile = """
                wildfly:
                      format:
                        description:
                          regexes:
                            - pattern: "https://issues.redhat.com/browse/WFLY-\\\\d+"
                """;

        pullRequestJson = TestModel.setPullRequestJsonBuilderBuild(pullRequestJsonBuilder -> pullRequestJsonBuilder
                .userLogin(RuntimeConstants.DEPENDABOT)
                .title("Bump some version from x.y to x.y+1")
                .description("Very detailed description of this upgrade."));

        TestModel.given(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson))
                .sseEventOptions(eventSenderOptions -> eventSenderOptions.payloadFromString(pullRequestJson.jsonString())
                        .event(GHEvent.PULL_REQUEST))
                .pollingEventOptions(eventSenderOptions -> eventSenderOptions.eventFromPayload(pullRequestJson.jsonString()))
                .then(mocks -> {
                    GHPullRequest mockedPR = mocks.pullRequest(pullRequestJson.id());
                    Mockito.verify(mockedPR).comment("WildFly Bot recognized this PR as dependabot dependency update. " +
                            "Please create a WFLY issue and add new comment containing this JIRA link please.");
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    WildflyGitHubBotTesting.verifyFormatFailure(repo, pullRequestJson, "description, title");
                    WildflyGitHubBotTesting.verifyFailedFormatComment(mocks, pullRequestJson, String.format("""
                            - Invalid description content

                            - %s""", String.format(RuntimeConstants.DEFAULT_TITLE_MESSAGE,
                            PROJECT_PATTERN_REGEX.formatted("WFLY"))));
                })
                .run();
    }

    @Test
    void testDependabotPREmptyBody() throws Throwable {
        wildflyConfigFile = """
                wildfly:
                      format:
                        description:
                          regexes:
                            - pattern: "https://issues.redhat.com/browse/WFLY-\\\\d+"
                """;

        pullRequestJson = TestModel.setPullRequestJsonBuilderBuild(pullRequestJsonBuilder -> pullRequestJsonBuilder
                .userLogin(RuntimeConstants.DEPENDABOT)
                .title("Bump some version from x.y to x.y+1")
                .description(null));

        TestModel.given(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson))
                .sseEventOptions(eventSenderOptions -> eventSenderOptions.payloadFromString(pullRequestJson.jsonString())
                        .event(GHEvent.PULL_REQUEST))
                .pollingEventOptions(eventSenderOptions -> eventSenderOptions.eventFromPayload(pullRequestJson.jsonString()))
                .then(mocks -> {
                    GHPullRequest mockedPR = mocks.pullRequest(pullRequestJson.id());
                    Mockito.verify(mockedPR).comment("WildFly Bot recognized this PR as dependabot dependency update. " +
                            "Please create a WFLY issue and add new comment containing this JIRA link please.");
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    WildflyGitHubBotTesting.verifyFormatFailure(repo, pullRequestJson, "description, title");
                    WildflyGitHubBotTesting.verifyFailedFormatComment(mocks, pullRequestJson, String.format("""
                            - Invalid description content

                            - %s""", String.format(RuntimeConstants.DEFAULT_TITLE_MESSAGE,
                            PROJECT_PATTERN_REGEX.formatted("WFLY"))));
                })
                .run();
    }
}
