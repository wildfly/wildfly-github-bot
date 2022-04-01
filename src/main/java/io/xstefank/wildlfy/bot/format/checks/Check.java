package io.xstefank.wildlfy.bot.format.checks;

import org.kohsuke.github.GHPullRequest;

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
     * is limited by GitHub status to 140 characters)
     */
    String check(GHPullRequest pullRequest);


    String getName();
}
