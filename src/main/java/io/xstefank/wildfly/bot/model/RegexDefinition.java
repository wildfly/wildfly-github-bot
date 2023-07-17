package io.xstefank.wildfly.bot.model;

import java.util.regex.Pattern;

public class RegexDefinition {

    public Pattern pattern;
    public String message;

    public RegexDefinition() {}

    public RegexDefinition (Pattern pattern, String message) {
        this.pattern = pattern;
        this.message = message;
    }
}
