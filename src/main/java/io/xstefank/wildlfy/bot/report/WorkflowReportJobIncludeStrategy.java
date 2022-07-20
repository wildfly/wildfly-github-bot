package io.xstefank.wildlfy.bot.report;

public interface WorkflowReportJobIncludeStrategy {

    boolean include(WorkflowReport report, WorkflowReportJob job);
}
