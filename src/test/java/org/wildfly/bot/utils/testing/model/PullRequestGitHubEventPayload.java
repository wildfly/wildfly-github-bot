package org.wildfly.bot.utils.testing.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.kohsuke.github.GHEvent;
import org.wildfly.bot.utils.PullRequestJson;
import org.wildfly.bot.utils.PullRequestJsonBuilder;
import org.wildfly.bot.utils.model.Action;

import java.io.IOException;

public class PullRequestGitHubEventPayload extends GitHubEventPayload implements PullRequestJsonBuilder, PullRequestJson {

    private static final String fileName = "events/raw_pr_template.json";

    public PullRequestGitHubEventPayload(String repository, long eventId) throws IOException {
        this(fileName, repository, GHEvent.PULL_REQUEST, eventId);
    }

    protected PullRequestGitHubEventPayload(String fileName, String repository, GHEvent event, long eventId)
            throws IOException {
        super(fileName, repository, event, eventId);
    }

    @Override
    public PullRequestGitHubEventPayload action(Action action) {
        getPayload(PULL_REQUEST).put(ACTION, action.getValue());
        return this;
    }

    @Override
    public PullRequestGitHubEventPayload title(String title) {
        getPayload(PULL_REQUEST).put(TITLE, title);
        return this;
    }

    @Override
    public PullRequestGitHubEventPayload description(String description) {
        getPayload(PULL_REQUEST).put(BODY, description);
        return this;
    }

    @Override
    public PullRequestGitHubEventPayload userLogin(String login) {
        ((ObjectNode) getPayload(PULL_REQUEST).get(USER)).put(LOGIN, login);
        return this;
    }

    @Override
    public PullRequestGitHubEventPayload build() {
        return this;
    }

    @Override
    public JsonNode payload() {
        return super.getPayload();
    }
}
