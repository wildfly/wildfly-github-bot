package io.xstefank.wildfly.bot;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;
import io.xstefank.wildfly.bot.utils.MockedContext;
import io.xstefank.wildfly.bot.utils.PullRequestReviewJson;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHEvent;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Arrays;

import static io.quarkiverse.githubapp.testing.GitHubAppTesting.given;
import static io.xstefank.wildfly.bot.model.RuntimeConstants.LABEL_FIX_ME;
import static io.xstefank.wildfly.bot.utils.Action.EDITED;
import static io.xstefank.wildfly.bot.utils.Action.SYNCHRONIZE;
import static io.xstefank.wildfly.bot.utils.PullRequestReviewJson.Builder.ReviewState.APPROVE;
import static io.xstefank.wildfly.bot.utils.PullRequestReviewJson.Builder.ReviewState.CHANGES_REQUESTED;
import static io.xstefank.wildfly.bot.utils.PullRequestReviewJson.Builder.ReviewState.COMMENT;
import static io.xstefank.wildfly.bot.utils.TestConstants.VALID_PR_REVIEW_TEMPLATE_JSON;
import static io.xstefank.wildfly.bot.utils.TestConstants.VALID_PR_TEMPLATE_JSON;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@QuarkusTest
@GitHubAppTest
public class PRReviewFixMeLabelTest {

    private static PullRequestReviewJson pullRequestReviewJson;

    private MockedContext mockedContext;

    @Test
    void notAddingFixMeLabelOnPROpen() throws IOException {
        pullRequestReviewJson = PullRequestReviewJson.<PullRequestReviewJson.Builder<PullRequestReviewJson>> builder(
                VALID_PR_REVIEW_TEMPLATE_JSON).build();
        given().github(mocks -> MockedContext.builder(pullRequestReviewJson.id()).mock(mocks))
                .when().payloadFromString(pullRequestReviewJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    final ArgumentCaptor<String[]> argumentCaptor = ArgumentCaptor.forClass(String[].class);
                    verify(mocks.pullRequest(pullRequestReviewJson.id()), Mockito.never()).addLabels(argumentCaptor.capture());
                });
    }

    @Test
    void notAddingFixMeLabelOnReviewComment() throws IOException {
        pullRequestReviewJson = PullRequestReviewJson.<PullRequestReviewJson.Builder<PullRequestReviewJson>> builder(
                VALID_PR_REVIEW_TEMPLATE_JSON).state(COMMENT).build();
        given().github(mocks -> MockedContext.builder(pullRequestReviewJson.id()).mock(mocks))
                .when().payloadFromString(pullRequestReviewJson.jsonString())
                .event(GHEvent.PULL_REQUEST_REVIEW)
                .then().github(mocks -> {
                    final ArgumentCaptor<String[]> argumentCaptor = ArgumentCaptor.forClass(String[].class);
                    verify(mocks.pullRequest(pullRequestReviewJson.id()), Mockito.never()).addLabels(argumentCaptor.capture());
                });
    }

    @Test
    void notAddingFixMeLabelOnReviewApprove() throws IOException {
        pullRequestReviewJson = PullRequestReviewJson.<PullRequestReviewJson.Builder<PullRequestReviewJson>> builder(
                VALID_PR_REVIEW_TEMPLATE_JSON).state(APPROVE).build();
        given().github(mocks -> MockedContext.builder(pullRequestReviewJson.id()).mock(mocks))
                .when().payloadFromString(pullRequestReviewJson.jsonString())
                .event(GHEvent.PULL_REQUEST_REVIEW)
                .then().github(mocks -> {
                    final ArgumentCaptor<String[]> argumentCaptor = ArgumentCaptor.forClass(String[].class);
                    verify(mocks.pullRequest(pullRequestReviewJson.id()), Mockito.never()).addLabels(argumentCaptor.capture());
                });
    }

    @Test
    void addFixMeLabelOnReviewChangesRequested() throws IOException {
        pullRequestReviewJson = PullRequestReviewJson.<PullRequestReviewJson.Builder<PullRequestReviewJson>> builder(
                VALID_PR_REVIEW_TEMPLATE_JSON).state(CHANGES_REQUESTED)
                .build();
        given().github(mocks -> MockedContext.builder(pullRequestReviewJson.id()).mock(mocks))
                .when().payloadFromString(pullRequestReviewJson.jsonString())
                .event(GHEvent.PULL_REQUEST_REVIEW)
                .then().github(mocks -> {
                    final ArgumentCaptor<String[]> argumentCaptor = ArgumentCaptor.forClass(String[].class);
                    verify(mocks.pullRequest(pullRequestReviewJson.id())).addLabels(argumentCaptor.capture());
                    MatcherAssert.assertThat(Arrays.asList(argumentCaptor.getValue()),
                            Matchers.containsInAnyOrder(LABEL_FIX_ME));
                });
    }

    @Test
    void removeFixMeLabelOnReviewOnNewCommit() throws IOException {
        pullRequestReviewJson = PullRequestReviewJson.<PullRequestReviewJson.Builder<PullRequestReviewJson>> builder(
                VALID_PR_TEMPLATE_JSON)
                .action(SYNCHRONIZE).build();
        given().github(mocks -> MockedContext.builder(pullRequestReviewJson.id())
                .commit("Mock commit")
                .prLabels(LABEL_FIX_ME)
                .mock(mocks))
                .when().payloadFromString(pullRequestReviewJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    final ArgumentCaptor<String[]> argumentCaptor = ArgumentCaptor.forClass(String[].class);
                    verify(mocks.pullRequest(pullRequestReviewJson.id())).removeLabels(argumentCaptor.capture());
                    MatcherAssert.assertThat(Arrays.asList(argumentCaptor.getValue()),
                            Matchers.containsInAnyOrder(LABEL_FIX_ME));
                });
    }

    @Test
    void notRemoveFixMeLabelOnReviewOnPullRequestEdited() throws IOException {
        pullRequestReviewJson = PullRequestReviewJson.<PullRequestReviewJson.Builder<PullRequestReviewJson>> builder(
                VALID_PR_TEMPLATE_JSON)
                .action(EDITED).build();
        given().github(mocks -> MockedContext.builder(pullRequestReviewJson.id())
                .commit("Mock commit")
                .prLabels(LABEL_FIX_ME)
                .mock(mocks))
                .when().payloadFromString(pullRequestReviewJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    verify(mocks.pullRequest(pullRequestReviewJson.id()), never()).removeLabels(any(String.class));
                });
    }
}
