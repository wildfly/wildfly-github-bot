package org.wildfly.bot.util;

import static java.util.Objects.requireNonNull;
import static org.wildfly.bot.model.RuntimeConstants.BOT_JIRA_LINKS_HEADER;
import static org.wildfly.bot.model.RuntimeConstants.BOT_JIRA_LINK_COMMENT_TEMPLATE;
import static org.wildfly.bot.model.RuntimeConstants.BOT_JIRA_LINK_TEMPLATE;
import static org.wildfly.bot.model.RuntimeConstants.BOT_MESSAGE_DELIMINER;
import static org.wildfly.bot.model.RuntimeConstants.BOT_MESSAGE_WARNING;
import static org.wildfly.bot.model.RuntimeConstants.BOT_REPO_REF_FOOTER;
import static org.wildfly.bot.util.Strings.blockQuoted;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.SequencedSet;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.kohsuke.github.GHPullRequest;

/**
 * Handles the description body of a pull request.
 *
 * <p>
 *
 * This class is responsible for managing the user-provided body and the
 * bot-generated body
 * of a pull request. It checks for missing issue links in the user body and
 * generates
 * a full description body that combines both user and bot sections.
 *
 * @author mskacelik
 */
public final class PullRequestDescriptionHandler {
    private final String userBody;
    private final String botBody;
    private final String gitHubAppName;
    private final SequencedSet<String> missingIssueLinksWithinUserBody;

    public PullRequestDescriptionHandler(GHPullRequest pullRequest, Pattern issueKeyPattern, String githubAppName) {
        requireNonNull(pullRequest, "Pull request must not be null");
        requireNonNull(issueKeyPattern, "Issue link pattern must not be null");

        this.gitHubAppName = githubAppName;

        String body = pullRequest.getBody();
        final String normalizedFullBody = (body != null) ? body.replaceAll("\\r", "") : "";
        final int startOfBotBodyIndex = normalizedFullBody.lastIndexOf("\n" + BOT_MESSAGE_DELIMINER);

        if (startOfBotBodyIndex != -1) {
            this.userBody = normalizedFullBody.substring(0, startOfBotBodyIndex);
            this.botBody = normalizedFullBody.substring(startOfBotBodyIndex);
        } else {
            this.userBody = normalizedFullBody;
            this.botBody = ""; // empty
        }

        List<String> textSources = new ArrayList<>(); // title + commit messages
        textSources.add(pullRequest.getTitle());
        for (var commit : pullRequest.listCommits()) {
            textSources.add(commit.getCommit().getMessage());
        }
        this.missingIssueLinksWithinUserBody = textSources.stream()
                .flatMap(text -> issueKeyPattern.matcher(text).results().map(MatchResult::group))
                .distinct()
                .filter(issueKey -> !userBody.contains(BOT_JIRA_LINK_TEMPLATE.formatted(issueKey)))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Generates the full description body for the pull request.
     * <p>
     * This method combines the user-provided body with the bot-generated body,
     * ensuring that any missing issue links are included in the bot section.
     * If there are no changes in the bot body, it returns an empty {@code Optional}
     * because
     * no updates are needed to the pull request description.
     * For example, if the user body already contains all the necessary issue links,
     * and was updated by the user, the bot won't change the description.
     *
     * @return an {@code Optional} containing the full description body if changes
     *         are made, or an empty Optional if no changes are needed.
     */
    public Optional<String> generateFullDescriptionBody() {
        StringBuilder newBotBody = generateBotBodySection();
        if (newBotBody.toString().equals(botBody)) {
            return Optional.empty(); // no changes in the bot body
        }
        String fullDescription = generateUserBodySection()
                .append(newBotBody)
                .toString();
        return Optional.of(fullDescription);
    }

    private StringBuilder generateUserBodySection() {
        return new StringBuilder(userBody);
    }

    private StringBuilder generateBotBodySection() {
        StringBuilder botBodySection = new StringBuilder();

        if (missingIssueLinksWithinUserBody.isEmpty()) {
            return botBodySection; // empty bot body section if no issues are missing
        }

        botBodySection
                .append("\n")
                .append(BOT_MESSAGE_DELIMINER)
                .append("\n\n")
                .append(blockQuoted(BOT_MESSAGE_WARNING))
                .append("\n\n")
                .append(blockQuoted(BOT_JIRA_LINKS_HEADER));

        missingIssueLinksWithinUserBody.stream()
                .map(BOT_JIRA_LINK_COMMENT_TEMPLATE::formatted)
                .map(Strings::blockQuoted)
                .forEach(link -> botBodySection.append(link));

        botBodySection
                .append("\n\n")
                .append(BOT_REPO_REF_FOOTER.formatted(gitHubAppName));

        return botBodySection;
    }
}
