package io.xstefank.wildfly.bot;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;
import io.xstefank.wildfly.bot.config.WildFlyBotConfig;
import io.xstefank.wildfly.bot.utils.GitHubJson;
import io.xstefank.wildfly.bot.utils.MockedContext;
import io.xstefank.wildfly.bot.utils.Util;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GHIssueComment;
import org.kohsuke.github.GHRepository;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.quarkiverse.githubapp.testing.GitHubAppTesting.given;
import static io.xstefank.wildfly.bot.PullRequestFormatProcessor.FAILED_FORMAT_COMMENT;
import static io.xstefank.wildfly.bot.model.RuntimeConstants.DEFAULT_COMMIT_MESSAGE;
import static io.xstefank.wildfly.bot.model.RuntimeConstants.DEFAULT_TITLE_MESSAGE;
import static io.xstefank.wildfly.bot.model.RuntimeConstants.PROJECT_PATTERN_REGEX;
import static io.xstefank.wildfly.bot.utils.TestConstants.INVALID_DESCRIPTION;
import static io.xstefank.wildfly.bot.utils.TestConstants.INVALID_TITLE;
import static io.xstefank.wildfly.bot.utils.TestConstants.TEST_REPO;
import static io.xstefank.wildfly.bot.utils.TestConstants.VALID_PR_TEMPLATE_JSON;

@QuarkusTest
@GitHubAppTest
public class PRUpdateCommentOnEditTest {

    private static String wildflyConfigFile;
    private static GitHubJson gitHubJson;
    private MockedContext mockedContext;

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
        gitHubJson = GitHubJson.builder(VALID_PR_TEMPLATE_JSON)
                .title(INVALID_TITLE)
                .description(INVALID_DESCRIPTION)
                .build();
        mockedContext = MockedContext.builder(gitHubJson.id())
                .comment(FAILED_FORMAT_COMMENT.formatted(Stream.of(
                        DEFAULT_COMMIT_MESSAGE.formatted(PROJECT_PATTERN_REGEX.formatted("WFLY")),
                        DEFAULT_TITLE_MESSAGE.formatted(PROJECT_PATTERN_REGEX.formatted("WFLY")),
                        "The PR description must contain a link to the JIRA issue")
                        .map("- %s"::formatted)
                        .collect(Collectors.joining("\n\n"))), wildFlyBotConfig.githubName());

        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, gitHubJson, mockedContext))
                .when().payloadFromString(gitHubJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHIssueComment comment = mocks.issueComment(0);
                    Mockito.verify(comment).delete();
                    GHRepository repo = mocks.repository(TEST_REPO);
                    Util.verifyFormatSuccess(repo, gitHubJson);
                });
    }
}
