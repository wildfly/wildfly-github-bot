package io.xstefank.wildfly.bot;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;
import io.xstefank.wildfly.bot.model.RuntimeConstants;
import io.xstefank.wildfly.bot.utils.Action;
import io.xstefank.wildfly.bot.utils.MockedContext;
import io.xstefank.wildfly.bot.utils.PullRequestJson;
import io.xstefank.wildfly.bot.utils.TestConstants;
import io.xstefank.wildfly.bot.utils.Util;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHEvent;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.io.IOException;

import static io.quarkiverse.githubapp.testing.GitHubAppTesting.given;
import static io.xstefank.wildfly.bot.util.Strings.blockQuoted;

@QuarkusTest
@GitHubAppTest
public class PRAppendingMessageTest {

    private static final String wildflyConfigFile = """
            wildfly:
              format:
                commit:
                  enabled: false
            """;

    private static final String appendedMessage = String.format("""
            %s

            %s%%s%s""", RuntimeConstants.BOT_MESSAGE_HEADER, blockQuoted(RuntimeConstants.BOT_JIRA_LINKS_HEADER),
            RuntimeConstants.BOT_MESSAGE_FOOTER);

    PullRequestJson pullRequestJson;

    MockedContext mockedContext;

    @Test
    public void testEmptyBodyAppendMessage() throws IOException {
        pullRequestJson = PullRequestJson.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .action(Action.EDITED)
                .title("WFLY-00000 title")
                .description(null)
                .build();

        mockedContext = MockedContext.builder(pullRequestJson.id())
                .commit("WFLY-00000 commit");
        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, pullRequestJson, mockedContext))
                .when().payloadFromString(pullRequestJson.jsonString()).event(GHEvent.PULL_REQUEST).then().github(mocks -> {
                    StringBuilder sb = new StringBuilder();
                    Mockito.verify(mocks.pullRequest(pullRequestJson.id())).setBody(String.format(appendedMessage,
                            sb.append(blockQuoted(
                                    String.format(RuntimeConstants.BOT_JIRA_LINK_COMMENT_TEMPLATE, "WFLY-00000")))));
                });

    }

    @Test
    public void testNonEmptyBodyAppendMessage() throws IOException {
        String body = """
                This is my
                testing

                body, which
                should not be
                cleared.""";
        pullRequestJson = PullRequestJson.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .action(Action.EDITED)
                .title("WFLY-00000 title")
                .description(body)
                .build();

        mockedContext = MockedContext.builder(pullRequestJson.id())
                .commit("WFLY-00000 commit");
        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, pullRequestJson, mockedContext))
                .when().payloadFromString(pullRequestJson.jsonString()).event(GHEvent.PULL_REQUEST).then().github(mocks -> {
                    StringBuilder sb = new StringBuilder();
                    Mockito.verify(mocks.pullRequest(pullRequestJson.id()))
                            .setBody(String.format(body + "\n\n" + appendedMessage,
                                    sb.append(blockQuoted(
                                            String.format(RuntimeConstants.BOT_JIRA_LINK_COMMENT_TEMPLATE, "WFLY-00000")))));
                });
    }

    @Test
    public void testEmptyBodyAppendMessageMultipleLinks() throws IOException {
        pullRequestJson = PullRequestJson.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .action(Action.EDITED)
                .title("WFLY-00001 title")
                .description(null)
                .build();

        mockedContext = MockedContext.builder(pullRequestJson.id())
                .commit("WFLY-00002 commit");
        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, pullRequestJson, mockedContext))
                .when().payloadFromString(pullRequestJson.jsonString()).event(GHEvent.PULL_REQUEST).then().github(mocks -> {
                    StringBuilder sb = new StringBuilder();
                    Mockito.verify(mocks.pullRequest(pullRequestJson.id())).setBody(String.format(appendedMessage,
                            sb.append(blockQuoted(String.format(RuntimeConstants.BOT_JIRA_LINK_COMMENT_TEMPLATE, "WFLY-00001")))
                                    .append(blockQuoted(
                                            String.format(RuntimeConstants.BOT_JIRA_LINK_COMMENT_TEMPLATE, "WFLY-00002")))));
                });
    }

    @Test
    public void testNonEmptyBodyAppendMessageMultipleLinks() throws IOException {
        String body = """
                This is my
                testing

                body, which
                should not be
                cleared.""";
        pullRequestJson = PullRequestJson.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .action(Action.EDITED)
                .title("WFLY-00001 title")
                .description(body)
                .build();

        mockedContext = MockedContext.builder(pullRequestJson.id())
                .commit("WFLY-00002 commit");
        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, pullRequestJson, mockedContext))
                .when().payloadFromString(pullRequestJson.jsonString()).event(GHEvent.PULL_REQUEST).then().github(mocks -> {
                    StringBuilder sb = new StringBuilder();
                    Mockito.verify(mocks.pullRequest(pullRequestJson.id())).setBody(String.format(
                            body + "\n\n" + appendedMessage,
                            sb.append(blockQuoted(String.format(RuntimeConstants.BOT_JIRA_LINK_COMMENT_TEMPLATE, "WFLY-00001")))
                                    .append(blockQuoted(
                                            String.format(RuntimeConstants.BOT_JIRA_LINK_COMMENT_TEMPLATE, "WFLY-00002")))));
                });
    }

    @Test
    public void testEmptyBodyAppendMessageMultipleDifferentLinks() throws IOException {
        pullRequestJson = PullRequestJson.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .action(Action.EDITED)
                .title("WFLY-00001, WFLY-00002 title")
                .description(null)
                .build();

        mockedContext = MockedContext.builder(pullRequestJson.id())
                .commit("WFLY-00002 commit")
                .commit("WFLY-00003 commit");
        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, pullRequestJson, mockedContext))
                .when().payloadFromString(pullRequestJson.jsonString()).event(GHEvent.PULL_REQUEST).then().github(mocks -> {
                    StringBuilder sb = new StringBuilder();
                    Mockito.verify(mocks.pullRequest(pullRequestJson.id())).setBody(String.format(appendedMessage,
                            sb.append(blockQuoted(String.format(RuntimeConstants.BOT_JIRA_LINK_COMMENT_TEMPLATE, "WFLY-00001")))
                                    .append(blockQuoted(
                                            String.format(RuntimeConstants.BOT_JIRA_LINK_COMMENT_TEMPLATE, "WFLY-00002")))
                                    .append(blockQuoted(
                                            String.format(RuntimeConstants.BOT_JIRA_LINK_COMMENT_TEMPLATE, "WFLY-00003")))));
                });
    }

    @Test
    public void testNonEmptyBodyAppendMessageMultipleDifferentLinks() throws IOException {
        String body = """
                This is my
                testing

                body, which
                should not be
                cleared.""";
        pullRequestJson = PullRequestJson.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .action(Action.EDITED)
                .title("WFLY-00001, WFLY-00002 title")
                .description(body)
                .build();

        mockedContext = MockedContext.builder(pullRequestJson.id())
                .commit("WFLY-00002 commit")
                .commit("WFLY-00003 commit");
        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, pullRequestJson, mockedContext))
                .when().payloadFromString(pullRequestJson.jsonString()).event(GHEvent.PULL_REQUEST).then().github(mocks -> {
                    StringBuilder sb = new StringBuilder();
                    Mockito.verify(mocks.pullRequest(pullRequestJson.id())).setBody(String.format(
                            body + "\n\n" + appendedMessage,
                            sb.append(blockQuoted(String.format(RuntimeConstants.BOT_JIRA_LINK_COMMENT_TEMPLATE, "WFLY-00001")))
                                    .append(blockQuoted(
                                            String.format(RuntimeConstants.BOT_JIRA_LINK_COMMENT_TEMPLATE, "WFLY-00002")))
                                    .append(blockQuoted(
                                            String.format(RuntimeConstants.BOT_JIRA_LINK_COMMENT_TEMPLATE, "WFLY-00003")))));
                });
    }

    @Test
    public void testBodyContainingSomeJirasAppendMessageMultipleDifferentLinks() throws IOException {
        String body = """
                This is my
                testing

                body, which
                should not be

                cleared.
                Here is one jira for you https://issues.redhat.com/browse/WFLY-00002""";
        pullRequestJson = PullRequestJson.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .action(Action.EDITED)
                .title("WFLY-00001, WFLY-00002 title")
                .description(body)
                .build();

        mockedContext = MockedContext.builder(pullRequestJson.id())
                .commit("WFLY-00002 commit")
                .commit("WFLY-00003 commit");
        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, pullRequestJson, mockedContext))
                .when().payloadFromString(pullRequestJson.jsonString()).event(GHEvent.PULL_REQUEST).then().github(mocks -> {
                    StringBuilder sb = new StringBuilder();
                    Mockito.verify(mocks.pullRequest(pullRequestJson.id())).setBody(String.format(
                            body + "\n\n" + appendedMessage,
                            sb.append(blockQuoted(String.format(RuntimeConstants.BOT_JIRA_LINK_COMMENT_TEMPLATE, "WFLY-00001")))
                                    .append(blockQuoted(
                                            String.format(RuntimeConstants.BOT_JIRA_LINK_COMMENT_TEMPLATE, "WFLY-00003")))));
                });
    }

    @Test
    public void testBodyContainingAllJirasAppendMessageMultipleDifferentLinks() throws IOException {
        String body = """
                This is my
                testing

                body, which
                should not be
                cleared.

                Listing all jiras:
                https://issues.redhat.com/browse/WFLY-00001
                https://issues.redhat.com/browse/WFLY-00002
                https://issues.redhat.com/browse/WFLY-00003""";
        pullRequestJson = PullRequestJson.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .action(Action.EDITED)
                .title("WFLY-00001, WFLY-00002 title")
                .description(body)
                .build();

        mockedContext = MockedContext.builder(pullRequestJson.id())
                .commit("WFLY-00002 commit")
                .commit("WFLY-00003 commit");
        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, pullRequestJson, mockedContext))
                .when().payloadFromString(pullRequestJson.jsonString()).event(GHEvent.PULL_REQUEST).then().github(mocks -> {
                    Mockito.verify(mocks.pullRequest(pullRequestJson.id()), Mockito.times(0))
                            .setBody(ArgumentMatchers.anyString());
                });
    }
}
