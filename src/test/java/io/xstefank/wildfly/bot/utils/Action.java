package io.xstefank.wildfly.bot.utils;

public enum Action {

    ASSIGNED("assigned"),
    AUTO_MERGE_DISABLED("auto_merge_disabled"),
    AUTO_MERGE_ENABLED("auto_merge_enabled"),
    CLOSED("closed"),
    CONVERTED_TO_DRAFT("converted_to_draft"),
    EDITED("edited"),
    LABELED("labeled"),
    LOCKED("locked"),
    OPENED("opened"),
    READY_FOR_REVIEW("ready_for_review"),
    REOPENED("reopened"),
    REVIEW_REQUESTED("review_requested"),
    REVIEW_REQUEST_REMOVED("review_request_removed"),
    SYNCHRONIZE("synchronize"),
    UNASSIGNED("unassigned"),
    UNLABELED("unlabeled"),
    UNLOCKED("unlocked");

    private String value;

    Action(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
