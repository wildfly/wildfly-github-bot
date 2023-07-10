package io.xstefank.wildfly.bot;

import io.quarkiverse.githubapp.ConfigFile;
import io.quarkiverse.githubapp.GitHubClientProvider;
import io.quarkiverse.githubapp.GitHubConfigFileProvider;
import io.quarkus.runtime.StartupEvent;
import io.xstefank.wildfly.bot.config.RuntimeConstants;
import io.xstefank.wildfly.bot.config.WildFlyConfigFile;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.kohsuke.github.GHAppInstallation;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class LifecycleProcessor {

    private static final Logger LOG = Logger.getLogger(LifecycleProcessor.class);

    @Inject
    GitHubClientProvider clientProvider;

    @Inject
    GitHubConfigFileProvider fileProvider;

    void onStart(@Observes StartupEvent event) {
        try {
            for (GHAppInstallation installation : clientProvider.getApplicationClient().getApp().listInstallations()) {
                GitHub app = clientProvider.getInstallationClient(installation.getId());
                for (GHRepository repository : app.getInstallation().listRepositories()) {
                    List<String> invalidRules = new ArrayList<>();
                    WildFlyConfigFile wildflyBotConfigFile = fileProvider.fetchConfigFile(repository, RuntimeConstants.CONFIG_FILE_NAME, ConfigFile.Source.DEFAULT, WildFlyConfigFile.class).get();
                    for (WildFlyConfigFile.WildFlyRule rule : wildflyBotConfigFile.wildfly.rules) {
                        if (rule.id == null) {
                            invalidRules.add(rule.toString());
                        }
                    }
                    LOG.errorf("In repository %s the following rules are missing ids. [%s]", repository.getFullName(), String.join(", ", invalidRules));
                }
            }
        } catch (IOException e) {
            LOG.error("Unable to verify rules in repository.", e);
        }
    }
}
