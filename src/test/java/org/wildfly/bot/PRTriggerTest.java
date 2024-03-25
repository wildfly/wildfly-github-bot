package org.wildfly.bot;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;
import org.wildfly.bot.utils.Action;
import org.wildfly.bot.utils.PullRequestJson;
import org.wildfly.bot.utils.Util;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GHRepository;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.wildfly.bot.utils.TestConstants;

import java.io.IOException;

import static io.quarkiverse.githubapp.testing.GitHubAppTesting.given;

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
    private static PullRequestJson pullRequestJson;

    @Test
    void testTriggeredWhenOpened() throws IOException {
        pullRequestJson = PullRequestJson.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .action(Action.OPENED)
                .build();
        checkValidRun(pullRequestJson, Action.OPENED);
    }

    @Test
    void testTriggeredWhenEdited() throws IOException {
        pullRequestJson = PullRequestJson.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .action(Action.EDITED)
                .build();
        checkValidRun(pullRequestJson, Action.EDITED);
    }

    @Test
    void testTriggeredWhenSynchronize() throws IOException {
        pullRequestJson = PullRequestJson.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .action(Action.SYNCHRONIZE)
                .build();
        checkValidRun(pullRequestJson, Action.SYNCHRONIZE);
    }

    @Test
    void testTriggeredWhenAssigned() throws IOException {
        pullRequestJson = PullRequestJson.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .action(Action.ASSIGNED)
                .build();
        checkNoInteraction(pullRequestJson, Action.ASSIGNED);
    }

    @Test
    void testTriggeredWhenAutoMergeDisabled() throws IOException {
        pullRequestJson = PullRequestJson.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .action(Action.AUTO_MERGE_DISABLED)
                .build();
        checkNoInteraction(pullRequestJson, Action.AUTO_MERGE_DISABLED);
    }

    @Test
    void testTriggeredWhenAutoMergeEnabled() throws IOException {
        pullRequestJson = PullRequestJson.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .action(Action.AUTO_MERGE_ENABLED)
                .build();
        checkNoInteraction(pullRequestJson, Action.AUTO_MERGE_ENABLED);
    }

    @Test
    void testTriggeredWhenClosed() throws IOException {
        pullRequestJson = PullRequestJson.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .action(Action.CLOSED)
                .build();
        checkNoInteraction(pullRequestJson, Action.CLOSED);
    }

    @Test
    void testTriggeredWhenConvertedToDraft() throws IOException {
        pullRequestJson = PullRequestJson.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .action(Action.CONVERTED_TO_DRAFT)
                .build();
        checkNoInteraction(pullRequestJson, Action.CONVERTED_TO_DRAFT);
    }

    @Test
    void testTriggeredWhenLabeled() throws IOException {
        pullRequestJson = PullRequestJson.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .action(Action.LABELED)
                .build();
        checkNoInteraction(pullRequestJson, Action.LABELED);
    }

    @Test
    void testTriggeredWhenLocked() throws IOException {
        pullRequestJson = PullRequestJson.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .action(Action.LOCKED)
                .build();
        checkNoInteraction(pullRequestJson, Action.LOCKED);
    }

    @Test
    void testTriggeredWhenReadyForReview() throws IOException {
        pullRequestJson = PullRequestJson.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .action(Action.READY_FOR_REVIEW)
                .build();
        checkValidRun(pullRequestJson, Action.READY_FOR_REVIEW);
    }

    @Test
    void testTriggeredWhenReopened() throws IOException {
        pullRequestJson = PullRequestJson.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .action(Action.REOPENED)
                .build();
        checkValidRun(pullRequestJson, Action.REOPENED);
    }

    @Test
    void testTriggeredWhenReviewRequested() throws IOException {
        pullRequestJson = PullRequestJson.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .action(Action.REVIEW_REQUESTED)
                .build();
        checkNoInteraction(pullRequestJson, Action.REVIEW_REQUESTED);
    }

    @Test
    void testTriggeredWhenReviewRequestRemoved() throws IOException {
        pullRequestJson = PullRequestJson.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .action(Action.REVIEW_REQUEST_REMOVED)
                .build();
        checkNoInteraction(pullRequestJson, Action.REVIEW_REQUEST_REMOVED);
    }

    @Test
    void testTriggeredWhenUnassigned() throws IOException {
        pullRequestJson = PullRequestJson.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .action(Action.UNASSIGNED)
                .build();
        checkNoInteraction(pullRequestJson, Action.UNASSIGNED);
    }

    @Test
    void testTriggeredWhenUnlabeled() throws IOException {
        pullRequestJson = PullRequestJson.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .action(Action.UNLABELED)
                .build();
        checkNoInteraction(pullRequestJson, Action.UNLABELED);
    }

    @Test
    void testTriggeredWhenUnlocked() throws IOException {
        pullRequestJson = PullRequestJson.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .action(Action.UNLOCKED)
                .build();
        checkNoInteraction(pullRequestJson, Action.UNLOCKED);
    }

    /**
     * Checks if the Github Bot processed the PR.
     *
     * If a test calling this method fails, an event caught by the Bot was probably removed.
     * Either fix the test or use {@link PRTriggerTest#checkNoInteraction(PullRequestJson, Action)} instead.
     */
    private void checkValidRun(PullRequestJson pullRequestJson, Action action) throws IOException {
        Assertions.assertEquals(action.getValue().toLowerCase(), pullRequestJson.status());
        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, pullRequestJson))
                .when().payloadFromString(pullRequestJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    Util.verifyFormatSuccess(repo, pullRequestJson);
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
     * Either fix the test or use {@link PRTriggerTest#checkValidRun(PullRequestJson, Action)} instead.
     */
    private void checkNoInteraction(PullRequestJson pullRequestJson, Action action) throws IOException {
        Assertions.assertEquals(action.getValue().toLowerCase(), pullRequestJson.status());
        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, pullRequestJson))
                .when().payloadFromString(pullRequestJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    Mockito.verifyNoInteractions(repo);
                });
    }

}
