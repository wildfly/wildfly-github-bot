package io.xstefank.wildfly.bot.config.util;

import jakarta.enterprise.context.ApplicationScoped;
import org.kohsuke.github.GHCommitState;
import org.kohsuke.github.GHPullRequest;

import java.io.IOException;

@ApplicationScoped
public class GithubCommitProcessor {

    public void updateFormatCommitStatus(GHPullRequest pullRequest, GHCommitState commitState, String check_name, String description) throws IOException {
        String sha = pullRequest.getHead().getSha();

        pullRequest.getRepository().createCommitStatus(sha, commitState, "", description, check_name);
    }
}
