package io.xstefank.wildfly.bot.model;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

public final class Format {

    public boolean enabled = true;

    @JsonSetter(nulls = Nulls.SKIP)
    public TitlePattern title = new TitlePattern();

    @JsonSetter(nulls = Nulls.SKIP)
    public Description description;

    @JsonSetter(nulls = Nulls.SKIP)
    public CommitPattern commit = new CommitPattern();

    public abstract static class RegexPattern {
        public String message;
        public boolean enabled = true;

        public RegexPattern(String message) {
            this.message = message;
        }
    }

    public static class TitlePattern extends RegexPattern {
        public TitlePattern() {
            super(RuntimeConstants.DEFAULT_TITLE_MESSAGE);
        }
    }

    public static class CommitPattern extends RegexPattern {
        public CommitPattern() {
            super(RuntimeConstants.DEFAULT_COMMIT_MESSAGE);
        }
    }
}
