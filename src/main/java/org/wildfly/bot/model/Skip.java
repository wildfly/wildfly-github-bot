package org.wildfly.bot.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public final class Skip {

    @JsonSetter(nulls = Nulls.SKIP)
    public SkipRule format = new SkipRule();

    @JsonSetter(nulls = Nulls.SKIP)
    public SkipRule rules = new SkipRule();

    public static final class SkipRule {

        @JsonSetter(nulls = Nulls.SKIP)
        public PatternList title = new PatternList();

        @JsonSetter(nulls = Nulls.SKIP)
        public PatternList body = new PatternList();

        public boolean matches(String prTitle, String prBody) {
            return title.matches(prTitle) || body.matches(prBody);
        }
    }

    public static final class PatternList {

        private final List<Pattern> patterns;

        public PatternList() {
            this.patterns = Collections.emptyList();
        }

        @JsonCreator
        public PatternList(
                @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY) List<String> regexes) {
            if (regexes == null || regexes.isEmpty()) {
                this.patterns = Collections.emptyList();
            } else {
                this.patterns = regexes.stream()
                        .map(r -> Pattern.compile(r, Pattern.CASE_INSENSITIVE))
                        .toList();
            }
        }

        public boolean matches(String text) {
            if (text == null || patterns.isEmpty()) {
                return false;
            }
            return patterns.stream().anyMatch(p -> p.matcher(text).find());
        }
    }
}
