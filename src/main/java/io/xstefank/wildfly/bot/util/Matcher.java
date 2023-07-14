package io.xstefank.wildfly.bot.util;

import io.xstefank.wildfly.bot.model.WildFlyConfigFile.WildFlyRule;
import org.jboss.logging.Logger;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHPullRequestFileDetail;


public class Matcher {

    private static final Logger LOG = Logger.getLogger(Matcher.class);

    public static boolean matches(GHPullRequest pullRequest, WildFlyRule rule) {
        if (Strings.isNotBlank(rule.title)) {
            if (Patterns.find(rule.title, pullRequest.getTitle())) {
                return true;
            }
        }

        if (Strings.isNotBlank(rule.body)) {
            if (Patterns.find(rule.body, pullRequest.getBody())) {
                return true;
            }
        }

        if (Strings.isNotBlank(rule.titleBody)) {
            if (Patterns.find(rule.titleBody, pullRequest.getTitle()) || Patterns.find(rule.titleBody, pullRequest.getBody())) {
                return true;
            }
        }

        if (!rule.directories.isEmpty()) {
            for (GHPullRequestFileDetail changedFile : pullRequest.listFiles()) {
                for (String directory : rule.directories) {
                    if (changedFile.getFilename().startsWith(directory)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}
