package io.xstefank.wildfly.bot;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;
import io.xstefank.wildfly.bot.helper.MockedGHPullRequestProcessor;
import io.xstefank.wildfly.bot.utils.Action;
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
import org.mockito.Mockito;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import static io.quarkiverse.githubapp.testing.GitHubAppTesting.given;
import static org.mockito.ArgumentMatchers.anyString;

@QuarkusTest
@GitHubAppTest
public class PRNotifyChangeOnPREditTest {

    private String wildflyConfigFile;
    private static GitHubJson gitHubJson;

    @BeforeAll
    static void setupTests() throws IOException {
        gitHubJson = GitHubJson.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .action(Action.EDITED)
                .build();
    }

    @Test
    public void testCommentAndReviewAssignmentCombination() throws IOException {
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - id: "new rule"
                      title: "WFLY"
                      notify: [user2]
                    - id: "previous rule"
                      body: "JIRA"
                      notify: [user1]""";
        given().github(mocks -> {
                    Util.mockRepo(mocks, wildflyConfigFile, gitHubJson, null);
                    MockedGHPullRequestProcessor.builder(gitHubJson.id())
                            .collaborators(Set.of("user1", "user2"))
                            .reviewers(Set.of("user1"))
                            .mock(mocks);
                })
                .when().payloadFromString(gitHubJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    Mockito.verify(mocks.pullRequest(gitHubJson.id()), Mockito.never())
                            .comment(anyString());
                    ArgumentCaptor<List<GHUser>> captor = ArgumentCaptor.forClass(List.class);
                    Mockito.verify(mocks.pullRequest(gitHubJson.id())).requestReviewers(captor.capture());
                    Assertions.assertEquals(captor.getValue().size(), 1);
                    MatcherAssert.assertThat(captor.getValue().stream()
                            .map(GHPerson::getLogin)
                            .toList(), Matchers.containsInAnyOrder("user2"));
                });
    }
}
