package io.xstefank.wildfly.bot;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.MockMailbox;
import io.quarkus.test.InMemoryLogHandler;
import io.quarkus.test.junit.QuarkusTest;
import io.xstefank.wildfly.bot.util.GithubProcessor;
import io.xstefank.wildfly.bot.utils.GitHubJson;
import io.xstefank.wildfly.bot.utils.MockedContext;
import io.xstefank.wildfly.bot.utils.TestConstants;
import io.xstefank.wildfly.bot.utils.Util;
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

import java.io.IOException;
import java.util.List;
import java.util.logging.LogManager;

import static io.quarkiverse.githubapp.testing.GitHubAppTesting.given;
import static io.xstefank.wildfly.bot.utils.TestConstants.TEST_REPO;

@QuarkusTest
@GitHubAppTest
public class PRReviewAssignmentTest {

    private static final java.util.logging.Logger rootLogger = LogManager.getLogManager().getLogger("io.xstefank.wildfly.bot");
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
                      notify: [user1, user2]
                  format:
                    title:
                      enabled: false
                    commit:
                      enabled: false
                  emails:
                    - foo@bar.baz
                """;

    private static GitHubJson gitHubJson;
    private MockedContext mockedContext;

    @Inject
    MockMailbox mailbox;

    @BeforeAll
    static void setupTests() throws IOException {
        gitHubJson = GitHubJson.builder(TestConstants.VALID_PR_TEMPLATE_JSON).build();
    }

    @BeforeEach
    void setup() {
        mailbox.clear();
    }

    @Test
    public void testOnlyMissingCollaboratorsNoReviewAssignment() throws IOException {
        mockedContext = MockedContext.builder(gitHubJson.id())
                .prFiles("src/main/java/resource/application.properties");
        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, gitHubJson, mockedContext))
                .when().payloadFromString(gitHubJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    Mockito.verify(mocks.pullRequest(gitHubJson.id()), Mockito.never())
                            .comment(ArgumentMatchers.anyString());
                    Assertions.assertTrue(inMemoryLogHandler.getRecords().stream().anyMatch(
                            logRecord -> logRecord.getMessage().equals(
                                    "Following people are not collaborators in this repository [%s] and can not be requested for PR review: %s")));

                    List<Mail> sent = mailbox.getMailsSentTo("foo@bar.baz");
                    Assertions.assertEquals(sent.size(), 1);
                    Assertions.assertEquals(sent.get(0).getSubject(), GithubProcessor.COLLABORATOR_MISSING_SUBJECT.formatted(TEST_REPO));
                    Assertions.assertEquals(sent.get(0).getText(), GithubProcessor.COLLABORATOR_MISSING_BODY.formatted(
                            TEST_REPO, gitHubJson.number(), List.of("user1", "user2")));
                });
    }

    @Test
    public void testNoCommentOnlyReviewAssignment() throws IOException {
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
    public void testCommentAndReviewAssignmentCombination() throws IOException {
        mockedContext = MockedContext.builder(gitHubJson.id())
                .prFiles("src/main/java/resource/application.properties")
                .collaborators("user1");
        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, gitHubJson, mockedContext))
                .when().payloadFromString(gitHubJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    Mockito.verify(mocks.pullRequest(gitHubJson.id()), Mockito.never())
                            .comment(ArgumentMatchers.anyString());
                    ArgumentCaptor<List<GHUser>> captor = ArgumentCaptor.forClass(List.class);
                    Mockito.verify(mocks.pullRequest(gitHubJson.id())).requestReviewers(captor.capture());
                    Assertions.assertEquals(captor.getValue().size(), 1);
                    MatcherAssert.assertThat(captor.getValue().stream()
                            .map(GHPerson::getLogin)
                            .toList(), Matchers.containsInAnyOrder("user1"));
                    Assertions.assertTrue(inMemoryLogHandler.getRecords().stream().anyMatch(
                            logRecord -> logRecord.getMessage().equals(
                                    "Following people are not collaborators in this repository [%s] and can not be requested for PR review: %s")));

                    List<Mail> sent = mailbox.getMailsSentTo("foo@bar.baz");
                    Assertions.assertEquals(sent.size(), 1);
                    Assertions.assertEquals(sent.get(0).getSubject(), GithubProcessor.COLLABORATOR_MISSING_SUBJECT.formatted(TEST_REPO));
                    Assertions.assertEquals(sent.get(0).getText(), GithubProcessor.COLLABORATOR_MISSING_BODY.formatted(
                            TEST_REPO, gitHubJson.number(), List.of("user2")));
                });
    }
}
