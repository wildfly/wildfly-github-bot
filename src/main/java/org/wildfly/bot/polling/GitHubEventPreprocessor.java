package org.wildfly.bot.polling;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkiverse.githubapp.GitHubEvent;
import io.quarkus.arc.Arc;
import io.quarkus.runtime.LaunchMode;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import org.kohsuke.github.GHRepository;

/**
 * Due to events retrieved from GitHub's API and events received as SSE,
 * we do not have fully compatible payloads. Thus, if the payload received
 * from SSE has some extra parameters, which are missing in the event retrieved
 * from GitHub's API & you are using these parameters in your reactive CDI
 * beans, you should manually update those fields.
 * For such case you receive a raw value of created {@link GitHubEvent} and
 * you can then create new object with updated values.
 * You can expect authenticated {@link GHRepository} instance for retrieving
 * the additional info.
 * This would usually involve static info, where there are no further requests made.
 * Opposite to most listSomething methods, which actually fetch up-to-date info.
 */
@FunctionalInterface
public interface GitHubEventPreprocessor {
    GitHubEventPreprocessor INSTANCE = new DefaultGitHubEventPreprocessor();
    ObjectMapper objectMapper = new ObjectMapper();

    GitHubEvent process(GitHubEvent gitHubEvent, GHRepository repository) throws JsonProcessingException;

    static GitHubEvent updatePayload(GitHubEvent event, JsonNode payload) {
        String payloadJson = payload.toPrettyString();
        return new GitHubEvent(event.getInstallationId(),
                event.getAppName().orElse(null), event.getDeliveryId(), event.getRepositoryOrThrow(),
                event.getEvent(), event.getAction(), payloadJson, (JsonObject) Json.decodeValue(payloadJson),
                event.isReplayed());
    }

    class DefaultGitHubEventPreprocessor implements GitHubEventPreprocessor {

        private static final String REPOSITORY = "repository";

        LaunchMode launchMode;

        @Override
        public GitHubEvent process(GitHubEvent gitHubEvent, GHRepository repository) throws JsonProcessingException {
            // due to static initialization in INSTANCE = new ... we need to inject programmatically
            if (launchMode == null) {
                launchMode = Arc.container().instance(LaunchMode.class).get();
            }
            if (launchMode == LaunchMode.TEST) {
                return gitHubEvent;
            }
            JsonNode payload = objectMapper.readTree(gitHubEvent.getPayload());
            String repositoryValue = objectMapper.writeValueAsString(repository);
            ((ObjectNode) payload).put(REPOSITORY, repositoryValue);

            return updatePayload(gitHubEvent, payload);
        }
    }
}
