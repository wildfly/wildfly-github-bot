package io.xstefank.wildfly.bot.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

public final class Format {

    public RegexPattern title = new RegexPattern(RuntimeConstants.DEFAULT_TITLE_MESSAGE);

    public Description description;

    public RegexPattern commit = new RegexPattern(RuntimeConstants.DEFAULT_COMMIT_MESSAGE);

    @JsonProperty("commits-quantity")
    public CommitsQuantity commitsQuantity;

    public static class RegexPattern {
        @JsonSetter(nulls = Nulls.SKIP)
        public String message;
        public boolean enabled = true;

        public RegexPattern() {}

        public RegexPattern(String message) {
            this.message = message;
        }
    }
}
