package io.xstefank.wildfly.bot.util;

import io.xstefank.wildfly.bot.model.WildFlyConfigFile.WildFlyRule;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHPullRequestFileDetail;


public class Matcher {

    public static boolean matches(GHPullRequest pullRequest, WildFlyRule rule) {
        if (Strings.isNotBlank(rule.title)) {
            if (Patterns.find("\\b" + rule.title + "\\b", pullRequest.getTitle())) {
                return true;
            }
        }

        if (Strings.isNotBlank(rule.body)) {
            if (Patterns.find("\\b" + rule.body + "\\b", pullRequest.getBody())) {
                return true;
            }
        }

        if (Strings.isNotBlank(rule.titleBody)) {
            if (Patterns.find("\\b" + rule.titleBody + "\\b", pullRequest.getTitle()) ||
                Patterns.find("\\b" + rule.titleBody + "\\b", pullRequest.getBody())) {
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
