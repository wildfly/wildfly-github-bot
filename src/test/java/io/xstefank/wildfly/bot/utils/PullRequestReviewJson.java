package io.xstefank.wildfly.bot.utils;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;

public class PullRequestReviewJson extends PullRequestJson {

    private static final String REVIEW = "review";

    private static final String STATE = "state";

    protected <T extends PullRequestReviewJson> PullRequestReviewJson(Builder<T> builder) {
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
    public static <T extends PullRequestJson.Builder<? extends PullRequestJson>> T builder(String fileName) throws IOException {
        return (T) new Builder<>(fileName);
    }

    public static class Builder<T extends PullRequestReviewJson> extends PullRequestJson.Builder<T> {

        public enum ReviewState {
            COMMENT("commented"),
            APPROVE("approved"),
            CHANGES_REQUESTED("changes_requested");

            private final String state;

            private ReviewState(String state) {
                this.state = state;
            }

            @Override
            public String toString() {
                return this.state;
            }
        }

        /**
         * Constructs a new builder instance.
         *
         * @param fileName name of the JSON file in src/test/resources.
         * @throws IOException if an error occurred during the JSON file obtaining.
         */
        protected Builder(String fileName) throws IOException {
            super(fileName);
        }

        public Builder<T> state(ReviewState state) {
            ((ObjectNode) this.jsonFile.get(REVIEW)).put(STATE, state.toString());
            return this;
        }

        @Override
        @SuppressWarnings("unchecked")
        public T build() {
            return (T) new PullRequestReviewJson(this);
        }
    }
}
