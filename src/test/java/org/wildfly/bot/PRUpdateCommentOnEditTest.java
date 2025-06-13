package org.wildfly.bot;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHIssueComment;
import org.kohsuke.github.GHRepository;
import org.mockito.Mockito;
import org.wildfly.bot.util.GitHubBotContextProvider;
import org.wildfly.bot.utils.WildflyGitHubBotTesting;
import org.wildfly.bot.utils.mocking.Mockable;
import org.wildfly.bot.utils.mocking.MockedGHPullRequest;
import org.wildfly.bot.utils.mocking.MockedGHRepository;
import org.wildfly.bot.utils.model.Action;
import org.wildfly.bot.utils.testing.PullRequestJson;
import org.wildfly.bot.utils.testing.internal.TestModel;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.wildfly.bot.model.RuntimeConstants.DEFAULT_COMMIT_MESSAGE;
import static org.wildfly.bot.model.RuntimeConstants.DEFAULT_TITLE_MESSAGE;
import static org.wildfly.bot.model.RuntimeConstants.FAILED_FORMAT_COMMENT;
import static org.wildfly.bot.model.RuntimeConstants.PROJECT_PATTERN_REGEX;
import static org.wildfly.bot.utils.TestConstants.INVALID_DESCRIPTION;
import static org.wildfly.bot.utils.TestConstants.INVALID_TITLE;
import static org.wildfly.bot.utils.TestConstants.TEST_REPO;

@QuarkusTest
@GitHubAppTest
public class PRUpdateCommentOnEditTest {

    private static String wildflyConfigFile;
    private static PullRequestJson pullRequestJson;
    private Mockable mockedContext;

    @Inject
    GitHubBotContextProvider botContextProvider;

    @BeforeAll
    static void setPullRequestJson() throws Exception {
        TestModel.defaultBeforeEachJsons();
    }

    @Test
    void testUpdateToValid() throws Throwable {
        wildflyConfigFile = """
                wildfly:
                  format:
                    title:
                      enabled: false
                    commit:
                      enabled: false
                """;
        pullRequestJson = TestModel
                .setPullRequestJsonBuilder(pullRequestJsonBuilder -> pullRequestJsonBuilder.title(INVALID_TITLE)
                        .description(INVALID_DESCRIPTION));
        mockedContext = MockedGHPullRequest.builder(pullRequestJson.id())
                .comment(FAILED_FORMAT_COMMENT.formatted(Stream.of(
                        DEFAULT_COMMIT_MESSAGE.formatted(PROJECT_PATTERN_REGEX.formatted("WFLY")),
                        DEFAULT_TITLE_MESSAGE.formatted(PROJECT_PATTERN_REGEX.formatted("WFLY")),
                        "The PR description must contain a link to the JIRA issue")
                        .map("- %s"::formatted)
                        .collect(Collectors.joining("\n\n"))), botContextProvider.getBotName());

        TestModel.given(
                mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson, mockedContext))
                .pullRequestEvent(pullRequestJson)
                .then(mocks -> {
                    GHIssueComment comment = mocks.issueComment(0);
                    Mockito.verify(comment).delete();
                    GHRepository repo = mocks.repository(TEST_REPO);
                    WildflyGitHubBotTesting.verifyFormatSuccess(repo, pullRequestJson);
                });
    }

    @Test
    void testRemoveCommentAndUpdateCommitStatusOnEditToSkipFormatCheck() throws Throwable {
        wildflyConfigFile = """
                wildfly:
                  format:
                """;
        pullRequestJson = TestModel
                .setPullRequestJsonBuilder(pullRequestJsonBuilder -> pullRequestJsonBuilder.title(INVALID_TITLE)
                        .description("@%s skip format".formatted(botContextProvider.getBotName()))
                        .action(Action.EDITED));
        mockedContext = MockedGHPullRequest.builder(pullRequestJson.id())
                .comment(FAILED_FORMAT_COMMENT.formatted(Stream.of(
                        DEFAULT_COMMIT_MESSAGE.formatted(PROJECT_PATTERN_REGEX.formatted("WFLY")),
                        DEFAULT_TITLE_MESSAGE.formatted(PROJECT_PATTERN_REGEX.formatted("WFLY")),
                        "The PR description must contain a link to the JIRA issue")
                        .map("- %s"::formatted)
                        .collect(Collectors.joining("\n\n"))), botContextProvider.getBotName())
                .mockNext(MockedGHRepository.builder())
                .commitStatuses(pullRequestJson.commitSHA(), "Format")
                .commitStatusCreator(botContextProvider.getBotName());

        TestModel.given(
                mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson, mockedContext))
                .pullRequestEvent(pullRequestJson)
                .then(mocks -> {
                    GHIssueComment comment = mocks.issueComment(0);
                    Mockito.verify(comment).delete();
                    WildflyGitHubBotTesting.verifyFormatSkipped(mocks.repository(TEST_REPO), pullRequestJson);
                });
    }
}
