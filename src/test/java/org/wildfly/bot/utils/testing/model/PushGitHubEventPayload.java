package org.wildfly.bot.utils.testing.model;

import org.kohsuke.github.GHEvent;
import org.wildfly.bot.utils.TestConstants;

import java.io.IOException;

public class PushGitHubEventPayload extends GitHubEventPayload {

    private static final String fileName = "events/raw_push.json";

    public PushGitHubEventPayload() throws IOException {
        this(TestConstants.TEST_REPO, TestConstants.INSTALLATION_ID);
    }

    public PushGitHubEventPayload(String repository, long eventId) throws IOException {
        super(fileName, repository, GHEvent.PUSH, eventId);
    }
}
