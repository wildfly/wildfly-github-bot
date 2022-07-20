package io.xstefank.wildfly.bot;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

@QuarkusTest
@GitHubAppTest
public class WorkFlowTest {
    private String wildflyConfigFile;

    @BeforeEach
    void setUp() {
        wildflyConfigFile =
                "wildfly:\n" +
                "  fullWorkflowReport: yes";
    }

    @Test
    void firstTest() throws IOException {
//        GitHubAppTesting.given()
//                .github(mocks -> mocks.configFileFromString("wildfly-bot.yml", wildflyConfigFile))
//                .when().payloadFromClasspath("/workflow-run-completed.json")
//                .event(GHEvent.WORKFLOW_RUN)
//                .then().github(mocks -> {
//                    GHRepository repo = mocks.repository("workflow-trigger");
//
//                });
    }
}
