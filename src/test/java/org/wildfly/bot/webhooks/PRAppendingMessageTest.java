package org.wildfly.bot.webhooks;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeAll;
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
import org.wildfly.bot.utils.testing.PullRequestJson;
import org.wildfly.bot.utils.testing.internal.TestModel;
import org.wildfly.bot.utils.testing.model.PullRequestGitHubEventPayload;

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

    PullRequestJson pullRequestJson;

    MockedGHPullRequest mockedContext;

    @BeforeAll
    static void setPullRequestJson() {
        TestModel.setAllCallables(
                () -> SsePullRequestPayload.builder(TestConstants.VALID_PR_TEMPLATE_JSON),
                PullRequestGitHubEventPayload::new);
    }

    @Test
    public void testEmptyBodyAppendMessage() throws Throwable {
        pullRequestJson = TestModel.setPullRequestJsonBuilderBuild(pullRequestJsonBuilder -> pullRequestJsonBuilder
                .action(Action.EDITED)
                .title("WFLY-00000 title")
                .description(null));

        mockedContext = MockedGHPullRequest.builder(pullRequestJson.id())
                .commit("WFLY-00000 commit");
        TestModel.given(
                mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson, mockedContext))
                .sseEventOptions(eventSenderOptions -> eventSenderOptions.payloadFromString(pullRequestJson.jsonString())
                        .event(GHEvent.PULL_REQUEST))
                .pollingEventOptions(eventSenderOptions -> eventSenderOptions.eventFromPayload(pullRequestJson.jsonString()))
                .then(mocks -> {
                    StringBuilder sb = new StringBuilder();
                    Mockito.verify(mocks.pullRequest(pullRequestJson.id()))
                            .setBody(ArgumentMatchers.contains(String.format(appendedMessage,
                                    sb.append(blockQuoted(
                                            String.format(RuntimeConstants.BOT_JIRA_LINK_COMMENT_TEMPLATE, "WFLY-00000"))))));
                })
                .run();
    }

    @Test
    public void testNonEmptyBodyAppendMessage() throws Throwable {
        String body = """
                This is my
                testing

                body, which
                should not be
                cleared.""";
        pullRequestJson = TestModel.setPullRequestJsonBuilderBuild(pullRequestJsonBuilder -> pullRequestJsonBuilder
                .action(Action.EDITED)
                .title("WFLY-00000 title")
                .description(body));

        mockedContext = MockedGHPullRequest.builder(pullRequestJson.id())
                .commit("WFLY-00000 commit");

        TestModel.given(
                mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson, mockedContext))
                .sseEventOptions(eventSenderOptions -> eventSenderOptions.payloadFromString(pullRequestJson.jsonString())
                        .event(GHEvent.PULL_REQUEST))
                .pollingEventOptions(eventSenderOptions -> eventSenderOptions.eventFromPayload(pullRequestJson.jsonString()))
                .then(mocks -> {
                    StringBuilder sb = new StringBuilder();
                    Mockito.verify(mocks.pullRequest(pullRequestJson.id()))
                            .setBody(ArgumentMatchers.contains(String.format(body + WITH_DELIMINER + appendedMessage,
                                    sb.append(blockQuoted(
                                            String.format(RuntimeConstants.BOT_JIRA_LINK_COMMENT_TEMPLATE, "WFLY-00000"))))));
                })
                .run();
    }

    @Test
    public void testEmptyBodyAppendMessageMultipleLinks() throws Throwable {
        pullRequestJson = TestModel.setPullRequestJsonBuilderBuild(pullRequestJsonBuilder -> pullRequestJsonBuilder
                .action(Action.EDITED)
                .title("WFLY-00001 title")
                .description(null));

        mockedContext = MockedGHPullRequest.builder(pullRequestJson.id())
                .commit("WFLY-00002 commit");

        TestModel.given(
                mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson, mockedContext))
                .sseEventOptions(eventSenderOptions -> eventSenderOptions.payloadFromString(pullRequestJson.jsonString())
                        .event(GHEvent.PULL_REQUEST))
                .pollingEventOptions(eventSenderOptions -> eventSenderOptions.eventFromPayload(pullRequestJson.jsonString()))
                .then(mocks -> {
                    StringBuilder sb = new StringBuilder();
                    Mockito.verify(mocks.pullRequest(pullRequestJson.id()))
                            .setBody(ArgumentMatchers.contains(String.format(
                                    appendedMessage,
                                    sb.append(blockQuoted(
                                            String.format(RuntimeConstants.BOT_JIRA_LINK_COMMENT_TEMPLATE, "WFLY-00001")))
                                            .append(blockQuoted(
                                                    String.format(RuntimeConstants.BOT_JIRA_LINK_COMMENT_TEMPLATE,
                                                            "WFLY-00002"))))));
                })
                .run();
    }

    @Test
    public void testNonEmptyBodyAppendMessageMultipleLinks() throws Throwable {
        String body = """
                This is my
                testing

                body, which
                should not be
                cleared.""";
        pullRequestJson = TestModel.setPullRequestJsonBuilderBuild(pullRequestJsonBuilder -> pullRequestJsonBuilder
                .action(Action.EDITED)
                .title("WFLY-00001 title")
                .description(body));

        mockedContext = MockedGHPullRequest.builder(pullRequestJson.id())
                .commit("WFLY-00002 commit");
        TestModel.given(
                mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson, mockedContext))
                .sseEventOptions(eventSenderOptions -> eventSenderOptions.payloadFromString(pullRequestJson.jsonString())
                        .event(GHEvent.PULL_REQUEST))
                .pollingEventOptions(eventSenderOptions -> eventSenderOptions.eventFromPayload(pullRequestJson.jsonString()))
                .then(mocks -> {
                    StringBuilder sb = new StringBuilder();
                    Mockito.verify(mocks.pullRequest(pullRequestJson.id()))
                            .setBody(ArgumentMatchers.contains(String.format(
                                    body + WITH_DELIMINER + appendedMessage,
                                    sb.append(blockQuoted(
                                            String.format(RuntimeConstants.BOT_JIRA_LINK_COMMENT_TEMPLATE, "WFLY-00001")))
                                            .append(blockQuoted(
                                                    String.format(RuntimeConstants.BOT_JIRA_LINK_COMMENT_TEMPLATE,
                                                            "WFLY-00002"))))));
                })
                .run();
    }

    @Test
    public void testEmptyBodyAppendMessageMultipleDifferentLinks() throws Throwable {
        pullRequestJson = TestModel.setPullRequestJsonBuilderBuild(pullRequestJsonBuilder -> pullRequestJsonBuilder
                .action(Action.EDITED)
                .title("WFLY-00001, WFLY-00002 title")
                .description(null));

        mockedContext = MockedGHPullRequest.builder(pullRequestJson.id())
                .commit("WFLY-00002 commit")
                .commit("WFLY-00003 commit");

        TestModel.given(
                mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson, mockedContext))
                .sseEventOptions(eventSenderOptions -> eventSenderOptions.payloadFromString(pullRequestJson.jsonString())
                        .event(GHEvent.PULL_REQUEST))
                .pollingEventOptions(eventSenderOptions -> eventSenderOptions.eventFromPayload(pullRequestJson.jsonString()))
                .then(mocks -> {
                    StringBuilder sb = new StringBuilder();
                    Mockito.verify(mocks.pullRequest(pullRequestJson.id()))
                            .setBody(ArgumentMatchers.contains(String.format(
                                    appendedMessage,
                                    sb.append(blockQuoted(
                                            String.format(RuntimeConstants.BOT_JIRA_LINK_COMMENT_TEMPLATE, "WFLY-00001")))
                                            .append(blockQuoted(
                                                    String.format(RuntimeConstants.BOT_JIRA_LINK_COMMENT_TEMPLATE,
                                                            "WFLY-00002")))
                                            .append(blockQuoted(
                                                    String.format(RuntimeConstants.BOT_JIRA_LINK_COMMENT_TEMPLATE,
                                                            "WFLY-00003"))))));
                })
                .run();
    }

    @Test
    public void testNonEmptyBodyAppendMessageMultipleDifferentLinks() throws Throwable {
        String body = """
                This is my
                testing

                body, which
                should not be
                cleared.""";

        pullRequestJson = TestModel.setPullRequestJsonBuilderBuild(pullRequestJsonBuilder -> pullRequestJsonBuilder
                .action(Action.EDITED)
                .title("WFLY-00001, WFLY-00002 title")
                .description(body));

        mockedContext = MockedGHPullRequest.builder(pullRequestJson.id())
                .commit("WFLY-00002 commit")
                .commit("WFLY-00003 commit");

        TestModel.given(
                mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson, mockedContext))
                .sseEventOptions(eventSenderOptions -> eventSenderOptions.payloadFromString(pullRequestJson.jsonString())
                        .event(GHEvent.PULL_REQUEST))
                .pollingEventOptions(eventSenderOptions -> eventSenderOptions.eventFromPayload(pullRequestJson.jsonString()))
                .then(mocks -> {
                    StringBuilder sb = new StringBuilder();
                    Mockito.verify(mocks.pullRequest(pullRequestJson.id()))
                            .setBody(ArgumentMatchers.contains(String.format(
                                    body + WITH_DELIMINER + appendedMessage,
                                    sb.append(blockQuoted(
                                            String.format(RuntimeConstants.BOT_JIRA_LINK_COMMENT_TEMPLATE, "WFLY-00001")))
                                            .append(blockQuoted(
                                                    String.format(RuntimeConstants.BOT_JIRA_LINK_COMMENT_TEMPLATE,
                                                            "WFLY-00002")))
                                            .append(blockQuoted(
                                                    String.format(RuntimeConstants.BOT_JIRA_LINK_COMMENT_TEMPLATE,
                                                            "WFLY-00003"))))));
                })
                .run();
    }

    @Test
    public void testBodyContainingSomeJirasAppendMessageMultipleDifferentLinks() throws Throwable {
        String body = """
                This is my
                testing

                body, which
                should not be

                cleared.
                Here is one jira for you https://issues.redhat.com/browse/WFLY-00002""";

        pullRequestJson = TestModel.setPullRequestJsonBuilderBuild(pullRequestJsonBuilder -> pullRequestJsonBuilder
                .action(Action.EDITED)
                .title("WFLY-00001, WFLY-00002 title")
                .description(body));

        mockedContext = MockedGHPullRequest.builder(pullRequestJson.id())
                .commit("WFLY-00002 commit")
                .commit("WFLY-00003 commit");

        TestModel.given(
                mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson, mockedContext))
                .sseEventOptions(eventSenderOptions -> eventSenderOptions.payloadFromString(pullRequestJson.jsonString())
                        .event(GHEvent.PULL_REQUEST))
                .pollingEventOptions(eventSenderOptions -> eventSenderOptions.eventFromPayload(pullRequestJson.jsonString()))
                .then(mocks -> {
                    StringBuilder sb = new StringBuilder();
                    Mockito.verify(mocks.pullRequest(pullRequestJson.id()))
                            .setBody(ArgumentMatchers.contains(String.format(
                                    body + WITH_DELIMINER + appendedMessage,
                                    sb.append(blockQuoted(
                                            String.format(RuntimeConstants.BOT_JIRA_LINK_COMMENT_TEMPLATE, "WFLY-00001")))
                                            .append(blockQuoted(
                                                    String.format(RuntimeConstants.BOT_JIRA_LINK_COMMENT_TEMPLATE,
                                                            "WFLY-00003"))))));
                })
                .run();
    }

    @Test
    public void testBodyContainingAllJirasAppendMessageMultipleDifferentLinks() throws Throwable {
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

        pullRequestJson = TestModel.setPullRequestJsonBuilderBuild(pullRequestJsonBuilder -> pullRequestJsonBuilder
                .action(Action.EDITED)
                .title("WFLY-00001, WFLY-00002 title")
                .description(body));

        mockedContext = MockedGHPullRequest.builder(pullRequestJson.id())
                .commit("WFLY-00002 commit")
                .commit("WFLY-00003 commit");

        TestModel.given(
                mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson, mockedContext))
                .sseEventOptions(eventSenderOptions -> eventSenderOptions.payloadFromString(pullRequestJson.jsonString())
                        .event(GHEvent.PULL_REQUEST))
                .pollingEventOptions(eventSenderOptions -> eventSenderOptions.eventFromPayload(pullRequestJson.jsonString()))
                .then(mocks -> {
                    Mockito.verify(mocks.pullRequest(pullRequestJson.id()), Mockito.times(0))
                            .setBody(ArgumentMatchers.anyString());
                })
                .run();
    }

    @Test
    public void testRepoRefFooterAppendedMessage() throws Throwable {
        pullRequestJson = TestModel.setPullRequestJsonBuilderBuild(pullRequestJsonBuilder -> pullRequestJsonBuilder
                .action(Action.EDITED)
                .title("WFLY-00000 title")
                .description(null));

        String jiraLinkDescription = String.format(appendedMessage,
                blockQuoted(BOT_JIRA_LINK_COMMENT_TEMPLATE.formatted("WFLY-00000")));
        // even as the description is set, it's after the start, thus we need to mock it's content to match
        mockedContext = MockedGHPullRequest.builder(pullRequestJson.id())
                .commit("WFLY-00000 commit");

        TestModel.given(
                mocks -> WildflyGitHubBotTesting.mockRepo(mocks, wildflyConfigFile, pullRequestJson, mockedContext))
                .sseEventOptions(eventSenderOptions -> eventSenderOptions.payloadFromString(pullRequestJson.jsonString())
                        .event(GHEvent.PULL_REQUEST))
                .pollingEventOptions(eventSenderOptions -> eventSenderOptions.eventFromPayload(pullRequestJson.jsonString()))
                .then(mocks -> {
                    String repoRef = jiraLinkDescription + "\n\n"
                            + BOT_REPO_REF_FOOTER.formatted(wildFlyBotConfig.githubName());
                    Mockito.verify(mocks.pullRequest(pullRequestJson.id())).setBody(repoRef);
                })
                .run();
    }
}
