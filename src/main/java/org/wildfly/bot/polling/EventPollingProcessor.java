package org.wildfly.bot.polling;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkiverse.githubapp.GitHubEvent;
import io.quarkiverse.githubapp.runtime.github.GitHubService;
import io.quarkus.scheduler.Scheduled;
import io.smallrye.mutiny.tuples.Tuple2;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.kohsuke.github.GHAppInstallation;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GHEventInfo;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.wildfly.bot.polling.processors.PushEventPreprocessor;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Map;

@ApplicationScoped
public class EventPollingProcessor implements GitHubEventEmitter<Throwable> {

    private static final String ACTION = "action";

    /**
     * @implNote Mapping between Events retrievable via GitHub API
     *           https://docs.github.com/en/rest/using-the-rest-api/github-event-types?apiVersion=2022-11-28
     *           and Events sent by GitHub SSE https://docs.github.com/en/webhooks/webhook-events-and-payloads
     */
    private static final Map<GHEvent, Tuple2<String, Boolean>> typeToEventMap = Map.of(
            GHEvent.PULL_REQUEST, Tuple2.of("pull_request", true),
            GHEvent.PULL_REQUEST_REVIEW, Tuple2.of("pull_request_review", true),
            GHEvent.PUSH, Tuple2.of("push", false));

    private static final Map<GHEvent, GitHubEventPreprocessor> eventProcessorMap = Map.of(
            GHEvent.PUSH, new PushEventPreprocessor());

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final Logger LOG = Logger.getLogger(EventPollingProcessor.class);

    @Inject
    Event<GitHubEvent> gitHubEventEmitter;

    @Inject
    GitHubService gitHubService;

    @Scheduled(every = "60s", delayed = "10s")
    @Override
    public void fire() throws IOException {
        for (GHAppInstallation app : gitHubService.getApplicationClient().getApp().listInstallations()) {
            GitHub gitHub = gitHubService.getInstallationClient(app.getId());
            for (GHRepository repository : gitHub.getInstallation().listRepositories()) {
                for (GHEventInfo eventInfo : repository.listEvents()) {
                    try {
                        String payload = payload(eventInfo);
                        Tuple2<String, String> eventTuple = getEventTuple(payload, eventInfo.getType());
                        String type = eventTuple.getItem1();
                        if (type == null) {
                            LOG.infof("Unable to determine the type of event with payload\n%s", payload);
                        }
                        GitHubEvent gitHubEvent = new GitHubEvent(app.getId(),
                                null, null, eventInfo.getRepository().getFullName(),
                                type,
                                eventTuple.getItem2(),
                                payload,
                                (JsonObject) Json.decodeValue(payload), true);

                        try {
                            GitHubEvent processedGitHubEvent = eventProcessorMap
                                    .getOrDefault(eventInfo.getType(), GitHubEventPreprocessor.INSTANCE)
                                    .process(gitHubEvent, repository);
                            gitHubEventEmitter.fire(processedGitHubEvent);
                        } catch (JsonProcessingException e) {
                            LOG.warnf(e, "The preprocessors failed to process [%s] event with the payload [%s]",
                                    eventInfo.getType(), payload);
                        }

                        // TODO logic for saving parsed events
                        return;
                    } catch (NoSuchFieldException | IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    /**
     * Retrieves the event name and event action, if applicable
     *
     * @param payload
     * @param event
     * @return Tuple2 of event name and event action, if event contains action
     */
    private static Tuple2<String, String> getEventTuple(String payload, GHEvent event) {
        Tuple2<String, Boolean> eventTuple = typeToEventMap.getOrDefault(event, Tuple2.of(null, false));
        Tuple2<String, String> noActionTuple = Tuple2.of(eventTuple.getItem1(), null);
        if (!eventTuple.getItem2()) {
            return noActionTuple;
        }
        try {
            JsonNode jsonNode = objectMapper.readTree(payload);

            if (!jsonNode.has(ACTION)) {
                LOG.warnf(
                        "For event [%s] there was an \"%s\" attribute in the json expected, but none found. Make sure, this attribute is defined",
                        eventTuple.getItem1(), ACTION);
                return noActionTuple;
            }

            return Tuple2.of(eventTuple.getItem1(), jsonNode.get(ACTION).asText());
        } catch (JsonProcessingException e) {
            return noActionTuple;
        }
    }

    /**
     * Retrieves payload from {@link GHEventInfo} using reflection. Unfortunately, there is no other way
     * to retrieve this raw payload attribute
     *
     * @param ghEventInfo
     * @return full String representation of the payload in Json.
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    public static String payload(GHEventInfo ghEventInfo) throws NoSuchFieldException, IllegalAccessException {
        Field payloadField = GHEventInfo.class.getDeclaredField("payload");
        payloadField.setAccessible(true);

        return payloadField.get(ghEventInfo).toString();
    }
}