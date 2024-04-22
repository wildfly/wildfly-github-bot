package org.wildfly.bot.utils.testing.model;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.kohsuke.github.GHEvent;
import org.wildfly.bot.utils.model.Action;

import java.io.IOException;

public class PullRequestGitHubEventPayload extends GitHubEventPayload {

    private static final String PULL_REQUEST = "pull_request";
    private static final String ACTION = "action";
    private static final String TITLE = "title";
    private static final String DESCRIPTION = "description";
    private static final String USER = "user";
    private static final String LOGIN = "login";

    private static final String fileName = "events/raw_pr_template.json";

    public PullRequestGitHubEventPayload(String repository, long eventId) throws IOException {
        this(fileName, repository, GHEvent.PULL_REQUEST, eventId);
    }

    protected PullRequestGitHubEventPayload(String fileName, String repository, GHEvent event, long eventId) throws IOException {
        super(fileName, repository, event, eventId);
    }

    @SuppressWarnings("unchecked")
    public <T extends PullRequestGitHubEventPayload> T action(Action action) {
        getPayload(PULL_REQUEST).put(ACTION, action.getValue());
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public <T extends PullRequestGitHubEventPayload> T title(String title) {
        getPayload(PULL_REQUEST).put(TITLE, title);
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public <T extends PullRequestGitHubEventPayload> T description(String description) {
        getPayload(PULL_REQUEST).put(DESCRIPTION, description);
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public <T extends PullRequestGitHubEventPayload> T userLogin(String login) {
        ((ObjectNode) getPayload(PULL_REQUEST).get(USER)).put(LOGIN, login);
        return (T) this;
    }
}
