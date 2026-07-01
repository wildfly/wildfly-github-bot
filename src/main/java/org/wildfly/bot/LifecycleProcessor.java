package org.wildfly.bot;

import io.quarkiverse.githubapp.ConfigFile;
import io.quarkiverse.githubapp.GitHubClientProvider;
import io.quarkiverse.githubapp.GitHubConfigFileProvider;

import io.quarkus.runtime.StartupEvent;
import org.wildfly.bot.config.WildFlyBotConfig;
import org.wildfly.bot.model.RuntimeConstants;
import org.wildfly.bot.model.WildFlyConfigFile;
import org.wildfly.bot.util.GitHubBotContextProvider;
import org.wildfly.bot.util.GithubProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.jboss.logging.Logger;
import org.kohsuke.github.GHAppInstallation;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.HttpException;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class LifecycleProcessor {

    public static final String EMAIL_SUBJECT = "Unsuccessful installation of WildFly Bot Application";
    public static final String EMAIL_TEXT = """
            Hello,

            The configuration file %s has some invalid rules in the following GitHub repository: %s. The following problems were detected:

            %s

            ---
            This is a generated message, please do not respond.""";

    private static final Logger logger = Logger.getLogger(LifecycleProcessor.class);

    private final WildFlyBotConfig wildFlyBotConfig;
    private final GitHubClientProvider clientProvider;
    private final GitHubConfigFileProvider fileProvider;
    private final GitHubBotContextProvider botContextProvider;
    private final ConfigFileChangeProcessor configFileChangeProcessor;
    private final GithubProcessor githubProcessor;

    public LifecycleProcessor(WildFlyBotConfig wildFlyBotConfig, GitHubClientProvider clientProvider,
            GitHubConfigFileProvider fileProvider, GitHubBotContextProvider botContextProvider,
            ConfigFileChangeProcessor configFileChangeProcessor, GithubProcessor githubProcessor) {
        // constructor injection
        this.wildFlyBotConfig = wildFlyBotConfig;
        this.clientProvider = clientProvider;
        this.fileProvider = fileProvider;
        this.botContextProvider = botContextProvider;
        this.configFileChangeProcessor = configFileChangeProcessor;
        this.githubProcessor = githubProcessor;
    }

    void onStart(@Observes StartupEvent event) {
        if (wildFlyBotConfig.isDryRun()) {
            logger.info(RuntimeConstants.DRY_RUN_PREPEND
                    .formatted("GitHub requests will only log statements and not send actual requests to GitHub."));
        }

        long installationId = 0L;
        try {
            for (GHAppInstallation installation : clientProvider.getApplicationClient().getApp().listInstallations()) {
                installationId = installation.getId();
                GitHub app = clientProvider.getInstallationClient(installation.getId());
                for (GHRepository repository : app.getInstallation().listRepositories()) {
                    try {
                        WildFlyConfigFile wildflyBotConfigFile = fileProvider.fetchConfigFile(repository,
                                RuntimeConstants.CONFIG_FILE_NAME, ConfigFile.Source.DEFAULT, WildFlyConfigFile.class)
                                .orElseThrow(
                                        () -> new IllegalStateException(
                                                "Unable to read file %s for repository %s. Either the file does not exist or the 'Contents' permission has not been set for the application."
                                                        .formatted(".github/" + RuntimeConstants.CONFIG_FILE_NAME,
                                                                repository.getFullName())));
                        List<String> emailAddresses = wildflyBotConfigFile.wildfly.emails;
                        List<String> problems = configFileChangeProcessor.validateFile(wildflyBotConfigFile, repository);

                        githubProcessor.createLabelsIfMissing(repository,
                                Set.of(RuntimeConstants.LABEL_NEEDS_REBASE, RuntimeConstants.LABEL_FIX_ME));

                        if (problems.isEmpty()) {
                            logger.infof("The configuration file from the repository %s was parsed successfully.",
                                    repository.getFullName());
                        } else {
                            logger.errorf(
                                    "The configuration file from the repository %s was not parsed successfully due to following problems: %s",
                                    repository.getFullName(), problems);
                            githubProcessor.sendEmail(
                                    EMAIL_SUBJECT,
                                    EMAIL_TEXT.formatted(RuntimeConstants.CONFIG_FILE_NAME, repository.getHttpTransportUrl(),
                                            prettyString(problems)),
                                    emailAddresses);
                        }
                    } catch (IllegalStateException e) {
                        logger.errorf(e, "Unable to retrieve or parse the configuration file from the repository %s",
                                repository.getFullName());
                    }
                }
            }
        } catch (IOException | IllegalStateException e) {
            if (e instanceof IOException) {
                logger.warnf(e, "Unable to verify rules in repository.");
            } else {
                if (e.getCause() instanceof HttpException && e.getCause() != null
                        && e.getCause().getMessage().contains("suspended")) {
                    logger.warnf(
                            "Your installation has been suspended. No events will be received until you unsuspend the github app installation.");
                } else {
                    logger.errorf(e, "%s is unable to start.", botContextProvider.getBotName());
                }
            }
            logger.errorf(e, "Unable to correctly start %s for following installation id [%d]",
                    botContextProvider.getBotName(), installationId);
        }
    }

    private String prettyString(List<String> problems) {
        return problems.stream()
                .map(s -> "- " + s)
                .collect(Collectors.joining("\n\n"));
    }
}
