package io.xstefank.wildfly.bot;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;
import io.xstefank.wildfly.bot.utils.Mockable;
import io.xstefank.wildfly.bot.utils.MockedGHPullRequest;
import io.xstefank.wildfly.bot.utils.PullRequestJson;
import io.xstefank.wildfly.bot.utils.TestConstants;
import io.xstefank.wildfly.bot.utils.Util;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHEvent;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Arrays;

import static io.quarkiverse.githubapp.testing.GitHubAppTesting.given;
import static io.xstefank.wildfly.bot.model.RuntimeConstants.LABEL_FIX_ME;
import static io.xstefank.wildfly.bot.model.RuntimeConstants.LABEL_NEEDS_REBASE;
import static io.xstefank.wildfly.bot.utils.Action.SYNCHRONIZE;

@QuarkusTest
@GitHubAppTest
public class PRAutomaticLabelingTest {

    private String wildflyConfigFile;
    private static PullRequestJson pullRequestJson;
    private Mockable mockedContext;

    @BeforeAll
    static void setupTests() throws IOException {
        pullRequestJson = PullRequestJson.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .action(SYNCHRONIZE)
                .build();
    }

    @Test
    public void testNotApplyAndRemoveRebaseThisLabel() throws IOException {
        mockedContext = MockedGHPullRequest.builder(pullRequestJson.id())
                .labels(LABEL_NEEDS_REBASE, LABEL_FIX_ME);
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - id: "Label rule"
                      title: WFLY
                       """;
        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, pullRequestJson, mockedContext))
                .when().payloadFromString(pullRequestJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    Mockito.verify(mocks.pullRequest(pullRequestJson.id()), Mockito.never())
                            .addLabels(ArgumentMatchers.anyString());
                    final ArgumentCaptor<String[]> argumentCaptor = ArgumentCaptor.forClass(String[].class);
                    Mockito.verify(mocks.pullRequest(pullRequestJson.id())).removeLabels(argumentCaptor.capture());
                    MatcherAssert.assertThat(Arrays.asList(argumentCaptor.getValue()),
                            Matchers.containsInAnyOrder(LABEL_NEEDS_REBASE, LABEL_FIX_ME));
                });
    }

    @Test
    public void testApplyRebaseThisLabel() throws IOException {
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - id: "Label rule"
                      title: WFLY
                       """;
        mockedContext = MockedGHPullRequest.builder(pullRequestJson.id())
                .mergeable(Boolean.FALSE);
        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, pullRequestJson, mockedContext))
                .when().payloadFromString(pullRequestJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    ArgumentCaptor<String[]> argumentCaptor = ArgumentCaptor.forClass(String[].class);
                    Mockito.verify(mocks.pullRequest(pullRequestJson.id())).addLabels(argumentCaptor.capture());
                    MatcherAssert.assertThat(Arrays.asList(argumentCaptor.getValue()),
                            Matchers.containsInAnyOrder(LABEL_NEEDS_REBASE));
                    // we need to capture it otherwise we would not be able to correctly verify this
                    //  any() fails on ambigious call, which is only matcher for vararg params
                    Mockito.verify(mocks.pullRequest(pullRequestJson.id()), Mockito.never())
                            .removeLabels(argumentCaptor.capture());
                });
    }
}
