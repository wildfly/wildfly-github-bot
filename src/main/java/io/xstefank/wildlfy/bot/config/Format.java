package io.xstefank.wildlfy.bot.config;

import com.fasterxml.jackson.annotation.JsonProperty;

public final class Format {

    @JsonProperty("title-check")
    public RegexDefinition titleCheck;

    public RegexDefinition description;

    @JsonProperty("commits-quantity")
    public CommitsQuantity commitsQuantity;
}
