package io.xstefank.wildfly.bot.util;

import io.xstefank.wildfly.bot.model.WildFlyConfigFile.WildFlyRule;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHPullRequestFileDetail;


public class Matcher {

    public static boolean matches(GHPullRequest pullRequest, WildFlyRule rule) {
        if (Strings.isNotBlank(rule.title)) {
            if (Patterns.find(regexWordWrap(rule.title), pullRequest.getTitle())) {
                return true;
            }
        }

        if (Strings.isNotBlank(rule.body)) {
            if (Patterns.find(regexWordWrap(rule.body), pullRequest.getBody())) {
                return true;
            }
        }

        if (Strings.isNotBlank(rule.titleBody)) {
            if (Patterns.find(regexWordWrap(rule.titleBody), pullRequest.getTitle()) ||
                Patterns.find(regexWordWrap(rule.titleBody), pullRequest.getBody())) {
                return true;
            }
        }

        if (!rule.directories.isEmpty()) {
            for (GHPullRequestFileDetail changedFile : pullRequest.listFiles()) {
                for (String directory : rule.directories) {
                    if (changedFile.getFilename().startsWith(directory.endsWith("/") ? directory : directory + "/")) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private static String regexWordWrap(String word) {
        return "\\b(" + word + ")\\b";
    }
}
