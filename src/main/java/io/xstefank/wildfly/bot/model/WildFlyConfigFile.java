package io.xstefank.wildfly.bot.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.regex.Pattern;

public class WildFlyConfigFile {

    public WildFlyConfig wildfly;

    public static final class WildFlyConfig {

        private Pattern projectPattern = Pattern.compile(String.format("\\[%s-\\d+]\\s+.*|%s-\\d+\\s+.*", RuntimeConstants.DEFAULT_PROJECT_KEY, RuntimeConstants.DEFAULT_PROJECT_KEY));

        public List<WildFlyRule> rules;

        public Format format = new Format();

        public void setProjectKey(String name) {
            projectPattern = Pattern.compile(String.format("\\[%s-\\d+]\\s+.*|%s-\\d+\\s+.*", name, name));
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
