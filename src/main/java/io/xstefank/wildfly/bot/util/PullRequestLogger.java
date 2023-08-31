package io.xstefank.wildfly.bot.util;

import org.jboss.logging.Logger;
import org.kohsuke.github.GHPullRequest;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class PullRequestLogger {
    private final Set<FormatFlag> formatFlags;
    private final Logger delegate;
    private GHPullRequest pullRequest;

    /**
     * Creates custom wrapper for logger, prepending pull request info before message.
     * By default only `Pull Request [#%d] - ` is prepended with number of the Pull Request.
     * To change prepended logged info see {@link PullRequestLogger#flags(EnumSet)}
     *
     * @param delegate Logger to delegate the messages to
     */
    public PullRequestLogger(Logger delegate) {
        this(delegate, EnumSet.of(FormatFlag.NUMBER));
    }

    // We want to enforce usage of EnumSet instead of Set, which might internally use long for bit operations
    public PullRequestLogger(Logger delegate, EnumSet<FormatFlag> formatFlags) {
        this.delegate = delegate;
        this.formatFlags = formatFlags;
    }

    /**
     * @param formatFlags new format flags for prepended logged info
     * @return new instance for Logger with changed format flags
     */
    // We want to enforce usage of EnumSet instead of Set, which might internally use long for bit operations
    public PullRequestLogger flags(EnumSet<FormatFlag> formatFlags) {
        PullRequestLogger logger = new PullRequestLogger(delegate, formatFlags);
        logger.setPullRequest(this.pullRequest);
        return logger;
    }

    public void setPullRequest(GHPullRequest pullRequest) {
        this.pullRequest = pullRequest;
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

    /**
     * generates prepended string to every log message based on used flags
     *
     * @param message logged message
     * @return prepended info concatenated with logged message
     */
    private String prependPullRequest(String message) {
        if (this.pullRequest == null) {
            return message;
        }
        return String.join(" ", Stream.<Supplier<String>> of(
                () -> "Pull Request",
                () -> formatFlags.contains(FormatFlag.NUMBER) ? "[#%d]".formatted(this.pullRequest.getNumber()) : null,
                () -> formatFlags.contains(FormatFlag.ID) ? "[id - #%d]".formatted(this.pullRequest.getId()) : null,
                () -> formatFlags.contains(FormatFlag.TITLE) ? "[title - \"%s\"]".formatted(this.pullRequest.getTitle()) : null,
                () -> "- %s".formatted(message)).map(Supplier::get).filter(Objects::nonNull).toList());
    }

    public enum FormatFlag {
        NUMBER,
        TITLE,
        ID
    }
}
