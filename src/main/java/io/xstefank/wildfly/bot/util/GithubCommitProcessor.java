package io.xstefank.wildfly.bot.util;

import io.xstefank.wildfly.bot.config.WildFlyBotConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.kohsuke.github.GHCommitState;
import org.kohsuke.github.GHPullRequest;

import java.io.IOException;

@ApplicationScoped
public class GithubCommitProcessor {

    private static final Logger LOG = Logger.getLogger(GithubCommitProcessor.class);

    @Inject
    WildFlyBotConfig wildFlyBotConfig;

    public void commitStatusSuccess(GHPullRequest pullRequest, String checkName, String description) throws IOException {
        String sha = pullRequest.getHead().getSha();

        if (wildFlyBotConfig.isDryRun()) {
            LOG.infof("Pull request #%d - Commit status success {%s, %s, %s}", pullRequest.getNumber(), sha, checkName, description);
        } else {
            pullRequest.getRepository().createCommitStatus(sha, GHCommitState.SUCCESS, "", description, checkName);
        }
    }

    public void commitStatusError(GHPullRequest pullRequest, String checkName, String description) throws IOException {
        String sha = pullRequest.getHead().getSha();

        if (wildFlyBotConfig.isDryRun()) {
            LOG.infof("Pull request #%d - Commit status failure {%s, %s, %s}", pullRequest.getNumber(), sha, checkName, description);
        } else {
            pullRequest.getRepository().createCommitStatus(sha, GHCommitState.ERROR, "", description, checkName);
        }
    }
}
