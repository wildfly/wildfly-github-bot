package io.xstefank.wildlfy.bot.report;

import io.quarkus.arc.DefaultBean;

import javax.inject.Singleton;

@Singleton
@DefaultBean
public class DefaultWorkflowReportJobIncludeStrategy implements WorkflowReportJobIncludeStrategy {
    @Override
    public boolean include(WorkflowReport report, WorkflowReportJob job) {
        return true;
    }
}
