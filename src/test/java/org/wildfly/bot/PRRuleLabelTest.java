package org.wildfly.bot;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;
import org.wildfly.bot.utils.Mockable;
import org.wildfly.bot.utils.MockedGHRepository;
import org.wildfly.bot.utils.PullRequestJson;
import org.wildfly.bot.utils.TestConstants;
import org.wildfly.bot.utils.Util;
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
import static org.wildfly.bot.utils.Action.SYNCHRONIZE;

@QuarkusTest
@GitHubAppTest
public class PRRuleLabelTest {

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
    public void testHittingLabelRuleLabelInRepository() throws IOException {
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - id: "Label rule"
                      title: WFLY
                      labels: [label1]
                       """;
        mockedContext = MockedGHRepository.builder()
                .labels(Set.of("label1"));
        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, pullRequestJson, mockedContext))
                .when().payloadFromString(pullRequestJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    Mockito.verify(mocks.pullRequest(pullRequestJson.id())).addLabels("label1");
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
        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, pullRequestJson))
                .when().payloadFromString(pullRequestJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repository = mocks.repository(TestConstants.TEST_REPO);
                    Mockito.verify(repository).createLabel(ArgumentMatchers.eq("label1"), ArgumentMatchers.anyString());
                    final ArgumentCaptor<String[]> argumentCaptor = ArgumentCaptor.forClass(String[].class);
                    Mockito.verify(mocks.pullRequest(pullRequestJson.id())).addLabels(argumentCaptor.capture());
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
        mockedContext = MockedGHRepository.builder()
                .labels(Set.of("label2", "label4"));
        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, pullRequestJson, mockedContext))
                .when().payloadFromString(pullRequestJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repository = mocks.repository(TestConstants.TEST_REPO);
                    Mockito.verify(repository).createLabel(ArgumentMatchers.eq("label1"), ArgumentMatchers.anyString());
                    Mockito.verify(repository).createLabel(ArgumentMatchers.eq("label3"), ArgumentMatchers.anyString());
                    final ArgumentCaptor<String[]> argumentCaptor = ArgumentCaptor.forClass(String[].class);
                    Mockito.verify(mocks.pullRequest(pullRequestJson.id())).addLabels(argumentCaptor.capture());
                    MatcherAssert.assertThat(Arrays.asList(argumentCaptor.getValue()),
                            Matchers.containsInAnyOrder("label1", "label2", "label3", "label4"));
                });
    }
}
