package io.xstefank;

import io.quarkiverse.githubapp.event.PullRequest;
import org.kohsuke.github.GHEventPayload;

import java.io.IOException;

public class PullRequestOpened {

    void onPullRequestOpened(@PullRequest.Opened GHEventPayload.PullRequest pullRequestPayload) throws IOException {
        pullRequestPayload.getPullRequest().comment("Thank you for your Pull Request to WildFly!");
    }
}
