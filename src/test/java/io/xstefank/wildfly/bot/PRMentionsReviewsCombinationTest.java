package io.xstefank.wildfly.bot;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;
import io.xstefank.wildfly.bot.utils.GitHubJson;
import io.xstefank.wildfly.bot.utils.MockedContext;
import io.xstefank.wildfly.bot.utils.TestConstants;
import io.xstefank.wildfly.bot.utils.Util;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GHPerson;
import org.kohsuke.github.GHUser;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.List;

import static io.quarkiverse.githubapp.testing.GitHubAppTesting.given;
import static io.xstefank.wildfly.bot.utils.TestConstants.INVALID_TITLE;

@QuarkusTest
@GitHubAppTest
public class PRMentionsReviewsCombinationTest {
    private static String wildflyConfigFile;

    private static GitHubJson gitHubJson;
    private MockedContext mockedContext;

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
        gitHubJson = GitHubJson.builder(TestConstants.VALID_PR_TEMPLATE_JSON).build();
        mockedContext = MockedContext.builder(gitHubJson.id())
                .prFiles("src/main/java/resource/application.properties")
                .collaborators("user1", "user2");

        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, gitHubJson, mockedContext))
                .when().payloadFromString(gitHubJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    Mockito.verify(mocks.pullRequest(gitHubJson.id()), Mockito.never())
                            .comment(ArgumentMatchers.anyString());
                    ArgumentCaptor<List<GHUser>> captor = ArgumentCaptor.forClass(List.class);
                    Mockito.verify(mocks.pullRequest(gitHubJson.id())).requestReviewers(captor.capture());
                    Assertions.assertEquals(captor.getValue().size(), 2);
                    MatcherAssert.assertThat(captor.getValue().stream()
                            .map(GHPerson::getLogin)
                            .toList(), Matchers.containsInAnyOrder("user1", "user2"));
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
        gitHubJson = GitHubJson.builder(TestConstants.VALID_PR_TEMPLATE_JSON).build();
        mockedContext = MockedContext.builder(gitHubJson.id())
                .collaborators("user1", "user2");

        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, gitHubJson, mockedContext))
                .when().payloadFromString(gitHubJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    Mockito.verify(mocks.pullRequest(gitHubJson.id())).comment("/cc @user1, @user2");
                    Mockito.verify(mocks.pullRequest(gitHubJson.id()), Mockito.never()).requestReviewers(ArgumentMatchers.anyList());
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
        gitHubJson = GitHubJson.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .title(INVALID_TITLE)
                .build();
        mockedContext = MockedContext.builder(gitHubJson.id())
                .prFiles("src/main/java/resource/application.properties")
                .collaborators("user1", "user2");

        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, gitHubJson, mockedContext))
                .when().payloadFromString(gitHubJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    Mockito.verify(mocks.pullRequest(gitHubJson.id()), Mockito.never())
                            .comment(ArgumentMatchers.anyString());
                    ArgumentCaptor<List<GHUser>> captor = ArgumentCaptor.forClass(List.class);
                    Mockito.verify(mocks.pullRequest(gitHubJson.id())).requestReviewers(captor.capture());
                    Assertions.assertEquals(captor.getValue().size(), 2);
                    MatcherAssert.assertThat(captor.getValue().stream()
                            .map(GHPerson::getLogin)
                            .toList(), Matchers.containsInAnyOrder("user1", "user2"));
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
        gitHubJson = GitHubJson.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .title(INVALID_TITLE)
                .build();
        mockedContext = MockedContext.builder(gitHubJson.id())
                .prFiles("src/main/java/resource/application.properties")
                .collaborators("user1", "user2");

        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, gitHubJson, mockedContext))
                .when().payloadFromString(gitHubJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    Mockito.verify(mocks.pullRequest(gitHubJson.id()), Mockito.never())
                            .comment(ArgumentMatchers.anyString());
                    ArgumentCaptor<List<GHUser>> captor = ArgumentCaptor.forClass(List.class);
                    Mockito.verify(mocks.pullRequest(gitHubJson.id())).requestReviewers(captor.capture());
                    Assertions.assertEquals(captor.getValue().size(), 2);
                    MatcherAssert.assertThat(captor.getValue().stream()
                            .map(GHPerson::getLogin)
                            .toList(), Matchers.containsInAnyOrder("user1", "user2"));
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
        gitHubJson = GitHubJson.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .build();
        mockedContext = MockedContext.builder(gitHubJson.id())
                .prFiles("src/main/java/resource/application.properties")
                .collaborators("user1", "user2");

        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, gitHubJson, mockedContext))
                .when().payloadFromString(gitHubJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    Mockito.verify(mocks.pullRequest(gitHubJson.id())).comment("/cc @user3");
                    ArgumentCaptor<List<GHUser>> captor = ArgumentCaptor.forClass(List.class);
                    Mockito.verify(mocks.pullRequest(gitHubJson.id())).requestReviewers(captor.capture());
                    Assertions.assertEquals(captor.getValue().size(), 2);
                    MatcherAssert.assertThat(captor.getValue().stream()
                            .map(GHPerson::getLogin)
                            .toList(), Matchers.containsInAnyOrder("user1", "user2"));
                });
    }
}
