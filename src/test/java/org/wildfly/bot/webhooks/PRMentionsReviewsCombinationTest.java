package org.wildfly.bot.webhooks;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GHUser;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.wildfly.bot.utils.TestConstants;
import org.wildfly.bot.utils.WildflyGitHubBotTesting;
import org.wildfly.bot.utils.mocking.Mockable;
import org.wildfly.bot.utils.mocking.MockedGHPullRequest;
import org.wildfly.bot.utils.mocking.MockedGHRepository;
import org.wildfly.bot.utils.model.SsePullRequestPayload;
import org.wildfly.bot.utils.testing.PullRequestJson;
import org.wildfly.bot.utils.testing.internal.TestModel;
import org.wildfly.bot.utils.testing.model.PullRequestGitHubEventPayload;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@QuarkusTest
@GitHubAppTest
public class PRMentionsReviewsCombinationTest {
    private static String wildflyConfigFile;

    private static PullRequestJson pullRequestJson;
    private Mockable mockedContext;

    @BeforeAll
    static void setPullRequestJson() {
        TestModel.setAllCallables(
                () -> SsePullRequestPayload.builder(TestConstants.VALID_PR_TEMPLATE_JSON),
                PullRequestGitHubEventPayload::new);
    }

    @Test
    public void testMentionsReviewsBothHitSameRule() throws Throwable {
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - id: "test"
                      title: "WFLY"
                      directories: [src]
                      notify: [Tadpole, Butterfly]
                  format:
                    title:
                      enabled: false
                    commit:
                      enabled: false
                """;

        pullRequestJson = TestModel.getPullRequestJson();
        mockedContext = MockedGHPullRequest.builder(pullRequestJson.id())
                .files("src/main/java/resource/application.properties")
                .mockNext(MockedGHRepository.builder())
                .users("Tadpole", "Butterfly");

        TestModel.given(
                mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson, mockedContext))
                .sseEventOptions(eventSenderOptions -> eventSenderOptions.payloadFromString(pullRequestJson.jsonString())
                        .event(GHEvent.PULL_REQUEST))
                .pollingEventOptions(eventSenderOptions -> eventSenderOptions.eventFromPayload(pullRequestJson.jsonString()))
                .then(mocks -> {
                    Mockito.verify(mocks.pullRequest(pullRequestJson.id()), Mockito.never())
                            .comment(ArgumentMatchers.anyString());
                    ArgumentCaptor<List<GHUser>> captor = ArgumentCaptor.forClass(List.class);
                    Mockito.verify(mocks.pullRequest(pullRequestJson.id()), Mockito.times(2))
                            .requestReviewers(captor.capture());
                    List<GHUser> requestedReviewers = captor.getAllValues().stream().flatMap(List::stream).toList();
                    Set<String> requestedReviewersLogins = requestedReviewers.stream().map(GHUser::getLogin)
                            .collect(Collectors.toSet());
                    Assertions.assertEquals(requestedReviewersLogins, Set.of("Tadpole", "Butterfly"));
                })
                .run();
    }

    @Test
    public void testMentionsReviewsMentionHitSameRule() throws Throwable {
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - id: "test"
                      title: "WFLY"
                      directories: [src]
                      notify: [Tadpole, Butterfly]
                  format:
                    title:
                      enabled: false
                    commit:
                      enabled: false
                """;

        pullRequestJson = TestModel.getPullRequestJson();
        mockedContext = MockedGHPullRequest.builder(pullRequestJson.id())
                .mockNext(MockedGHRepository.builder())
                .users("Tadpole", "Butterfly");

        TestModel.given(
                mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson, mockedContext))
                .sseEventOptions(eventSenderOptions -> eventSenderOptions.payloadFromString(pullRequestJson.jsonString())
                        .event(GHEvent.PULL_REQUEST))
                .pollingEventOptions(eventSenderOptions -> eventSenderOptions.eventFromPayload(pullRequestJson.jsonString()))
                .then(mocks -> {
                    Mockito.verify(mocks.pullRequest(pullRequestJson.id())).comment("/cc @Butterfly, @Tadpole");
                    Mockito.verify(mocks.pullRequest(pullRequestJson.id()), Mockito.never())
                            .requestReviewers(ArgumentMatchers.anyList());
                })
                .run();
    }

    @Test
    public void testMentionsReviewsReviewsHitSameRule() throws Throwable {
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - id: "test"
                      title: "WFLY"
                      directories: [src]
                      notify: [Tadpole, Butterfly]
                  format:
                    title:
                      enabled: false
                    commit:
                      enabled: false
                """;

        pullRequestJson = TestModel.setPullRequestJsonBuilderBuild(
                pullRequestJsonBuilder -> pullRequestJsonBuilder.title(TestConstants.INVALID_TITLE));
        mockedContext = MockedGHPullRequest.builder(pullRequestJson.id())
                .files("src/main/java/resource/application.properties")
                .mockNext(MockedGHRepository.builder())
                .users("Tadpole", "Butterfly");

        TestModel.given(
                mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson, mockedContext))
                .sseEventOptions(eventSenderOptions -> eventSenderOptions.payloadFromString(pullRequestJson.jsonString())
                        .event(GHEvent.PULL_REQUEST))
                .pollingEventOptions(eventSenderOptions -> eventSenderOptions.eventFromPayload(pullRequestJson.jsonString()))
                .then(mocks -> {
                    Mockito.verify(mocks.pullRequest(pullRequestJson.id()), Mockito.never())
                            .comment(ArgumentMatchers.anyString());
                    ArgumentCaptor<List<GHUser>> captor = ArgumentCaptor.forClass(List.class);
                    Mockito.verify(mocks.pullRequest(pullRequestJson.id()), Mockito.times(2))
                            .requestReviewers(captor.capture());
                    List<GHUser> requestedReviewers = captor.getAllValues().stream().flatMap(List::stream).toList();
                    Set<String> requestedReviewersLogins = requestedReviewers.stream().map(GHUser::getLogin)
                            .collect(Collectors.toSet());
                    Assertions.assertEquals(requestedReviewersLogins, Set.of("Tadpole", "Butterfly"));
                })
                .run();
    }

    @Test
    public void testMentionsReviewsReviewsHitTwoRules() throws Throwable {
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - id: "test"
                      title: "WFLY"
                      directories: [src]
                      notify: [Tadpole, Butterfly]
                    - id: "test2"
                      title: "WFLY"
                      notify: [Tadpole, Butterfly]
                  format:
                    title:
                      enabled: false
                    commit:
                      enabled: false
                """;

        pullRequestJson = TestModel.setPullRequestJsonBuilderBuild(
                pullRequestJsonBuilder -> pullRequestJsonBuilder.title(TestConstants.INVALID_TITLE));
        mockedContext = MockedGHPullRequest.builder(pullRequestJson.id())
                .files("src/main/java/resource/application.properties")
                .mockNext(MockedGHRepository.builder())
                .users("Tadpole", "Butterfly");

        TestModel.given(
                mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson, mockedContext))
                .sseEventOptions(eventSenderOptions -> eventSenderOptions.payloadFromString(pullRequestJson.jsonString())
                        .event(GHEvent.PULL_REQUEST))
                .pollingEventOptions(eventSenderOptions -> eventSenderOptions.eventFromPayload(pullRequestJson.jsonString()))
                .then(mocks -> {
                    Mockito.verify(mocks.pullRequest(pullRequestJson.id()), Mockito.never())
                            .comment(ArgumentMatchers.anyString());
                    ArgumentCaptor<List<GHUser>> captor = ArgumentCaptor.forClass(List.class);
                    Mockito.verify(mocks.pullRequest(pullRequestJson.id()), Mockito.times(2))
                            .requestReviewers(captor.capture());
                    List<GHUser> requestedReviewers = captor.getAllValues().stream().flatMap(List::stream).toList();
                    Set<String> requestedReviewersLogins = requestedReviewers.stream().map(GHUser::getLogin)
                            .collect(Collectors.toSet());
                    Assertions.assertEquals(requestedReviewersLogins, Set.of("Tadpole", "Butterfly"));
                })
                .run();
    }

    @Test
    public void testMentionsReviewsMentionsReviewsHitTwoRules() throws Throwable {
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - id: "test"
                      title: "WFLY"
                      directories: [src]
                      notify: [Tadpole, Butterfly]
                    - id: "test2"
                      title: "WFLY"
                      notify: [Butterfly, Duke]
                  format:
                    title:
                      enabled: false
                    commit:
                      enabled: false
                """;

        pullRequestJson = TestModel.getPullRequestJson();
        mockedContext = MockedGHPullRequest.builder(pullRequestJson.id())
                .files("src/main/java/resource/application.properties")
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
                    Mockito.verify(mocks.pullRequest(pullRequestJson.id()), Mockito.times(2))
                            .requestReviewers(captor.capture());
                    List<GHUser> requestedReviewers = captor.getAllValues().stream().flatMap(List::stream).toList();
                    Set<String> requestedReviewersLogins = requestedReviewers.stream().map(GHUser::getLogin)
                            .collect(Collectors.toSet());
                    Assertions.assertEquals(requestedReviewersLogins, Set.of("Tadpole", "Butterfly"));
                })
                .run();
    }
}
