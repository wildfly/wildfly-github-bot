package io.xstefank.wildfly.bot;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHEvent;

import java.io.IOException;

import static io.quarkiverse.githubapp.testing.GitHubAppTesting.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@QuarkusTest
@GitHubAppTest
public class PROpenedTest {

    @Test
    void testMentionsCCComment() throws IOException {
        given().github(mocks -> mocks.configFileFromString(
                        "wildfly-bot.yml",
                        "wildfly:\n" +
                                "  rules:\n" +
                                "    - title: \"test\"\n" +
                                "      notify: [7125767235,0979986727]"))
                .when().payloadFromClasspath("/pr-opened.json")
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    verify(mocks.pullRequest(851821439))
                            .comment("/cc @0979986727, @7125767235");
                    verifyNoMoreInteractions(mocks.ghObjects());
                });
    }

    @Test
    void testTitleBodyCheckForTitle() throws IOException {
        given().github(mocks -> mocks.configFileFromString(
                        "wildfly-bot.yml",
                        "wildfly:\n" +
                                "  rules:\n" +
                                "    - title: \"Hello\"\n" +
                                "    - body: \"there\"\n" +
                                "    - titleBody: \"test\"\n" +
                                "      notify: [7125767235]"))
                .when().payloadFromClasspath("/pr-opened.json")
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    verify(mocks.pullRequest(851821439))
                        .comment("/cc @7125767235");
                    verifyNoMoreInteractions(mocks.ghObjects());
        });
    }

    @Test
    void testTitleBodyCheckForBody() throws IOException {
        given().github(mocks -> mocks.configFileFromString(
                        "wildfly-bot.yml",
                        "wildfly:\n" +
                                "  rules:\n" +
                                "    - title: \"Hello\"\n" +
                                "    - body: \"there\"\n" +
                                "    - titleBody: \"foobar\"\n" +
                                "      notify: [7125767235]"))
                .when().payloadFromClasspath("/pr-opened.json")
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    verify(mocks.pullRequest(851821439))
                            .comment("/cc @7125767235");
                    verifyNoMoreInteractions(mocks.ghObjects());
                });
    }

    @Test
    void testFailedTitleBodyCheck() throws IOException {
        given().github(mocks -> mocks.configFileFromString(
                        "wildfly-bot.yml",
                        "wildfly:\n" +
                                "  rules:\n" +
                                "    - title: \"Hello\"\n" +
                                "    - body: \"there\"\n" +
                                "    - titleBody: \"General Kenobi\"\n" +
                                "      notify: [7125767235]"))
                .when().payloadFromClasspath("/pr-opened.json")
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    verify(mocks.pullRequest(851821439), never()).comment("/cc @7125767235");
                    verifyNoMoreInteractions(mocks.ghObjects());
                });
    }

}
