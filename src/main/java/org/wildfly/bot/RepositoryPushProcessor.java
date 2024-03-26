package org.wildfly.bot;

import io.quarkiverse.githubapp.event.Push;
import org.wildfly.bot.util.PullRequestMergableProcessor;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import org.kohsuke.github.GHEventPayload;
import org.wildfly.bot.model.RuntimeConstants;

@RequestScoped
public class RepositoryPushProcessor {

    @Inject
    PullRequestMergableProcessor pullRequestMergableProcessor;

    void branchUpdated(@Push GHEventPayload.Push pushPayload) {
        if (pushPayload.getRef().equals(RuntimeConstants.MAIN_BRANCH_REF)) {
            pullRequestMergableProcessor.addPushPayload(pushPayload);
        }
    }
}
