package io.xstefank.wildfly.bot;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkiverse.githubapp.event.PullRequest;
import io.quarkiverse.githubapp.runtime.UtilsProducer;
import io.quarkiverse.githubapp.runtime.github.GitHubConfigFileProviderImpl;
import io.xstefank.wildfly.bot.model.RuntimeConstants;
import io.xstefank.wildfly.bot.model.WildFlyConfigFile;
import io.xstefank.wildfly.bot.util.GithubProcessor;
import io.xstefank.wildfly.bot.util.PullRequestLogger;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.GHFileNotFoundException;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHPullRequestFileDetail;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.HttpException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static io.xstefank.wildfly.bot.model.RuntimeConstants.FAILED_CONFIGFILE_COMMENT;

@RequestScoped
public class ConfigFileChangeProcessor {

    private static final String CHECK_NAME = "Configuration File";
    private static final Logger LOG_DELEGATE = Logger.getLogger(ConfigFileChangeProcessor.class);
    private final PullRequestLogger LOG = new PullRequestLogger(LOG_DELEGATE);
    private static final String WARN_RULE = "[WARN] - %s";
    private static final String ERROR_RULE = "[ERROR] - %s";

    @Inject
    GitHubConfigFileProviderImpl fileProvider;

    @Inject
    @UtilsProducer.Yaml
    ObjectMapper yamlObjectMapper;

    @Inject
    GithubProcessor githubProcessor;

    void onFileChanged(
            @PullRequest.Opened @PullRequest.Edited @PullRequest.Synchronize @PullRequest.Reopened @PullRequest.ReadyForReview GHEventPayload.PullRequest pullRequestPayload,
            GitHub gitHub) throws IOException {
        GHPullRequest pullRequest = pullRequestPayload.getPullRequest();
        LOG.setPullRequest(pullRequest);

        GHRepository repository = pullRequest.getRepository();
        for (GHPullRequestFileDetail changedFile : pullRequest.listFiles()) {
            if (changedFile.getFilename().equals(fileProvider.getFilePath(RuntimeConstants.CONFIG_FILE_NAME))) {
                try {
                    GHContent updatedFile = gitHub.getRepository(pullRequest.getHead().getRepository().getFullName())
                            .getFileContent(".github/"
                                    + RuntimeConstants.CONFIG_FILE_NAME, pullRequest.getHead().getSha());
                    String updatedFileContent = new String(updatedFile.read().readAllBytes());
                    Optional<WildFlyConfigFile> file = Optional
                            .ofNullable(yamlObjectMapper.readValue(updatedFileContent, WildFlyConfigFile.class));

                    if (file.isPresent()) {
                        List<String> problems = validateFile(file.get(), repository);
                        if (problems.isEmpty()) {
                            githubProcessor.commitStatusSuccess(pullRequest, CHECK_NAME, "Valid");
                            LOG.debug("Configuration File check successful");
                        } else {
                            githubProcessor.commitStatusError(pullRequest, CHECK_NAME,
                                    "One or multiple rules are invalid, please see the comment stating the problems");
                            LOG.warnf("Configuration File check unsuccessful. %s", String.join(",", problems));
                        }
                        githubProcessor.formatComment(pullRequest, FAILED_CONFIGFILE_COMMENT, problems);
                    } else {
                        String message = "Configuration File check unsuccessful. Unable to correctly map loaded file to YAML.";
                        githubProcessor.commitStatusError(pullRequest, CHECK_NAME, message);
                        LOG.debugf(message);
                    }
                } catch (JsonProcessingException e) {
                    LOG.errorf(e, "Unable to parse the configuration file from the repository %s",
                            pullRequest.getHead().getRepository().getFullName());
                    githubProcessor.commitStatusError(pullRequest, CHECK_NAME, "Unable to parse the configuration file. " +
                            "Make sure it can be loaded to model at https://github.com/wildfly/wildfly-github-bot/blob/main/CONFIGURATION.yml");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    List<String> validateFile(WildFlyConfigFile file, GHRepository repository) throws IOException {
        List<String> problems = new ArrayList<>();
        Set<WildFlyConfigFile.WildFlyRule> rules = new HashSet<>();
        Set<String> repoLabels = repository.listLabels()
                .toList()
                .stream()
                .map(ghLabel -> ghLabel.getName())
                .collect(Collectors.toSet());

        if (file.wildfly.rules != null) {
            for (WildFlyConfigFile.WildFlyRule rule : file.wildfly.rules) {
                rules.stream()
                        .filter(wildFlyRule -> wildFlyRule.id.equals(rule.id))
                        .forEach(wildFlyRule -> problems.add("Rule [" + wildFlyRule.toPrettyString() + "] and ["
                                + rule.toPrettyString() + "] have the same id"));
                if (rule.id == null) {
                    problems.add(WARN_RULE.formatted("Rule [" + rule.toPrettyString() + "] is missing an id"));
                } else {
                    rules.add(rule);
                }

                for (String label : rule.labels) {
                    if (!repoLabels.contains(label)) {
                        problems.add(WARN_RULE
                                .formatted("Rule [" + rule.toPrettyString() + "] points to non-existing label: " + label));
                    }
                }

                for (String directory : rule.directories) {
                    try {
                        repository.getDirectoryContent(directory);
                    } catch (IOException e) {
                        // non-existing directory or it is not a file
                        if (e instanceof GHFileNotFoundException ||
                                (e instanceof HttpException && !e.getMessage().startsWith(
                                        "Server returned HTTP response code: 200, message: 'null' for URL: https://api.github.com/repos/"))) {
                            problems.add(ERROR_RULE.formatted("Rule [" + rule.toPrettyString()
                                    + "] has the following non-existing directory specified: " + directory));
                            LOG.debugf(e, "Exception on directories check caught");
                        }
                    }
                }
            }
        }

        return problems;
    }
}
