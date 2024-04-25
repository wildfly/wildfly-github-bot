package org.wildfly.bot.webhooks;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHEvent;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.wildfly.bot.config.WildFlyBotConfig;
import org.wildfly.bot.model.RuntimeConstants;
import org.wildfly.bot.utils.TestConstants;
import org.wildfly.bot.utils.WildflyGitHubBotTesting;
import org.wildfly.bot.utils.mocking.MockedGHPullRequest;
import org.wildfly.bot.utils.model.Action;
import org.wildfly.bot.utils.model.SsePullRequestPayload;

import java.io.IOException;

import static io.quarkiverse.githubapp.testing.GitHubAppTesting.given;
import static org.wildfly.bot.model.RuntimeConstants.BOT_JIRA_LINK_COMMENT_TEMPLATE;
import static org.wildfly.bot.model.RuntimeConstants.BOT_MESSAGE_DELIMINER;
import static org.wildfly.bot.model.RuntimeConstants.BOT_REPO_REF_FOOTER;
import static org.wildfly.bot.util.Strings.blockQuoted;

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
            %s%s%%s%s""", RuntimeConstants.BOT_MESSAGE_HEADER,
            blockQuoted(RuntimeConstants.BOT_JIRA_LINKS_HEADER),
            RuntimeConstants.BOT_MESSAGE_FOOTER);

    private static final String WITH_DELIMINER = BOT_MESSAGE_DELIMINER + "\n\n";

    @Inject
    WildFlyBotConfig wildFlyBotConfig;

    SsePullRequestPayload ssePullRequestPayload;

    MockedGHPullRequest mockedContext;

    @Test
    public void testEmptyBodyAppendMessage() throws IOException {
        ssePullRequestPayload = SsePullRequestPayload.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .action(Action.EDITED)
                .title("WFLY-00000 title")
                .description(null)
                .build();

        mockedContext = MockedGHPullRequest.builder(ssePullRequestPayload.id())
                .commit("WFLY-00000 commit");
        given().github(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, ssePullRequestPayload, mockedContext))
                .when().payloadFromString(ssePullRequestPayload.jsonString()).event(GHEvent.PULL_REQUEST).then().github(mocks -> {
                    StringBuilder sb = new StringBuilder();
                    Mockito.verify(mocks.pullRequest(ssePullRequestPayload.id()))
                            .setBody(ArgumentMatchers.contains(String.format(appendedMessage,
                                    sb.append(blockQuoted(
                                            String.format(RuntimeConstants.BOT_JIRA_LINK_COMMENT_TEMPLATE, "WFLY-00000"))))));
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
        ssePullRequestPayload = SsePullRequestPayload.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .action(Action.EDITED)
                .title("WFLY-00000 title")
                .description(body)
                .build();

        mockedContext = MockedGHPullRequest.builder(ssePullRequestPayload.id())
                .commit("WFLY-00000 commit");
        given().github(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, ssePullRequestPayload, mockedContext))
                .when().payloadFromString(ssePullRequestPayload.jsonString()).event(GHEvent.PULL_REQUEST).then().github(mocks -> {
                    StringBuilder sb = new StringBuilder();
                    Mockito.verify(mocks.pullRequest(ssePullRequestPayload.id()))
                            .setBody(ArgumentMatchers.contains(String.format(body + WITH_DELIMINER + appendedMessage,
                                    sb.append(blockQuoted(
                                            String.format(RuntimeConstants.BOT_JIRA_LINK_COMMENT_TEMPLATE, "WFLY-00000"))))));
                });
    }

    @Test
    public void testEmptyBodyAppendMessageMultipleLinks() throws IOException {
        ssePullRequestPayload = SsePullRequestPayload.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .action(Action.EDITED)
                .title("WFLY-00001 title")
                .description(null)
                .build();

        mockedContext = MockedGHPullRequest.builder(ssePullRequestPayload.id())
                .commit("WFLY-00002 commit");
        given().github(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, ssePullRequestPayload, mockedContext))
                .when().payloadFromString(ssePullRequestPayload.jsonString()).event(GHEvent.PULL_REQUEST).then().github(mocks -> {
                    StringBuilder sb = new StringBuilder();
                    Mockito.verify(mocks.pullRequest(ssePullRequestPayload.id())).setBody(ArgumentMatchers.contains(String.format(
                            appendedMessage,
                            sb.append(blockQuoted(String.format(RuntimeConstants.BOT_JIRA_LINK_COMMENT_TEMPLATE, "WFLY-00001")))
                                    .append(blockQuoted(
                                            String.format(RuntimeConstants.BOT_JIRA_LINK_COMMENT_TEMPLATE, "WFLY-00002"))))));
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
        ssePullRequestPayload = SsePullRequestPayload.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .action(Action.EDITED)
                .title("WFLY-00001 title")
                .description(body)
                .build();

        mockedContext = MockedGHPullRequest.builder(ssePullRequestPayload.id())
                .commit("WFLY-00002 commit");
        given().github(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, ssePullRequestPayload, mockedContext))
                .when().payloadFromString(ssePullRequestPayload.jsonString()).event(GHEvent.PULL_REQUEST).then().github(mocks -> {
                    StringBuilder sb = new StringBuilder();
                    Mockito.verify(mocks.pullRequest(ssePullRequestPayload.id())).setBody(ArgumentMatchers.contains(String.format(
                            body + WITH_DELIMINER + appendedMessage,
                            sb.append(blockQuoted(String.format(RuntimeConstants.BOT_JIRA_LINK_COMMENT_TEMPLATE, "WFLY-00001")))
                                    .append(blockQuoted(
                                            String.format(RuntimeConstants.BOT_JIRA_LINK_COMMENT_TEMPLATE, "WFLY-00002"))))));
                });
    }

    @Test
    public void testEmptyBodyAppendMessageMultipleDifferentLinks() throws IOException {
        ssePullRequestPayload = SsePullRequestPayload.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .action(Action.EDITED)
                .title("WFLY-00001, WFLY-00002 title")
                .description(null)
                .build();

        mockedContext = MockedGHPullRequest.builder(ssePullRequestPayload.id())
                .commit("WFLY-00002 commit")
                .commit("WFLY-00003 commit");
        given().github(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, ssePullRequestPayload, mockedContext))
                .when().payloadFromString(ssePullRequestPayload.jsonString()).event(GHEvent.PULL_REQUEST).then().github(mocks -> {
                    StringBuilder sb = new StringBuilder();
                    Mockito.verify(mocks.pullRequest(ssePullRequestPayload.id())).setBody(ArgumentMatchers.contains(String.format(
                            appendedMessage,
                            sb.append(blockQuoted(String.format(RuntimeConstants.BOT_JIRA_LINK_COMMENT_TEMPLATE, "WFLY-00001")))
                                    .append(blockQuoted(
                                            String.format(RuntimeConstants.BOT_JIRA_LINK_COMMENT_TEMPLATE, "WFLY-00002")))
                                    .append(blockQuoted(
                                            String.format(RuntimeConstants.BOT_JIRA_LINK_COMMENT_TEMPLATE, "WFLY-00003"))))));
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
        ssePullRequestPayload = SsePullRequestPayload.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .action(Action.EDITED)
                .title("WFLY-00001, WFLY-00002 title")
                .description(body)
                .build();

        mockedContext = MockedGHPullRequest.builder(ssePullRequestPayload.id())
                .commit("WFLY-00002 commit")
                .commit("WFLY-00003 commit");
        given().github(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, ssePullRequestPayload, mockedContext))
                .when().payloadFromString(ssePullRequestPayload.jsonString()).event(GHEvent.PULL_REQUEST).then().github(mocks -> {
                    StringBuilder sb = new StringBuilder();
                    Mockito.verify(mocks.pullRequest(ssePullRequestPayload.id())).setBody(ArgumentMatchers.contains(String.format(
                            body + WITH_DELIMINER + appendedMessage,
                            sb.append(blockQuoted(String.format(RuntimeConstants.BOT_JIRA_LINK_COMMENT_TEMPLATE, "WFLY-00001")))
                                    .append(blockQuoted(
                                            String.format(RuntimeConstants.BOT_JIRA_LINK_COMMENT_TEMPLATE, "WFLY-00002")))
                                    .append(blockQuoted(
                                            String.format(RuntimeConstants.BOT_JIRA_LINK_COMMENT_TEMPLATE, "WFLY-00003"))))));
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
        ssePullRequestPayload = SsePullRequestPayload.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .action(Action.EDITED)
                .title("WFLY-00001, WFLY-00002 title")
                .description(body)
                .build();

        mockedContext = MockedGHPullRequest.builder(ssePullRequestPayload.id())
                .commit("WFLY-00002 commit")
                .commit("WFLY-00003 commit");
        given().github(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, ssePullRequestPayload, mockedContext))
                .when().payloadFromString(ssePullRequestPayload.jsonString()).event(GHEvent.PULL_REQUEST).then().github(mocks -> {
                    StringBuilder sb = new StringBuilder();
                    Mockito.verify(mocks.pullRequest(ssePullRequestPayload.id())).setBody(ArgumentMatchers.contains(String.format(
                            body + WITH_DELIMINER + appendedMessage,
                            sb.append(blockQuoted(String.format(RuntimeConstants.BOT_JIRA_LINK_COMMENT_TEMPLATE, "WFLY-00001")))
                                    .append(blockQuoted(
                                            String.format(RuntimeConstants.BOT_JIRA_LINK_COMMENT_TEMPLATE, "WFLY-00003"))))));
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
        ssePullRequestPayload = SsePullRequestPayload.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .action(Action.EDITED)
                .title("WFLY-00001, WFLY-00002 title")
                .description(body)
                .build();

        mockedContext = MockedGHPullRequest.builder(ssePullRequestPayload.id())
                .commit("WFLY-00002 commit")
                .commit("WFLY-00003 commit");
        given().github(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, ssePullRequestPayload, mockedContext))
                .when().payloadFromString(ssePullRequestPayload.jsonString()).event(GHEvent.PULL_REQUEST).then().github(mocks -> {
                    Mockito.verify(mocks.pullRequest(ssePullRequestPayload.id()), Mockito.times(0))
                            .setBody(ArgumentMatchers.anyString());
                });
    }

    @Test
    public void testRepoRefFooterAppendedMessage() throws IOException {
        ssePullRequestPayload = SsePullRequestPayload.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .action(Action.EDITED)
                .title("WFLY-00000 title")
                .description(null)
                .build();

        String jiraLinkDescription = String.format(appendedMessage,
                blockQuoted(BOT_JIRA_LINK_COMMENT_TEMPLATE.formatted("WFLY-00000")));
        // even as the description is set, it's after the start, thus we need to mock it's content to match
        mockedContext = MockedGHPullRequest.builder(ssePullRequestPayload.id())
                .commit("WFLY-00000 commit");
        given().github(mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, ssePullRequestPayload, mockedContext))
                .when()
                .payloadFromString(ssePullRequestPayload.jsonString()).event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    String repoRef = jiraLinkDescription + "\n\n"
                            + BOT_REPO_REF_FOOTER.formatted(wildFlyBotConfig.githubName());
                    Mockito.verify(mocks.pullRequest(ssePullRequestPayload.id())).setBody(repoRef);
                });
    }
}
