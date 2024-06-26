package org.wildfly.bot.utils.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.wildfly.bot.utils.PullRequestJsonBuildable;
import org.wildfly.bot.utils.testing.PullRequestJson;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Class responsible for handling and changing properties of the GitHub JSON template file.
 */
public class SsePullRequestPayload implements PullRequestJson {
    private final JsonNode file;

    protected <T extends SsePullRequestPayload> SsePullRequestPayload(Builder<T> jsonHandlerBuilder) {
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
    public static <T extends Builder<? extends SsePullRequestPayload>> T builder(String fileName) throws IOException {
        return (T) new Builder<>(fileName);
    }

    @Override
    public JsonNode payload() {
        return file;
    }

    public static class Builder<T extends SsePullRequestPayload> implements PullRequestJsonBuildable {

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

        @Override
        public Builder<T> action(Action action) {
            ((ObjectNode) this.jsonFile).put(ACTION, action.getValue());
            return this;
        }

        @Override
        public Builder<T> title(String title) {
            ((ObjectNode) this.jsonFile.get(PULL_REQUEST)).put(TITLE, title);
            return this;
        }

        @Override
        public Builder<T> description(String description) {
            ((ObjectNode) this.jsonFile.get(PULL_REQUEST)).put(BODY, description);
            return this;
        }

        @Override
        public Builder<T> userLogin(String login) {
            ((ObjectNode) this.jsonFile.get(PULL_REQUEST).get(USER)).put(LOGIN, login);
            return this;
        }

        @SuppressWarnings("unchecked")
        @Override
        public T build() {
            return (T) new SsePullRequestPayload(this);
        }

    }
}
