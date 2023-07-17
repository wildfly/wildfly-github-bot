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
public class GitHubJson {

    private static String PULL_REQUEST = "pull_request";
    private static String COMMITS = "commits";
    private static String BODY = "body";
    private static String TITLE = "title";
    private static String SHA = "sha";
    private static String HEAD = "head";
    private static String ACTION = "action";

    private static JsonNode file;
    private static Builder builder;

    private GitHubJson(Builder jsonHandlerBuilder) {
        builder = jsonHandlerBuilder;
        file = jsonHandlerBuilder.jsonFile;
    }

    /**
     * Constructs a new builder instance.
     *
     * @param fileName name of the JSON file in src/test/resources.
     * @return the new builder instance.
     * @throws IOException if an error occurred during the JSON file obtaining.
     */
    public static Builder builder(String fileName) throws IOException {
        return new Builder(fileName);
    }

    public String commitSHA() {
        return file.get(PULL_REQUEST).get(HEAD).get(SHA).textValue();
    }

    public String jsonString() {
        return file.toPrettyString();
    }

    public static final class Builder {

        private final JsonNode jsonFile;
        private final ObjectMapper objectMapper = new ObjectMapper();

        private Builder(String fileName) throws IOException {
            File jsonFile = new File("src/test/resources/" + fileName);
            if (!jsonFile.exists()) {
                throw new FileNotFoundException("File " + fileName + " cannot be found or does not exist.");
            }
            this.jsonFile = objectMapper.readTree(jsonFile);
        }

        public Builder action(Action action) {
            ((ObjectNode) this.jsonFile).put(ACTION, action.getValue());
            return this;
        }

        public Builder title(String title) {
            ((ObjectNode) this.jsonFile.get(PULL_REQUEST)).put(TITLE, title);
            return this;
        }

        public Builder body(String body) {
            ((ObjectNode) this.jsonFile.get(PULL_REQUEST)).put(BODY, body);
            return this;
        }

        public Builder commitQuantity(int quantity) {
            ((ObjectNode) this.jsonFile.get(PULL_REQUEST)).put(COMMITS, quantity);
            return this;
        }
        public GitHubJson build() {
            return new GitHubJson(this);
        }

    }
}
