package org.wildfly.bot.webhooks;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GHPerson;
import org.kohsuke.github.GHUser;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.wildfly.bot.config.WildFlyBotConfig;
import org.wildfly.bot.utils.TestConstants;
import org.wildfly.bot.utils.WildflyGitHubBotTesting;
import org.wildfly.bot.utils.mocking.Mockable;
import org.wildfly.bot.utils.mocking.MockedGHPullRequest;
import org.wildfly.bot.utils.mocking.MockedGHRepository;
import org.wildfly.bot.utils.model.Action;
import org.wildfly.bot.utils.model.SsePullRequestPayload;
import org.wildfly.bot.utils.testing.PullRequestJson;
import org.wildfly.bot.utils.testing.internal.TestModel;
import org.wildfly.bot.utils.testing.model.PullRequestGitHubEventPayload;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;

@QuarkusTest
@GitHubAppTest
public class PRNotifyChangeOnPREditTest {

    private String wildflyConfigFile;
    private static PullRequestJson pullRequestJson;
    private Mockable mockedContext;

    @Inject
    WildFlyBotConfig wildFlyBotConfig;

    @BeforeAll
    static void setPullRequestJson() throws Exception {
        TestModel.setAllCallables(
                () -> SsePullRequestPayload.builder(TestConstants.VALID_PR_TEMPLATE_JSON),
                PullRequestGitHubEventPayload::new);

        pullRequestJson = TestModel
                .setPullRequestJsonBuilderBuild(pullRequestJsonBuilder -> pullRequestJsonBuilder.action(Action.EDITED));
    }

    @Test
    public void updateReviewNoMentionsTest() throws Throwable {
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - id: "new rule"
                      directories: [src]
                      notify: [Butterfly]
                    - id: "previous rule"
                      directories: [app]
                      notify: [Tadpole]""";

        mockedContext = MockedGHPullRequest.builder(pullRequestJson.id())
                .commit("WFLY-123 commit")
                .files("src/pom.xml", "app/pom.xml")
                .reviewers("Tadpole")
                .mockNext(MockedGHRepository.builder())
                .users("Tadpole", "Butterfly");

        TestModel.given(
                mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson, mockedContext))
                .sseEventOptions(eventSenderOptions -> eventSenderOptions.payloadFromString(pullRequestJson.jsonString())
                        .event(GHEvent.PULL_REQUEST))
                .pollingEventOptions(eventSenderOptions -> eventSenderOptions.eventFromPayload(pullRequestJson.jsonString()))
                .then(mocks -> {
                    Mockito.verify(mocks.pullRequest(pullRequestJson.id()), Mockito.never())
                            .comment(anyString());
                    ArgumentCaptor<List<GHUser>> captor = ArgumentCaptor.forClass(List.class);
                    Mockito.verify(mocks.pullRequest(pullRequestJson.id())).requestReviewers(captor.capture());
                    Assertions.assertEquals(captor.getValue().size(), 1);
                    MatcherAssert.assertThat(captor.getValue().stream()
                            .map(GHPerson::getLogin)
                            .toList(), Matchers.containsInAnyOrder("Butterfly"));
                })
                .run();
    }

    @Test
    public void updateReviewKeepMentionsTest() throws Throwable {
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - id: "test"
                      title: "WFLY"
                      directories: [src]
                      notify: [Tadpole, Butterfly]
                    - id: "test2"
                      title: "WFLY"
                      notify: [Butterfly, Duke]""";

        mockedContext = MockedGHPullRequest.builder(pullRequestJson.id())
                .comment("/cc @Duke", wildFlyBotConfig.githubName())
                .commit("WFLY-123 commit")
                .files("src/pom.xml", "app/pom.xml")
                .reviewers("Tadpole")
                .mockNext(MockedGHRepository.builder())
                .users("Tadpole", "Butterfly");

        TestModel.given(
                mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson, mockedContext))
                .sseEventOptions(eventSenderOptions -> eventSenderOptions.payloadFromString(pullRequestJson.jsonString())
                        .event(GHEvent.PULL_REQUEST))
                .pollingEventOptions(eventSenderOptions -> eventSenderOptions.eventFromPayload(pullRequestJson.jsonString()))
                .then(mocks -> {
                    Mockito.verify(mocks.pullRequest(pullRequestJson.id()), Mockito.never())
                            .comment(anyString());
                    ArgumentCaptor<List<GHUser>> captor = ArgumentCaptor.forClass(List.class);
                    Mockito.verify(mocks.pullRequest(pullRequestJson.id())).requestReviewers(captor.capture());
                    Assertions.assertEquals(captor.getValue().size(), 1);
                    MatcherAssert.assertThat(captor.getValue().stream()
                            .map(GHPerson::getLogin)
                            .toList(), Matchers.containsInAnyOrder("Butterfly"));
                })
                .run();
    }

    @Test
    public void updateReviewUpdateMentionsTest() throws Throwable {
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - id: "test"
                      title: "WFLY"
                      directories: [src]
                      notify: [Tadpole, Butterfly]
                    - id: "test2"
                      title: "WFLY"
                      notify: [Butterfly, Duke]""";

        mockedContext = MockedGHPullRequest.builder(pullRequestJson.id())
                .commit("WFLY-123 commit")
                .files("src/pom.xml", "app/pom.xml")
                .reviewers("Tadpole")
                .mockNext(MockedGHRepository.builder())
                .users("Tadpole", "Butterfly");

        TestModel.given(
                mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson, mockedContext))
                .sseEventOptions(eventSenderOptions -> eventSenderOptions.payloadFromString(pullRequestJson.jsonString())
                        .event(GHEvent.PULL_REQUEST))
                .pollingEventOptions(eventSenderOptions -> eventSenderOptions.eventFromPayload(pullRequestJson.jsonString()))
                .then(mocks -> {
                    Mockito.verify(mocks.pullRequest(pullRequestJson.id())).comment("/cc @Duke");
                    ArgumentCaptor<List<GHUser>> captor = ArgumentCaptor.forClass(List.class);
                    Mockito.verify(mocks.pullRequest(pullRequestJson.id())).requestReviewers(captor.capture());
                    Assertions.assertEquals(captor.getValue().size(), 1);
                    MatcherAssert.assertThat(captor.getValue().stream()
                            .map(GHPerson::getLogin)
                            .toList(), Matchers.containsInAnyOrder("Butterfly"));
                })
                .run();
    }

    @Test
    public void noReviewUpdateMentionsTest() throws Throwable {
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - id: "new rule"
                      title: "WFLY"
                      notify: [Butterfly]
                    - id: "previous rule"
                      titleBody: "WFLY"
                      notify: [Tadpole]""";

        mockedContext = MockedGHPullRequest.builder(pullRequestJson.id())
                .comment("/cc @Tadpole", wildFlyBotConfig.githubName())
                .commit("WFLY-123 commit");

        TestModel.given(
                mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson, mockedContext))
                .sseEventOptions(eventSenderOptions -> eventSenderOptions.payloadFromString(pullRequestJson.jsonString())
                        .event(GHEvent.PULL_REQUEST))
                .pollingEventOptions(eventSenderOptions -> eventSenderOptions.eventFromPayload(pullRequestJson.jsonString()))
                .then(mocks -> {
                    Mockito.verify(mocks.issueComment(0L)).update("/cc @Tadpole, @Butterfly");
                    Mockito.verify(mocks.pullRequest(pullRequestJson.id()), Mockito.never()).requestReviewers(anyList());
                })
                .run();
    }

    @Test
    public void keepReviewUpdateMentionsTest() throws Throwable {
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - id: "test"
                      title: "WFLY"
                      directories: [src]
                      notify: [Tadpole, Butterfly]
                    - id: "test2"
                      title: "WFLY"
                      notify: [Butterfly, Duke]""";

        mockedContext = MockedGHPullRequest.builder(pullRequestJson.id())
                .commit("WFLY-123 commit")
                .files("src/pom.xml", "app/pom.xml")
                .reviewers("Tadpole", "Butterfly")
                .mockNext(MockedGHRepository.builder())
                .users("Tadpole", "Butterfly");

        TestModel.given(
                mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson, mockedContext))
                .sseEventOptions(eventSenderOptions -> eventSenderOptions.payloadFromString(pullRequestJson.jsonString())
                        .event(GHEvent.PULL_REQUEST))
                .pollingEventOptions(eventSenderOptions -> eventSenderOptions.eventFromPayload(pullRequestJson.jsonString()))
                .then(mocks -> {
                    Mockito.verify(mocks.pullRequest(pullRequestJson.id())).comment("/cc @Duke");
                    Mockito.verify(mocks.pullRequest(pullRequestJson.id()), Mockito.never()).requestReviewers(anyList());
                })
                .run();
    }
}
