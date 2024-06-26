package org.wildfly.bot.utils.model;

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
