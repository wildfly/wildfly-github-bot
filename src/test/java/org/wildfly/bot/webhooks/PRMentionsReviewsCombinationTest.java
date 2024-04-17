package org.wildfly.bot.webhooks;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;
import org.wildfly.bot.utils.mocking.Mockable;
import org.wildfly.bot.utils.mocking.MockedGHPullRequest;
import org.wildfly.bot.utils.mocking.MockedGHRepository;
import org.wildfly.bot.utils.model.PullRequestJson;
import org.wildfly.bot.utils.TestConstants;
import org.wildfly.bot.utils.WildflyGitHubBotTesting;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GHUser;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static io.quarkiverse.githubapp.testing.GitHubAppTesting.given;

@QuarkusTest
@GitHubAppTest
public class PRMentionsReviewsCombinationTest {
    private static String wildflyConfigFile;

    private static PullRequestJson pullRequestJson;
    private Mockable mockedContext;

    @Test
    public void testMentionsReviewsBothHitSameRule() throws IOException {
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - id: "test"
                      title: "WFLY"
                      directories: [src]
                      notify: [user1, user2]
                  format:
                    title:
                      enabled: false
                    commit:
                      enabled: false
                """;
        pullRequestJson = PullRequestJson.builder(TestConstants.VALID_PR_TEMPLATE_JSON).build();
        mockedContext = MockedGHPullRequest.builder(pullRequestJson.id())
                .files("src/main/java/resource/application.properties")
                .mockNext(MockedGHRepository.builder())
                .users("user1", "user2");

        given().github(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson, mockedContext))
                .when().payloadFromString(pullRequestJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    Mockito.verify(mocks.pullRequest(pullRequestJson.id()), Mockito.never())
                            .comment(ArgumentMatchers.anyString());
                    ArgumentCaptor<List<GHUser>> captor = ArgumentCaptor.forClass(List.class);
                    Mockito.verify(mocks.pullRequest(pullRequestJson.id()), Mockito.times(2))
                            .requestReviewers(captor.capture());
                    List<GHUser> requestedReviewers = captor.getAllValues().stream().flatMap(List::stream).toList();
                    Set<String> requestedReviewersLogins = requestedReviewers.stream().map(GHUser::getLogin)
                            .collect(Collectors.toSet());
                    Assertions.assertEquals(requestedReviewersLogins, Set.of("user1", "user2"));
                });
    }

    @Test
    public void testMentionsReviewsMentionHitSameRule() throws IOException {
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - id: "test"
                      title: "WFLY"
                      directories: [src]
                      notify: [user1, user2]
                  format:
                    title:
                      enabled: false
                    commit:
                      enabled: false
                """;
        pullRequestJson = PullRequestJson.builder(TestConstants.VALID_PR_TEMPLATE_JSON).build();
        mockedContext = MockedGHPullRequest.builder(pullRequestJson.id())
                .mockNext(MockedGHRepository.builder())
                .users("user1", "user2");

        given().github(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson, mockedContext))
                .when().payloadFromString(pullRequestJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    Mockito.verify(mocks.pullRequest(pullRequestJson.id())).comment("/cc @user1, @user2");
                    Mockito.verify(mocks.pullRequest(pullRequestJson.id()), Mockito.never())
                            .requestReviewers(ArgumentMatchers.anyList());
                });
    }

    @Test
    public void testMentionsReviewsReviewsHitSameRule() throws IOException {
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - id: "test"
                      title: "WFLY"
                      directories: [src]
                      notify: [user1, user2]
                  format:
                    title:
                      enabled: false
                    commit:
                      enabled: false
                """;
        pullRequestJson = PullRequestJson.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .title(TestConstants.INVALID_TITLE)
                .build();
        mockedContext = MockedGHPullRequest.builder(pullRequestJson.id())
                .files("src/main/java/resource/application.properties")
                .mockNext(MockedGHRepository.builder())
                .users("user1", "user2");

        given().github(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson, mockedContext))
                .when().payloadFromString(pullRequestJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    Mockito.verify(mocks.pullRequest(pullRequestJson.id()), Mockito.never())
                            .comment(ArgumentMatchers.anyString());
                    ArgumentCaptor<List<GHUser>> captor = ArgumentCaptor.forClass(List.class);
                    Mockito.verify(mocks.pullRequest(pullRequestJson.id()), Mockito.times(2))
                            .requestReviewers(captor.capture());
                    List<GHUser> requestedReviewers = captor.getAllValues().stream().flatMap(List::stream).toList();
                    Set<String> requestedReviewersLogins = requestedReviewers.stream().map(GHUser::getLogin)
                            .collect(Collectors.toSet());
                    Assertions.assertEquals(requestedReviewersLogins, Set.of("user1", "user2"));
                });
    }

    @Test
    public void testMentionsReviewsReviewsHitTwoRules() throws IOException {
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - id: "test"
                      title: "WFLY"
                      directories: [src]
                      notify: [user1, user2]
                    - id: "test2"
                      title: "WFLY"
                      notify: [user1, user2]
                  format:
                    title:
                      enabled: false
                    commit:
                      enabled: false
                """;
        pullRequestJson = PullRequestJson.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .title(TestConstants.INVALID_TITLE)
                .build();
        mockedContext = MockedGHPullRequest.builder(pullRequestJson.id())
                .files("src/main/java/resource/application.properties")
                .mockNext(MockedGHRepository.builder())
                .users("user1", "user2");

        given().github(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson, mockedContext))
                .when().payloadFromString(pullRequestJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    Mockito.verify(mocks.pullRequest(pullRequestJson.id()), Mockito.never())
                            .comment(ArgumentMatchers.anyString());
                    ArgumentCaptor<List<GHUser>> captor = ArgumentCaptor.forClass(List.class);
                    Mockito.verify(mocks.pullRequest(pullRequestJson.id()), Mockito.times(2))
                            .requestReviewers(captor.capture());
                    List<GHUser> requestedReviewers = captor.getAllValues().stream().flatMap(List::stream).toList();
                    Set<String> requestedReviewersLogins = requestedReviewers.stream().map(GHUser::getLogin)
                            .collect(Collectors.toSet());
                    Assertions.assertEquals(requestedReviewersLogins, Set.of("user1", "user2"));
                });
    }

    @Test
    public void testMentionsReviewsMentionsReviewsHitTwoRules() throws IOException {
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - id: "test"
                      title: "WFLY"
                      directories: [src]
                      notify: [user1, user2]
                    - id: "test2"
                      title: "WFLY"
                      notify: [user2, user3]
                  format:
                    title:
                      enabled: false
                    commit:
                      enabled: false
                """;
        pullRequestJson = PullRequestJson.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .build();
        mockedContext = MockedGHPullRequest.builder(pullRequestJson.id())
                .files("src/main/java/resource/application.properties")
                .mockNext(MockedGHRepository.builder())
                .users("user1", "user2");

        given().github(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson, mockedContext))
                .when().payloadFromString(pullRequestJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    Mockito.verify(mocks.pullRequest(pullRequestJson.id())).comment("/cc @user3");
                    ArgumentCaptor<List<GHUser>> captor = ArgumentCaptor.forClass(List.class);
                    Mockito.verify(mocks.pullRequest(pullRequestJson.id()), Mockito.times(2))
                            .requestReviewers(captor.capture());
                    List<GHUser> requestedReviewers = captor.getAllValues().stream().flatMap(List::stream).toList();
                    Set<String> requestedReviewersLogins = requestedReviewers.stream().map(GHUser::getLogin)
                            .collect(Collectors.toSet());
                    Assertions.assertEquals(requestedReviewersLogins, Set.of("user1", "user2"));
                });
    }
}
