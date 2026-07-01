package org.wildfly.bot;

import io.quarkiverse.githubapp.ConfigFile;
import io.quarkiverse.githubapp.event.PullRequest;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.GHPullRequest;
import org.wildfly.bot.config.WildFlyBotConfig;
import org.wildfly.bot.model.WildFlyConfigFile;
import org.wildfly.bot.util.GitHubBotContextProvider;
import org.wildfly.bot.util.PullRequestLogger;
import org.wildfly.bot.util.Strings;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.SequencedSet;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.wildfly.bot.model.RuntimeConstants.BOT_JIRA_LINKS_HEADER;
import static org.wildfly.bot.model.RuntimeConstants.BOT_JIRA_LINK_COMMENT_TEMPLATE;
import static org.wildfly.bot.model.RuntimeConstants.BOT_JIRA_LINK_TEMPLATE;
import static org.wildfly.bot.model.RuntimeConstants.BOT_MESSAGE_DELIMITER;
import static org.wildfly.bot.model.RuntimeConstants.BOT_MESSAGE_WARNING;
import static org.wildfly.bot.model.RuntimeConstants.BOT_REPO_REF_FOOTER;
import static org.wildfly.bot.model.RuntimeConstants.CONFIG_FILE_NAME;
import static org.wildfly.bot.model.RuntimeConstants.DRY_RUN_PREPEND;

@RequestScoped
public class PullRequestDescriptionProcessor {

    private final PullRequestLogger logger = PullRequestLogger.getLogger(PullRequestDescriptionProcessor.class);

    @Inject
    WildFlyBotConfig wildFlyBotConfig;

    @Inject
    GitHubBotContextProvider botContextProvider;

    void appendJiraLinks(
            @PullRequest.Edited @PullRequest.Opened @PullRequest.Synchronize @PullRequest.Reopened @PullRequest.ReadyForReview GHEventPayload.PullRequest pullRequestPayload,
            @ConfigFile(CONFIG_FILE_NAME) WildFlyConfigFile wildflyConfigFile) throws IOException {
        GHPullRequest pullRequest = pullRequestPayload.getPullRequest();
        logger.setPullRequest(pullRequest);

        if (shouldSkipDescriptionAppending(pullRequest, wildflyConfigFile)) {
            return;
        }

        generateUpdatedDescription(pullRequest, wildflyConfigFile.wildfly.getProjectPattern())
                .ifPresent(newBody -> {
                    if (wildFlyBotConfig.isDryRun()) {
                        logger.infof(DRY_RUN_PREPEND.formatted("Updated PR body:\n%s"), newBody);
                        return;
                    }
                    try {
                        pullRequest.setBody(newBody);
                    } catch (IOException e) {
                        logger.errorf(e, "Failed to set body for pull request #%s", pullRequest.getNumber());
                        throw new UncheckedIOException(e);
                    }
                });
    }

    private boolean shouldSkipDescriptionAppending(GHPullRequest pullRequest,
            WildFlyConfigFile configFile) throws IOException {
        if (configFile == null) {
            logger.info("Skipping JIRA link appending: no configuration file found");
            return true;
        }
        if (pullRequest.isDraft()) {
            logger.info("Skipping JIRA link appending: pull request is a draft");
            return true;
        }
        return false;
    }

    private Optional<String> generateUpdatedDescription(GHPullRequest pullRequest, Pattern projectPattern) {
        String body = pullRequest.getBody();
        String normalizedFullBody = (body != null) ? body.replace("\r", "") : "";
        int startOfBotBodyIndex = normalizedFullBody.lastIndexOf("\n" + BOT_MESSAGE_DELIMITER);

        String userBody;
        String existingBotBody;
        if (startOfBotBodyIndex != -1) {
            userBody = normalizedFullBody.substring(0, startOfBotBodyIndex);
            existingBotBody = normalizedFullBody.substring(startOfBotBodyIndex);
        } else {
            userBody = normalizedFullBody;
            existingBotBody = "";
        }

        SequencedSet<String> missingIssueLinks = findMissingIssueLinks(pullRequest, projectPattern, userBody);
        String newBotBody = generateBotBodySection(missingIssueLinks);

        if (newBotBody.equals(existingBotBody)) {
            return Optional.empty();
        }
        return Optional.of(userBody + newBotBody);
    }

    private SequencedSet<String> findMissingIssueLinks(GHPullRequest pullRequest, Pattern projectPattern,
            String userBody) {
        List<String> textSources = new ArrayList<>();
        textSources.add(pullRequest.getTitle());
        for (var commit : pullRequest.listCommits()) {
            textSources.add(commit.getCommit().getMessage());
        }
        return textSources.stream()
                .flatMap(text -> projectPattern.matcher(text).results().map(MatchResult::group))
                .distinct()
                .filter(issueKey -> !userBody.contains(BOT_JIRA_LINK_TEMPLATE.formatted(issueKey)))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String generateBotBodySection(SequencedSet<String> missingIssueLinks) {
        if (missingIssueLinks.isEmpty()) {
            return "";
        }

        StringBuilder botBodySection = new StringBuilder();
        botBodySection
                .append("\n")
                .append(BOT_MESSAGE_DELIMITER)
                .append("\n\n")
                .append(Strings.blockQuoted(BOT_MESSAGE_WARNING))
                .append("\n\n")
                .append(Strings.blockQuoted(BOT_JIRA_LINKS_HEADER));

        missingIssueLinks.stream()
                .map(BOT_JIRA_LINK_COMMENT_TEMPLATE::formatted)
                .map(Strings::blockQuoted)
                .forEach(botBodySection::append);

        botBodySection
                .append("\n\n")
                .append(BOT_REPO_REF_FOOTER.formatted(botContextProvider.getBotName()));

        return botBodySection.toString();
    }
}
