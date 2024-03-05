package io.xstefank.wildfly.bot;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;
import io.xstefank.wildfly.bot.config.WildFlyBotConfig;
import io.xstefank.wildfly.bot.utils.Action;
import io.xstefank.wildfly.bot.utils.Mockable;
import io.xstefank.wildfly.bot.utils.MockedGHPullRequest;
import io.xstefank.wildfly.bot.utils.MockedGHRepository;
import io.xstefank.wildfly.bot.utils.PullRequestJson;
import io.xstefank.wildfly.bot.utils.TestConstants;
import io.xstefank.wildfly.bot.utils.Util;
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

import java.io.IOException;
import java.util.List;

import static io.quarkiverse.githubapp.testing.GitHubAppTesting.given;
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
    static void setupTests() throws IOException {
        pullRequestJson = PullRequestJson.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .action(Action.EDITED)
                .build();
    }

    @Test
    public void updateReviewNoMentionsTest() throws IOException {
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - id: "new rule"
                      directories: [src]
                      notify: [user2]
                    - id: "previous rule"
                      directories: [app]
                      notify: [user1]""";
        mockedContext = MockedGHPullRequest.builder(pullRequestJson.id())
                .commit("WFLY-123 commit")
                .files("src/pom.xml", "app/pom.xml")
                .reviewers("user1")
                .mockNext(MockedGHRepository.builder())
                .users("user1", "user2");
        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, pullRequestJson, mockedContext))
                .when().payloadFromString(pullRequestJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    Mockito.verify(mocks.pullRequest(pullRequestJson.id()), Mockito.never())
                            .comment(anyString());
                    ArgumentCaptor<List<GHUser>> captor = ArgumentCaptor.forClass(List.class);
                    Mockito.verify(mocks.pullRequest(pullRequestJson.id())).requestReviewers(captor.capture());
                    Assertions.assertEquals(captor.getValue().size(), 1);
                    MatcherAssert.assertThat(captor.getValue().stream()
                            .map(GHPerson::getLogin)
                            .toList(), Matchers.containsInAnyOrder("user2"));
                });
    }

    @Test
    public void updateReviewKeepMentionsTest() throws IOException {
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - id: "test"
                      title: "WFLY"
                      directories: [src]
                      notify: [user1, user2]
                    - id: "test2"
                      title: "WFLY"
                      notify: [user2, user3]""";
        mockedContext = MockedGHPullRequest.builder(pullRequestJson.id())
                .comment("/cc @user3", wildFlyBotConfig.githubName())
                .commit("WFLY-123 commit")
                .files("src/pom.xml", "app/pom.xml")
                .reviewers("user1")
                .mockNext(MockedGHRepository.builder())
                .users("user1", "user2");
        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, pullRequestJson, mockedContext))
                .when().payloadFromString(pullRequestJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    Mockito.verify(mocks.pullRequest(pullRequestJson.id()), Mockito.never())
                            .comment(anyString());
                    ArgumentCaptor<List<GHUser>> captor = ArgumentCaptor.forClass(List.class);
                    Mockito.verify(mocks.pullRequest(pullRequestJson.id())).requestReviewers(captor.capture());
                    Assertions.assertEquals(captor.getValue().size(), 1);
                    MatcherAssert.assertThat(captor.getValue().stream()
                            .map(GHPerson::getLogin)
                            .toList(), Matchers.containsInAnyOrder("user2"));
                });
    }

    @Test
    public void updateReviewUpdateMentionsTest() throws IOException {
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - id: "test"
                      title: "WFLY"
                      directories: [src]
                      notify: [user1, user2]
                    - id: "test2"
                      title: "WFLY"
                      notify: [user2, user3]""";
        mockedContext = MockedGHPullRequest.builder(pullRequestJson.id())
                .commit("WFLY-123 commit")
                .files("src/pom.xml", "app/pom.xml")
                .reviewers("user1")
                .mockNext(MockedGHRepository.builder())
                .users("user1", "user2");
        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, pullRequestJson, mockedContext))
                .when().payloadFromString(pullRequestJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    Mockito.verify(mocks.pullRequest(pullRequestJson.id())).comment("/cc @user3");
                    ArgumentCaptor<List<GHUser>> captor = ArgumentCaptor.forClass(List.class);
                    Mockito.verify(mocks.pullRequest(pullRequestJson.id())).requestReviewers(captor.capture());
                    Assertions.assertEquals(captor.getValue().size(), 1);
                    MatcherAssert.assertThat(captor.getValue().stream()
                            .map(GHPerson::getLogin)
                            .toList(), Matchers.containsInAnyOrder("user2"));
                });
    }

    @Test
    public void noReviewUpdateMentionsTest() throws IOException {
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - id: "new rule"
                      title: "WFLY"
                      notify: [user2]
                    - id: "previous rule"
                      titleBody: "WFLY"
                      notify: [user1]""";
        mockedContext = MockedGHPullRequest.builder(pullRequestJson.id())
                .comment("/cc @user1", wildFlyBotConfig.githubName())
                .commit("WFLY-123 commit");
        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, pullRequestJson, mockedContext))
                .when().payloadFromString(pullRequestJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    Mockito.verify(mocks.issueComment(0L)).update("/cc @user1, @user2");
                    Mockito.verify(mocks.pullRequest(pullRequestJson.id()), Mockito.never()).requestReviewers(anyList());
                });
    }

    @Test
    public void keepReviewUpdateMentionsTest() throws IOException {
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - id: "test"
                      title: "WFLY"
                      directories: [src]
                      notify: [user1, user2]
                    - id: "test2"
                      title: "WFLY"
                      notify: [user2, user3]""";
        mockedContext = MockedGHPullRequest.builder(pullRequestJson.id())
                .commit("WFLY-123 commit")
                .files("src/pom.xml", "app/pom.xml")
                .reviewers("user1", "user2")
                .mockNext(MockedGHRepository.builder())
                .users("user1", "user2");
        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, pullRequestJson, mockedContext))
                .when().payloadFromString(pullRequestJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    Mockito.verify(mocks.pullRequest(pullRequestJson.id())).comment("/cc @user3");
                    Mockito.verify(mocks.pullRequest(pullRequestJson.id()), Mockito.never()).requestReviewers(anyList());
                });
    }
}
