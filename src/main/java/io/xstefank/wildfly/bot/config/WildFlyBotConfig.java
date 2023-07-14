package io.xstefank.wildfly.bot.config;

import io.smallrye.config.ConfigMapping;

import java.util.Optional;

@ConfigMapping(prefix = "wildfly-bot")
public interface WildFlyBotConfig {

    Optional<Boolean> dryRun();

    default boolean isDryRun() {
        return dryRun().orElse(false);
    }
}
