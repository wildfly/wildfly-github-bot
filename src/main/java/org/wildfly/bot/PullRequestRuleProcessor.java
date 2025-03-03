package org.wildfly.bot;

import io.quarkiverse.githubapp.ConfigFile;
import io.quarkiverse.githubapp.event.PullRequest;
import org.wildfly.bot.model.RuntimeConstants;
import org.wildfly.bot.model.WildFlyConfigFile;
import org.wildfly.bot.util.GithubProcessor;
import org.wildfly.bot.util.Matcher;
import org.wildfly.bot.util.PullRequestLogger;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.GHLabel;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;
import java.util.Set;

@RequestScoped
public class PullRequestRuleProcessor {
    private static final Logger LOG_DELEGATE = Logger.getLogger(PullRequestRuleProcessor.class);
    private final PullRequestLogger LOG = new PullRequestLogger(LOG_DELEGATE);

    @Inject
    GithubProcessor githubProcessor;

    void pullRequestRuleCheck(
            @PullRequest.Edited @PullRequest.Opened @PullRequest.Synchronize @PullRequest.Reopened @PullRequest.ReadyForReview GHEventPayload.PullRequest pullRequestPayload,
            @ConfigFile(RuntimeConstants.CONFIG_FILE_NAME) WildFlyConfigFile wildflyBotConfigFile,
            GitHub gitHub) throws IOException {
        GHPullRequest pullRequest = pullRequestPayload.getPullRequest();
        LOG.setPullRequest(pullRequest);
        githubProcessor.LOG.setPullRequest(pullRequest);

        String message = githubProcessor.skipPullRequest(pullRequest, wildflyBotConfigFile);
        if (message != null) {
            LOG.infof("Skipping rules due to %s", message);
            return;
        }

        GHRepository repository = pullRequest.getRepository();
        SequencedMap<String, List<String>> ccMentionsWithRules = new LinkedHashMap<>();
        Set<String> reviewers = new HashSet<>();
        Set<String> labels = new HashSet<>();

        for (WildFlyConfigFile.WildFlyRule rule : wildflyBotConfigFile.wildfly.rules) {
            if (Matcher.notifyRequestReview(pullRequest, rule)) {
                if (!rule.notify.isEmpty()) {
                    LOG.infof("title \"%s\" was matched with a rule, containing notify, with the id: %s.",
                            pullRequest.getTitle(), rule.id != null ? rule.id : "N/A");
                    reviewers.addAll(rule.notify);
                }
                labels.addAll(rule.labels);
            } else if (Matcher.notifyComment(pullRequest, rule)) {
                if (!rule.notify.isEmpty()) {
                    LOG.infof("title \"%s\" was matched with a rule, containing notify, with the id: %s.",
                            pullRequest.getTitle(), rule.id != null ? rule.id : "N/A");
                    rule.notify.forEach(user -> ccMentionsWithRules.computeIfAbsent(user, v -> new ArrayList<>()).add(rule.id));
                }
                labels.addAll(rule.labels);
            }
        }

        ccMentionsWithRules.remove(pullRequest.getUser().getLogin());
        reviewers.remove(pullRequest.getUser().getLogin());

        githubProcessor.createLabelsIfMissing(repository, labels);

        List<String> currentLabels = pullRequest.getLabels().stream()
                .map(GHLabel::getName)
                .toList();
        labels.removeIf(currentLabels::contains);

        if (!labels.isEmpty()) {
            LOG.debugf("Adding following labels: %s.", labels);
            pullRequest.addLabels(labels.toArray(String[]::new));
        }

        githubProcessor.processNotifies(pullRequest, gitHub, ccMentionsWithRules, reviewers,
                wildflyBotConfigFile.wildfly.emails);
    }
}
