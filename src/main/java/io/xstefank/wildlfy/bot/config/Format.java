package io.xstefank.wildlfy.bot.config;

import com.fasterxml.jackson.annotation.JsonProperty;

public final class Format {

//    @ConfigProperty(name = "title-check")
    @JsonProperty("title-check")
    public RegexDefinition titleCheck;
}
