package io.xstefank.wildlfy.bot.config.util;

import io.xstefank.wildlfy.bot.config.WildFlyConfigFile.WildFlyRule;

public class Matcher {

    public static boolean matches(String title, String body, WildFlyRule rule) {
        if (Strings.isNotBlank(rule.title)) {
            if (Patterns.find(rule.title, title)) {
                return true;
            }
        }

        if (Strings.isNotBlank(rule.body)) {
            if (Patterns.find(rule.body, body)) {
                return true;
            }
        }

        return false;
    }
}
