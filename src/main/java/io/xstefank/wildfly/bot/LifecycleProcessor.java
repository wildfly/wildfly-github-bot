package io.xstefank.wildfly.bot;

import io.quarkiverse.githubapp.ConfigFile;
import io.quarkiverse.githubapp.GitHubClientProvider;
import io.quarkiverse.githubapp.GitHubConfigFileProvider;
import io.quarkus.logging.Log;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import io.quarkus.runtime.StartupEvent;
import io.xstefank.wildfly.bot.config.WildFlyBotConfig;
import io.xstefank.wildfly.bot.model.RuntimeConstants;
import io.xstefank.wildfly.bot.model.WildFlyConfigFile;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.kohsuke.github.GHAppInstallation;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
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
    Mailer mailer;

    @ConfigProperty(name = "quarkus.mailer.username")
    Optional<String> username;

    @Inject
    @Any
    ConfigFileChangeProcessor configFileChangeProcessor;

    void onStart(@Observes StartupEvent event) {
        if (wildFlyBotConfig.isDryRun()) {
            Log.info("Dry Run enabled. GitHub requests will only log statements and not send actual requests to GitHub.");
        }

        try {
            for (GHAppInstallation installation : clientProvider.getApplicationClient().getApp().listInstallations()) {
                if (installation.getUrl() == null) {
                    continue;
                }
                GitHub app = clientProvider.getInstallationClient(installation.getId());
                for (GHRepository repository : app.getInstallation().listRepositories()) {
                    try {
                        WildFlyConfigFile wildflyBotConfigFile = fileProvider.fetchConfigFile(repository, RuntimeConstants.CONFIG_FILE_NAME, ConfigFile.Source.DEFAULT, WildFlyConfigFile.class).get();
                        List<String> emailAddresses = wildflyBotConfigFile.wildfly.emails;
                        List<String> problems = configFileChangeProcessor.validateFile(wildflyBotConfigFile);
                        if (problems.isEmpty()) {
                            LOG.infof("The configuration file from the repository %s was parsed successfully.", repository.getFullName());
                        } else {
                            LOG.errorf("The configuration file from the repository %s was not parsed successfully due to following problems: %s", repository.getFullName(), problems);

                            if (username.isPresent() && emailAddresses != null && !emailAddresses.isEmpty()) {
                                LOG.infof("Sending email to the following emails [%s].", String.join(", ", emailAddresses));
                                mailer.send(
                                    new Mail()
                                        .setSubject(EMAIL_SUBJECT)
                                        .setText(EMAIL_TEXT.formatted(RuntimeConstants.CONFIG_FILE_NAME,
                                            repository.getHttpTransportUrl(), prettyString(problems)))
                                        .setTo(emailAddresses)
                                );
                            } else {
                                LOG.debug("No emails setup to receive warnings or no email address setup to send emails from.");
                            }
                        }
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

    private String prettyString(List<String> problems) {
        return problems.stream()
            .map(s -> "- " + s)
            .collect(Collectors.joining("\n\n"));
    }
}
