package io.xstefank.wildfly.bot;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;
import io.xstefank.wildfly.bot.helper.MockedGHPullRequestProcessor;
import io.xstefank.wildfly.bot.utils.GitHubJson;
import io.xstefank.wildfly.bot.utils.TestConstants;
import io.xstefank.wildfly.bot.utils.Util;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GHPerson;
import org.kohsuke.github.GHUser;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import static io.quarkiverse.githubapp.testing.GitHubAppTesting.given;

@QuarkusTest
@GitHubAppTest
public class PRReviewAssignmentTest {

    private static final String wildflyConfigFile = """
                wildfly:
                  rules:
                    - id: "test"
                      title: "WFLY"
                      notify: [user1, user2]
                  format:
                    title:
                      enabled: false
                    commit:
                      enabled: false
                """;

    private static GitHubJson gitHubJson;

    @BeforeAll
    static void setupTests() throws IOException {
        gitHubJson = GitHubJson.builder(TestConstants.VALID_PR_TEMPLATE_JSON).build();
    }

    @Test
    public void testOnlyCommentNoReviewAssignment() throws IOException {
        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, gitHubJson, null))
                .when().payloadFromString(gitHubJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    Mockito.verify(mocks.pullRequest(gitHubJson.id()), Mockito.times(2))
                            .comment("/cc @user1, @user2");
                });
    }

    @Test
    public void testNoCommentOnlyReviewAssignment() throws IOException {
        given().github(mocks -> {
                    Util.mockRepo(mocks, wildflyConfigFile, gitHubJson, null);
                    MockedGHPullRequestProcessor.builder(gitHubJson.id())
                            .collaborators(Set.of("user1", "user2"))
                            .mock(mocks);
                })
                .when().payloadFromString(gitHubJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    Mockito.verify(mocks.pullRequest(gitHubJson.id()), Mockito.never())
                            .comment(ArgumentMatchers.anyString());
                    ArgumentCaptor<List<GHUser>> captor = ArgumentCaptor.forClass(List.class);
                    Mockito.verify(mocks.pullRequest(gitHubJson.id()), Mockito.times(2)).requestReviewers(captor.capture());
                    Assertions.assertEquals(captor.getValue().size(), 2);
                    MatcherAssert.assertThat(captor.getValue().stream()
                            .map(GHPerson::getLogin)
                            .toList(), Matchers.containsInAnyOrder("user1", "user2"));
                });
    }

    @Test
    public void testCommentAndReviewAssignmentCombination() throws IOException {
        given().github(mocks -> {
                    Util.mockRepo(mocks, wildflyConfigFile, gitHubJson, null);
                    MockedGHPullRequestProcessor.builder(gitHubJson.id())
                            .collaborators(Set.of("user1"))
                            .mock(mocks);
                })
                .when().payloadFromString(gitHubJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    Mockito.verify(mocks.pullRequest(gitHubJson.id()), Mockito.times(2))
                            .comment("/cc @user2");
                    ArgumentCaptor<List<GHUser>> captor = ArgumentCaptor.forClass(List.class);
                    Mockito.verify(mocks.pullRequest(gitHubJson.id()), Mockito.times(2)).requestReviewers(captor.capture());
                    Assertions.assertEquals(captor.getValue().size(), 1);
                    MatcherAssert.assertThat(captor.getValue().stream()
                            .map(GHPerson::getLogin)
                            .toList(), Matchers.containsInAnyOrder("user1"));
                });
    }
}
