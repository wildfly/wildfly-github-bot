package io.xstefank.wildfly.bot;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;
import io.xstefank.wildfly.bot.utils.Action;
import io.xstefank.wildfly.bot.utils.GitHubJson;
import io.xstefank.wildfly.bot.utils.Util;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GHRepository;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.io.IOException;

import static io.quarkiverse.githubapp.testing.GitHubAppTesting.given;
import static io.xstefank.wildfly.bot.utils.TestConstants.VALID_PR_TEMPLATE_JSON;
import static io.xstefank.wildfly.bot.utils.TestConstants.TEST_REPO;

/**
 * Tests whether the Bot is running at specific PR events.
 */
@QuarkusTest
@GitHubAppTest
public class PRTriggerTest {

    private static final String wildflyConfigFile = """
            wildfly:
             format:
               title:
                 enabled: true
            """;
    private static GitHubJson gitHubJson;

    @Test
    void testTriggeredWhenOpened() throws IOException {
        gitHubJson = GitHubJson.builder(VALID_PR_TEMPLATE_JSON)
                .action(Action.OPENED)
                .build();
        checkValidRun(gitHubJson, Action.OPENED);
    }

    @Test
    void testTriggeredWhenEdited() throws IOException {
        gitHubJson = GitHubJson.builder(VALID_PR_TEMPLATE_JSON)
                .action(Action.EDITED)
                .build();
        checkValidRun(gitHubJson, Action.EDITED);
    }

    @Test
    void testTriggeredWhenSynchronize() throws IOException {
        gitHubJson = GitHubJson.builder(VALID_PR_TEMPLATE_JSON)
                .action(Action.SYNCHRONIZE)
                .build();
        checkValidRun(gitHubJson, Action.SYNCHRONIZE);
    }

    @Test
    void testTriggeredWhenAssigned() throws IOException {
        gitHubJson = GitHubJson.builder(VALID_PR_TEMPLATE_JSON)
                .action(Action.ASSIGNED)
                .build();
        checkNoInteraction(gitHubJson, Action.ASSIGNED);
    }

    @Test
    void testTriggeredWhenAutoMergeDisabled() throws IOException {
        gitHubJson = GitHubJson.builder(VALID_PR_TEMPLATE_JSON)
                .action(Action.AUTO_MERGE_DISABLED)
                .build();
        checkNoInteraction(gitHubJson, Action.AUTO_MERGE_DISABLED);
    }

    @Test
    void testTriggeredWhenAutoMergeEnabled() throws IOException {
        gitHubJson = GitHubJson.builder(VALID_PR_TEMPLATE_JSON)
                .action(Action.AUTO_MERGE_ENABLED)
                .build();
        checkNoInteraction(gitHubJson, Action.AUTO_MERGE_ENABLED);
    }

    @Test
    void testTriggeredWhenClosed() throws IOException {
        gitHubJson = GitHubJson.builder(VALID_PR_TEMPLATE_JSON)
                .action(Action.CLOSED)
                .build();
        checkNoInteraction(gitHubJson, Action.CLOSED);
    }

    @Test
    void testTriggeredWhenConvertedToDraft() throws IOException {
        gitHubJson = GitHubJson.builder(VALID_PR_TEMPLATE_JSON)
                .action(Action.CONVERTED_TO_DRAFT)
                .build();
        checkNoInteraction(gitHubJson, Action.CONVERTED_TO_DRAFT);
    }

    @Test
    void testTriggeredWhenLabeled() throws IOException {
        gitHubJson = GitHubJson.builder(VALID_PR_TEMPLATE_JSON)
                .action(Action.LABELED)
                .build();
        checkNoInteraction(gitHubJson, Action.LABELED);
    }

    @Test
    void testTriggeredWhenLocked() throws IOException {
        gitHubJson = GitHubJson.builder(VALID_PR_TEMPLATE_JSON)
                .action(Action.LOCKED)
                .build();
        checkNoInteraction(gitHubJson, Action.LOCKED);
    }

    @Test
    void testTriggeredWhenReadyForReview() throws IOException {
        gitHubJson = GitHubJson.builder(VALID_PR_TEMPLATE_JSON)
                .action(Action.READY_FOR_REVIEW)
                .build();
        checkValidRun(gitHubJson, Action.READY_FOR_REVIEW);
    }

    @Test
    void testTriggeredWhenReopened() throws IOException {
        gitHubJson = GitHubJson.builder(VALID_PR_TEMPLATE_JSON)
                .action(Action.REOPENED)
                .build();
        checkValidRun(gitHubJson, Action.REOPENED);
    }

    @Test
    void testTriggeredWhenReviewRequested() throws IOException {
        gitHubJson = GitHubJson.builder(VALID_PR_TEMPLATE_JSON)
                .action(Action.REVIEW_REQUESTED)
                .build();
        checkNoInteraction(gitHubJson, Action.REVIEW_REQUESTED);
    }

    @Test
    void testTriggeredWhenReviewRequestRemoved() throws IOException {
        gitHubJson = GitHubJson.builder(VALID_PR_TEMPLATE_JSON)
                .action(Action.REVIEW_REQUEST_REMOVED)
                .build();
        checkNoInteraction(gitHubJson, Action.REVIEW_REQUEST_REMOVED);
    }

    @Test
    void testTriggeredWhenUnassigned() throws IOException {
        gitHubJson = GitHubJson.builder(VALID_PR_TEMPLATE_JSON)
                .action(Action.UNASSIGNED)
                .build();
        checkNoInteraction(gitHubJson, Action.UNASSIGNED);
    }

    @Test
    void testTriggeredWhenUnlabeled() throws IOException {
        gitHubJson = GitHubJson.builder(VALID_PR_TEMPLATE_JSON)
                .action(Action.UNLABELED)
                .build();
        checkNoInteraction(gitHubJson, Action.UNLABELED);
    }

    @Test
    void testTriggeredWhenUnlocked() throws IOException {
        gitHubJson = GitHubJson.builder(VALID_PR_TEMPLATE_JSON)
                .action(Action.UNLOCKED)
                .build();
        checkNoInteraction(gitHubJson, Action.UNLOCKED);
    }

    /**
     * Checks if the Github Bot processed the PR.
     *
     * If a test calling this method fails, an event caught by the Bot was probably removed.
     * Either fix the test or use {@link PRTriggerTest#checkNoInteraction(GitHubJson, Action)} instead.
     */
    private void checkValidRun(GitHubJson gitHubJson, Action action) throws IOException {
        Assertions.assertEquals(action.getValue().toLowerCase(), gitHubJson.status());
        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, gitHubJson))
                .when().payloadFromString(gitHubJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository(TEST_REPO);
                    Util.verifyFormatSuccess(repo, gitHubJson);
                    Mockito.verify(repo, Mockito.atMostOnce()).listLabels();
                    Mockito.verify(repo, Mockito.atMostOnce()).createLabel(ArgumentMatchers.anyString(),
                            ArgumentMatchers.anyString());
                    Mockito.verifyNoMoreInteractions(repo);
                });
    }

    /**
     * Checks if the Github Bot didn't process the PR.
     *
     * If a test calling this method fails, an event caught by the Bot was probably added.
     * Either fix the test or use {@link PRTriggerTest#checkValidRun(GitHubJson, Action)} instead.
     */
    private void checkNoInteraction(GitHubJson gitHubJson, Action action) throws IOException {
        Assertions.assertEquals(action.getValue().toLowerCase(), gitHubJson.status());
        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, gitHubJson))
                .when().payloadFromString(gitHubJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository(TEST_REPO);
                    Mockito.verifyNoInteractions(repo);
                });
    }

}
