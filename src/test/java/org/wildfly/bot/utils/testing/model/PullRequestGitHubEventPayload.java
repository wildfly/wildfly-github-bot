package org.wildfly.bot.utils.testing.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.kohsuke.github.GHEvent;
import org.wildfly.bot.utils.PullRequestJsonBuildable;
import org.wildfly.bot.utils.model.Action;
import org.wildfly.bot.utils.testing.PullRequestJson;

import java.io.IOException;

import static org.wildfly.bot.utils.TestConstants.INSTALLATION_ID;
import static org.wildfly.bot.utils.TestConstants.TEST_REPO;

public class PullRequestGitHubEventPayload extends GitHubEventPayload implements PullRequestJsonBuildable, PullRequestJson {

    private static final String fileName = "events/raw_pr_template.json";

    public PullRequestGitHubEventPayload() throws IOException {
        this(fileName, TEST_REPO, GHEvent.PULL_REQUEST, INSTALLATION_ID);
    }

    public PullRequestGitHubEventPayload(String repository, long eventId) throws IOException {
        this(fileName, repository, GHEvent.PULL_REQUEST, eventId);
    }

    protected PullRequestGitHubEventPayload(String fileName, GHEvent event) throws IOException {
        this(fileName, TEST_REPO, event, INSTALLATION_ID);
    }

    protected PullRequestGitHubEventPayload(String fileName, String repository, GHEvent event, long eventId)
            throws IOException {
        super(fileName, repository, event, eventId);
    }

    @Override
    public PullRequestGitHubEventPayload action(Action action) {
        getPayload().put(ACTION, action.getValue());
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

    @Override
    public String jsonString() {
        return super.toString();
    }
}
