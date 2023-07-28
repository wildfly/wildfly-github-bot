package io.xstefank.wildfly.bot.util;

/**
 * Originally from https://github.com/quarkusio/quarkus-github-bot
 */

public class Strings {

    public static boolean isNotBlank(String string) {
        return string != null && !string.trim().isEmpty();
    }

    public static boolean isBlank(String string) {
        return string == null || string.trim().isEmpty();
    }

    public static String blockQuoted(String line) {
        return "> " + line;
    }

    private Strings() {
    }
}
