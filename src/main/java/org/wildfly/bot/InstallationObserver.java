package org.wildfly.bot;

import io.quarkiverse.githubapp.event.Installation;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;
import org.kohsuke.github.GHAppInstallation;
import org.kohsuke.github.GHEventPayload;
import org.wildfly.bot.util.GitHubBotContextProvider;

@ApplicationScoped
public class InstallationObserver {

    private static final Logger LOG = Logger.getLogger(InstallationObserver.class);

    @Inject
    GitHubBotContextProvider botContextProvider;

    void suspendedInstallation(@Installation.Suspend GHEventPayload.Installation installationPayload) {
        GHAppInstallation installation = installationPayload.getInstallation();
        LOG.infof(
                "%s has been suspended for following installation id: %d and will not be able to listen for any incoming Events.",
                botContextProvider.getBotName(), installation.getAppId());
    }

    void unsuspendedInstallation(@Installation.Unsuspend GHEventPayload.Installation installationPayload) {
        GHAppInstallation installation = installationPayload.getInstallation();
        LOG.infof(
                "%s has been unsuspended for following installation id: %d and has started to listen for new incoming Events.",
                botContextProvider.getBotName(), installation.getAppId());
    }
}
