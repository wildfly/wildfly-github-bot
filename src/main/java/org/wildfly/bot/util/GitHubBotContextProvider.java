package org.wildfly.bot.util; // Or a suitable package like org.wildfly.bot.config or org.wildfly.bot.github

import io.quarkiverse.githubapp.GitHubClientProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.io.IOException;

/**
 * Provides the dynamically fetched name of the GitHub Bot.
 * The name is fetched once during application startup.
 */
@ApplicationScoped
public class GitHubBotContextProvider {

    private static final Logger LOG = Logger.getLogger(GitHubBotContextProvider.class);
    private static final String BOT_SUFFIX = "[bot]";
    private static final String DEFAULT_APP_NAME_PLACEHOLDER = "wildfly-bot";

    private final String botName;

    @Inject
    public GitHubBotContextProvider(GitHubClientProvider clientProvider) {
        this.botName = initializeBotName(clientProvider) + BOT_SUFFIX;
        LOG.infof("GitHub Bot name initialized to: %s", this.botName);
    }

    private String initializeBotName(GitHubClientProvider clientProvider) {
        String appName = null;
        try {
            appName = clientProvider.getApplicationClient().getApp().getSlug();
        } catch (IOException e) {
            LOG.errorf(e, "Failed to retrieve GitHub App Name due to IOException. Using placeholder.");
        } catch (Exception e) {
            LOG.errorf(e, "An unexpected error occurred while retrieving GitHub App Name. Using placeholder.");
        }

        if (appName != null && !appName.isEmpty()) {
            LOG.infof("Successfully fetched GitHub App Name: '%s'", appName);
            return appName;
        } else {
            LOG.warnf("GitHub App Name was null, empty, or failed to fetch. Using placeholder '%s'.",
                    DEFAULT_APP_NAME_PLACEHOLDER);
            return DEFAULT_APP_NAME_PLACEHOLDER;
        }
    }

    /**
     * Gets the fetched GitHub Bot name.
     *
     * @return The name of the GitHub Bot, or a placeholder if fetching failed during startup.
     */
    public String getBotName() {
        return botName;
    }
}