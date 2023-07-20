package io.xstefank.wildfly.bot.utils;

public enum Action {

    OPENED("opened"),
    EDITED("edited");

    private String value;

    Action(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
