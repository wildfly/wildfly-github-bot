package org.wildfly.bot.format;

import org.kohsuke.github.GHPullRequest;

import java.io.IOException;

/**
 * Validates pull request payload with a specified check
 */
public interface Check {

    /**
     * Performs any custom validation of the pull request. This method
     * is invoked when the PR is updated (GitHub PR events)
     *
     * @param pullRequest pull request JSON as received from GitHub
     * @return null if check passed, error message otherwise
     */
    String check(GHPullRequest pullRequest) throws IOException;

    String getName();
}
