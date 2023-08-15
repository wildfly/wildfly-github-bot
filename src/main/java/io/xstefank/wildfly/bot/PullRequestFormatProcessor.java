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
import io.xstefank.wildfly.bot.util.GithubCommitProcessor;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.GHIssueComment;
import org.kohsuke.github.GHPullRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.xstefank.wildfly.bot.model.RuntimeConstants.DEPENDABOT;

@RequestScoped
public class PullRequestFormatProcessor {

    private static final Logger LOG = Logger.getLogger(PullRequestFormatProcessor.class);

    private static final String CHECK_NAME = "Format";
    public static final String FAILED_FORMAT_COMMENT = """
        Failed format check on this pull request:

        %s

        Please fix the format according to these guidelines.
        """;

    @Inject
    GithubCommitProcessor githubCommitProcessor;

    @Inject
    WildFlyBotConfig wildFlyBotConfig;

    void verifyFormat(@PullRequest.Edited @PullRequest.Opened @PullRequest.Synchronize @PullRequest.Reopened
                      @PullRequest.ReadyForReview GHEventPayload.PullRequest pullRequestPayload,
                      @ConfigFile(RuntimeConstants.CONFIG_FILE_NAME) WildFlyConfigFile wildflyConfigFile) throws IOException {
        if (wildflyConfigFile == null) {
            LOG.error("No configuration file available. ");
            return;
        }
        List<Check> checks = initializeChecks(wildflyConfigFile);

        GHPullRequest pullRequest = pullRequestPayload.getPullRequest();
        Map<String, String> errors = new HashMap<>();

        for (Check check : checks) {
            String result = check.check(pullRequest);
            if (result != null) {
                errors.put(check.getName(), result);
            }
        }

        if (errors.isEmpty()) {
            githubCommitProcessor.commitStatusSuccess(pullRequest, CHECK_NAME, "Valid");
            deleteFormatComment(pullRequest);
        } else {
            githubCommitProcessor.commitStatusError(pullRequest, CHECK_NAME, "Failed checks: " + String.join(", ", errors.keySet()));
            formatComment(pullRequest, errors.values());
        }

    }

    void postDependabotInfo(@PullRequest.Opened GHEventPayload.PullRequest pullRequestPayload,
                            @ConfigFile(RuntimeConstants.CONFIG_FILE_NAME) WildFlyConfigFile wildflyConfigFile) throws IOException {
        GHPullRequest pullRequest = pullRequestPayload.getPullRequest();
        if (pullRequest.getUser().getLogin().equals(DEPENDABOT)) {
            LOG.infof("Dependabot Pull Request [#%d] detected.", pullRequest.getNumber());
            String comment = ("WildFly Bot recognized this PR as dependabot dependency update. Please create a %s issue" +
                " and add its ID to the title and its link to the description.").formatted(wildflyConfigFile.wildfly.projectKey);
            if (wildFlyBotConfig.isDryRun()) {
                LOG.infof("Pull request #%d - Add new comment %s", pullRequest.getNumber(), comment);
            } else {
                pullRequest.comment(comment);
            }
        }

    }

    private void deleteFormatComment(GHPullRequest pullRequest) throws IOException {
        formatComment(pullRequest, null);
    }

    private void formatComment(GHPullRequest pullRequest, Collection<String> errors) throws IOException {
        boolean update = false;
        for (GHIssueComment comment : pullRequest.listComments()) {
            if (comment.getUser().getLogin().equals(wildFlyBotConfig.githubName())
                && comment.getBody().startsWith("Failed format check")) {
                if (errors == null) {
                    if (wildFlyBotConfig.isDryRun()) {
                        LOG.infof("Pull request #%d - Delete comment %s", pullRequest.getNumber(), comment);
                    } else {
                        comment.delete();
                    }
                    update = true;
                    break;
                }

                String updatedBody = FAILED_FORMAT_COMMENT.formatted(errors.stream()
                    .map("- %s"::formatted)
                    .collect(Collectors.joining("\n\n")));

                if (wildFlyBotConfig.isDryRun()) {
                    LOG.infof("Pull request #%d - Update comment \"%s\" to \"%s\"", pullRequest.getNumber(),
                        comment.getBody(), updatedBody);
                } else {
                    comment.update(updatedBody);
                }
                update = true;
                break;
            }
        }

        if (!update && errors != null) {
            String updatedBody = FAILED_FORMAT_COMMENT.formatted(errors.stream()
                .map("- %s"::formatted)
                .collect(Collectors.joining("\n\n")));
            if (wildFlyBotConfig.isDryRun()) {
                LOG.infof("Pull request #%d - Add new comment %s", pullRequest.getNumber(), updatedBody);
            } else {
                pullRequest.comment(updatedBody);
            }
        }
    }

    private List<Check> initializeChecks(WildFlyConfigFile wildflyConfigFile) {
        List<Check> checks = new ArrayList<>();

        if (wildflyConfigFile.wildfly.format == null) {
            return checks;
        }

        if (wildflyConfigFile.wildfly.format.title.enabled) {
            checks.add(new TitleCheck(new RegexDefinition(wildflyConfigFile.wildfly.getProjectPatternAllowingPrefix(),
                wildflyConfigFile.wildfly.format.title.message)));
        }

        if (wildflyConfigFile.wildfly.format.commit.enabled) {
            checks.add(new CommitMessagesCheck(new RegexDefinition(wildflyConfigFile.wildfly.getProjectPattern(), wildflyConfigFile.wildfly.format.commit.message)));
        }

        if (wildflyConfigFile.wildfly.format.description != null) {
            checks.add(new DescriptionCheck(wildflyConfigFile.wildfly.format.description));
        }

        return checks;
    }
}
