package io.xstefank.wildfly.bot.utils;

import static io.xstefank.wildfly.bot.utils.TestConstants.AUTHOR;
import static io.xstefank.wildfly.bot.utils.TestConstants.SHA;

public class MockedCommit {
    final String message;
    String sha = SHA;
    String author = AUTHOR;

    private MockedCommit(String message) {
        this.message = message;
    }

    public static MockedCommit commit(String message) {
        return new MockedCommit(message);
    }

    public MockedCommit sha(String sha) {
        this.sha = sha;
        return this;
    }

    public MockedCommit author(String author) {
        this.author = author;
        return this;
    }
}
