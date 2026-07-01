package org.wildfly.bot.model;

import java.util.regex.Pattern;

public class RegexDefinition {

    public Pattern pattern;
    public String failMessage;

    public RegexDefinition() {
    }

    public RegexDefinition(Pattern pattern, String failMessage) {
        this.pattern = pattern;
        this.failMessage = failMessage;
    }
}
