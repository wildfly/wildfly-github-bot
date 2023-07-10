package io.xstefank.wildfly.bot.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.ArrayList;
import java.util.List;

public class WildFlyConfigFile {

    public WildFlyConfig wildfly;

    public static final class WildFlyConfig {

        public List<WildFlyRule> rules;

        public Format format;

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
            return "id=" + stringify(id) + " title=" + stringify(title) + " body=" + stringify(body) + " titleBody=" + stringify(titleBody) + " directories=" + directories + " notify=" + notify;
        }

        private String stringify(String value) {
            return value == null ? "null" : value;
        }
    }
}
