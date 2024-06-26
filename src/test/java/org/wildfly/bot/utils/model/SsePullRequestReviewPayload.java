package org.wildfly.bot.utils.model;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.wildfly.bot.utils.testing.PullRequestJson;
import org.wildfly.bot.utils.testing.PullRequestReviewJsonBuilder;

import java.io.IOException;

public class SsePullRequestReviewPayload extends SsePullRequestPayload implements PullRequestJson {

    private static final String REVIEW = "review";

    private static final String STATE = "state";

    protected <T extends SsePullRequestReviewPayload> SsePullRequestReviewPayload(Builder<T> builder) {
        super(builder);
    }

    /**
     * Constructs a new builder instance.
     *
     * @param fileName name of the JSON file in src/test/resources.
     * @return the new builder instance.
     * @throws IOException if an error occurred during the JSON file obtaining.
     */
    @SuppressWarnings("unchecked")
    public static <T extends SsePullRequestPayload.Builder<? extends SsePullRequestPayload>> T builder(String fileName)
            throws IOException {
        return (T) new Builder<>(fileName);
    }

    public static class Builder<T extends SsePullRequestReviewPayload> extends SsePullRequestPayload.Builder<T>
            implements PullRequestReviewJsonBuilder {
        /**
         * Constructs a new builder instance.
         *
         * @param fileName name of the JSON file in src/test/resources.
         * @throws IOException if an error occurred during the JSON file obtaining.
         */
        protected Builder(String fileName) throws IOException {
            super(fileName);
        }

        @Override
        public Builder<T> state(ReviewState state) {
            ((ObjectNode) this.jsonFile.get(REVIEW)).put(STATE, state.toString());
            return this;
        }

        @Override
        @SuppressWarnings("unchecked")
        public T build() {
            return (T) new SsePullRequestReviewPayload(this);
        }
    }
}
