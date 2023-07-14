package io.xstefank.wildfly.bot;

import io.quarkiverse.githubapp.ConfigFile;
import io.quarkiverse.githubapp.event.PullRequest;
import io.xstefank.wildfly.bot.format.Check;
import io.xstefank.wildfly.bot.format.CommitMessagesCheck;
import io.xstefank.wildfly.bot.format.CommitsQuantityCheck;
import io.xstefank.wildfly.bot.format.DescriptionCheck;
import io.xstefank.wildfly.bot.format.TitleCheck;
import io.xstefank.wildfly.bot.model.RuntimeConstants;
import io.xstefank.wildfly.bot.model.WildFlyConfigFile;
import io.xstefank.wildfly.bot.util.GithubCommitProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.GHPullRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class PullRequestFormatProcessor {

    private static final Logger LOG = Logger.getLogger(PullRequestFormatProcessor.class);

    private static final String CHECK_NAME = "Format";

    private boolean initialized = false;
    private final List<Check> checks = new ArrayList<>();

    @Inject
    GithubCommitProcessor githubCommitProcessor;

    void onPullRequestEdited(@PullRequest.Edited  @PullRequest.Opened @PullRequest.Synchronize GHEventPayload.PullRequest pullRequestPayload,
                             @ConfigFile(RuntimeConstants.CONFIG_FILE_NAME) WildFlyConfigFile wildflyConfigFile) throws IOException {

        if (wildflyConfigFile == null) {
            LOG.error("No configuration file available. ");
            return;
        } else if (!initialized) {
            initialize(wildflyConfigFile);
        }

        GHPullRequest pullRequest = pullRequestPayload.getPullRequest();

        for (Check check : checks) {
            String result = check.check(pullRequest);
            if (result != null) {
                githubCommitProcessor.commitStatusError(pullRequest, CHECK_NAME, check.getName() + ": " + result);
                return;
            }
        }

        githubCommitProcessor.commitStatusSuccess(pullRequest, CHECK_NAME, "Valid");
    }

    private void initialize(WildFlyConfigFile wildflyConfigFile) {
        if (wildflyConfigFile.wildfly.format == null) {
            return;
        }

        if (wildflyConfigFile.wildfly.format.titleCheck != null) {
            checks.add(new TitleCheck(wildflyConfigFile.wildfly.format.titleCheck));
        }

        if (wildflyConfigFile.wildfly.format.description != null) {
            checks.add(new DescriptionCheck(wildflyConfigFile.wildfly.format.description));
        }


        if (wildflyConfigFile.wildfly.format.commitsQuantity != null) {
            checks.add(new CommitsQuantityCheck(wildflyConfigFile.wildfly.format.commitsQuantity));
        }

        if (wildflyConfigFile.wildfly.format.commitsMessage != null) {
            checks.add(new CommitMessagesCheck(wildflyConfigFile.wildfly.format.commitsMessage));
        }

        initialized = true;
    }
}
