package io.xstefank.wildlfy.bot.format.checks;

import io.xstefank.wildlfy.bot.config.RegexDefinition;
import org.kohsuke.github.GHPullRequest;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TitleCheck implements Check {

    static final String DEFAULT_MESSAGE = "Invalid title content";

    private Pattern pattern;
    private String message;

    public TitleCheck(RegexDefinition title) {
        if (title == null || title.pattern == null) {
            throw new IllegalArgumentException("Input argument cannot be null");
        }
        pattern = title.pattern;
        message = (title.message != null) ? title.message : DEFAULT_MESSAGE;

        System.out.println(pattern);
        System.out.println(message);
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
        return "title-check";
    }
}
