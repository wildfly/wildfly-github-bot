package io.xstefank.wildfly.bot;

import io.quarkiverse.githubapp.ConfigFile;
import io.quarkiverse.githubapp.event.PullRequest;
import io.xstefank.wildfly.bot.config.WildFlyBotConfig;
import io.xstefank.wildfly.bot.format.Check;
import io.xstefank.wildfly.bot.format.CommitMessagesCheck;
import io.xstefank.wildfly.bot.format.DescriptionCheck;
import io.xstefank.wildfly.bot.format.TitleCheck;
import io.xstefank.wildfly.bot.model.RegexDefinition;
import io.xstefank.wildfly.bot.model.RuntimeConstants;
import io.xstefank.wildfly.bot.model.WildFlyConfigFile;
import io.xstefank.wildfly.bot.util.GithubProcessor;
import io.xstefank.wildfly.bot.util.PullRequestLogger;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHPullRequestCommitDetail;
import org.kohsuke.github.PagedIterable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static io.xstefank.wildfly.bot.model.RuntimeConstants.DEPENDABOT;
import static io.xstefank.wildfly.bot.model.RuntimeConstants.FAILED_FORMAT_COMMENT;
import static io.xstefank.wildfly.bot.util.Strings.blockQuoted;

@RequestScoped
public class PullRequestFormatProcessor {

    private static final Logger LOG_DELEGATE = Logger.getLogger(PullRequestFormatProcessor.class);
    private final PullRequestLogger LOG = new PullRequestLogger(LOG_DELEGATE);
    private static final String CHECK_NAME = "Format";

    @Inject
    GithubProcessor githubProcessor;

    @Inject
    WildFlyBotConfig wildFlyBotConfig;

    void pullRequestFormatCheck(
            @PullRequest.Edited @PullRequest.Opened @PullRequest.Synchronize @PullRequest.Reopened @PullRequest.ReadyForReview GHEventPayload.PullRequest pullRequestPayload,
            @ConfigFile(RuntimeConstants.CONFIG_FILE_NAME) WildFlyConfigFile wildflyConfigFile) throws IOException {
        GHPullRequest pullRequest = pullRequestPayload.getPullRequest();
        LOG.setPullRequest(pullRequest);
        githubProcessor.LOG.setPullRequest(pullRequest);

        String message = githubProcessor.skipPullRequest(pullRequest, wildflyConfigFile);
        if (message != null) {
            LOG.infof("Skipping format due to %s", message);
            return;
        }

        if (!wildflyConfigFile.wildfly.format.enabled) {
            LOG.info("Skipping format due to format being disabled");
            return;
        }

        List<Check> checks = initializeChecks(wildflyConfigFile);
        Map<String, String> errors = new HashMap<>();

        for (Check check : checks) {
            String result = check.check(pullRequest);
            if (result != null) {
                errors.put(check.getName(), result);
            }
        }

        generateAppendedMessage(pullRequest, wildflyConfigFile.wildfly.getIssuePattern());

        if (errors.isEmpty()) {
            githubProcessor.commitStatusSuccess(pullRequest, CHECK_NAME, "Valid");
        } else {
            githubProcessor.commitStatusError(pullRequest, CHECK_NAME, "Failed checks: " + String.join(", ", errors.keySet()));
        }
        githubProcessor.formatComment(pullRequest, FAILED_FORMAT_COMMENT, errors.values());
    }

    void postDependabotInfo(@PullRequest.Opened GHEventPayload.PullRequest pullRequestPayload,
            @ConfigFile(RuntimeConstants.CONFIG_FILE_NAME) WildFlyConfigFile wildflyConfigFile) throws IOException {
        GHPullRequest pullRequest = pullRequestPayload.getPullRequest();
        LOG.setPullRequest(pullRequest);
        githubProcessor.LOG.setPullRequest(pullRequest);

        if (pullRequest.getUser().getLogin().equals(DEPENDABOT)) {
            LOG.infof("Dependabot detected.");
            String comment = ("WildFly Bot recognized this PR as dependabot dependency update. Please create a %s issue" +
                    " and add new comment containing this JIRA link please.")
                    .formatted(wildflyConfigFile.wildfly.projectKey);
            if (wildFlyBotConfig.isDryRun()) {
                LOG.infof(RuntimeConstants.DRY_RUN_PREPEND.formatted("Add new comment %s"), comment);
            } else {
                pullRequest.comment(comment);
            }
        }

    }


    private void generateAppendedMessage(GHPullRequest pullRequest, Pattern projectPattern) throws IOException {
        Set<String> jiraIssues = parseJiraIssues(pullRequest, projectPattern);
        if (jiraIssues.isEmpty()) {
            LOG.debugf("No JIRA issues found for Pull Request [#%s]: \"%s\"", pullRequest.getNumber(), pullRequest.getTitle());
        }

        String originalBody = pullRequest.getBody();
        final StringBuilder sb = new StringBuilder();

        if (originalBody != null) {
            final String trimmedOriginalBody = originalBody.replaceAll("\\r", "");
            final int startIndex = trimmedOriginalBody.indexOf(RuntimeConstants.BOT_MESSAGE_HEADER);
            sb.append(trimmedOriginalBody.substring(0, startIndex > -1 ? startIndex : trimmedOriginalBody.length()).trim())
                    .append("\n\n");

            // we create links, search for ones contained in the original body
            Set<String> jiraLinks = jiraIssues.stream()
                    .map(s -> String.format(RuntimeConstants.BOT_JIRA_LINK_TEMPLATE, s))
                    .collect(Collectors.toSet());

            // collect back jira issues from links, which are missing in the description
            jiraIssues = jiraLinks.stream()
                    .filter(s -> !trimmedOriginalBody.contains(s))
                    .map(s -> {
                        Matcher matcher = projectPattern.matcher(s);
                        matcher.find();
                        return matcher.group();
                    })
                    .collect(Collectors.toSet());
        }

        if (jiraIssues.isEmpty()) {
            return;
        }

        sb.append(RuntimeConstants.BOT_MESSAGE_HEADER)
                .append("\n\n")
                .append(blockQuoted(RuntimeConstants.BOT_JIRA_LINKS_HEADER));

        for (String jira : jiraIssues) {

            sb.append(blockQuoted(String.format(RuntimeConstants.BOT_JIRA_LINK_COMMENT_TEMPLATE, jira)));
        }

        sb.append(RuntimeConstants.BOT_MESSAGE_FOOTER);
        String newBody = sb.toString();

        if (wildFlyBotConfig.isDryRun()) {
            LOG.infof("Pull Request #%s - Updated PR body:\n%s", newBody);
        } else {
            pullRequest.setBody(newBody);
        }
    }

    private Set<String> parseJiraIssues(GHPullRequest pullRequest, Pattern jiraPattern) {
        Set<String> jiras = parseJirasFromString(pullRequest.getTitle(), jiraPattern);
        PagedIterable<GHPullRequestCommitDetail> commits = pullRequest.listCommits();
        for (GHPullRequestCommitDetail commit : commits) {
            String commitMessage = commit.getCommit().getMessage();
            jiras.addAll(parseJirasFromString(commitMessage, jiraPattern));
        }
        return jiras;
    }

    private Set<String> parseJirasFromString(String fromString, Pattern jiraPattern) {
        Set<String> jiras = new TreeSet<>();
        Matcher matcher = jiraPattern.matcher(fromString);
        while (matcher.find()) {
            jiras.add(matcher.group());
        }

        return jiras;
    }

    private List<Check> initializeChecks(WildFlyConfigFile wildflyConfigFile) {
        List<Check> checks = new ArrayList<>();

        if (wildflyConfigFile.wildfly.format == null) {
            return checks;
        }

        if (wildflyConfigFile.wildfly.format.title.enabled) {
            checks.add(new TitleCheck(new RegexDefinition(wildflyConfigFile.wildfly.getProjectPattern(),
                    wildflyConfigFile.wildfly.format.title.message)));
        }

        if (wildflyConfigFile.wildfly.format.commit.enabled) {
            checks.add(new CommitMessagesCheck(new RegexDefinition(wildflyConfigFile.wildfly.getProjectPattern(),
                    wildflyConfigFile.wildfly.format.commit.message)));
        }

        if (wildflyConfigFile.wildfly.format.description != null) {
            checks.add(new DescriptionCheck(wildflyConfigFile.wildfly.format.description));
        }

        return checks;
    }
}
