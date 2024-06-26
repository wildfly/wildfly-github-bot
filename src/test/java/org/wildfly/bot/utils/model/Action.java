package org.wildfly.bot.utils.model;

public enum Action {

    ASSIGNED("assigned"),
    AUTO_MERGE_DISABLED("auto_merge_disabled", true),
    AUTO_MERGE_ENABLED("auto_merge_enabled", true),
    CLOSED("closed"),
    CONVERTED_TO_DRAFT("converted_to_draft", true),
    EDITED("edited"),
    LABELED("labeled"),
    LOCKED("locked", true),
    OPENED("opened"),
    READY_FOR_REVIEW("ready_for_review", true),
    REOPENED("reopened"),
    REVIEW_REQUESTED("review_requested"),
    REVIEW_REQUEST_REMOVED("review_request_removed"),
    SYNCHRONIZE("synchronize"),
    UNASSIGNED("unassigned"),
    UNLABELED("unlabeled"),
    UNLOCKED("unlocked");

    private String value;
    private boolean webhookOnly = false;

    private Action(String value) {
        this.value = value;
    }

    private Action(String value, boolean webhookOnly) {
        this.value = value;
        this.webhookOnly = webhookOnly;
    }

    public String getValue() {
        return value;
    }

    public boolean isWebhookOnly() {
        return webhookOnly;
    }
}
