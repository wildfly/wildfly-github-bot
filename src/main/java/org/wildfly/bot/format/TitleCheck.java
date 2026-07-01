package org.wildfly.bot.format;

import org.wildfly.bot.model.RegexDefinition;
import org.wildfly.bot.util.Patterns;
import org.kohsuke.github.GHPullRequest;

import java.util.regex.Pattern;

public class TitleCheck implements Check {

    private final Pattern pattern;
    private final String failMessage;

    public TitleCheck(RegexDefinition title) {
        if (title.pattern == null) {
            throw new IllegalArgumentException("Input argument cannot be null");
        }
        pattern = title.pattern;
        failMessage = title.failMessage;
    }

    @Override
    public String check(GHPullRequest pullRequest) {
        if (!Patterns.matches(pattern, pullRequest.getTitle())) {
            return failMessage.formatted(pattern.pattern());
        }

        return null;
    }

    @Override
    public String getName() {
        return "title";
    }
}
