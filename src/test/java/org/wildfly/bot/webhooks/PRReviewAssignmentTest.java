package org.wildfly.bot.webhooks;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.MockMailbox;
import io.quarkus.test.InMemoryLogHandler;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jboss.logmanager.Level;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GHPerson;
import org.kohsuke.github.GHUser;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.wildfly.bot.util.GithubProcessor;
import org.wildfly.bot.utils.TestConstants;
import org.wildfly.bot.utils.WildflyGitHubBotTesting;
import org.wildfly.bot.utils.mocking.Mockable;
import org.wildfly.bot.utils.mocking.MockedGHPullRequest;
import org.wildfly.bot.utils.mocking.MockedGHRepository;
import org.wildfly.bot.utils.model.PullRequestJson;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.logging.LogManager;
import java.util.stream.Collectors;

import static io.quarkiverse.githubapp.testing.GitHubAppTesting.given;

@QuarkusTest
@GitHubAppTest
public class PRReviewAssignmentTest {

    private static final java.util.logging.Logger rootLogger = LogManager.getLogManager().getLogger("org.wildfly.bot");
    private static final InMemoryLogHandler inMemoryLogHandler = new InMemoryLogHandler(
            record -> record.getLevel().intValue() >= Level.ALL.intValue());

    static {
        rootLogger.addHandler(inMemoryLogHandler);
    }

    private static final String wildflyConfigFile = """
            wildfly:
              rules:
                - id: "test"
                  directories: [src]
                  notify: [Tadpole, Butterfly]
              format:
                title:
                  enabled: false
                commit:
                  enabled: false
              emails:
                - foo@bar.baz
            """;

    private static PullRequestJson pullRequestJson;
    private Mockable mockedContext;

    @Inject
    MockMailbox mailbox;

    @BeforeAll
    static void setupTests() throws IOException {
        pullRequestJson = PullRequestJson.builder(TestConstants.VALID_PR_TEMPLATE_JSON).build();
    }

    @BeforeEach
    void setup() {
        mailbox.clear();
    }

    @Test
    public void testOnlyMissingCollaboratorsNoReviewAssignment() throws IOException {
        mockedContext = MockedGHPullRequest.builder(pullRequestJson.id())
                .files("src/main/java/resource/application.properties");
        given().github(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson, mockedContext))
                .when().payloadFromString(pullRequestJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    Mockito.verify(mocks.pullRequest(pullRequestJson.id()), Mockito.never())
                            .comment(ArgumentMatchers.anyString());
                    Assertions.assertTrue(inMemoryLogHandler.getRecords().stream().anyMatch(
                            logRecord -> logRecord.getMessage().contains(
                                    "Bot can not request PR review from the following people: [Butterfly, Tadpole]")));

                    List<Mail> sent = mailbox.getMailsSentTo("foo@bar.baz");
                    Assertions.assertEquals(sent.size(), 1);
                    Assertions.assertEquals(sent.get(0).getSubject(),
                            GithubProcessor.COLLABORATOR_MISSING_SUBJECT.formatted(TestConstants.TEST_REPO));
                    Assertions.assertEquals(sent.get(0).getText(), GithubProcessor.COLLABORATOR_MISSING_BODY.formatted(
                            TestConstants.TEST_REPO, pullRequestJson.number(), List.of("Butterfly", "Tadpole")));
                });
    }

    @Test
    public void testNoCommentOnlyReviewAssignment() throws IOException {
        mockedContext = MockedGHPullRequest.builder(pullRequestJson.id())
                .files("src/main/java/resource/application.properties")
                .mockNext(MockedGHRepository.builder())
                .users("Tadpole", "Butterfly");
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
                    Assertions.assertEquals(requestedReviewersLogins, Set.of("Tadpole", "Butterfly"));
                });
    }

    @Test
    public void testCommentAndReviewAssignmentCombination() throws IOException {
        mockedContext = MockedGHPullRequest.builder(pullRequestJson.id())
                .files("src/main/java/resource/application.properties")
                .mockNext(MockedGHRepository.builder())
                .users("Tadpole");
        given().github(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson, mockedContext))
                .when().payloadFromString(pullRequestJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    Mockito.verify(mocks.pullRequest(pullRequestJson.id()), Mockito.never())
                            .comment(ArgumentMatchers.anyString());
                    ArgumentCaptor<List<GHUser>> captor = ArgumentCaptor.forClass(List.class);
                    Mockito.verify(mocks.pullRequest(pullRequestJson.id())).requestReviewers(captor.capture());
                    Assertions.assertEquals(captor.getValue().size(), 1);
                    MatcherAssert.assertThat(captor.getValue().stream()
                            .map(GHPerson::getLogin)
                            .toList(), Matchers.containsInAnyOrder("Tadpole"));
                    Assertions.assertTrue(inMemoryLogHandler.getRecords().stream().anyMatch(
                            logRecord -> logRecord.getMessage().contains(
                                    "Bot can not request PR review from the following people: [Butterfly]")));

                    List<Mail> sent = mailbox.getMailsSentTo("foo@bar.baz");
                    Assertions.assertEquals(sent.size(), 1);
                    Assertions.assertEquals(sent.get(0).getSubject(),
                            GithubProcessor.COLLABORATOR_MISSING_SUBJECT.formatted(TestConstants.TEST_REPO));
                    Assertions.assertEquals(sent.get(0).getText(), GithubProcessor.COLLABORATOR_MISSING_BODY.formatted(
                            TestConstants.TEST_REPO, pullRequestJson.number(), List.of("Butterfly")));
                });
    }
}
