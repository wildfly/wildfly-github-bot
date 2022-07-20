package io.xstefank.wildlfy.bot;

import io.quarkiverse.githubapp.ConfigFile;
import io.quarkiverse.githubapp.event.WorkflowRun;
import io.smallrye.graphql.client.dynamic.api.DynamicGraphQLClient;
import io.xstefank.wildlfy.bot.config.Feature;
import io.xstefank.wildlfy.bot.config.QuarkusGitHubBotConfig;
import io.xstefank.wildlfy.bot.config.QuarkusGitHubBotConfigFile;
import io.xstefank.wildlfy.bot.report.BuildReporterConfig;
import io.xstefank.wildlfy.bot.report.BuildReporterEventHandler;
import io.xstefank.wildlfy.bot.workflow.QuarkusWorkflowConstants;
import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.GHWorkflowJob;
import org.kohsuke.github.GitHub;


import javax.inject.Inject;
import java.io.IOException;

import java.util.Comparator;


public class AnalyzeWorkflowRunResults {

    @Inject
    BuildReporterEventHandler buildReporterEventHandler;

    @Inject
    QuarkusGitHubBotConfig quarkusBotConfig;

    void analyzeWorkflowResults(@WorkflowRun.Completed @WorkflowRun.Requested GHEventPayload.WorkflowRun workflowRunPayload,
                                @ConfigFile("quarkus-github-bot.yml") QuarkusGitHubBotConfigFile quarkusBotConfigFile,
                                GitHub gitHub, DynamicGraphQLClient gitHubGraphQLClient) throws IOException {
        if (!Feature.ANALYZE_WORKFLOW_RUN_RESULTS.isEnabled(quarkusBotConfigFile)) {
            return;
        }
        System.out.println("It works");
        BuildReporterConfig buildReporterConfig = BuildReporterConfig.builder()
                .dryRun(quarkusBotConfig.isDryRun())
                .monitoredWorkflows(quarkusBotConfigFile.workflowRunAnalysis.workflows)
                .workflowJobComparator(QuarkusWorkflowJobComparator.INSTANCE)
                .build();
        System.out.println(buildReporterConfig);
        buildReporterEventHandler.handle(workflowRunPayload, buildReporterConfig, gitHub, gitHubGraphQLClient);
    }

    private final static class QuarkusWorkflowJobComparator implements Comparator<GHWorkflowJob> {

        private static final QuarkusWorkflowJobComparator INSTANCE = new QuarkusWorkflowJobComparator();

        @Override
        public int compare(GHWorkflowJob o1, GHWorkflowJob o2) {
            if (o1.getName().startsWith(QuarkusWorkflowConstants.JOB_NAME_INITIAL_JDK_PREFIX)
                    && !o2.getName().startsWith(QuarkusWorkflowConstants.JOB_NAME_INITIAL_JDK_PREFIX)) {
                return -1;
            }
            if (!o1.getName().startsWith(QuarkusWorkflowConstants.JOB_NAME_INITIAL_JDK_PREFIX)
                    && o2.getName().startsWith(QuarkusWorkflowConstants.JOB_NAME_INITIAL_JDK_PREFIX)) {
                return 1;
            }

            return o1.getName().compareTo(o2.getName());
        }
    }
}
