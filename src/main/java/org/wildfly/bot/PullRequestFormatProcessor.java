package org.wildfly.bot;

import io.quarkiverse.githubapp.ConfigFile;
import io.quarkiverse.githubapp.event.PullRequest;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.kohsuke.github.GHCommitStatus;
import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHPullRequestCommitDetail;
import org.kohsuke.github.PagedIterable;
import org.wildfly.bot.config.WildFlyBotConfig;
import org.wildfly.bot.format.Check;
import org.wildfly.bot.format.CommitMessagesCheck;
import org.wildfly.bot.format.DescriptionCheck;
import org.wildfly.bot.format.TitleCheck;
import org.wildfly.bot.model.RegexDefinition;
import org.wildfly.bot.model.WildFlyConfigFile;
import org.wildfly.bot.util.GithubProcessor;
import org.wildfly.bot.util.PullRequestLogger;

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

import static org.wildfly.bot.model.RuntimeConstants.BOT_JIRA_LINKS_HEADER;
import static org.wildfly.bot.model.RuntimeConstants.BOT_JIRA_LINK_COMMENT_TEMPLATE;
import static org.wildfly.bot.model.RuntimeConstants.BOT_JIRA_LINK_TEMPLATE;
import static org.wildfly.bot.model.RuntimeConstants.BOT_MESSAGE_DELIMINER;
import static org.wildfly.bot.model.RuntimeConstants.BOT_MESSAGE_FOOTER;
import static org.wildfly.bot.model.RuntimeConstants.BOT_MESSAGE_HEADER;
import static org.wildfly.bot.model.RuntimeConstants.BOT_REPO_REF_FOOTER;
import static org.wildfly.bot.model.RuntimeConstants.CONFIG_FILE_NAME;
import static org.wildfly.bot.model.RuntimeConstants.DEPENDABOT;
import static org.wildfly.bot.model.RuntimeConstants.DRY_RUN_PREPEND;
import static org.wildfly.bot.model.RuntimeConstants.FAILED_FORMAT_COMMENT;
import static org.wildfly.bot.util.Strings.blockQuoted;

@RequestScoped
public class PullRequestFormatProcessor {

    private static final Logger LOG_DELEGATE = Logger.getLogger(PullRequestFormatProcessor.class);
    private final PullRequestLogger LOG = new PullRequestLogger(LOG_DELEGATE);
    private static final String CHECK_NAME = "Format";

    @Inject
    GithubProcessor githubProcessor;

    @Inject
    WildFlyBotConfig wildFlyBotConfig;

    void postDependabotInfo(@PullRequest.Opened GHEventPayload.PullRequest pullRequestPayload,
            @ConfigFile(CONFIG_FILE_NAME) WildFlyConfigFile wildflyConfigFile) throws IOException {
        GHPullRequest pullRequest = pullRequestPayload.getPullRequest();
        LOG.setPullRequest(pullRequest);
        githubProcessor.LOG.setPullRequest(pullRequest);

        if (pullRequest.getUser().getLogin().equals(DEPENDABOT)) {
            LOG.infof("Dependabot detected.");
            String comment = ("WildFly Bot recognized this PR as dependabot dependency update. Please create a %s issue" +
                    " and add new comment containing this JIRA link please.")
                    .formatted(wildflyConfigFile.wildfly.projectKey);
            if (wildFlyBotConfig.isDryRun()) {
                LOG.infof(DRY_RUN_PREPEND.formatted("Add new comment %s"), comment);
            } else {
                pullRequest.comment(comment);
            }
        }

    }

    void pullRequestFormatCheck(
            @PullRequest.Edited @PullRequest.Opened @PullRequest.Synchronize @PullRequest.Reopened @PullRequest.ReadyForReview GHEventPayload.PullRequest pullRequestPayload,
            @ConfigFile(CONFIG_FILE_NAME) WildFlyConfigFile wildflyConfigFile) throws IOException {
        GHPullRequest pullRequest = pullRequestPayload.getPullRequest();
        LOG.setPullRequest(pullRequest);
        githubProcessor.LOG.setPullRequest(pullRequest);

        String message = githubProcessor.skipPullRequest(pullRequest, wildflyConfigFile);
        if (message != null) {
            String sha = pullRequest.getHead().getSha();
            for (GHCommitStatus commitStatus : pullRequest.getRepository().listCommitStatuses(sha)) {
                // only update format check, if it was created
                if (commitStatus.getContext().equals(CHECK_NAME)) {
                    githubProcessor.commitStatusSuccess(pullRequest, CHECK_NAME, "Valid [Skipped]");
                }
            }
            githubProcessor.deleteFormatComment(pullRequest, FAILED_FORMAT_COMMENT);
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

    private void generateAppendedMessage(GHPullRequest pullRequest, Pattern projectPattern) throws IOException {
        String originalBody = pullRequest.getBody();
        final StringBuilder sb = new StringBuilder();

        if (originalBody != null) {
            final String trimmedOriginalBody = originalBody.replaceAll("\\r", "");
            final int startIndex = trimmedOriginalBody.indexOf(BOT_MESSAGE_DELIMINER);
            sb.append(trimmedOriginalBody.substring(0, startIndex > -1 ? startIndex : trimmedOriginalBody.length()).trim())
                    .append(BOT_MESSAGE_DELIMINER);
        }

        appendJiraIssues(sb, pullRequest, projectPattern);
        appendInfoFooter(sb);

        String newBody = sb.toString();

        if (wildFlyBotConfig.isDryRun()) {
            LOG.infof("Pull Request #%s - Updated PR body:\n%s", newBody);
        } else {
            pullRequest.setBody(newBody);
        }
    }

    private void appendJiraIssues(StringBuilder sb, GHPullRequest pullRequest, Pattern projectPattern) {
        Set<String> jiraIssues = parseJiraIssues(pullRequest, projectPattern);
        String currentBody = sb.toString();
        if (jiraIssues.isEmpty()) {
            LOG.debugf("No JIRA issues found for Pull Request [#%s]: \"%s\"", pullRequest.getNumber(), pullRequest.getTitle());
        }

        // we create links, search for ones contained in the original body
        Set<String> jiraLinks = jiraIssues.stream()
                .map(s -> String.format(BOT_JIRA_LINK_TEMPLATE, s))
                .collect(Collectors.toSet());

        // collect back jira issues from links, which are missing in the description
        jiraIssues = jiraLinks.stream()
                .filter(s -> !currentBody.contains(s))
                .map(s -> {
                    Matcher matcher = projectPattern.matcher(s);
                    matcher.find();
                    return matcher.group();
                })
                .collect(Collectors.toSet());

        if (jiraIssues.isEmpty()) {
            return;
        }

        if (!currentBody.isEmpty()) {
            sb.append("\n\n");
        }

        sb.append(BOT_MESSAGE_HEADER)
                .append(blockQuoted(BOT_JIRA_LINKS_HEADER));

        for (String jira : jiraIssues) {

            sb.append(blockQuoted(String.format(BOT_JIRA_LINK_COMMENT_TEMPLATE, jira)));
        }

        sb.append(BOT_MESSAGE_FOOTER);
    }

    private void appendInfoFooter(StringBuilder sb) {
        String footer = BOT_REPO_REF_FOOTER.formatted((wildFlyBotConfig.githubName()));
        if (!sb.toString().contains(footer)) {
            if (!sb.toString().isEmpty()) {
                sb.append("\n\n");
            }
            sb.append(footer);
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
