package io.xstefank.wildfly.bot.format;

import io.xstefank.wildfly.bot.model.RegexDefinition;
import org.kohsuke.github.GHPullRequest;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TitleCheck implements Check {

    private Pattern pattern;
    private String message;

    public TitleCheck(RegexDefinition title) {
        if (title.pattern == null) {
            throw new IllegalArgumentException("Input argument cannot be null");
        }
        pattern = title.pattern;
        message = title.message;
    }

    @Override
    public String check(GHPullRequest pullRequest) {
        Matcher matcher = pattern.matcher(pullRequest.getTitle());
        if (!matcher.matches()) {
            return message;
        }

        return null;
    }

    @Override
    public String getName() {
        return "title";
    }
}
