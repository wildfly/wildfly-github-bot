package io.xstefank.wildfly.bot;

import io.quarkiverse.githubapp.ConfigFile;
import io.quarkiverse.githubapp.GitHubClientProvider;
import io.quarkiverse.githubapp.GitHubConfigFileProvider;
import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import io.xstefank.wildfly.bot.config.WildFlyBotConfig;
import io.xstefank.wildfly.bot.model.RuntimeConstants;
import io.xstefank.wildfly.bot.model.WildFlyConfigFile;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.kohsuke.github.GHAppInstallation;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

import java.io.IOException;

@ApplicationScoped
public class LifecycleProcessor {

    private static final Logger LOG = Logger.getLogger(LifecycleProcessor.class);

    @Inject
    WildFlyBotConfig wildFlyBotConfig;

    @Inject
    GitHubClientProvider clientProvider;

    @Inject
    GitHubConfigFileProvider fileProvider;

    @Inject @Any
    ConfigFileChangeProcessor configFileChangeProcessor;

    void onStart(@Observes StartupEvent event) {
        if (wildFlyBotConfig.isDryRun()) {
            Log.info("Dry Run enabled. GitHub requests will only log statements and not send actual requests to GitHub.");
        }

        try {
            for (GHAppInstallation installation : clientProvider.getApplicationClient().getApp().listInstallations()) {
                GitHub app = clientProvider.getInstallationClient(installation.getId());
                for (GHRepository repository : app.getInstallation().listRepositories()) {
                    try {
                        fileProvider.fetchConfigFile(repository, RuntimeConstants.CONFIG_FILE_NAME, ConfigFile.Source.DEFAULT, WildFlyConfigFile.class).get();
                        LOG.infof("The configuration file from the repository %s was parsed successfully.", repository.getFullName());
                    } catch (IllegalStateException e) {
                        LOG.errorf(e, "Unable to retrieve or parse the configuration file from the repository %s", repository.getFullName());
                    }
                }
            }
        } catch (IOException e) {
            LOG.warn("Unable to verify rules in repository. Use debug log level for more details.");
            LOG.debug("Unable to verify rules in repository.", e);
        }
    }
}
