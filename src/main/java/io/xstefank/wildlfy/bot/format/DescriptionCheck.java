package io.xstefank.wildlfy.bot.format;

import io.xstefank.wildlfy.bot.config.RegexDefinition;
import org.kohsuke.github.GHPullRequest;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DescriptionCheck implements Check {
    static final String DEFAULT_MESSAGE = "Invalid description content";

    private Pattern pattern;
    private String message;

    public DescriptionCheck(RegexDefinition description) {
        if (description.pattern == null) {
            throw new IllegalArgumentException("Input argument cannot be null");
        }
        pattern = description.pattern;
        message = (description.message != null) ? description.message : DEFAULT_MESSAGE;
    }

    @Override
    public String check(GHPullRequest pullRequest) {
        try {
            Matcher matcher = pattern.matcher(pullRequest.getBody());

            if (!matcher.matches()) {
                return message;
            }
        } catch (NullPointerException e) {
            return message;
        }

        return null;
    }

    @Override
    public String getName() {
        return "description";
    }
}
