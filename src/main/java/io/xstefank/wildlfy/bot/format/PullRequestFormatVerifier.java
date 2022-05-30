package io.xstefank.wildlfy.bot.format;

import io.quarkiverse.githubapp.ConfigFile;
import io.quarkiverse.githubapp.event.PullRequest;
import io.xstefank.wildlfy.bot.config.WildFlyConfigFile;
import io.xstefank.wildlfy.bot.format.checks.Check;
import io.xstefank.wildlfy.bot.format.checks.CommitsQuantityCheck;
import io.xstefank.wildlfy.bot.format.checks.DescriptionCheck;
import io.xstefank.wildlfy.bot.format.checks.TitleCheck;
import org.jboss.logging.Logger;
import org.kohsuke.github.GHCommitState;
import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.GHPullRequest;

import javax.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class PullRequestFormatVerifier {

    private static final Logger LOG = Logger.getLogger(PullRequestFormatVerifier.class);

    private boolean initialized = false;
    private final List<Check> checks = new ArrayList<>();

    void onPullRequestEdited(@PullRequest.Edited GHEventPayload.PullRequest pullRequestPayload,
                             @ConfigFile("wildfly-bot.yml") WildFlyConfigFile wildflyConfigFile) throws IOException {

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
                updateFormatCommitStatus(pullRequest, GHCommitState.ERROR, "\u274C " + check.getName() + ": " + result);
                return;
            }
        }

        updateFormatCommitStatus(pullRequest, GHCommitState.SUCCESS, "\u2705 Correct");
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

        initialized = true;
    }

    private void updateFormatCommitStatus(GHPullRequest pullRequest, GHCommitState commitState, String description) throws IOException {
        String sha = pullRequest.getHead().getSha();

        pullRequest.getRepository().createCommitStatus(sha, commitState, "", description, "Format");
    }
}
