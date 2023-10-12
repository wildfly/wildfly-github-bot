package io.xstefank.wildfly.bot.util;

import org.jboss.logging.Logger;
import org.kohsuke.github.GHPullRequest;

public class PullRequestLogger {
    private final Logger delegate;
    private GHPullRequest pullRequest;

    public PullRequestLogger(Logger delegate) {
        this.delegate = delegate;
    }

    public void setPullRequest(GHPullRequest pullRequest) {
        this.pullRequest = pullRequest;
    }

    public boolean isPullRequestSet() {
        return pullRequest == null;
    }

    public void debug(String message) {
        delegate.debug(prependPullRequest(message));
    }

    public void debugf(String format, Object... params) {
        debug(format.formatted(params));
    }

    public void debugf(Throwable t, String format, Object... params) {
        delegate.debug(prependPullRequest(format).formatted(params), t);
    }

    public void info(String message) {
        delegate.info(prependPullRequest(message));
    }

    public void infof(String format, Object... params) {
        info(format.formatted(params));
    }

    public void infof(Throwable t, String format, Object... params) {
        delegate.info(prependPullRequest(format).formatted(params), t);
    }

    public void warn(String message) {
        delegate.warn(prependPullRequest(message));
    }

    public void warnf(String format, Object... params) {
        warn(prependPullRequest(format).formatted(params));
    }

    public void warnf(Throwable t, String format, Object... params) {
        delegate.warn(prependPullRequest(format).formatted(params), t);

    }

    public void error(String message) {
        delegate.error(prependPullRequest(message));
    }

    public void errorf(String format, Object... params) {
        error(prependPullRequest(format).formatted(params));
    }

    public void errorf(Throwable t, String format, Object... params) {
        delegate.error(prependPullRequest(format).formatted(params), t);
    }

    public void fatal(String message) {
        delegate.fatal(prependPullRequest(message));
    }

    public void fatalf(String format, Object... params) {
        fatal(prependPullRequest(format).formatted(params));
    }

    public void fatalf(Throwable t, String format, Object... params) {
        delegate.fatal(prependPullRequest(format).formatted(params), t);
    }

    public void log(Logger.Level level, String message) {
        delegate.log(level, prependPullRequest(message));
    }

    public void logf(Logger.Level level, String format, Object... params) {
        log(level, prependPullRequest(format).formatted(params));
    }

    public void logf(Logger.Level level, Throwable t, String format, Object... params) {
        delegate.log(level, prependPullRequest(format).formatted(params), t);
    }

    private String prependPullRequest(String message) {
        return this.pullRequest == null ? message
                : "Pull Request [#%d] - %s".formatted(this.pullRequest.getNumber(), message);
    }
}