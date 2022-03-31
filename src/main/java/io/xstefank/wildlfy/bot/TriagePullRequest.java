package io.xstefank.wildlfy.bot;

import io.quarkiverse.githubapp.ConfigFile;
import io.quarkiverse.githubapp.event.PullRequest;
import io.xstefank.wildlfy.bot.config.WildFlyConfigFile;
import io.xstefank.wildlfy.bot.config.WildFlyConfigFile.WildFlyRule;
import io.xstefank.wildlfy.bot.config.util.Matcher;
import org.jboss.logging.Logger;
import org.kohsuke.github.GHCommitState;
import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;


import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;

public class TriagePullRequest {
    private static final String TOKEN = "Put your token here";
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

        if (!mentions.isEmpty()) {
            pullRequest.comment("/cc @" + String.join(", @", mentions));
        }

    }

    void onPullRequestEdited(@PullRequest.Edited GHEventPayload.PullRequest pullRequestPayload,
                             @ConfigFile("wildfly-bot.yml") WildFlyConfigFile wildflyBotConfigFile) throws IOException {

        if (wildflyBotConfigFile == null) {
            LOG.error("No configuration file available. ");
            return;
        }

        GHPullRequest pullRequest = pullRequestPayload.getPullRequest();

        if (Matcher.matches(pullRequest, wildflyBotConfigFile.wildfly.rules.get(0))) {
            setCommitStatus(pullRequest, GHCommitState.SUCCESS);
        } else {
            setCommitStatus(pullRequest, GHCommitState.FAILURE);
        }
    }

    private void setCommitStatus(GHPullRequest pullRequest, GHCommitState commitState) throws IOException {
        String sha = pullRequest.getHead().getSha();
        String url = pullRequest.getHtmlUrl().toString();
        String FullName = pullRequest.getRepository().getFullName();

        GitHub gitHub = new GitHubBuilder().withOAuthToken(TOKEN).build();

        gitHub.getRepository(FullName).createCommitStatus(sha, commitState, url, "Title is correct");

        if (commitState.equals(GHCommitState.SUCCESS)) {
            pullRequest.setBody("- [x] PR title check\n");
        } else {
            pullRequest.setBody("- [ ] PR title check\n");
        }
    }
}

