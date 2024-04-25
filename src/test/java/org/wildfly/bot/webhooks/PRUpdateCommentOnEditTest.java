package org.wildfly.bot.webhooks;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GHIssueComment;
import org.kohsuke.github.GHRepository;
import org.mockito.Mockito;
import org.wildfly.bot.config.WildFlyBotConfig;
import org.wildfly.bot.utils.WildflyGitHubBotTesting;
import org.wildfly.bot.utils.mocking.Mockable;
import org.wildfly.bot.utils.mocking.MockedGHPullRequest;
import org.wildfly.bot.utils.mocking.MockedGHRepository;
import org.wildfly.bot.utils.model.Action;
import org.wildfly.bot.utils.model.SsePullRequestPayload;

import java.io.IOException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.quarkiverse.githubapp.testing.GitHubAppTesting.given;
import static org.wildfly.bot.model.RuntimeConstants.DEFAULT_COMMIT_MESSAGE;
import static org.wildfly.bot.model.RuntimeConstants.DEFAULT_TITLE_MESSAGE;
import static org.wildfly.bot.model.RuntimeConstants.FAILED_FORMAT_COMMENT;
import static org.wildfly.bot.model.RuntimeConstants.PROJECT_PATTERN_REGEX;
import static org.wildfly.bot.utils.TestConstants.INVALID_DESCRIPTION;
import static org.wildfly.bot.utils.TestConstants.INVALID_TITLE;
import static org.wildfly.bot.utils.TestConstants.TEST_REPO;
import static org.wildfly.bot.utils.TestConstants.VALID_PR_TEMPLATE_JSON;

@QuarkusTest
@GitHubAppTest
public class PRUpdateCommentOnEditTest {

    private static String wildflyConfigFile;
    private static SsePullRequestPayload ssePullRequestPayload;
    private Mockable mockedContext;

    @Inject
    WildFlyBotConfig wildFlyBotConfig;

    @Test
    void testUpdateToValid() throws IOException {
        wildflyConfigFile = """
                wildfly:
                  format:
                    title:
                      enabled: false
                    commit:
                      enabled: false
                """;
        ssePullRequestPayload = SsePullRequestPayload.builder(VALID_PR_TEMPLATE_JSON)
                .title(INVALID_TITLE)
                .description(INVALID_DESCRIPTION)
                .build();
        mockedContext = MockedGHPullRequest.builder(ssePullRequestPayload.id())
                .comment(FAILED_FORMAT_COMMENT.formatted(Stream.of(
                        DEFAULT_COMMIT_MESSAGE.formatted(PROJECT_PATTERN_REGEX.formatted("WFLY")),
                        DEFAULT_TITLE_MESSAGE.formatted(PROJECT_PATTERN_REGEX.formatted("WFLY")),
                        "The PR description must contain a link to the JIRA issue")
                        .map("- %s"::formatted)
                        .collect(Collectors.joining("\n\n"))), wildFlyBotConfig.githubName());

        given().github(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, ssePullRequestPayload, mockedContext))
                .when().payloadFromString(ssePullRequestPayload.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHIssueComment comment = mocks.issueComment(0);
                    Mockito.verify(comment).delete();
                    GHRepository repo = mocks.repository(TEST_REPO);
                    WildflyGitHubBotTesting.verifyFormatSuccess(repo, ssePullRequestPayload);
                });
    }

    @Test
    void testRemoveCommentAndUpdateCommitStatusOnEditToSkipFormatCheck() throws IOException {
        wildflyConfigFile = """
                wildfly:
                  format:
                """;
        ssePullRequestPayload = SsePullRequestPayload.builder(VALID_PR_TEMPLATE_JSON)
                .title(INVALID_TITLE)
                .description("@%s skip format".formatted(wildFlyBotConfig.githubName()))
                .action(Action.EDITED)
                .build();
        mockedContext = MockedGHPullRequest.builder(ssePullRequestPayload.id())
                .comment(FAILED_FORMAT_COMMENT.formatted(Stream.of(
                        DEFAULT_COMMIT_MESSAGE.formatted(PROJECT_PATTERN_REGEX.formatted("WFLY")),
                        DEFAULT_TITLE_MESSAGE.formatted(PROJECT_PATTERN_REGEX.formatted("WFLY")),
                        "The PR description must contain a link to the JIRA issue")
                        .map("- %s"::formatted)
                        .collect(Collectors.joining("\n\n"))), wildFlyBotConfig.githubName())
                .mockNext(MockedGHRepository.builder())
                .commitStatuses(ssePullRequestPayload.commitSHA(), "Format");

        given().github(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, ssePullRequestPayload, mockedContext))
                .when().payloadFromString(ssePullRequestPayload.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHIssueComment comment = mocks.issueComment(0);
                    Mockito.verify(comment).delete();
                    WildflyGitHubBotTesting.verifyFormatSkipped(mocks.repository(TEST_REPO), ssePullRequestPayload);
                });
    }
}
