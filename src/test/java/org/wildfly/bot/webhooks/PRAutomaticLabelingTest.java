package org.wildfly.bot.webhooks;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHEvent;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.wildfly.bot.utils.TestConstants;
import org.wildfly.bot.utils.WildflyGitHubBotTesting;
import org.wildfly.bot.utils.mocking.Mockable;
import org.wildfly.bot.utils.mocking.MockedGHPullRequest;
import org.wildfly.bot.utils.model.SsePullRequestPayload;
import org.wildfly.bot.utils.testing.PullRequestJson;
import org.wildfly.bot.utils.testing.internal.TestModel;
import org.wildfly.bot.utils.testing.model.PullRequestGitHubEventPayload;

import java.util.Arrays;

import static org.wildfly.bot.model.RuntimeConstants.LABEL_FIX_ME;
import static org.wildfly.bot.model.RuntimeConstants.LABEL_NEEDS_REBASE;
import static org.wildfly.bot.utils.model.Action.SYNCHRONIZE;

@QuarkusTest
@GitHubAppTest
public class PRAutomaticLabelingTest {

    private String wildflyConfigFile;
    private static PullRequestJson pullRequestJson;
    private Mockable mockedContext;

    @BeforeAll
    static void setupTests() throws Exception {
        TestModel.setAllCallables(
                () -> SsePullRequestPayload.builder(TestConstants.VALID_PR_TEMPLATE_JSON),
                PullRequestGitHubEventPayload::new);

        pullRequestJson = TestModel
                .setPullRequestJsonBuilderBuild(pullRequestJsonBuilder -> pullRequestJsonBuilder.action(SYNCHRONIZE));
    }

    @Test
    public void testNotApplyAndRemoveRebaseThisLabel() throws Throwable {
        mockedContext = MockedGHPullRequest.builder(pullRequestJson.id())
                .labels(LABEL_NEEDS_REBASE, LABEL_FIX_ME);
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - id: "Label rule"
                      title: WFLY
                       """;

        TestModel.given(
                mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson, mockedContext))
                .sseEventOptions(eventSenderOptions -> eventSenderOptions.payloadFromString(pullRequestJson.jsonString())
                        .event(GHEvent.PULL_REQUEST))
                .pollingEventOptions(eventSenderOptions -> eventSenderOptions.eventFromPayload(pullRequestJson.jsonString()))
                .then(mocks -> {
                    Mockito.verify(mocks.pullRequest(pullRequestJson.id()), Mockito.never())
                            .addLabels(ArgumentMatchers.anyString());
                    final ArgumentCaptor<String[]> argumentCaptor = ArgumentCaptor.forClass(String[].class);
                    Mockito.verify(mocks.pullRequest(pullRequestJson.id())).removeLabels(argumentCaptor.capture());
                    MatcherAssert.assertThat(Arrays.asList(argumentCaptor.getValue()),
                            Matchers.containsInAnyOrder(LABEL_NEEDS_REBASE, LABEL_FIX_ME));
                })
                .run();
    }

    @Test
    public void testApplyRebaseThisLabel() throws Throwable {
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - id: "Label rule"
                      title: WFLY
                       """;
        mockedContext = MockedGHPullRequest.builder(pullRequestJson.id())
                .mergeable(Boolean.FALSE);

        TestModel.given(
                mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson, mockedContext))
                .sseEventOptions(eventSenderOptions -> eventSenderOptions.payloadFromString(pullRequestJson.jsonString())
                        .event(GHEvent.PULL_REQUEST))
                .pollingEventOptions(eventSenderOptions -> eventSenderOptions.eventFromPayload(pullRequestJson.jsonString()))
                .then(mocks -> {
                    ArgumentCaptor<String[]> argumentCaptor = ArgumentCaptor.forClass(String[].class);
                    Mockito.verify(mocks.pullRequest(pullRequestJson.id())).addLabels(argumentCaptor.capture());
                    MatcherAssert.assertThat(Arrays.asList(argumentCaptor.getValue()),
                            Matchers.containsInAnyOrder(LABEL_NEEDS_REBASE));
                    // we need to capture it otherwise we would not be able to correctly verify this
                    //  any() fails on ambigious call, which is only matcher for vararg params
                    Mockito.verify(mocks.pullRequest(pullRequestJson.id()), Mockito.never())
                            .removeLabels(argumentCaptor.capture());
                })
                .run();
    }
}
