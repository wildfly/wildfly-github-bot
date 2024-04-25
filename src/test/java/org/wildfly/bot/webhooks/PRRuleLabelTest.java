package org.wildfly.bot.webhooks;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GHRepository;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.wildfly.bot.utils.TestConstants;
import org.wildfly.bot.utils.WildflyGitHubBotTesting;
import org.wildfly.bot.utils.mocking.Mockable;
import org.wildfly.bot.utils.mocking.MockedGHRepository;
import org.wildfly.bot.utils.model.SsePullRequestPayload;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;

import static io.quarkiverse.githubapp.testing.GitHubAppTesting.given;
import static org.wildfly.bot.utils.model.Action.SYNCHRONIZE;

@QuarkusTest
@GitHubAppTest
public class PRRuleLabelTest {

    private String wildflyConfigFile;
    private static SsePullRequestPayload ssePullRequestPayload;
    private Mockable mockedContext;

    @BeforeAll
    static void setupTests() throws IOException {
        ssePullRequestPayload = SsePullRequestPayload.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
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
        given().github(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, ssePullRequestPayload, mockedContext))
                .when().payloadFromString(ssePullRequestPayload.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    Mockito.verify(mocks.pullRequest(ssePullRequestPayload.id())).addLabels("label1");
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
        given().github(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, ssePullRequestPayload))
                .when().payloadFromString(ssePullRequestPayload.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repository = mocks.repository(TestConstants.TEST_REPO);
                    Mockito.verify(repository).createLabel(ArgumentMatchers.eq("label1"), ArgumentMatchers.anyString());
                    final ArgumentCaptor<String[]> argumentCaptor = ArgumentCaptor.forClass(String[].class);
                    Mockito.verify(mocks.pullRequest(ssePullRequestPayload.id())).addLabels(argumentCaptor.capture());
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
        given().github(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, ssePullRequestPayload, mockedContext))
                .when().payloadFromString(ssePullRequestPayload.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHRepository repository = mocks.repository(TestConstants.TEST_REPO);
                    Mockito.verify(repository).createLabel(ArgumentMatchers.eq("label1"), ArgumentMatchers.anyString());
                    Mockito.verify(repository).createLabel(ArgumentMatchers.eq("label3"), ArgumentMatchers.anyString());
                    final ArgumentCaptor<String[]> argumentCaptor = ArgumentCaptor.forClass(String[].class);
                    Mockito.verify(mocks.pullRequest(ssePullRequestPayload.id())).addLabels(argumentCaptor.capture());
                    MatcherAssert.assertThat(Arrays.asList(argumentCaptor.getValue()),
                            Matchers.containsInAnyOrder("label1", "label2", "label3", "label4"));
                });
    }
}
