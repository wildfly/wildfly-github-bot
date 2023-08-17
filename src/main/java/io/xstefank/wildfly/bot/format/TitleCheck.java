package io.xstefank.wildfly.bot.format;

import io.xstefank.wildfly.bot.model.RegexDefinition;
import io.xstefank.wildfly.bot.util.Patterns;
import org.kohsuke.github.GHPullRequest;

import java.util.regex.Pattern;

public class TitleCheck implements Check {

    private final Pattern pattern;
    private final String message;

    public TitleCheck(RegexDefinition title) {
        if (title.pattern == null) {
            throw new IllegalArgumentException("Input argument cannot be null");
        }
        pattern = title.pattern;
        message = title.message;
    }

    @Override
    public String check(GHPullRequest pullRequest) {
        if (!Patterns.matches(pattern, pullRequest.getTitle())) {
            return message.formatted(pattern.pattern());
        }

        return null;
    }

    @Override
    public String getName() {
        return "title";
    }
}
