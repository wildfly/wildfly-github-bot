package io.xstefank.wildfly.bot;

import io.quarkiverse.githubapp.ConfigFile;
import io.quarkiverse.githubapp.event.PullRequest;
import io.xstefank.wildfly.bot.config.RuntimeConstants;
import io.xstefank.wildfly.bot.config.WildFlyConfigFile;
import io.xstefank.wildfly.bot.config.WildFlyConfigFile.WildFlyRule;
import io.xstefank.wildfly.bot.config.util.Matcher;
import org.jboss.logging.Logger;
import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.GHPullRequest;

import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;

public class TriagePullRequestProcessor {
    private static final Logger LOG = Logger.getLogger(TriagePullRequestProcessor.class);

    void onPullRequestOpened(@PullRequest.Opened GHEventPayload.PullRequest pullRequestPayload,
                             @ConfigFile(RuntimeConstants.CONFIG_FILE_NAME) WildFlyConfigFile wildflyBotConfigFile) throws IOException {

        if (wildflyBotConfigFile == null) {
            LOG.error("No configuration file available. ");
            return;
        }

        GHPullRequest pullRequest = pullRequestPayload.getPullRequest();
        Set<String> mentions = new TreeSet<>();

        for (WildFlyRule rule : wildflyBotConfigFile.wildfly.rules) {
            if (Matcher.matches(pullRequest, rule)) {
                 LOG.debugf("Pull Request %s was matched with a rule with the id: %s.", pullRequest.getTitle(), rule.id != null ? rule.id : "N/A");
                for (String nick : rule.notify) {
                    if (!nick.equals(pullRequest.getUser().getLogin())) {
                        mentions.add(nick);
                    }
                }
            }
        }

        if (!mentions.isEmpty()) {
            pullRequest.comment("/cc @" + String.join(", @", mentions));
        }

    }
}

