package io.xstefank.wildfly.bot.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

import java.util.Optional;

@ConfigMapping(prefix = "wildfly-bot")
public interface WildFlyBotConfig {

    @WithDefault("wildfly-bot[bot]")
    String githubName();

    Optional<Boolean> dryRun();

    default boolean isDryRun() {
        return dryRun().orElse(false);
    }

    @WithName("mergable-status-update.timeout")
    @WithDefault("30")
    int timeout();
}
