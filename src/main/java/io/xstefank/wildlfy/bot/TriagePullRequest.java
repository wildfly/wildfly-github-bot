package io.xstefank.wildlfy.bot;

import io.quarkiverse.githubapp.ConfigFile;
import io.quarkiverse.githubapp.event.PullRequest;
import io.xstefank.wildlfy.bot.config.WildFlyConfigFile;
import io.xstefank.wildlfy.bot.config.WildFlyConfigFile.WildFlyRule;
import io.xstefank.wildlfy.bot.config.util.Matcher;
import org.jboss.logging.Logger;
import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;

import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class TriagePullRequest {

    private static final Logger LOG = Logger.getLogger(TriagePullRequest.class);

    void onPullRequestOpened(@PullRequest.Opened GHEventPayload.PullRequest pullRequestPayload,
                             @ConfigFile("wildfly-bot.yml") WildFlyConfigFile wildflyBotConfigFile) throws IOException {

        if (wildflyBotConfigFile == null) {
            LOG.error("No configuration file available. ");
            return;
        }

        GHPullRequest pullRequest = pullRequestPayload.getPullRequest();
        Set<String> mentions = new TreeSet<>();

        for (WildFlyRule rule : wildflyBotConfigFile.wildfly.rules) {
            if (Matcher.matches(pullRequest, rule)) {
                for (String nick : rule.notify) {
                    if (!nick.equals(pullRequest.getUser().getLogin())) {
                        mentions.add(nick);
                    }
                }
            }
        }

        GitHub gitHub = GitHubBuilder.fromEnvironment().build();

        if (!mentions.isEmpty()) {
            pullRequest.comment("/cc @" + String.join(", @", mentions));
            pullRequest.requestReviewers(mentions.stream().map(nick -> {
                try {
                    return gitHub.getUser(nick);
                } catch (IOException e) {
                    LOG.error("Could find user " + nick);
                    return null;
                }
            }).collect(Collectors.toList()));
        }

    }
}

