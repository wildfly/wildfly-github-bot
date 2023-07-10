package io.xstefank.wildfly.bot;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkiverse.githubapp.event.PullRequest;
import io.quarkiverse.githubapp.runtime.UtilsProducer;
import io.quarkiverse.githubapp.runtime.github.GitHubConfigFileProviderImpl;
import io.quarkus.logging.Log;
import io.xstefank.wildfly.bot.config.util.GithubCommitProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.xstefank.wildfly.bot.config.RuntimeConstants;
import io.xstefank.wildfly.bot.config.WildFlyConfigFile;
import org.kohsuke.github.GHCommitState;
import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHPullRequestFileDetail;
import org.kohsuke.github.GitHub;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@ApplicationScoped
public class ConfigFileChangeProcessor {

    private static final Pattern DIFF_REGEX = Pattern.compile("^@@\\s+-(?:(\\d+)(?:,(\\d+))?)\\s+\\+(?:(\\d+)(?:,(\\d+))?)\\s+@@", Pattern.MULTILINE);

    private static final String CHECK_NAME = "Configuration File";

    @Inject
    GitHubConfigFileProviderImpl fileProvider;

    @Inject
    @UtilsProducer.Yaml
    ObjectMapper yamlObjectMapper;

    @Inject
    GithubCommitProcessor githubCommitProcessor;

    void onFileChanged(@PullRequest.Opened @PullRequest.Edited GHEventPayload.PullRequest pullRequestPayload, GitHub gitHub) throws IOException {
        GHPullRequest pullRequest = pullRequestPayload.getPullRequest();
        for (GHPullRequestFileDetail changedFile : pullRequest.listFiles()) {
            if (changedFile.getFilename().equals(fileProvider.getFilePath(RuntimeConstants.CONFIG_FILE_NAME))) {
                try {
                    GHContent updatedFile = gitHub.getRepository(pullRequest.getHead().getRepository().getFullName()).getFileContent(".github/" + RuntimeConstants.CONFIG_FILE_NAME, pullRequest.getHead().getSha());
                    String updatedFileContent = new String( updatedFile.read().readAllBytes() );
                    Optional<WildFlyConfigFile> file = Optional.ofNullable(yamlObjectMapper.readValue(updatedFileContent, WildFlyConfigFile.class));
                    List<String> invalidRules = validateFile(file.get());

                    if (invalidRules.isEmpty()) {
                        githubCommitProcessor.updateFormatCommitStatus(pullRequest, GHCommitState.SUCCESS, CHECK_NAME, "\u2705");
                        Log.debug("Configuration File check successful");
                    } else {
                        githubCommitProcessor.updateFormatCommitStatus(pullRequest, GHCommitState.ERROR, CHECK_NAME, "\u274C " + String.join(", ", invalidRules));
                        Log.debugf("Configuration File check unsuccessful [%s]", String.join(",", invalidRules));
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    List<String> validateFile(WildFlyConfigFile wildflyConfigFile) {
        List<String> invalidRules = new ArrayList<>();
        for (WildFlyConfigFile.WildFlyRule rule : wildflyConfigFile.wildfly.rules) {
            if (rule.id == null) {
                invalidRules.add("Invalid rule: " + rule);
            }
        }
        return invalidRules;
    }
}
