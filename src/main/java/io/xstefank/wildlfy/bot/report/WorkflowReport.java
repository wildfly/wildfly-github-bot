package io.xstefank.wildlfy.bot.report;

import io.quarkus.runtime.annotations.RegisterForReflection;
import org.kohsuke.github.GHWorkflowRun;

import java.util.List;
import java.util.stream.Collectors;


@RegisterForReflection
public class WorkflowReport {

    private final String sha;
    private final List<WorkflowReportJob> jobs;
    private final boolean sameRepository;
    private final GHWorkflowRun.Conclusion conclusion;
    private final String workflowRunUrl;

    public WorkflowReport(String sha, List<WorkflowReportJob> jobs, boolean sameRepository, GHWorkflowRun.Conclusion conclusion,
                          String workflowRunUrl) {
        this.sha = sha;
        this.jobs = jobs;
        this.sameRepository = sameRepository;
        this.conclusion = conclusion;
        this.workflowRunUrl = workflowRunUrl;
    }

    public String getSha() {
        return sha;
    }

    public void addJob(WorkflowReportJob job) {
        this.jobs.add(job);
    }

    public List<WorkflowReportJob> getJobs() {
        return jobs;
    }

    public boolean hasJobsFailing() {
        for (WorkflowReportJob job : jobs) {
            if (job.isFailing()) {
                return true;
            }
        }
        return false;
    }

    public List<WorkflowReportJob> getJobsWithReportedFailures() {
        return jobs.stream().filter(j -> j.hasReportedFailures()).collect(Collectors.toList());
    }

    public boolean hasReportedFailures() {
        return hasBuildReportFailures() || hasTestFailures();
    }

    public boolean hasBuildReportFailures() {
        for (WorkflowReportJob job : jobs) {
            if (job.hasBuildReportFailures()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasTestFailures() {
        int i = 0;
        for (WorkflowReportJob job : jobs) {
            System.out.println(i);

            if (job.hasTestFailures()) {
                System.out.println(" job is true" + i);

                return true;
            }
            i++;
        }
        System.out.println(" job is false" + i);


        return false;
    }

    public boolean isSameRepository() {
        return sameRepository;
    }

    public boolean isCancelled() {
        if (GHWorkflowRun.Conclusion.CANCELLED.equals(conclusion)) {
            return true;
        }

        return jobs.stream()
                .noneMatch(j -> GHWorkflowRun.Conclusion.CANCELLED != j.getConclusion()
                        && GHWorkflowRun.Conclusion.SKIPPED != j.getConclusion()
                        && GHWorkflowRun.Conclusion.NEUTRAL != j.getConclusion());
    }

    public boolean isFailure() {
        return GHWorkflowRun.Conclusion.FAILURE.equals(conclusion);
    }

    public String getWorkflowRunUrl() {
        return workflowRunUrl;
    }

    public boolean hasErrorDownloadingBuildReports() {
        for (WorkflowReportJob job : jobs) {
            if (job.hasErrorDownloadingBuildReports()) {
                return true;
            }
        }
        return false;
    }
}

//
//public class WorkflowReport {
//
//    private final GHWorkflowRun.Conclusion conclusion;
//    private final List<WorkflowReportJob> jobs;
//    private final String sha;
//    private final String workflowRunUrl;
//
//    public WorkflowReport(GHWorkflowRun.Conclusion conclusion, List<WorkflowReportJob> jobs,
//                          String sha, String workflowRunUrl) {
//        this.conclusion = conclusion;
//        this.jobs = jobs;
//        this.sha = sha;
//        this.workflowRunUrl = workflowRunUrl;
//    }
//
//    public List<WorkflowReportJob> getJobs() {
//        return jobs;
//    }
//
//    public String getSha() {
//        return sha;
//    }
//
//    public String getWorkflowRunUrl() {
//        return workflowRunUrl;
//    }
//
//    public List<WorkflowReportJob> getJobsWithReportedFailures() {
//        return jobs.stream().filter(j -> j.hasReportedFailures()).collect(Collectors.toList());
//    }
//
//    public boolean hasJobsFailing() {
//        for (WorkflowReportJob job : jobs) {
//            if (job.isFailing()) {
//                return true;
//            }
//        }
//        return false;
//    }
//
//    public boolean isFailure() {
//        System.out.println("In isFailure");
//        System.out.println(conclusion);
//        System.out.println("result - " + GHWorkflowRun.Conclusion.FAILURE.equals(conclusion));
//        return GHWorkflowRun.Conclusion.FAILURE.equals(conclusion);
//    }
//}
