package org.wildfly.bot.util;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import org.wildfly.bot.config.WildFlyBotConfig;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jboss.logging.Logger;
import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.wildfly.bot.model.RuntimeConstants;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.function.Function;

/**
 * This class was designed to retrieve mergable status for all pull
 * requests in a repository upon {@code io.quarkiverse.githubapp.event.Push} event.
 * This results in querying and listing all repository's open pull requests.
 * After a certain timeout it will re-query the repository's pull requests
 * again and update these pull requests if GitHub has updated the mergable
 * status for individual pull requests. To adjust the timeout time please
 * see {@see config.org.wildfly.bot.WildFlyBotConfig#timeout()}
 * <p>
 * In case a re-queried pull request fails, it will be only logged.
 * <p>
 * Note: Do not call githubProcessor.LOG.setPullRequest inside parallel
 * Uni-s, i.e. inside the parameter `uniToExecute` in method
 * {@code combineUnis(Function<GHPullRequest, Uni<?>> uniToExecute)}
 * due to possible race condition of the {@code util.org.wildfly.bot.PullRequestLogger}.
 */
@Singleton
public class PullRequestMergableProcessor {

    private static final Logger LOGGER = Logger.getLogger(PullRequestMergableProcessor.class);
    private static final Queue<Uni<List<GHPullRequest>>> pushPayloadsQueue = new LinkedList<>();
    private boolean currentlyExecuting = false;

    @Inject
    GithubProcessor githubProcessor;

    @Inject
    WildFlyBotConfig wildFlyBotConfig;

    private final Function<GHPullRequest, Uni<GHPullRequest>> pollGitHub = pullRequest -> Uni.createFrom()
            .item(pullRequest)
            .invoke(pullRequest1 -> {
                try {
                    if (wildFlyBotConfig.isDryRun()) {
                        LOGGER.info(
                                RuntimeConstants.DRY_RUN_PREPEND.formatted("Sending a request to GitHub for mergable status"));
                    } else {
                        pullRequest1.getMergeable();
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            })
            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());

    private final Function<GHPullRequest, Uni<Void>> applyLabels = pullRequest -> {
        try {
            List<String> labelsToAdd = new ArrayList<>();
            List<String> labelsToRemove = new ArrayList<>();
            if (wildFlyBotConfig.isDryRun()) {
                LOGGER.info(RuntimeConstants.DRY_RUN_PREPEND
                        .formatted("Retrieving mergable status and then we would apply labels accordingly"));
            } else {
                Optional<Boolean> mergable = Optional.ofNullable(pullRequest.getMergeable());
                if (mergable.isPresent()) {
                    if (mergable.get()) {
                        labelsToRemove.add(RuntimeConstants.LABEL_NEEDS_REBASE);
                    } else {
                        labelsToAdd.add(RuntimeConstants.LABEL_NEEDS_REBASE);
                    }
                }
            }

            return Uni.createFrom().voidItem().invoke(() -> {
                try {
                    githubProcessor.updateLabels(pullRequest, labelsToAdd, labelsToRemove);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    };

    public void addPushPayload(GHEventPayload.Push pushPayload) {
        GHRepository repository = pushPayload.getRepository();
        GHEventPayload.Push.PushCommit headCommit = pushPayload.getHeadCommit();

        Uni<List<GHPullRequest>> mergableStatusUpdateUni = Uni.createFrom()
                // Collect all Pull Requests
                .item(repository.queryPullRequests().state(GHIssueState.OPEN).base(RuntimeConstants.MAIN_BRANCH).list())
                .invoke(() -> LOGGER.infof(
                        "Scheduling a mergable status update for open pull requests for new head [%s - \"%s\"]",
                        headCommit.getSha(), headCommit.getMessage()))
                .map(ghPullRequests -> {
                    try {
                        return ghPullRequests.toList();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                // Prompt GitHub to recalculate mergable
                .call(combineUnis(pollGitHub::apply)::apply)
                // Give GitHub some time
                .onItem().delayIt().by(Duration.ofSeconds(wildFlyBotConfig.timeout()))
                // Retrieve the results from GitHub and apply labels accordingly
                .call(combineUnis(pollGitHub::apply)::apply)
                // Filter failed Pull Requests
                .call(combineUnis(applyLabels::apply)::apply);

        pushPayloadsQueue.add(mergableStatusUpdateUni);
        subscription(null, headCommit);
    }

    /**
     * Subscribes to the Uni<List<GHPullRequest>> and after the execution it subscribes to the next
     * such Uni, if available.
     *
     * @param ghPullRequests List<GHPullRequest> return from the Uni
     * @param headCommit Commit from the pushPayload prompting this invocation
     * @implNote We do not need any synchronization, as this bean is defined as {@code jakarta.inject.Singleton}
     */
    private void subscription(List<GHPullRequest> ghPullRequests, GHEventPayload.Push.PushCommit headCommit) {
        if (ghPullRequests != null && headCommit != null) {
            logResult(ghPullRequests, headCommit);
        }
        if (pushPayloadsQueue.peek() != null && !currentlyExecuting) {
            currentlyExecuting = true;
            pushPayloadsQueue.poll().subscribe().with(list -> {
                currentlyExecuting = false;
                this.subscription(list, headCommit);
            });
        }
    }

    private void logResult(List<GHPullRequest> ghPullRequests, GHEventPayload.Push.PushCommit headCommit) {
        List<String> unknownPullRequests = ghPullRequests.stream()
                .filter(pullRequest -> {
                    try {
                        return pullRequest.getMergeable() == null;
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .map(pullRequest -> "#%d".formatted(pullRequest.getNumber())).toList();
        if (!unknownPullRequests.isEmpty()) {
            List<String> missedPRs = unknownPullRequests.stream()
                    .map(String::valueOf)
                    .toList();
            String concatenatedPRs = String.join(", ", missedPRs);
            LOGGER.warnf(
                    "Unable to verify the mergable status for new %s branch head [%s - \"%s\"] for following pull requests: %s",
                    RuntimeConstants.MAIN_BRANCH_REF, headCommit.getSha(), headCommit.getMessage(), concatenatedPRs);
        } else {
            LOGGER.infof(
                    "Successfully scanned all pull requests for new %s branch head [%s - \"%s\"] and updated '%s' label accordingly. ",
                    RuntimeConstants.MAIN_BRANCH_REF, headCommit.getSha(), headCommit.getMessage(),
                    RuntimeConstants.LABEL_NEEDS_REBASE);
        }
    }

    private Function<List<GHPullRequest>, Uni<Void>> combineUnis(Function<GHPullRequest, Uni<?>> uniToExecute) {
        return ghPullRequests -> {
            List<Uni<?>> ghPullRequestsUni = ghPullRequests.stream()
                    .map(uniToExecute).toList();
            return Uni.combine().all().unis(ghPullRequestsUni)
                    .discardItems();
        };
    }
}
