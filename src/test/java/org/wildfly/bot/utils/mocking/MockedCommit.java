package org.wildfly.bot.utils.mocking;

import org.wildfly.bot.utils.TestConstants;

public class MockedCommit {
    final String message;
    String sha = TestConstants.SHA;
    String author = TestConstants.AUTHOR;

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
