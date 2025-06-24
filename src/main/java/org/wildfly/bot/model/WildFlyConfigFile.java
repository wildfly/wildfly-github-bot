package org.wildfly.bot.model;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class WildFlyConfigFile {

    @JsonSetter(nulls = Nulls.SKIP)
    public WildFlyConfig wildfly = new WildFlyConfig();

    public static final class WildFlyConfig {

        private Pattern projectPattern = Pattern.compile(RuntimeConstants.PROJECT_PATTERN_REGEX
                .formatted(RuntimeConstants.DEFAULT_PROJECT_KEY), Pattern.DOTALL);

        @JsonSetter(nulls = Nulls.SKIP)
        public List<WildFlyRule> rules = new ArrayList<>();

        @JsonSetter(nulls = Nulls.SKIP)
        public Format format = new Format();

        public String projectKey = RuntimeConstants.DEFAULT_PROJECT_KEY;

        public void setProjectKey(String key) {
            projectKey = key;
            projectPattern = Pattern.compile(String.format(RuntimeConstants.PROJECT_PATTERN_REGEX, key), Pattern.DOTALL);
        }

        public Pattern getProjectPattern() {
            return projectPattern;
        }

        public List<String> emails;
    }

    public static final class WildFlyRule {

        public String id;

        public String title;

        public String body;

        public String titleBody;

        @JsonSetter(nulls = Nulls.SKIP)
        @JsonDeserialize(as = ArrayList.class)
        public List<String> directories = new ArrayList<>();

        @JsonSetter(nulls = Nulls.SKIP)
        @JsonDeserialize(as = ArrayList.class)
        public List<String> notify = new ArrayList<>();

        @JsonSetter(nulls = Nulls.SKIP)
        @JsonDeserialize(as = ArrayList.class)
        public List<String> labels = new ArrayList<>();

        @Override
        public String toString() {
            return "id=" + stringify(id) + " title=" + stringify(title) + " body=" + stringify(body) + " titleBody="
                    + stringify(titleBody) + " directories=" + directories + " notify=" + notify + " labels=" + labels;
        }

        public String toPrettyString() {
            return String.join(", ", Stream.<Supplier<String>> of(
                    () -> id != null ? "id=" + id : null,
                    () -> title != null ? "title=" + title : null,
                    () -> body != null ? "body=" + body : null,
                    () -> titleBody != null ? "titleBody=" + titleBody : null,
                    () -> !directories.isEmpty() ? "directories=" + directories : null,
                    () -> !notify.isEmpty() ? "notify=" + notify : null,
                    () -> !labels.isEmpty() ? "labels=" + labels : null).map(Supplier::get).filter(Objects::nonNull).toList());

        }

        private String stringify(String value) {
            return value == null ? "null" : value;
        }
    }
}
