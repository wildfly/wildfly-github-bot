package org.wildfly.bot.polling.processors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkiverse.githubapp.GitHubEvent;
import io.quarkus.logging.Log;
import org.kohsuke.github.GHRepository;
import org.wildfly.bot.polling.GitHubEventPreprocessor;

public class PushEventPreprocessor implements GitHubEventPreprocessor {

    private static final String HEAD_COMMIT = "head_commit";
    private static final String COMMITS = "commits";
    private static final String HEAD = "head";
    private static final String SHA = "sha";

    /**
     * @implNote We simply copy the details of the latest head commit into a new field "head_commit". We do not
     *           query the commit's details from GitHub, which would be done by repository.getCommit(sha). As we don't need
     *           all the info and received info is sufficient.
     */
    @Override
    public GitHubEvent process(GitHubEvent gitHubEvent, GHRepository repository) throws JsonProcessingException {
        GitHubEvent preprocessed = GitHubEventPreprocessor.INSTANCE.process(gitHubEvent, repository);

        JsonNode payload = objectMapper.readTree(preprocessed.getPayload());
        JsonNode headCommit = null;
        for (JsonNode commit : payload.get(COMMITS)) {
            if (commit.has(SHA) && commit.get(SHA).asText().equals(payload.get(HEAD).asText())) {
                headCommit = commit;
            }
        }

        if (headCommit == null) {
            Log.errorf("Unable to retrieve head commit from received payload [%s]", payload);
            throw new RuntimeException("Unable to retrieve head commit from received payload [%s]".formatted(payload));
        }

        ((ObjectNode) payload).remove(HEAD_COMMIT);
        ((ObjectNode) payload).putIfAbsent(HEAD_COMMIT, headCommit);

        return GitHubEventPreprocessor.updatePayload(preprocessed, payload);
    }
}
