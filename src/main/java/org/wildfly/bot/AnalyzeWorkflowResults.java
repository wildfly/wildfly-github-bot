package org.wildfly.bot;

import io.quarkiverse.githubapp.ConfigFile;
import io.quarkiverse.githubapp.event.WorkflowRun;
import io.quarkus.bot.buildreporter.githubactions.BuildReporterConfig;
import io.quarkus.bot.buildreporter.githubactions.BuildReporterEventHandler;
import io.smallrye.graphql.client.dynamic.api.DynamicGraphQLClient;
import jakarta.inject.Inject;
import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.GitHub;
import org.wildfly.bot.config.WildFlyBotConfig;
import org.wildfly.bot.model.WildFlyConfigFile;

import java.io.IOException;
import java.util.Set;

import static org.wildfly.bot.model.RuntimeConstants.CONFIG_FILE_NAME;

public class AnalyzeWorkflowResults {
    @Inject
    BuildReporterEventHandler buildReporterEventHandler;

    @Inject
    WildFlyBotConfig wildFlyBotConfig;

    void analyzeWorkflowResults(@WorkflowRun.Completed @WorkflowRun.Requested GHEventPayload.WorkflowRun workflowRunPayload,
            @ConfigFile(CONFIG_FILE_NAME) WildFlyConfigFile wildFlyConfigFile,
            GitHub gitHub, DynamicGraphQLClient gitHubGraphQLClient) throws IOException {
        BuildReporterConfig buildReporterConfig = BuildReporterConfig.builder()
                .dryRun(wildFlyBotConfig.isDryRun())
                .workflowJobComparator((o1, o2) -> {
                    return 0;
                })
                .monitoredWorkflows(Set.of("Java CI with Maven"))
                .build();

        buildReporterEventHandler.handle(workflowRunPayload, buildReporterConfig, gitHub, gitHubGraphQLClient);
    }
}
