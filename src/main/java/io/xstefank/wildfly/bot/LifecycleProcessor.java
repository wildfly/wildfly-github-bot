package io.xstefank.wildfly.bot;

import io.quarkiverse.githubapp.ConfigFile;
import io.quarkiverse.githubapp.GitHubClientProvider;
import io.quarkiverse.githubapp.GitHubConfigFileProvider;
import io.quarkiverse.githubapp.event.Installation;
import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import io.xstefank.wildfly.bot.config.WildFlyBotConfig;
import io.xstefank.wildfly.bot.model.RuntimeConstants;
import io.xstefank.wildfly.bot.model.WildFlyConfigFile;
import io.xstefank.wildfly.bot.util.GithubProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.kohsuke.github.GHAppInstallation;
import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.HttpException;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class LifecycleProcessor {

    public static final String EMAIL_SUBJECT = "Unsuccessful installation of Wildfly Bot Application";
    public static final String EMAIL_TEXT = """
        Hello,

        The configuration file %s has some invalid rules in the following github repository: %s. The following problems were detected:

        %s

        ---
        This is generated message, please do not respond.""";

    private static final Logger LOG = Logger.getLogger(LifecycleProcessor.class);

    @Inject
    WildFlyBotConfig wildFlyBotConfig;

    @Inject
    GitHubClientProvider clientProvider;

    @Inject
    GitHubConfigFileProvider fileProvider;

    @Inject
    @Any
    ConfigFileChangeProcessor configFileChangeProcessor;

    @Inject
    GithubProcessor githubProcessor;

    void onStart(@Observes StartupEvent event) {
        if (wildFlyBotConfig.isDryRun()) {
            Log.info("Dry Run enabled. GitHub requests will only log statements and not send actual requests to GitHub.");
        }

        long installationId = 0L;
        try {
            for (GHAppInstallation installation : clientProvider.getApplicationClient().getApp().listInstallations()) {
                installationId = installation.getId();
                GitHub app = clientProvider.getInstallationClient(installation.getId());
                for (GHRepository repository : app.getInstallation().listRepositories()) {
                    try {
                        WildFlyConfigFile wildflyBotConfigFile = fileProvider.fetchConfigFile(repository, RuntimeConstants.CONFIG_FILE_NAME, ConfigFile.Source.DEFAULT, WildFlyConfigFile.class).get();
                        List<String> emailAddresses = wildflyBotConfigFile.wildfly.emails;
                        List<String> problems = configFileChangeProcessor.validateFile(wildflyBotConfigFile, repository);

                        githubProcessor.createLabelsIfMissing(repository, Set.of(RuntimeConstants.LABEL_NEEDS_REBASE));

                        if (problems.isEmpty()) {
                            LOG.infof("The configuration file from the repository %s was parsed successfully.", repository.getFullName());
                        } else {
                            LOG.errorf("The configuration file from the repository %s was not parsed successfully due to following problems: %s", repository.getFullName(), problems);
                            githubProcessor.sendEmail(
                                    EMAIL_SUBJECT,
                                    EMAIL_TEXT.formatted(RuntimeConstants.CONFIG_FILE_NAME, repository.getHttpTransportUrl(), prettyString(problems)),
                                    emailAddresses);
                        }
                    } catch (IllegalStateException e) {
                        LOG.errorf(e, "Unable to retrieve or parse the configuration file from the repository %s", repository.getFullName());
                    }
                }
            }
        } catch (IOException | IllegalStateException e) {
            if (e instanceof IOException) {
                LOG.warnf(e, "Unable to verify rules in repository.");
            } else {
                if (e.getCause() instanceof HttpException && e.getCause() != null && e.getCause().getMessage().contains("suspended")) {
                    LOG.warnf("Your installation has been suspended. No events will be received until you unsuspend the github app installation.");
                } else {
                    LOG.errorf(e, "%s is unable to start.", wildFlyBotConfig.githubName());
                }
            }
            LOG.errorf(e, "Unable to correctly start %s for following installation id [%d]", wildFlyBotConfig.githubName(), installationId);
        }
    }

    void suspendedInstallation(@Installation.Suspend GHEventPayload.Installation installationPayload) {
        GHAppInstallation installation = installationPayload.getInstallation();
        LOG.infof("%s has been suspended for following installation id: %d and will not be able to listen for any incoming Events.", wildFlyBotConfig.githubName(), installation.getAppId());
    }

    void unsuspendedInstallation(@Installation.Unsuspend GHEventPayload.Installation installationPayload) {
        GHAppInstallation installation = installationPayload.getInstallation();
        LOG.infof("%s has been unsuspended for following installation id: %d and has started to listen for new incoming Events.", wildFlyBotConfig.githubName(), installation.getAppId());
    }

    private String prettyString(List<String> problems) {
        return problems.stream()
            .map(s -> "- " + s)
            .collect(Collectors.joining("\n\n"));
    }
}
