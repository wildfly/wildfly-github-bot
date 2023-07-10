package io.xstefank.wildfly.bot.config.util;

import com.hrakaroo.glob.GlobPattern;
import com.hrakaroo.glob.MatchingEngine;
import io.xstefank.wildfly.bot.config.WildFlyConfigFile.WildFlyRule;
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
                    if (!directory.contains("*")) {
                        if (changedFile.getFilename().startsWith(directory)) {
                            return true;
                        }
                    } else {
                        try {
                            MatchingEngine matchingEngine = GlobPattern.compile(directory);
                            if (matchingEngine.matches(changedFile.getFilename())) {
                                return true;
                            }
                        } catch (Exception e) {
                            LOG.error("Error evaluating glob expression: " + directory, e);
                        }
                    }
                }
            }
        }

        return false;
    }
}
