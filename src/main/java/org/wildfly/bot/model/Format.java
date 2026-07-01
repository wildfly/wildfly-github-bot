package org.wildfly.bot.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class Format {

    public boolean enabled = true;

    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    @JsonSetter(nulls = Nulls.SKIP)
    public List<String> skip = new ArrayList<>();

    @JsonIgnore
    private List<Pattern> compiledSkipPatterns;

    @JsonSetter(nulls = Nulls.SKIP)
    public TitlePattern title = new TitlePattern();

    @JsonSetter(nulls = Nulls.SKIP)
    public Description description;

    @JsonSetter(nulls = Nulls.SKIP)
    public CommitPattern commit = new CommitPattern();

    private List<Pattern> getCompiledSkipPatterns() {
        // lazy
        if (compiledSkipPatterns == null) {
            compiledSkipPatterns = skip.stream()
                    .map(p -> Pattern.compile(p, Pattern.CASE_INSENSITIVE))
                    .toList();
        }
        return compiledSkipPatterns;
    }

    public boolean matchesSkipPattern(String text) {
        if (text == null || skip.isEmpty()) {
            return false;
        }
        return getCompiledSkipPatterns().stream()
                .anyMatch(pattern -> pattern.matcher(text).find());
    }

    public abstract static class RegexPattern {
        public String failMessage;
        public boolean enabled = true;

        public RegexPattern(String failMessage) {
            this.failMessage = failMessage;
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
