package io.xstefank.wildfly.bot;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkiverse.githubapp.event.PullRequest;
import io.quarkiverse.githubapp.runtime.UtilsProducer;
import io.quarkiverse.githubapp.runtime.github.GitHubConfigFileProviderImpl;
import io.quarkus.logging.Log;
import io.xstefank.wildfly.bot.model.RuntimeConstants;
import io.xstefank.wildfly.bot.model.WildFlyConfigFile;
import io.xstefank.wildfly.bot.util.GithubCommitProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHPullRequestFileDetail;
import org.kohsuke.github.GitHub;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@ApplicationScoped
public class ConfigFileChangeProcessor {

    private static final String CHECK_NAME = "Configuration File";

    @Inject
    GitHubConfigFileProviderImpl fileProvider;

    @Inject
    @UtilsProducer.Yaml
    ObjectMapper yamlObjectMapper;

    @Inject
    GithubCommitProcessor githubCommitProcessor;

    void onFileChanged(@PullRequest.Opened @PullRequest.Edited @PullRequest.Synchronize GHEventPayload.PullRequest pullRequestPayload, GitHub gitHub) throws IOException {
        GHPullRequest pullRequest = pullRequestPayload.getPullRequest();
        for (GHPullRequestFileDetail changedFile : pullRequest.listFiles()) {
            if (changedFile.getFilename().equals(fileProvider.getFilePath(RuntimeConstants.CONFIG_FILE_NAME))) {
                try {
                    GHContent updatedFile = gitHub.getRepository(pullRequest.getHead().getRepository().getFullName()).getFileContent(".github/"
                        + RuntimeConstants.CONFIG_FILE_NAME, pullRequest.getHead().getSha());
                    String updatedFileContent = new String( updatedFile.read().readAllBytes() );
                    Optional<WildFlyConfigFile> file = Optional.ofNullable(yamlObjectMapper.readValue(updatedFileContent, WildFlyConfigFile.class));

                    if (file.isPresent()) {
                        List<String> problems = validateFile(file.get());
                        if (problems.isEmpty()) {
                            githubCommitProcessor.commitStatusSuccess(pullRequest, CHECK_NAME, "Valid");
                            Log.debug("Configuration File check successful");
                        } else {
                            githubCommitProcessor.commitStatusError(pullRequest, CHECK_NAME, "Rule is missing an id or multiple rules have the same id.");
                            Log.warnf("Configuration File check unsuccessful. %s", String.join(",", problems));
                        }
                    } else {
                        String message = "Configuration File check unsuccessful. Unable to correctly map loaded file to YAML.";
                        githubCommitProcessor.commitStatusError(pullRequest, CHECK_NAME, message);
                        Log.debugf(message);
                    }
                } catch (JsonProcessingException e) {
                    Log.errorf(e, "Unable to parse the configuration file from the repository %s on the following Pull Request [%s]: %s",
                        pullRequest.getHead().getRepository().getFullName(), pullRequest.getId(), pullRequest.getTitle());
                    githubCommitProcessor.commitStatusError(pullRequest, CHECK_NAME, "Unable to parse the configuration file. " +
                        "Make sure it can be loaded to model at https://github.com/xstefank/wildfly-github-bot/blob/main/CONFIGURATION.yml");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    List<String> validateFile(WildFlyConfigFile file) {
        List<String> problems = new ArrayList<>();
        Set<WildFlyConfigFile.WildFlyRule> rules = new HashSet<>();
        for (WildFlyConfigFile.WildFlyRule rule : file.wildfly.rules) {
            rules.stream()
                    .filter(wildFlyRule -> wildFlyRule.id.equals(rule.id))
                    .forEach(wildFlyRule -> problems.add("Rule [" + wildFlyRule.toPrettyString() + "] and [" + rule.toPrettyString() + "] have the same id"));
            if (rule.id == null) {
                problems.add("Rule [" + rule.toPrettyString() + "] is missing an id");
            } else {
                rules.add(rule);
            }
        }
        return problems;
    }
}
