package io.xstefank.wildfly.bot;

import io.quarkiverse.githubapp.ConfigFile;
import io.quarkiverse.githubapp.event.PullRequest;
import io.xstefank.wildfly.bot.config.WildFlyBotConfig;
import io.xstefank.wildfly.bot.model.RuntimeConstants;
import io.xstefank.wildfly.bot.model.WildFlyConfigFile;
import io.xstefank.wildfly.bot.model.WildFlyConfigFile.WildFlyRule;
import io.xstefank.wildfly.bot.util.Matcher;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.GHPullRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RequestScoped
public class TriagePullRequestProcessor {
    private static final Logger LOG = Logger.getLogger(TriagePullRequestProcessor.class);

    @Inject
    WildFlyBotConfig wildFlyBotConfig;

    void onPullRequestOpened(@PullRequest.Opened GHEventPayload.PullRequest pullRequestPayload,
                             @ConfigFile(RuntimeConstants.CONFIG_FILE_NAME) WildFlyConfigFile wildflyBotConfigFile) throws IOException {
        GHPullRequest pullRequest = pullRequestPayload.getPullRequest();

        String message = skipPullRequestRules(pullRequest, wildflyBotConfigFile);
        if (message != null) {
            LOG.infof("Pull Request [#%d] - %s -- Skipping format due to %s", pullRequest.getNumber(), pullRequest.getTitle(), message);
            return;
        }

        List<String> mentions = new ArrayList<>();

        for (WildFlyRule rule : wildflyBotConfigFile.wildfly.rules) {
            if (Matcher.matches(pullRequest, rule)) {
                LOG.infof("Pull Request %s was matched with a rule with the id: %s.", pullRequest.getTitle(),
                        rule.id != null ? rule.id : "N/A");
                for (String nick : rule.notify) {
                    if (!nick.equals(pullRequest.getUser().getLogin()) && !mentions.contains(nick)) {
                        mentions.add(nick);
                    }
                }
            }
        }

        if (!mentions.isEmpty()) {
            String mentionsComment = "/cc @" + String.join(", @", mentions);
            if (wildFlyBotConfig.isDryRun()) {
                LOG.infof("Pull request #%d - Comment \"%s\"", pullRequest.getNumber(), mentionsComment);
            } else {
                pullRequest.comment(mentionsComment);
            }
        }

    }

    private String skipPullRequestRules(GHPullRequest pullRequest, WildFlyConfigFile wildflyBotConfigFile) throws IOException {
        if (wildflyBotConfigFile == null) {
            return "no configuration file found";
        }

        if (pullRequest.isDraft()) {
            return "pull request being a draft";
        }

        return null;
    }
}

