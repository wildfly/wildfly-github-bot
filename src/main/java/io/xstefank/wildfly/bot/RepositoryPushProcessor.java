package io.xstefank.wildfly.bot;

import io.quarkiverse.githubapp.event.Push;
import io.xstefank.wildfly.bot.util.PullRequestMergableProcessor;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import org.kohsuke.github.GHEventPayload;

import static io.xstefank.wildfly.bot.model.RuntimeConstants.MAIN_BRANCH_REF;

@RequestScoped
public class RepositoryPushProcessor {

    @Inject
    PullRequestMergableProcessor pullRequestMergableProcessor;

    void branchUpdated(@Push GHEventPayload.Push pushPayload) {
        if (pushPayload.getRef().equals(MAIN_BRANCH_REF)) {
            pullRequestMergableProcessor.addPushPayload(pushPayload);
        }
    }
}
