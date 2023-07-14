package io.xstefank.wildfly.bot.format;

import io.xstefank.wildfly.bot.model.Description;
import io.xstefank.wildfly.bot.model.RegexDefinition;
import org.kohsuke.github.GHPullRequest;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DescriptionCheck implements Check {
    static final String DEFAULT_MESSAGE = "Invalid description content";

    private Description description;
    private String message = DEFAULT_MESSAGE;

    public DescriptionCheck(Description description) {
        if (description == null) {
            throw new IllegalArgumentException("At least one regex definition must be provided");
        }
        this.description = description;

        if (description.regexes == null || description.regexes.isEmpty()) {
            throw new IllegalArgumentException("Input argument cannot be null");
        }

        if (description.message != null) {
            message = description.message;
        }

    }


    @Override
    public String check(GHPullRequest pullRequest) {
        try {
            String body = pullRequest.getBody();
            String[] lines = body.split("\\r?\\n");

            for (RegexDefinition regexDefinition : description.regexes) {
                Pattern pattern = regexDefinition.pattern;

                boolean regexMatched = false;
                for (String line : lines) {
                    Matcher matcher = pattern.matcher(line);
                    if (matcher.find()) {
                        regexMatched = true;
                        break;
                    }
                }

                if (!regexMatched) {
                    return regexDefinition.message != null ? regexDefinition.message : message;
                }
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
