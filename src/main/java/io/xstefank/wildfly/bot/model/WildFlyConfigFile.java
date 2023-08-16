package io.xstefank.wildfly.bot.model;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.regex.Pattern;

import static io.xstefank.wildfly.bot.model.RuntimeConstants.DEFAULT_PROJECT_KEY;

public class WildFlyConfigFile {

    public static final String PROJECT_PATTERN_REGEX = "\\[%s-\\d+]\\s+.*|%s-\\d+\\s+.*";
    public static final String PROJECT_PATTERN_REGEX_PREFIXED = ".*\\[%s-\\d+]\\s+.*|.*%s-\\d+\\s+.*";

    @JsonSetter(nulls = Nulls.SKIP)
    public WildFlyConfig wildfly = new WildFlyConfig();

    public static final class WildFlyConfig {

        private Pattern projectPattern = Pattern.compile(String.format(PROJECT_PATTERN_REGEX,
            DEFAULT_PROJECT_KEY, DEFAULT_PROJECT_KEY), Pattern.DOTALL);

        private Pattern projectPatternPrefixed = Pattern.compile(String.format(PROJECT_PATTERN_REGEX_PREFIXED,
            DEFAULT_PROJECT_KEY, DEFAULT_PROJECT_KEY), Pattern.DOTALL);

        @JsonSetter(nulls = Nulls.SKIP)
        public List<WildFlyRule> rules = new ArrayList<>();

        @JsonSetter(nulls = Nulls.SKIP)
        public Format format = new Format();

        public String projectKey = DEFAULT_PROJECT_KEY;

        public void setProjectKey(String key) {
            projectKey = key;
            projectPattern = Pattern.compile(String.format(PROJECT_PATTERN_REGEX, key, key), Pattern.DOTALL);
            projectPatternPrefixed = Pattern.compile(String.format(PROJECT_PATTERN_REGEX_PREFIXED, key, key), Pattern.DOTALL);
        }

        public Pattern getProjectPattern() {
            return projectPattern;
        }

        public Pattern getProjectPatternAllowingPrefix() {
            return projectPatternPrefixed;
        }

        public List<String> emails;
    }

    public static final class WildFlyRule {

        public String id;

        public String title;

        public String body;

        public String titleBody;

        @JsonDeserialize(as = ArrayList.class)
        public List<String> directories = new ArrayList<>();

        @JsonDeserialize(as = ArrayList.class)
        public List<String> notify = new ArrayList<>();

        @Override
        public String toString() {
            return "id=" + stringify(id) + " title=" + stringify(title) + " body=" + stringify(body) + " titleBody="
                + stringify(titleBody) + " directories=" + directories + " notify=" + notify;
        }

        public String toPrettyString() {
            return String.join(", ", Stream.<Supplier<String>>of(
                () -> id != null ? "id=" + id : null,
                () -> title != null ? "title=" + title : null,
                () -> body != null ? "body=" + body : null,
                () -> titleBody != null ? "titleBody=" + titleBody : null,
                () -> !directories.isEmpty() ? "directories=" + directories : null,
                () -> !notify.isEmpty() ? "notify=" + notify : null
            ).map(Supplier::get).filter(Objects::nonNull).toList());

        }

        private String stringify(String value) {
            return value == null ? "null" : value;
        }
    }
}
