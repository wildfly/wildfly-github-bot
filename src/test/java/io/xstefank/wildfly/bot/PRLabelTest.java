package io.xstefank.wildfly.bot;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;
import io.xstefank.wildfly.bot.model.RuntimeConstants;
import io.xstefank.wildfly.bot.utils.GitHubJson;
import io.xstefank.wildfly.bot.utils.MockedContext;
import io.xstefank.wildfly.bot.utils.TestConstants;
import io.xstefank.wildfly.bot.utils.Util;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GHRepository;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;

import static io.quarkiverse.githubapp.testing.GitHubAppTesting.given;

@QuarkusTest
@GitHubAppTest
public class PRLabelTest {

    private String wildflyConfigFile;
    private static GitHubJson gitHubJson;
    private MockedContext mockedContext;

    @BeforeAll
    static void setupTests() throws IOException {
        gitHubJson = GitHubJson.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .build();
    }

    @Test
    public void testHittingLabelRuleLabelInRepository() throws IOException {
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - id: "Label rule"
                      title: WFLY
                      labels: [label1]
                       """;
        mockedContext = MockedContext.builder(gitHubJson.id())
                .labels(Set.of("label1"));
        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, gitHubJson, mockedContext))
               .when().payloadFromString(gitHubJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    Mockito.verify(mocks.pullRequest(gitHubJson.id())).addLabels("label1");
                });
    }

    @Test
    public void testHittingLabelRuleLabelMissingInRepository() throws IOException {
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - id: "Label rule"
                      title: WFLY
                      labels: [label1]
                       """;
        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, gitHubJson))
                .when().payloadFromString(gitHubJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repository = mocks.repository(TestConstants.TEST_REPO);
                    Mockito.verify(repository).createLabel(ArgumentMatchers.eq("label1"), ArgumentMatchers.anyString());
                    final ArgumentCaptor<String[]> argumentCaptor = ArgumentCaptor.forClass(String[].class);
                    Mockito.verify(mocks.pullRequest(gitHubJson.id())).addLabels(argumentCaptor.capture());
                    MatcherAssert.assertThat(Arrays.asList(argumentCaptor.getValue()), Matchers.containsInAnyOrder("label1"));
                });
    }

    @Test
    public void testHittingLabelRuleSomeLabelsMissingInRepository() throws IOException {
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - id: "Label rule"
                      title: WFLY
                      labels: [label1, label2, label3, label4]
                       """;
        mockedContext = MockedContext.builder(gitHubJson.id())
                .labels(Set.of("label2", "label4"));
        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, gitHubJson, mockedContext))
                .when().payloadFromString(gitHubJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repository = mocks.repository(TestConstants.TEST_REPO);
                    Mockito.verify(repository).createLabel(ArgumentMatchers.eq("label1"), ArgumentMatchers.anyString());
                    Mockito.verify(repository).createLabel(ArgumentMatchers.eq("label3"), ArgumentMatchers.anyString());
                    final ArgumentCaptor<String[]> argumentCaptor = ArgumentCaptor.forClass(String[].class);
                    Mockito.verify(mocks.pullRequest(gitHubJson.id())).addLabels(argumentCaptor.capture());
                    MatcherAssert.assertThat(Arrays.asList(argumentCaptor.getValue()), Matchers.containsInAnyOrder("label1", "label2", "label3", "label4"));
                });
    }

    @Test
    public void testNotApplyRebaseThisLabel() throws IOException {
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - id: "Label rule"
                      title: WFLY
                       """;
        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, gitHubJson))
                .when().payloadFromString(gitHubJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> Mockito.verify(mocks.pullRequest(gitHubJson.id()), Mockito.never()).addLabels(ArgumentMatchers.anyString()));
    }

    @Test
    public void testApplyRebaseThisLabel() throws IOException {
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - id: "Label rule"
                      title: WFLY
                       """;
        mockedContext = MockedContext.builder(gitHubJson.id())
                        .mergeable(Boolean.FALSE);
        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, gitHubJson, mockedContext))
                .when().payloadFromString(gitHubJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    final ArgumentCaptor<String[]> argumentCaptor = ArgumentCaptor.forClass(String[].class);
                    Mockito.verify(mocks.pullRequest(gitHubJson.id())).addLabels(argumentCaptor.capture());
                    MatcherAssert.assertThat(Arrays.asList(argumentCaptor.getValue()), Matchers.containsInAnyOrder(RuntimeConstants.LABEL_NEEDS_REBASE));
                });
    }
}
