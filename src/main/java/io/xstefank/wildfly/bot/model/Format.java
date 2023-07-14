package io.xstefank.wildfly.bot.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public final class Format {

    @JsonProperty("title-check")
    public RegexDefinition titleCheck;

    @JsonProperty("description")
    public Description description;

    @JsonProperty("commits-message")
    public RegexDefinition commitsMessage;

    @JsonProperty("commits-quantity")
    public CommitsQuantity commitsQuantity;
}
