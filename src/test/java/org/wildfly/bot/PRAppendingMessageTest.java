package org.wildfly.bot;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.wildfly.bot.util.GitHubBotContextProvider;
import org.wildfly.bot.util.Strings;
import org.wildfly.bot.utils.WildflyGitHubBotTesting;
import org.wildfly.bot.utils.mocking.MockedCommit;
import org.wildfly.bot.utils.mocking.MockedGHPullRequest;
import org.wildfly.bot.utils.model.Action;
import org.wildfly.bot.utils.testing.PullRequestJson;
import org.wildfly.bot.utils.testing.internal.TestModel;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.wildfly.bot.model.RuntimeConstants.BOT_JIRA_LINKS_HEADER;
import static org.wildfly.bot.model.RuntimeConstants.BOT_JIRA_LINK_COMMENT_TEMPLATE;
import static org.wildfly.bot.model.RuntimeConstants.BOT_MESSAGE_DELIMITER;
import static org.wildfly.bot.model.RuntimeConstants.BOT_MESSAGE_WARNING;
import static org.wildfly.bot.model.RuntimeConstants.BOT_REPO_REF_FOOTER;
import static org.wildfly.bot.util.Strings.blockQuoted;

import java.util.List;
import java.util.SequencedCollection;
import java.util.stream.Collectors;

@QuarkusTest
@GitHubAppTest
class PRAppendingMessageTest {

    private static final String WILDFLY_CONFIG_FILE = """
            wildfly:
              format:
                commit:
                  enabled: false
            """;

    private static final String incompleteBotSectionDescription = """

            %s

            %s

            %s%%s

            %%s""" // due to wildFlyBotConfig.githubName() not being available at this point
            .formatted(
                    BOT_MESSAGE_DELIMITER,
                    blockQuoted(BOT_MESSAGE_WARNING),
                    blockQuoted(BOT_JIRA_LINKS_HEADER));

    @Inject
    GitHubBotContextProvider botContextProvider;

    PullRequestJson pullRequestJson;

    MockedGHPullRequest mockedContext;

    @BeforeAll
    static void setPullRequestJson() throws Exception {
        TestModel.defaultBeforeEachJsons();
    }

    @Test
    void testEmptyBodyAppendMessage() throws Throwable {
        final String userSectionBody = null; // empty body
        final String expectedDescriptionToBeChangedByBot = constructFullDescription(userSectionBody,
                List.of("WFLY-00000", "WFLY-00001", "WFLY-00002", "WFLY-00003", "WFLY-00004"));

        pullRequestJson = TestModel.setPullRequestJsonBuilder(pullRequestJsonBuilder -> pullRequestJsonBuilder
                .action(Action.EDITED)
                .title("[WFLY-00000, WFLY-00001] title")
                .description(userSectionBody));

        mockedContext = MockedGHPullRequest.builder(pullRequestJson.id())
                .commit("WFLY-00001 commit")
                .commit("commit without WFLY issue key")
                .commit("[WFLY-00002, WFLY-00003, WFLY-00004] commit");
        TestModel.given(
                mocks -> WildflyGitHubBotTesting.mockRepo(mocks, WILDFLY_CONFIG_FILE, pullRequestJson, mockedContext))
                .pullRequestEvent(pullRequestJson)
                .then(mocks -> verify(mocks.pullRequest(pullRequestJson.id()))
                        .setBody(expectedDescriptionToBeChangedByBot));
    }

    @Test
    void testNonEmptyBodyAppendMessage() throws Throwable {
        final String userBodySection = """
                This is my
                testing

                body, which
                should not be
                cleared. And also has missing WFLY-00000,
                WFLY-00001, WFLY-00002, WFLY-00003, and WFLY-00004
                issue links.""";
        final String expectedDescriptionToBeChangedByBot = constructFullDescription(userBodySection,
                List.of("WFLY-00000", "WFLY-00001", "WFLY-00002", "WFLY-00003", "WFLY-00004"));

        pullRequestJson = TestModel.setPullRequestJsonBuilder(pullRequestJsonBuilder -> pullRequestJsonBuilder
                .action(Action.EDITED)
                .title("[WFLY-00000, WFLY-00001] title")
                .description(userBodySection));

        mockedContext = MockedGHPullRequest.builder(pullRequestJson.id())
                .commit("WFLY-00001 commit")
                .commit("commit without WFLY issue key")
                .commit("[WFLY-00002, WFLY-00003, WFLY-00004] commit");

        TestModel.given(
                mocks -> WildflyGitHubBotTesting.mockRepo(mocks, WILDFLY_CONFIG_FILE, pullRequestJson, mockedContext))
                .pullRequestEvent(pullRequestJson)
                .then(mocks -> verify(mocks.pullRequest(pullRequestJson.id()))
                        .setBody(expectedDescriptionToBeChangedByBot));
    }

    @Test
    void testBodyContainingSomeJirasAppendMessageMultipleDifferentLinks() throws Throwable {
        final String userBodySection = """
                This is my
                testing

                body, which
                should not be

                cleared.
                Here is one jira for you https://issues.redhat.com/browse/WFLY-00002
                and here is another one https://issues.redhat.com/browse/WFLY-00003
                """;

        final String expectedDescriptionToBeChangedByBot = constructFullDescription(userBodySection,
                List.of("WFLY-00000", "WFLY-00001", "WFLY-00004"));

        pullRequestJson = TestModel.setPullRequestJsonBuilder(pullRequestJsonBuilder -> pullRequestJsonBuilder
                .action(Action.EDITED)
                .title("WFLY-00000, WFLY-00001 title")
                .description(userBodySection));

        mockedContext = MockedGHPullRequest.builder(pullRequestJson.id())
                .commit("WFLY-00001 commit")
                .commit("commit without WFLY issue key")
                .commit("WFLY-00002, WFLY-00003, WFLY-00004 commit");

        TestModel.given(
                mocks -> WildflyGitHubBotTesting.mockRepo(mocks, WILDFLY_CONFIG_FILE, pullRequestJson, mockedContext))
                .pullRequestEvent(pullRequestJson)
                .then(mocks -> verify(mocks.pullRequest(pullRequestJson.id()))
                        .setBody(expectedDescriptionToBeChangedByBot));
    }

    @Test
    void testBodyContainingAllJirasAppendMessageMultipleDifferentLinks() throws Throwable {
        final String userBodySection = """
                This is my
                testing

                body, which
                should not be
                cleared.

                Listing all jiras:
                https://issues.redhat.com/browse/WFLY-00001
                https://issues.redhat.com/browse/WFLY-00002
                https://issues.redhat.com/browse/WFLY-00003
                """;

        pullRequestJson = TestModel.setPullRequestJsonBuilder(pullRequestJsonBuilder -> pullRequestJsonBuilder
                .action(Action.EDITED)
                .title("WFLY-00001, WFLY-00002 title")
                .description(userBodySection));

        mockedContext = MockedGHPullRequest.builder(pullRequestJson.id())
                .commit("WFLY-00002 commit")
                .commit("WFLY-00003 commit");

        TestModel.given(
                mocks -> WildflyGitHubBotTesting.mockRepo(mocks, WILDFLY_CONFIG_FILE,
                        pullRequestJson, mockedContext))
                .pullRequestEvent(pullRequestJson)
                .then(mocks -> verify(mocks.pullRequest(pullRequestJson.id()), never()).setBody(any()));
    }

    @Test
    void testBotChangingBotSectionUponUserCorrectedSomeIssueLinks() throws Throwable {
        final String userBodySection = """
                This user body section has been corrected and here is the issue link:
                https://issues.redhat.com/browse/WFLY-00002
                """;
        final String descriptionToBeChangedByBot = constructFullDescription(userBodySection,
                List.of("WFLY-00000", "WFLY-00001", "WFLY-00002",
                        "WFLY-00003", "WFLY-00004"));

        final String finalFullDescriptionEditedByBot = constructFullDescription(userBodySection,
                List.of("WFLY-00000", "WFLY-00001", // WFLY-00002 link is corrected by user
                        "WFLY-00003", "WFLY-00004"));

        pullRequestJson = TestModel.setPullRequestJsonBuilder(pullRequestJsonBuilder -> pullRequestJsonBuilder
                .action(Action.EDITED)
                .title("[WFLY-00000, WFLY-00001] title")
                .description(descriptionToBeChangedByBot)); // user (fixed) + bot (redundant links) section

        mockedContext = MockedGHPullRequest.builder(pullRequestJson.id())
                .commit("WFLY-00001 commit")
                .commit("commit without WFLY issue key")
                .commit("[WFLY-00002, WFLY-00003, WFLY-00004] commit");

        TestModel.given(
                mocks -> WildflyGitHubBotTesting.mockRepo(mocks, WILDFLY_CONFIG_FILE, pullRequestJson, mockedContext))
                .pullRequestEvent(pullRequestJson)
                .then(mocks -> verify(mocks.pullRequest(pullRequestJson.id()))
                        .setBody(finalFullDescriptionEditedByBot));
    }

    @Test
    void testBotDeletingBotSectionUponUserCorrectedIssueLinks() throws Throwable {
        final String userBodySection = """
                This user body section has been corrected and here are the issue links:
                https://issues.redhat.com/browse/WFLY-00000
                https://issues.redhat.com/browse/WFLY-00001

                ____

                https://issues.redhat.com/browse/WFLY-00002
                https://issues.redhat.com/browse/WFLY-00003
                https://issues.redhat.com/browse/WFLY-00004
                """;
        final String descriptionToBeChangedByBot = constructFullDescription(userBodySection,
                List.of("WFLY-00000", "WFLY-00001", "WFLY-00002",
                        "WFLY-00003", "WFLY-00004"));

        pullRequestJson = TestModel.setPullRequestJsonBuilder(pullRequestJsonBuilder -> pullRequestJsonBuilder
                .action(Action.EDITED)
                .title("[WFLY-00000, WFLY-00001] title")
                .description(descriptionToBeChangedByBot)); // user (fixed) + bot (redundant links) section

        mockedContext = MockedGHPullRequest.builder(pullRequestJson.id())
                .commit("WFLY-00001 commit")
                .commit("commit without WFLY issue key")
                .commit("[WFLY-00002, WFLY-00003, WFLY-00004] commit");

        TestModel.given(
                mocks -> WildflyGitHubBotTesting.mockRepo(mocks, WILDFLY_CONFIG_FILE, pullRequestJson, mockedContext))
                .pullRequestEvent(pullRequestJson)
                .then(mocks -> {
                    verify(mocks.pullRequest(pullRequestJson.id()))
                            .setBody(userBodySection);
                    verify(mocks.pullRequest(pullRequestJson.id()), never())
                            .setBody(descriptionToBeChangedByBot);
                });
    }

    @Test
    void testEditingTitleToContainMissingIssueLink() throws Throwable {
        final String userBodySection = """
                This is my user section body with WFLY-00000 and WFLY-00001 issue links.
                https://issues.redhat.com/browse/WFLY-00001
                https://issues.redhat.com/browse/WFLY-00002
                """;

        final String originalBodyBeforeTheEdit = constructFullDescription(userBodySection, List.of("WFLY-00003"));
        final String expectedDescriptionToBeChangedByBot = constructFullDescription(userBodySection,
                List.of("WFLY-00000", "WFLY-00003"));

        pullRequestJson = TestModel.setPullRequestJsonBuilder(pullRequestJsonBuilder -> pullRequestJsonBuilder
                .action(Action.EDITED)
                .title("[WFLY-00000] title") // missing new WFLY-00000 link
                .description(originalBodyBeforeTheEdit));

        mockedContext = MockedGHPullRequest.builder(pullRequestJson.id())
                .commit("WFLY-00001 commit")
                .commit("WFLY-00002 commit")
                .commit("WFLY-00003 commit");

        TestModel.given(
                mocks -> WildflyGitHubBotTesting.mockRepo(mocks, WILDFLY_CONFIG_FILE, pullRequestJson, mockedContext))
                .pullRequestEvent(pullRequestJson)
                // the bot should update the title to contain all missing issue links
                .then(mocks -> verify(mocks.pullRequest(pullRequestJson.id()))
                        .setBody(expectedDescriptionToBeChangedByBot));
    }

    @Test
    void testDescriptionIsNotModifiedByBotIfBotSectionIsCorrect() throws Throwable {
        final String userBodySection = "This is my user section body without any WFLY issue links.";

        final String fullDescription = constructFullDescription(userBodySection,
                List.of("WFLY-00000", "WFLY-00001", "WFLY-00002", "WFLY-00003", "WFLY-00004"));
        pullRequestJson = TestModel.setPullRequestJsonBuilder(pullRequestJsonBuilder -> pullRequestJsonBuilder
                .action(Action.EDITED)
                .title("[WFLY-00000, WFLY-00001] title")
                .description(fullDescription));

        mockedContext = MockedGHPullRequest.builder(pullRequestJson.id())
                .commit("WFLY-00001 commit")
                .commit("commit without WFLY issue key")
                .commit("[WFLY-00002, WFLY-00003, WFLY-00004] commit");

        TestModel.given(
                mocks -> WildflyGitHubBotTesting.mockRepo(mocks, WILDFLY_CONFIG_FILE, pullRequestJson, mockedContext))
                .pullRequestEvent(pullRequestJson)
                // if there is no need to edit the description, the bot should not change it
                .then(mocks -> verify(mocks.pullRequest(pullRequestJson.id()), never()).setBody(any()));
    }

    @Test
    void testDescriptionIsModifiedByBotIfBotSectionIsIncorrect() throws Throwable {
        final String userBodySection = "This is my user section body without any WFLY issue links.";

        final String expectedBotBodySection = constructFullDescription(userBodySection,
                List.of("WFLY-00000", "WFLY-00001", "WFLY-00002", "WFLY-00003", "WFLY-00004"));
        pullRequestJson = TestModel.setPullRequestJsonBuilder(pullRequestJsonBuilder -> pullRequestJsonBuilder
                .action(Action.EDITED)
                .title("[WFLY-00000, WFLY-00001] title")
                .description(expectedBotBodySection + "\nsome additional text in the box section"));

        mockedContext = MockedGHPullRequest.builder(pullRequestJson.id())
                .commit("WFLY-00001 commit")
                .commit("commit without WFLY issue key")
                .commit("[WFLY-00002, WFLY-00003, WFLY-00004] commit");

        TestModel.given(
                mocks -> WildflyGitHubBotTesting.mockRepo(mocks, WILDFLY_CONFIG_FILE, pullRequestJson, mockedContext))
                .pullRequestEvent(pullRequestJson)
                // the additional text in the bot section should be removed
                .then(mocks -> verify(mocks.pullRequest(pullRequestJson.id())).setBody(expectedBotBodySection));
    }

    @Test
    void testNonBreakingChangesWithThePreviousAppendMessageFormat() throws Throwable {
        final String oldBody = """
                https://issues.redhat.com/browse/WFLY-20743

                ____

                <--- THIS SECTION IS AUTOMATICALLY GENERATED BY WILDFLY GITHUB BOT. ANY MANUAL CHANGES WILL BE LOST. --->

                > WildFly issue links:

                > * [WFLY-20730] (https://issues.redhat.com/browse/WFLY-20730)

                <--- END OF WILDFLY GITHUB BOT REPORT --->

                More information about the [wildfly-bot](https://github.com/wildfly/wildfly-bot)""";

        final String expectedNewBody = constructFullDescription("https://issues.redhat.com/browse/WFLY-20743",
                List.of("WFLY-20730"));

        pullRequestJson = TestModel.setPullRequestJsonBuilder(pullRequestJsonBuilder -> pullRequestJsonBuilder
                .action(Action.EDITED)
                .title("WFLY-20743 title")
                .description(oldBody));

        mockedContext = MockedGHPullRequest.builder(pullRequestJson.id())
                .commit("WFLY-20743 xyz")
                .commit("WFLY-20730 abc");

        TestModel.given(
                mocks -> WildflyGitHubBotTesting.mockRepo(mocks, WILDFLY_CONFIG_FILE, pullRequestJson, mockedContext))
                .pullRequestEvent(pullRequestJson)
                .then(mocks -> verify(mocks.pullRequest(pullRequestJson.id()))
                        .setBody(expectedNewBody));
    }

    @Test
    void testSequencedCommitsWithoutUserManualyEditingDescription() throws Throwable {
        final String userBodySection = "This is my user section body without any WFLY issue links.";

        final String expectedDescriptionAfterFirstCommit = constructFullDescription(userBodySection,
                List.of("WFLY-00000"));
        pullRequestJson = TestModel.setPullRequestJsonBuilder(pullRequestJsonBuilder -> pullRequestJsonBuilder
                .action(Action.EDITED)
                .title("title")
                .description(userBodySection));

        // first commit with WFLY-00000
        mockedContext = MockedGHPullRequest.builder(pullRequestJson.id())
                .commit("WFLY-00000 commit");
        TestModel.given(
                mocks -> WildflyGitHubBotTesting.mockRepo(mocks, WILDFLY_CONFIG_FILE, pullRequestJson, mockedContext))
                .pullRequestEvent(pullRequestJson)
                .then(mocks -> verify(mocks.pullRequest(pullRequestJson.id()))
                        .setBody(expectedDescriptionAfterFirstCommit));

        // second commit with WFLY-00001
        final String expectedDescriptionAfterSecondCommit = constructFullDescription(userBodySection,
                List.of("WFLY-00000", "WFLY-00001"));
        pullRequestJson = TestModel.setPullRequestJsonBuilder(pullRequestJsonBuilder -> pullRequestJsonBuilder
                .action(Action.EDITED)
                .title("title")
                .description(expectedDescriptionAfterFirstCommit));
        mockedContext.commit("WFLY-00001 commit");
        TestModel.given(
                mocks -> WildflyGitHubBotTesting.mockRepo(mocks, WILDFLY_CONFIG_FILE, pullRequestJson, mockedContext))
                .pullRequestEvent(pullRequestJson)
                .then(mocks -> verify(mocks.pullRequest(pullRequestJson.id()))
                        .setBody(expectedDescriptionAfterSecondCommit));
    }

    @Test
    void testBotChangingBotSectionUponUserFixupsCommitWithMissingIssueLink() throws Throwable {
        final String userBodySection = """
                    This is my user section body with one WFLY issue link:
                    https://issues.redhat.com/browse/WFLY-00001
                """;

        final String fullDescriptionBeforeTheFixup = constructFullDescription(userBodySection, List.of("WFLY-00002"));

        pullRequestJson = TestModel.setPullRequestJsonBuilder(pullRequestJsonBuilder -> pullRequestJsonBuilder
                .action(Action.EDITED)
                .title("WFLY-00001 title")
                .description(userBodySection));

        MockedCommit mockedCommit = MockedCommit.commit("WFLY-00002 commit");
        mockedContext = MockedGHPullRequest.builder(pullRequestJson.id())
                .commit("WFLY-00001 commit")
                .commit(mockedCommit); // commit with missing WFLY-00002 issue link that will be fixed up later

        TestModel.given(
                mocks -> WildflyGitHubBotTesting.mockRepo(mocks, WILDFLY_CONFIG_FILE, pullRequestJson, mockedContext))
                .pullRequestEvent(pullRequestJson)
                .then(mocks -> verify(mocks.pullRequest(pullRequestJson.id()))
                        .setBody(fullDescriptionBeforeTheFixup));

        pullRequestJson = TestModel.setPullRequestJsonBuilder(pullRequestJsonBuilder -> pullRequestJsonBuilder
                .action(Action.EDITED)
                .title("WFLY-00001 title")
                .description(fullDescriptionBeforeTheFixup));

        // now we fixup the commit, which should trigger the bot to update the description
        mockedContext.fixup(mockedCommit);
        TestModel.given(
                mocks -> WildflyGitHubBotTesting.mockRepo(mocks, WILDFLY_CONFIG_FILE, pullRequestJson, mockedContext))
                .pullRequestEvent(pullRequestJson)
                .then(mocks -> verify(mocks.pullRequest(pullRequestJson.id()))
                        .setBody(userBodySection)); // bot section should be removed
    }

    @Test
    void testBotChangingBotSectionUponUserRewordingCommit() throws Throwable {
        final String userBodySection = """
                This is my user section body with two WFLY issue links:
                https://issues.redhat.com/browse/WFLY-00001
                https://issues.redhat.com/browse/WFLY-00003
                """;
        final String fullDescriptionBeforeTheReword = constructFullDescription(userBodySection, List.of("WFLY-00002"));

        pullRequestJson = TestModel.setPullRequestJsonBuilder(pullRequestJsonBuilder -> pullRequestJsonBuilder
                .action(Action.EDITED)
                .title("WFLY-00001 title")
                .description(userBodySection));

        MockedCommit mockedCommit = MockedCommit.commit("WFLY-00002 commit");
        mockedContext = MockedGHPullRequest.builder(pullRequestJson.id())
                .commit("WFLY-00001 commit")
                .commit(mockedCommit); // commit with missing WFLY-00002 issue link that will be reworded later

        TestModel.given(
                mocks -> WildflyGitHubBotTesting.mockRepo(mocks, WILDFLY_CONFIG_FILE, pullRequestJson, mockedContext))
                .pullRequestEvent(pullRequestJson)
                .then(mocks -> verify(mocks.pullRequest(pullRequestJson.id()))
                        .setBody(fullDescriptionBeforeTheReword));

        pullRequestJson = TestModel.setPullRequestJsonBuilder(pullRequestJsonBuilder -> pullRequestJsonBuilder
                .action(Action.EDITED)
                .title("WFLY-00001 title")
                .description(fullDescriptionBeforeTheReword));

        // now we reword the commit, which should trigger the bot to update the description
        mockedContext.reword(mockedCommit, "WFLY-00003 commit");
        TestModel.given(
                mocks -> WildflyGitHubBotTesting.mockRepo(mocks, WILDFLY_CONFIG_FILE, pullRequestJson, mockedContext))
                .pullRequestEvent(pullRequestJson)
                .then(mocks -> verify(mocks.pullRequest(pullRequestJson.id()))
                        .setBody(userBodySection)); // bot section should be removed
    }

    private String constructFullDescription(String userBodySection, SequencedCollection<String> botAddedIssueKeys) {
        userBodySection = userBodySection == null ? "" : userBodySection;
        String issueLinks = botAddedIssueKeys.stream()
                .map(BOT_JIRA_LINK_COMMENT_TEMPLATE::formatted).map(Strings::blockQuoted)
                .collect(Collectors.joining());
        return userBodySection + incompleteBotSectionDescription.formatted(issueLinks,
                BOT_REPO_REF_FOOTER.formatted(botContextProvider.getBotName()));
    }
}
