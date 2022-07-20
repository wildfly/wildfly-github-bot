package io.xstefank.wildlfy.bot.config;

import io.smallrye.config.ConfigMapping;

import java.util.Optional;

@ConfigMapping(prefix = "quarkus-github-bot")
public interface QuarkusGitHubBotConfig {
    Optional<Boolean> dryRun();

    public default boolean isDryRun() {
        Optional<Boolean> dryRun = dryRun();
        return dryRun.isPresent() && dryRun.get();
    }
}
