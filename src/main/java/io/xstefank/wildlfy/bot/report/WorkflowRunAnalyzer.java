package io.xstefank.wildlfy.bot.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.bot.build.reporting.model.BuildReport;
import io.quarkus.bot.build.reporting.model.ProjectReport;
import io.xstefank.wildlfy.bot.report.BuildReportsUnarchiver.BuildReports;
import io.xstefank.wildlfy.bot.report.BuildReportsUnarchiver.TestResultsPath;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.surefire.log.api.NullConsoleLogger;
import org.apache.maven.plugins.surefire.report.ReportTestCase;
import org.apache.maven.plugins.surefire.report.ReportTestSuite;
import org.apache.maven.plugins.surefire.report.SurefireReportParser;
import org.jboss.logging.Logger;
import org.kohsuke.github.GHArtifact;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHWorkflowJob;
import org.kohsuke.github.GHWorkflowRun;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;


@ApplicationScoped
public class WorkflowRunAnalyzer {

    private static final Logger LOG = Logger.getLogger(WorkflowRunAnalyzer.class);

    private static final BuildReport EMPTY_BUILD_REPORT = new BuildReport();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Inject
    NoopUrlShortener urlShortener;

    @Inject
    BuildReportsUnarchiver buildReportsUnarchiver;

    @Inject
    StackTraceShortener stackTraceShortener;

    @Inject
    WorkflowJobLabeller workflowJobLabeller;

//    public WorkflowReport getReport(GHWorkflowRun workflowRun, List<GHWorkflowJob> jobs) throws IOException {
//
//        List<WorkflowReportJob> workflowReportJobs = new ArrayList<>();
//        //Заплатка
//        List<WorkflowReportModule> modules = Collections.emptyList();
//        try {
//            for (GHWorkflowJob job : jobs) {
//                workflowReportJobs.add(
//                        new WorkflowReportJob(job.getName(),
//                            job.getConclusion(),
//                            getFailingStep(job.getSteps()),
//                            job.getHtmlUrl().toString(),
//                            getRawLogsUrl(job, workflowRun.getHeadSha()),
//                            modules
//                        )
//                );
//            }
//        } catch (Exception e) {
//
//        }
//
//        return new WorkflowReport(workflowRun.getConclusion(), workflowReportJobs, workflowRun.getHeadSha(),
//                workflowRun.getHtmlUrl().toString());
//    }

    public Optional<WorkflowReport> getReport(GHWorkflowRun workflowRun,
              WorkflowContext workflowContext,
              List<GHWorkflowJob> jobs,
              List<GHArtifact> buildReportsArtifacts) throws IOException {
        if (jobs.isEmpty()) {
            LOG.error(" - No jobs found");
            return Optional.empty();
        }

        GHRepository workflowRunRepository = workflowRun.getRepository();
        String sha = workflowRun.getHeadSha();
        Path allBuildReportsDirectory = Files.createTempDirectory("build-reports-analyzer-");

        try {
            System.out.println("In try getReport");
            List<WorkflowReportJob> workflowReportJobs = new ArrayList<>();

            for (GHWorkflowJob job : jobs) {
                if (job.getConclusion() != GHWorkflowRun.Conclusion.FAILURE && job.getConclusion() != GHWorkflowRun.Conclusion.CANCELLED) {
                    workflowReportJobs.add(
                            new WorkflowReportJob(job.getName(),
                            workflowJobLabeller.label(job.getName()),
                            null,
                            job.getConclusion(),
                            null,
                            null,
                            null,
                            EMPTY_BUILD_REPORT,
                            Collections.emptyList(),
                            false));
                    System.out.println("We have something");

                    continue;
                }

                Optional<GHArtifact> buildReportsArtifactOptional = buildReportsArtifacts.stream()
                        .filter(a -> a.getName().replace(WorkflowConstants.BUILD_REPORTS_ARTIFACT_PREFIX, "")
                                .equals(job.getName()))
                        .findFirst();

                BuildReport buildReport = EMPTY_BUILD_REPORT;
                List<WorkflowReportModule> modules = Collections.emptyList();
                boolean errorDownloadingBuildReports = false;
                if (buildReportsArtifactOptional.isPresent()) {
                    GHArtifact buildReportsArtifact = buildReportsArtifactOptional.get();
                    Path jobDirectory = allBuildReportsDirectory.resolve(buildReportsArtifact.getName());

                    Optional<BuildReports> buildReportsOptional = buildReportsUnarchiver.getBuildReports(workflowContext,
                            buildReportsArtifact, jobDirectory);

                    if (buildReportsOptional.isPresent()) {
                        BuildReports buildReports = buildReportsOptional.get();
                        if (buildReports.getBuildReportPath() != null) {
                            buildReport = getBuildReport(workflowContext, buildReports.getBuildReportPath());
                        }

                        modules = buildReportsArtifactOptional.isPresent()
                                ? getModules(workflowContext, buildReport, jobDirectory, buildReports.getTestResultsPaths(),
                                sha)
                                : Collections.emptyList();
                        System.out.println("=== Modules size ===");
                        System.out.println(modules.size());

                    } else {
                        System.out.println("=== Modules error ===");
                        errorDownloadingBuildReports = true;
                        LOG.error(" - Unable to analyze build report for artifact " + buildReportsArtifact.getName() +
                                " - see exceptions above");
                    }
                }

                workflowReportJobs.add(new WorkflowReportJob(job.getName(),
                        workflowJobLabeller.label(job.getName()),
                        getFailuresAnchor(job.getId()),
                        job.getConclusion(),
                        getFailingStep(job.getSteps()),
                        getJobUrl(job),
                        getRawLogsUrl(job, workflowRun.getHeadSha()),
                        buildReport,
                        modules,
                        errorDownloadingBuildReports)
                );
            }

            if (workflowReportJobs.isEmpty()) {
                LOG.warn(workflowContext.getLogContext() + " - Report jobs empty");
                return Optional.empty();
            }

            WorkflowReport report = new WorkflowReport(sha, workflowReportJobs,
                    workflowRunRepository.getFullName().equals(workflowContext.getRepository()),
                    workflowRun.getConclusion(), workflowRun.getHtmlUrl().toString());

            return Optional.of(report);
        } finally {
            try {
                Files.walk(allBuildReportsDirectory)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            } catch (IOException e) {
                LOG.error(workflowContext.getLogContext() + " - Unable to delete temp directory " + allBuildReportsDirectory);
            }
        }
    }

    private static String getFailingStep(List<GHWorkflowJob.Step> steps) {
        for (GHWorkflowJob.Step step : steps) {
            if (step.getConclusion() != GHWorkflowRun.Conclusion.SUCCESS
                    && step.getConclusion() != GHWorkflowRun.Conclusion.SKIPPED
                    && step.getConclusion() != GHWorkflowRun.Conclusion.NEUTRAL) {
                return step.getName();
            }
        }
        return null;
    }

    private String getJobUrl(GHWorkflowJob job) {
        return urlShortener.shorten(job.getHtmlUrl().toString());
    }

    private String getRawLogsUrl(GHWorkflowJob job, String sha) {
        return urlShortener.shorten(job.getRepository().getHtmlUrl().toString() +
                "/commit/" + sha + "/checks/" + job.getId() + "/logs");
    }

    private static BuildReport getBuildReport(WorkflowContext workflowContext, Path buildReportPath) {
        if (buildReportPath == null) {
            return new BuildReport();
        }

        try {
            return OBJECT_MAPPER.readValue(buildReportPath.toFile(), BuildReport.class);
        } catch (Exception e) {
            LOG.error(" - Unable to deserialize " + WorkflowConstants.BUILD_REPORT_PATH, e);
            return new BuildReport();
        }
    }

    private List<WorkflowReportModule> getModules(
            WorkflowContext workflowContext,
            BuildReport buildReport,
            Path jobDirectory,
            Set<TestResultsPath> testResultsPaths,
            String sha) {
        List<WorkflowReportModule> modules = new ArrayList<>();

        Map<String, ModuleReports> moduleReportsMap = mapModuleReports(buildReport, testResultsPaths, jobDirectory);

        for (Map.Entry<String, ModuleReports> moduleReportsEntry : moduleReportsMap.entrySet()) {
            String moduleName = moduleReportsEntry.getKey();
            ModuleReports moduleReports = moduleReportsEntry.getValue();

            List<ReportTestSuite> reportTestSuites = new ArrayList<>();
            List<WorkflowReportTestCase> workflowReportTestCases = new ArrayList<>();
            for (TestResultsPath testResultPath : moduleReports.getTestResultsPaths()) {
                try {
                    SurefireReportParser surefireReportsParser = new SurefireReportParser(
                            Collections.singletonList(testResultPath.getPath().toFile()), Locale.ENGLISH,
                            new NullConsoleLogger());
                    reportTestSuites.addAll(surefireReportsParser.parseXMLReportFiles());
                    workflowReportTestCases.addAll(surefireReportsParser.getFailureDetails(reportTestSuites).stream()
                            .filter(rtc -> !rtc.hasSkipped())
                            .map(rtc -> new WorkflowReportTestCase(
                                    WorkflowUtils.getFilePath(moduleName, rtc.getFullClassName()),
                                    rtc,
                                    stackTraceShortener.shorten(rtc.getFailureDetail(), 1000, 3),
                                    getFailureUrl(workflowContext.getRepository(), sha, moduleName, rtc),
                                    urlShortener.shorten(getFailureUrl(workflowContext.getRepository(), sha, moduleName, rtc))))
                            .collect(Collectors.toList()));
                } catch (Exception e) {
                    LOG.error(workflowContext.getLogContext() + " - Unable to parse test results for file "
                            + testResultPath.getPath(), e);
                }
            }

            WorkflowReportModule module = new WorkflowReportModule(
                    moduleName,
                    moduleReports.getProjectReport(),
                    moduleReports.getProjectReport() != null ? firstLines(moduleReports.getProjectReport().getError(), 5)
                            : null,
                    reportTestSuites,
                    workflowReportTestCases);

            if (module.hasReportedFailures()) {
                modules.add(module);
            }
        }

        return modules;
    }

    private static String getFailuresAnchor(Long jobId) {
        return "test-failures-job-" + jobId;
    }

    private static class ModuleReports {

        private final ProjectReport projectReport;
        private final List<TestResultsPath> testResultsPaths;

        private ModuleReports(ProjectReport projectReport, List<TestResultsPath> testResultsPaths) {
            this.projectReport = projectReport;
            this.testResultsPaths = testResultsPaths;
        }

        public ProjectReport getProjectReport() {
            return projectReport;
        }

        public List<TestResultsPath> getTestResultsPaths() {
            return testResultsPaths;
        }
    }

    private static Map<String, ModuleReports> mapModuleReports(BuildReport buildReport, Set<TestResultsPath> testResultsPaths,
                                                               Path jobDirectory) {
        Set<String> modules = new TreeSet<>();
        modules.addAll(buildReport.getProjectReports().stream().map(pr -> normalizeModuleName(pr.getBasedir()))
                .collect(Collectors.toList()));
        modules.addAll(testResultsPaths.stream().map(trp -> normalizeModuleName(trp.getModuleName(jobDirectory)))
                .collect(Collectors.toList()));

        Map<String, ModuleReports> moduleReports = new TreeMap<>();
        for (String module : modules) {
            moduleReports.put(module, new ModuleReports(
                    buildReport.getProjectReports().stream().filter(pr -> normalizeModuleName(pr.getBasedir()).equals(module))
                            .findFirst().orElse(null),
                    testResultsPaths.stream().filter(trp -> normalizeModuleName(trp.getModuleName(jobDirectory)).equals(module))
                            .collect(Collectors.toList())));
        }

        return moduleReports;
    }

    private static String normalizeModuleName(String moduleName) {
        return moduleName.replace('\\', '/');
    }

    private static String getFailureUrl(String repository, String sha, String moduleName, ReportTestCase reportTestCase) {
        String classPath = reportTestCase.getFullClassName().replace(".", "/");
        int dollarIndex = reportTestCase.getFullClassName().indexOf('$');
        if (dollarIndex > 0) {
            classPath = classPath.substring(0, dollarIndex);
        }
        classPath = "src/test/java/" + classPath + ".java";

        StringBuilder sb = new StringBuilder();
        sb.append("https://github.com/").append(repository).append("/blob/").append(sha).append("/")
                .append(WorkflowUtils.getFilePath(moduleName, reportTestCase.getFullClassName()));
        if (StringUtils.isNotBlank(reportTestCase.getFailureErrorLine())) {
            sb.append("#L").append(reportTestCase.getFailureErrorLine());
        }
        return sb.toString();
    }

    private static String firstLines(String string, int numberOfLines) {
        if (string == null || string.isBlank()) {
            return null;
        }

        return string.lines().limit(numberOfLines).collect(Collectors.joining("\n"));
    }
}
