package org.wildfly.bot;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GHRepository;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.wildfly.bot.utils.TestConstants;
import org.wildfly.bot.utils.WildflyGitHubBotTesting;
import org.wildfly.bot.utils.model.Action;
import org.wildfly.bot.utils.model.SsePullRequestPayload;

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
    private static SsePullRequestPayload ssePullRequestPayload;

    @Test
    void testTriggeredWhenOpened() throws IOException {
        ssePullRequestPayload = SsePullRequestPayload.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .action(Action.OPENED)
                .build();
        checkValidRun(ssePullRequestPayload, Action.OPENED);
    }

    @Test
    void testTriggeredWhenEdited() throws IOException {
        ssePullRequestPayload = SsePullRequestPayload.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .action(Action.EDITED)
                .build();
        checkValidRun(ssePullRequestPayload, Action.EDITED);
    }

    @Test
    void testTriggeredWhenSynchronize() throws IOException {
        ssePullRequestPayload = SsePullRequestPayload.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .action(Action.SYNCHRONIZE)
                .build();
        checkValidRun(ssePullRequestPayload, Action.SYNCHRONIZE);
    }

    @Test
    void testTriggeredWhenAssigned() throws IOException {
        ssePullRequestPayload = SsePullRequestPayload.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .action(Action.ASSIGNED)
                .build();
        checkNoInteraction(ssePullRequestPayload, Action.ASSIGNED);
    }

    @Test
    void testTriggeredWhenAutoMergeDisabled() throws IOException {
        ssePullRequestPayload = SsePullRequestPayload.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .action(Action.AUTO_MERGE_DISABLED)
                .build();
        checkNoInteraction(ssePullRequestPayload, Action.AUTO_MERGE_DISABLED);
    }

    @Test
    void testTriggeredWhenAutoMergeEnabled() throws IOException {
        ssePullRequestPayload = SsePullRequestPayload.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .action(Action.AUTO_MERGE_ENABLED)
                .build();
        checkNoInteraction(ssePullRequestPayload, Action.AUTO_MERGE_ENABLED);
    }

    @Test
    void testTriggeredWhenClosed() throws IOException {
        ssePullRequestPayload = SsePullRequestPayload.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .action(Action.CLOSED)
                .build();
        checkNoInteraction(ssePullRequestPayload, Action.CLOSED);
    }

    @Test
    void testTriggeredWhenConvertedToDraft() throws IOException {
        ssePullRequestPayload = SsePullRequestPayload.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .action(Action.CONVERTED_TO_DRAFT)
                .build();
        checkNoInteraction(ssePullRequestPayload, Action.CONVERTED_TO_DRAFT);
    }

    @Test
    void testTriggeredWhenLabeled() throws IOException {
        ssePullRequestPayload = SsePullRequestPayload.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .action(Action.LABELED)
                .build();
        checkNoInteraction(ssePullRequestPayload, Action.LABELED);
    }

    @Test
    void testTriggeredWhenLocked() throws IOException {
        ssePullRequestPayload = SsePullRequestPayload.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .action(Action.LOCKED)
                .build();
        checkNoInteraction(ssePullRequestPayload, Action.LOCKED);
    }

    @Test
    void testTriggeredWhenReadyForReview() throws IOException {
        ssePullRequestPayload = SsePullRequestPayload.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .action(Action.READY_FOR_REVIEW)
                .build();
        checkValidRun(ssePullRequestPayload, Action.READY_FOR_REVIEW);
    }

    @Test
    void testTriggeredWhenReopened() throws IOException {
        ssePullRequestPayload = SsePullRequestPayload.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .action(Action.REOPENED)
                .build();
        checkValidRun(ssePullRequestPayload, Action.REOPENED);
    }

    @Test
    void testTriggeredWhenReviewRequested() throws IOException {
        ssePullRequestPayload = SsePullRequestPayload.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .action(Action.REVIEW_REQUESTED)
                .build();
        checkNoInteraction(ssePullRequestPayload, Action.REVIEW_REQUESTED);
    }

    @Test
    void testTriggeredWhenReviewRequestRemoved() throws IOException {
        ssePullRequestPayload = SsePullRequestPayload.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .action(Action.REVIEW_REQUEST_REMOVED)
                .build();
        checkNoInteraction(ssePullRequestPayload, Action.REVIEW_REQUEST_REMOVED);
    }

    @Test
    void testTriggeredWhenUnassigned() throws IOException {
        ssePullRequestPayload = SsePullRequestPayload.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .action(Action.UNASSIGNED)
                .build();
        checkNoInteraction(ssePullRequestPayload, Action.UNASSIGNED);
    }

    @Test
    void testTriggeredWhenUnlabeled() throws IOException {
        ssePullRequestPayload = SsePullRequestPayload.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .action(Action.UNLABELED)
                .build();
        checkNoInteraction(ssePullRequestPayload, Action.UNLABELED);
    }

    @Test
    void testTriggeredWhenUnlocked() throws IOException {
        ssePullRequestPayload = SsePullRequestPayload.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .action(Action.UNLOCKED)
                .build();
        checkNoInteraction(ssePullRequestPayload, Action.UNLOCKED);
    }

    /**
     * Checks if the Github Bot processed the PR.
     *
     * If a test calling this method fails, an event caught by the Bot was probably removed.
     * Either fix the test or use {@link PRTriggerTest#checkNoInteraction(SsePullRequestPayload, Action)} instead.
     */
    private void checkValidRun(SsePullRequestPayload ssePullRequestPayload, Action action) throws IOException {
        Assertions.assertEquals(action.getValue().toLowerCase(), ssePullRequestPayload.status());
        given().github(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, ssePullRequestPayload))
                .when().payloadFromString(ssePullRequestPayload.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    WildflyGitHubBotTesting.verifyFormatSuccess(repo, ssePullRequestPayload);
                    Mockito.verify(repo, Mockito.atMostOnce()).listLabels();
                    Mockito.verify(repo, Mockito.atMostOnce()).createLabel(ArgumentMatchers.anyString(),
                            ArgumentMatchers.anyString());
                    Mockito.verify(repo, Mockito.atMostOnce()).queryCommits();
                    Mockito.verifyNoMoreInteractions(repo);
                });
    }

    /**
     * Checks if the Github Bot didn't process the PR.
     *
     * If a test calling this method fails, an event caught by the Bot was probably added.
     * Either fix the test or use {@link PRTriggerTest#checkValidRun(SsePullRequestPayload, Action)} instead.
     */
    private void checkNoInteraction(SsePullRequestPayload ssePullRequestPayload, Action action) throws IOException {
        Assertions.assertEquals(action.getValue().toLowerCase(), ssePullRequestPayload.status());
        given().github(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, ssePullRequestPayload))
                .when().payloadFromString(ssePullRequestPayload.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    Mockito.verifyNoInteractions(repo);
                });
    }

}
