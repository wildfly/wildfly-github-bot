package io.xstefank.wildfly.bot.format;

import org.kohsuke.github.GHPullRequest;

import java.io.IOException;

/**
 * Validates pull request payload with a specified check
 */
public interface Check {

    /**
     * TODO
     * Performs any custom validation of the pull request. This method
     * is invoked when the PR is updated (GitHub PR events)
     *
     * @param payload pull request JSON as received from GitHub
     * @return null if check passed, error message otherwise (error message
     *         is limited by GitHub status to 140 characters)
     */
    String check(GHPullRequest pullRequest) throws IOException;

    String getName();
}
