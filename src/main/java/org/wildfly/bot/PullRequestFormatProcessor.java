package org.wildfly.bot;

import io.quarkiverse.githubapp.ConfigFile;
import io.quarkiverse.githubapp.event.PullRequest;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;
import org.kohsuke.github.GHCommitStatus;
import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.GHPullRequest;
import org.wildfly.bot.config.WildFlyBotConfig;
import org.wildfly.bot.format.Check;
import org.wildfly.bot.format.CommitMessagesCheck;
import org.wildfly.bot.format.DescriptionCheck;
import org.wildfly.bot.format.TitleCheck;
import org.wildfly.bot.model.RegexDefinition;
import org.wildfly.bot.model.WildFlyConfigFile;
import org.wildfly.bot.util.GitHubBotContextProvider;
import org.wildfly.bot.util.GithubProcessor;
import org.wildfly.bot.util.PullRequestDescriptionHandler;
import org.wildfly.bot.util.PullRequestLogger;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.wildfly.bot.model.RuntimeConstants.CONFIG_FILE_NAME;
import static org.wildfly.bot.model.RuntimeConstants.DEPENDABOT;
import static org.wildfly.bot.model.RuntimeConstants.DRY_RUN_PREPEND;
import static org.wildfly.bot.model.RuntimeConstants.FAILED_FORMAT_COMMENT;

@RequestScoped
public class PullRequestFormatProcessor {

    private static final Logger LOG_DELEGATE = Logger.getLogger(PullRequestFormatProcessor.class);
    private final PullRequestLogger LOG = new PullRequestLogger(LOG_DELEGATE);
    private static final String CHECK_NAME = "Format";

    @Inject
    GithubProcessor githubProcessor;

    @Inject
    WildFlyBotConfig wildFlyBotConfig;

    @Inject
    GitHubBotContextProvider botContextProvider;

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
                // Only update format check if it was created
                if (CHECK_NAME.equals(commitStatus.getContext())
                        && commitStatus.getCreator().getLogin().equals(botContextProvider.getBotName())) {
                    githubProcessor.commitStatusSuccess(pullRequest, CHECK_NAME, "Valid [Skipped]");
                }
            }
            githubProcessor.deleteFormatComment(pullRequest, FAILED_FORMAT_COMMENT);
            LOG.infof("Skipping format due to %s", message);
            return;
        }

        if (pullRequest.getUser().getLogin().equals(DEPENDABOT)
                && pullRequestPayload.getAction().equals(PullRequest.Opened.NAME)) {
            LOG.info("Skipping format check on newly opened dependabot PRs.");
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

        generateAppendedMessage(pullRequest, wildflyConfigFile.wildfly.getProjectPattern());

        if (errors.isEmpty()) {
            githubProcessor.commitStatusSuccess(pullRequest, CHECK_NAME, "Valid");
        } else {
            githubProcessor.commitStatusError(pullRequest, CHECK_NAME, "Failed checks: " + String.join(", ", errors.keySet()));
        }
        githubProcessor.formatComment(pullRequest, FAILED_FORMAT_COMMENT, errors.values());
    }

    private void generateAppendedMessage(GHPullRequest pullRequest, Pattern projectPattern) {
        new PullRequestDescriptionHandler(pullRequest, projectPattern, botContextProvider.getBotName())
                .generateFullDescriptionBody()
                .ifPresent(newBody -> {
                    if (wildFlyBotConfig.isDryRun()) {
                        LOG.infof("Pull Request #%s - Updated PR body:\n%s", pullRequest.getNumber(), newBody);
                        return;
                    }
                    try {
                        pullRequest.setBody(newBody);
                    } catch (IOException e) {
                        LOG.errorf(e, "Failed to set body for pull request #%s", pullRequest.getNumber());
                        throw new UncheckedIOException(e);
                    }

                });
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
