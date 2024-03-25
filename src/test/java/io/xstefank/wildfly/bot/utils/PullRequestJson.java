package io.xstefank.wildfly.bot.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Class responsible for handling and changing properties of the GitHub JSON template file.
 */
public class PullRequestJson {

    protected static final String ACTION = "action";
    protected static final String BODY = "body";
    private static final String HEAD = "head";
    protected static final String ID = "id";
    private static final String PULL_REQUEST = "pull_request";
    private static final String SHA = "sha";
    private static final String TITLE = "title";
    protected static final String USER = "user";
    private static final String LOGIN = "login";
    private static final String NUMBER = "number";

    private final JsonNode file;

    protected <T extends PullRequestJson> PullRequestJson(Builder<T> jsonHandlerBuilder) {
        file = jsonHandlerBuilder.jsonFile;
    }

    /**
     * Constructs a new builder instance.
     *
     * @param fileName name of the JSON file in src/test/resources.
     * @return the new builder instance.
     * @throws IOException if an error occurred during the JSON file obtaining.
     */
    @SuppressWarnings("unchecked")
    public static <T extends Builder<? extends PullRequestJson>> T builder(String fileName) throws IOException {
        return (T) new Builder<>(fileName);
    }

    public String commitSHA() {
        return file.get(PULL_REQUEST).get(HEAD).get(SHA).textValue();
    }

    public String jsonString() {
        return file.toPrettyString();
    }

    public long id() {
        return file.get(PULL_REQUEST).get(ID).longValue();
    }

    public long number() {
        return file.get(NUMBER).longValue();
    }

    public String status() {
        return file.get(ACTION).textValue();
    }

    public static class Builder<T extends PullRequestJson> {

        protected final JsonNode jsonFile;
        private static final ObjectMapper objectMapper = new ObjectMapper();

        /**
         * Constructs a new builder instance.
         *
         * @param fileName name of the JSON file in src/test/resources.
         * @throws IOException if an error occurred during the JSON file obtaining.
         */
        protected Builder(String fileName) throws IOException {
            File jsonFile = new File("src/test/resources/" + fileName);
            if (!jsonFile.exists()) {
                throw new FileNotFoundException("File " + fileName + " cannot be found or does not exist.");
            }
            this.jsonFile = objectMapper.readTree(jsonFile);
        }

        public Builder<T> action(Action action) {
            ((ObjectNode) this.jsonFile).put(ACTION, action.getValue());
            return this;
        }

        public Builder<T> title(String title) {
            ((ObjectNode) this.jsonFile.get(PULL_REQUEST)).put(TITLE, title);
            return this;
        }

        public Builder<T> description(String description) {
            ((ObjectNode) this.jsonFile.get(PULL_REQUEST)).put(BODY, description);
            return this;
        }

        public Builder<T> userLogin(String login) {
            ((ObjectNode) this.jsonFile.get(PULL_REQUEST).get(USER)).put(LOGIN, login);
            return this;
        }

        @SuppressWarnings("unchecked")
        public T build() {
            return (T) new PullRequestJson(this);
        }

    }
}
