package org.wildfly.bot.webhooks;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHEvent;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.wildfly.bot.utils.TestConstants;
import org.wildfly.bot.utils.mocking.MockedGHPullRequest;
import org.wildfly.bot.utils.model.ReviewState;
import org.wildfly.bot.utils.model.SsePullRequestReviewPayload;
import org.wildfly.bot.utils.testing.PullRequestJson;
import org.wildfly.bot.utils.testing.PullRequestReviewJsonBuilder;
import org.wildfly.bot.utils.testing.internal.TestModel;
import org.wildfly.bot.utils.testing.model.PullRequestGitHubEventPayload;
import org.wildfly.bot.utils.testing.model.PullRequestReviewGitHubEventPayload;

import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.wildfly.bot.model.RuntimeConstants.LABEL_FIX_ME;
import static org.wildfly.bot.utils.model.Action.EDITED;
import static org.wildfly.bot.utils.model.Action.SYNCHRONIZE;

@QuarkusTest
@GitHubAppTest
public class PRReviewFixMeLabelTest {

    private static PullRequestJson pullRequestReviewJson;

    @Test
    void notAddingFixMeLabelOnPROpen() throws Throwable {
        TestModel.setAllCallables(
                () -> SsePullRequestReviewPayload.builder(TestConstants.VALID_PR_TEMPLATE_JSON),
                PullRequestGitHubEventPayload::new);
        pullRequestReviewJson = TestModel.getPullRequestJson();

        TestModel.given(mocks -> MockedGHPullRequest.builder(pullRequestReviewJson.id()).mock(mocks))
                .sseEventOptions(eventSenderOptions -> eventSenderOptions.payloadFromString(pullRequestReviewJson.jsonString())
                        .event(GHEvent.PULL_REQUEST))
                .pollingEventOptions(
                        eventSenderOptions -> eventSenderOptions.eventFromPayload(pullRequestReviewJson.jsonString()))
                .then(mocks -> {
                    final ArgumentCaptor<String[]> argumentCaptor = ArgumentCaptor.forClass(String[].class);
                    verify(mocks.pullRequest(pullRequestReviewJson.id()), Mockito.never()).addLabels(argumentCaptor.capture());
                })
                .run();
    }

    @Test
    void notAddingFixMeLabelOnReviewComment() throws Throwable {
        TestModel.setAllCallables(
                () -> SsePullRequestReviewPayload.builder(TestConstants.VALID_PR_REVIEW_TEMPLATE_JSON),
                PullRequestReviewGitHubEventPayload::new);
        pullRequestReviewJson = TestModel.<PullRequestReviewJsonBuilder> setPullRequestJsonBuilderBuild(
                pullRequestReviewJsonBuilder -> pullRequestReviewJsonBuilder.state(ReviewState.COMMENT));

        TestModel.given(mocks -> MockedGHPullRequest.builder(pullRequestReviewJson.id()).mock(mocks))
                .sseEventOptions(eventSenderOptions -> eventSenderOptions.payloadFromString(pullRequestReviewJson.jsonString())
                        .event(GHEvent.PULL_REQUEST_REVIEW))
                .pollingEventOptions(
                        eventSenderOptions -> eventSenderOptions.eventFromPayload(pullRequestReviewJson.jsonString()))
                .then(mocks -> {
                    final ArgumentCaptor<String[]> argumentCaptor = ArgumentCaptor.forClass(String[].class);
                    verify(mocks.pullRequest(pullRequestReviewJson.id()), Mockito.never()).addLabels(argumentCaptor.capture());
                })
                .run();
    }

    @Test
    void notAddingFixMeLabelOnReviewApprove() throws Throwable {
        TestModel.setAllCallables(
                () -> SsePullRequestReviewPayload.builder(TestConstants.VALID_PR_REVIEW_TEMPLATE_JSON),
                PullRequestReviewGitHubEventPayload::new);
        pullRequestReviewJson = TestModel.<PullRequestReviewJsonBuilder> setPullRequestJsonBuilderBuild(
                pullRequestReviewJsonBuilder -> pullRequestReviewJsonBuilder.state(ReviewState.APPROVE));

        TestModel.given(mocks -> MockedGHPullRequest.builder(pullRequestReviewJson.id()).mock(mocks))
                .sseEventOptions(eventSenderOptions -> eventSenderOptions.payloadFromString(pullRequestReviewJson.jsonString())
                        .event(GHEvent.PULL_REQUEST_REVIEW))
                .pollingEventOptions(
                        eventSenderOptions -> eventSenderOptions.eventFromPayload(pullRequestReviewJson.jsonString()))
                .then(mocks -> {
                    final ArgumentCaptor<String[]> argumentCaptor = ArgumentCaptor.forClass(String[].class);
                    verify(mocks.pullRequest(pullRequestReviewJson.id()), Mockito.never()).addLabels(argumentCaptor.capture());
                })
                .run();
    }

    @Test
    void addFixMeLabelOnReviewChangesRequested() throws Throwable {
        TestModel.setAllCallables(
                () -> SsePullRequestReviewPayload.builder(TestConstants.VALID_PR_REVIEW_TEMPLATE_JSON),
                PullRequestReviewGitHubEventPayload::new);
        pullRequestReviewJson = TestModel.<PullRequestReviewJsonBuilder> setPullRequestJsonBuilderBuild(
                pullRequestReviewJsonBuilder -> pullRequestReviewJsonBuilder.state(ReviewState.CHANGES_REQUESTED));

        TestModel.given(mocks -> MockedGHPullRequest.builder(pullRequestReviewJson.id()).mock(mocks))
                .sseEventOptions(eventSenderOptions -> eventSenderOptions.payloadFromString(pullRequestReviewJson.jsonString())
                        .event(GHEvent.PULL_REQUEST_REVIEW))
                .pollingEventOptions(
                        eventSenderOptions -> eventSenderOptions.eventFromPayload(pullRequestReviewJson.jsonString()))
                .then(mocks -> {
                    final ArgumentCaptor<String[]> argumentCaptor = ArgumentCaptor.forClass(String[].class);
                    verify(mocks.pullRequest(pullRequestReviewJson.id())).addLabels(argumentCaptor.capture());
                    MatcherAssert.assertThat(Arrays.asList(argumentCaptor.getValue()),
                            Matchers.containsInAnyOrder(LABEL_FIX_ME));
                })
                .run();
    }

    @Test
    void removeFixMeLabelOnReviewOnNewCommit() throws Throwable {
        TestModel.setAllCallables(
                () -> SsePullRequestReviewPayload.builder(TestConstants.VALID_PR_TEMPLATE_JSON),
                PullRequestGitHubEventPayload::new);
        pullRequestReviewJson = TestModel
                .setPullRequestJsonBuilderBuild(pullRequestJsonBuilder -> pullRequestJsonBuilder.action(SYNCHRONIZE));

        TestModel.given(mocks -> MockedGHPullRequest.builder(pullRequestReviewJson.id())
                .commit("Mock commit")
                .labels(LABEL_FIX_ME)
                .mock(mocks))
                .sseEventOptions(eventSenderOptions -> eventSenderOptions.payloadFromString(pullRequestReviewJson.jsonString())
                        .event(GHEvent.PULL_REQUEST))
                .pollingEventOptions(
                        eventSenderOptions -> eventSenderOptions.eventFromPayload(pullRequestReviewJson.jsonString()))
                .then(mocks -> {
                    final ArgumentCaptor<String[]> argumentCaptor = ArgumentCaptor.forClass(String[].class);
                    verify(mocks.pullRequest(pullRequestReviewJson.id())).removeLabels(argumentCaptor.capture());
                    MatcherAssert.assertThat(Arrays.asList(argumentCaptor.getValue()),
                            Matchers.containsInAnyOrder(LABEL_FIX_ME));
                })
                .run();
    }

    @Test
    void notRemoveFixMeLabelOnReviewOnPullRequestEdited() throws Throwable {
        TestModel.setAllCallables(
                () -> SsePullRequestReviewPayload.builder(TestConstants.VALID_PR_TEMPLATE_JSON),
                PullRequestGitHubEventPayload::new);
        pullRequestReviewJson = TestModel
                .setPullRequestJsonBuilderBuild(pullRequestJsonBuilder -> pullRequestJsonBuilder.action(EDITED));

        TestModel.given(mocks -> MockedGHPullRequest.builder(pullRequestReviewJson.id())
                .commit("Mock commit")
                .labels(LABEL_FIX_ME)
                .mock(mocks))
                .sseEventOptions(eventSenderOptions -> eventSenderOptions.payloadFromString(pullRequestReviewJson.jsonString())
                        .event(GHEvent.PULL_REQUEST))
                .pollingEventOptions(
                        eventSenderOptions -> eventSenderOptions.eventFromPayload(pullRequestReviewJson.jsonString()))
                .then(mocks -> {
                    verify(mocks.pullRequest(pullRequestReviewJson.id()), never()).removeLabels(any(String.class));
                })
                .run();
    }
}
