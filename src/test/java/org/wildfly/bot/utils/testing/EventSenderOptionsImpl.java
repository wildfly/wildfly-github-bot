package org.wildfly.bot.utils.testing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkiverse.githubapp.testing.dsl.EventHandlingResponse;
import io.quarkus.arc.Arc;
import io.quarkus.logging.Log;
import org.kohsuke.github.GHEvent;
import org.wildfly.bot.polling.GitHubEventEmitter;
import org.wildfly.bot.utils.testing.dsl.EventSenderOptions;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionException;

import static org.wildfly.bot.utils.testing.reflection.ExposeGitHubAppTestingContext.getFromClassPath;

final class EventSenderOptionsImpl implements EventSenderOptions {

    /**
     * @implNote These are the events currently supported by GitHub's API for retrieving Events.
     *           Please, @see
     *           <a href="https://docs.github.com/en/rest/using-the-rest-api/github-event-types?apiVersion=2022-11-28">docs</a>
     */
    // note, there is missing Sponsorship Event
    private static Map<String, GHEvent> createEventMap() {
        HashMap<String, GHEvent> map = new HashMap<>();
        map.put("CommitCommentEvent", GHEvent.COMMIT_COMMENT);
        map.put("CreateEvent", GHEvent.CREATE);
        map.put("DeleteEvent", GHEvent.DELETE);
        map.put("ForkEvent", GHEvent.FORK);
        map.put("GollumEvent", GHEvent.GOLLUM);
        map.put("IssueCommentEvent", GHEvent.ISSUE_COMMENT);
        map.put("IssuesEvent", GHEvent.ISSUES);
        map.put("MemberEvent", GHEvent.MEMBER);
        map.put("PublicEvent", GHEvent.PUBLIC);
        map.put("PullRequestEvent", GHEvent.PULL_REQUEST);
        map.put("PullRequestReviewEvent", GHEvent.PULL_REQUEST_REVIEW);
        map.put("PullRequestReviewCommentEvent", GHEvent.PULL_REQUEST_REVIEW_COMMENT);
        map.put("PullRequestReviewThreadEvent", GHEvent.PULL_REQUEST_REVIEW_THREAD);
        map.put("PushEvent", GHEvent.PUSH);
        map.put("ReleaseEvent", GHEvent.RELEASE);
        map.put("WatchEvent", GHEvent.WATCH);
        return Collections.unmodifiableMap(map);
    }
    private static final Map<String, GHEvent> supportedEvents = createEventMap();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String TYPE = "type";
    private final ExtendedGitHubAppTestingContext testingContext;
    private boolean ignoreExceptions = false;
    private long installationId = 1L;

    EventSenderOptionsImpl(ExtendedGitHubAppTestingContext testingContext) {
        this.testingContext = testingContext;
    }

    private static Throwable unwrapCompletionException(Throwable captured) {
        if (captured instanceof CompletionException && captured.getCause() != null) {
            return captured.getCause();
        } else {
            return captured;
        }
    }

    @Override
    public EventSenderOptions ignoreExceptions() {
        this.ignoreExceptions = true;
        return this;
    }

    @Override
    public EventSenderOptions installationId(long installationId) {
        this.installationId = installationId;
        return this;
    }

    @Override
    public EventHandlingResponse eventFromPayload(final String payload) throws IOException {
        String adjustedPayload = payload.strip();

        if (!adjustedPayload.startsWith("[")) {
            adjustedPayload = "[" + adjustedPayload + "]";
        }

        if (!containsValidEvents(adjustedPayload)) {
            throw new RuntimeException(
                    "This type of event is not retrievable by GitHub's API, see https://docs.github.com/en/rest/using-the-rest-api/github-event-types?apiVersion=2022-11-28");
        }

        testingContext.initEventStubs(installationId, adjustedPayload);

        testingContext.getTestingContext().errorHandler.captured = null;

        for (GitHubEventEmitter<?> dispatcher : Arc.container().instance(EventFiringBeansProvider.class).get().getBeans()) {

            AssertionError callAssertionError = null;
            try {
                dispatcher.fire();
            } catch (Throwable e) {
                callAssertionError = new AssertionError("The fire event for %s has failed with the following message: %s"
                        .formatted(dispatcher.getClass().getName(), e.getMessage()), e);
            }
            AssertionError handlingAssertionError = null;
            if (testingContext.getTestingContext().errorHandler.captured != null) {
                // For some reason quarkus-github-app wraps the exceptions in CompletionException.
                // Unwrap the exceptions, as it's not what users expect.
                Throwable unwrappedCaptured = unwrapCompletionException(
                        testingContext.getTestingContext().errorHandler.captured);
                handlingAssertionError = new AssertionError("The event handler threw an exception: "
                        + unwrappedCaptured.getMessage(),
                        unwrappedCaptured);
            }
            if (handlingAssertionError != null) {
                if (callAssertionError != null) {
                    handlingAssertionError.addSuppressed(callAssertionError);
                }
                if (!ignoreExceptions) {
                    throw handlingAssertionError;
                }
            } else if (callAssertionError != null) {
                throw callAssertionError;
            }
        }

        return new EventHandlingResponseImpl(testingContext.getTestingContext());
    }

    @Override
    public EventHandlingResponse eventFromClassPath(String path) throws IOException {
        try {
            return eventFromPayload(getFromClassPath(testingContext.getTestingContext(), path));
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(
                    "Unable to call the original method from quarkus-github-app. Perhaps the declaration of method has changed?",
                    e);
        }
    }

    private static boolean containsValidEvents(String payload) {
        try {
            JsonNode events = objectMapper.readTree(payload);
            for (JsonNode event : events) {
                String type = event.get(TYPE).asText();
                if (supportedEvents.getOrDefault(type, GHEvent.UNKNOWN) == GHEvent.UNKNOWN) {
                    Log.errorf("Encountered unknown GitHub Event [%s] in one of the events received", type);
                    return false;
                }
            }
        } catch (JsonProcessingException ignored) {
            return false;
        }
        return true;
    }
}
