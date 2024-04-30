package org.wildfly.bot.webhooks;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHCommitState;
import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GHRepository;
import org.mockito.Mockito;
import org.wildfly.bot.model.RuntimeConstants;
import org.wildfly.bot.utils.TestConstants;
import org.wildfly.bot.utils.mocking.Mockable;
import org.wildfly.bot.utils.mocking.MockedGHPullRequest;
import org.wildfly.bot.utils.mocking.MockedGHRepository;
import org.wildfly.bot.utils.model.SsePullRequestPayload;
import org.wildfly.bot.utils.testing.PullRequestJson;
import org.wildfly.bot.utils.testing.internal.TestModel;
import org.wildfly.bot.utils.testing.model.PullRequestGitHubEventPayload;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@QuarkusTest
@GitHubAppTest
public class PRDirectoriesVerificationTest {

    private static PullRequestJson pullRequestJson;
    private Mockable mockedContext;

    @BeforeAll
    public static void setPullRequestJson() throws Exception {
        TestModel.setAllCallables(
                () -> SsePullRequestPayload.builder((TestConstants.VALID_PR_TEMPLATE_JSON)),
                PullRequestGitHubEventPayload::new);

        pullRequestJson = TestModel.getPullRequestJson();
    }

    @Test
    public void existingDirectoryTest() throws Throwable {
        mockedContext = MockedGHPullRequest.builder(pullRequestJson.id())
                .files(".github/wildfly-bot.yml")
                .mockNext(MockedGHRepository.builder())
                .directories("src");

        TestModel.given(mocks -> {
            mockedContext.mock(mocks);
            GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
            GHContent mockGHContent = mock(GHContent.class);
            when(repo.getFileContent(".github/" + RuntimeConstants.CONFIG_FILE_NAME, pullRequestJson.commitSHA()))
                    .thenReturn(mockGHContent);
            when(mockGHContent.read()).thenReturn(IOUtils.toInputStream("""
                    wildfly:
                      rules:
                        - id: "id"
                          directories: [src]""",
                    "UTF-8"));

        })
                .sseEventOptions(eventSenderOptions -> eventSenderOptions.payloadFromString(pullRequestJson.jsonString())
                        .event(GHEvent.PULL_REQUEST))
                .pollingEventOptions(eventSenderOptions -> eventSenderOptions.eventFromPayload(pullRequestJson.jsonString()))
                .then(mocks -> {
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    Mockito.verify(repo).getDirectoryContent("src");
                    Mockito.verify(repo).createCommitStatus(pullRequestJson.commitSHA(),
                            GHCommitState.SUCCESS, "", "Valid", "Configuration File");
                })
                .run();
    }

    @Test
    public void nonExistingDirectoryTest() throws Throwable {
        mockedContext = MockedGHPullRequest.builder(pullRequestJson.id())
                .files(".github/wildfly-bot.yml");

        TestModel.given(mocks -> {
            mockedContext.mock(mocks);
            GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
            GHContent mockGHContent = mock(GHContent.class);
            when(repo.getFileContent(".github/" + RuntimeConstants.CONFIG_FILE_NAME, pullRequestJson.commitSHA()))
                    .thenReturn(mockGHContent);
            when(mockGHContent.read()).thenReturn(IOUtils.toInputStream("""
                    wildfly:
                      rules:
                        - id: "id"
                          directories: [src]""",
                    "UTF-8"));

        })
                .sseEventOptions(eventSenderOptions -> eventSenderOptions.payloadFromString(pullRequestJson.jsonString())
                        .event(GHEvent.PULL_REQUEST))
                .pollingEventOptions(eventSenderOptions -> eventSenderOptions.eventFromPayload(pullRequestJson.jsonString()))
                .then(mocks -> {
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    Mockito.verify(repo).getDirectoryContent("src");
                    Mockito.verify(repo).createCommitStatus(pullRequestJson.commitSHA(),
                            GHCommitState.ERROR, "",
                            "One or multiple rules are invalid, please see the comment stating the problems",
                            "Configuration File");
                })
                .run();
    }

    @Test
    public void existingFileTest() throws Throwable {
        mockedContext = MockedGHPullRequest.builder(pullRequestJson.id())
                .files(".github/wildfly-bot.yml")
                .mockNext(MockedGHRepository.builder())
                .files("pom.xml");

        TestModel.given(mocks -> {
            mockedContext.mock(mocks);
            GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
            GHContent mockGHContent = mock(GHContent.class);
            when(repo.getFileContent(".github/" + RuntimeConstants.CONFIG_FILE_NAME, pullRequestJson.commitSHA()))
                    .thenReturn(mockGHContent);
            when(mockGHContent.read()).thenReturn(IOUtils.toInputStream("""
                    wildfly:
                      rules:
                        - id: "id"
                          directories: [pom.xml]""",
                    "UTF-8"));

        })
                .sseEventOptions(eventSenderOptions -> eventSenderOptions.payloadFromString(pullRequestJson.jsonString())
                        .event(GHEvent.PULL_REQUEST))
                .pollingEventOptions(eventSenderOptions -> eventSenderOptions.eventFromPayload(pullRequestJson.jsonString()))
                .then(mocks -> {
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    Mockito.verify(repo).getDirectoryContent("pom.xml");
                })
                .run();
    }

    @Test
    public void oneExistingSubdirectoryTest() throws Throwable {
        mockedContext = MockedGHPullRequest.builder(pullRequestJson.id())
                .files(".github/wildfly-bot.yml")
                .mockNext(MockedGHRepository.builder())
                .directories("src", "src/main", "src/main/java");

        TestModel.given(mocks -> {
            mockedContext.mock(mocks);
            GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
            GHContent mockGHContent = mock(GHContent.class);
            when(repo.getFileContent(".github/" + RuntimeConstants.CONFIG_FILE_NAME, pullRequestJson.commitSHA()))
                    .thenReturn(mockGHContent);
            when(mockGHContent.read()).thenReturn(IOUtils.toInputStream("""
                    wildfly:
                      rules:
                        - id: "id"
                          directories: [src/main, src/test]""",
                    "UTF-8"));

        })
                .sseEventOptions(eventSenderOptions -> eventSenderOptions.payloadFromString(pullRequestJson.jsonString())
                        .event(GHEvent.PULL_REQUEST))
                .pollingEventOptions(eventSenderOptions -> eventSenderOptions.eventFromPayload(pullRequestJson.jsonString()))
                .then(mocks -> {
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    Mockito.verify(repo).getDirectoryContent("src/main");
                    Mockito.verify(repo).getDirectoryContent("src/test");
                    Mockito.verify(repo).createCommitStatus(pullRequestJson.commitSHA(),
                            GHCommitState.ERROR, "",
                            "One or multiple rules are invalid, please see the comment stating the problems",
                            "Configuration File");
                })
                .run();
    }

    @Test
    public void oneExistingFileInSubdirectoryTest() throws Throwable {
        mockedContext = MockedGHPullRequest.builder(pullRequestJson.id())
                .files(".github/wildfly-bot.yml")
                .mockNext(MockedGHRepository.builder())
                .directories("src", "src/main", "src/main/java")
                .files("src/main/resources/application.properties");

        TestModel.given(mocks -> {
            mockedContext.mock(mocks);
            GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
            GHContent mockGHContent = mock(GHContent.class);
            when(repo.getFileContent(".github/" + RuntimeConstants.CONFIG_FILE_NAME, pullRequestJson.commitSHA()))
                    .thenReturn(mockGHContent);
            when(mockGHContent.read()).thenReturn(IOUtils.toInputStream("""
                    wildfly:
                      rules:
                        - id: "id"
                          directories: [src/main]
                        - id: "id2"
                          directories: [src/main/resources/application.properties] """,
                    "UTF-8"));

        })
                .sseEventOptions(eventSenderOptions -> eventSenderOptions.payloadFromString(pullRequestJson.jsonString())
                        .event(GHEvent.PULL_REQUEST))
                .pollingEventOptions(eventSenderOptions -> eventSenderOptions.eventFromPayload(pullRequestJson.jsonString()))
                .then(mocks -> {
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    Mockito.verify(repo).getDirectoryContent("src/main");
                    Mockito.verify(repo).getDirectoryContent("src/main/resources/application.properties");
                    Mockito.verify(repo).createCommitStatus(pullRequestJson.commitSHA(),
                            GHCommitState.SUCCESS, "", "Valid", "Configuration File");
                })
                .run();
    }
}
