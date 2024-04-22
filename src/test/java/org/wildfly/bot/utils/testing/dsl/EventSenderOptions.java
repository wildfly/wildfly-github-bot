package org.wildfly.bot.utils.testing.dsl;

import io.quarkiverse.githubapp.testing.dsl.EventHandlingResponse;
import org.wildfly.bot.polling.GitHubEventEmitter;

import java.io.IOException;

public interface EventSenderOptions {

    /**
     * By default, we do not ignore Exceptions. Calling this method results in ignoring
     * Exceptions thrown during testing {@link GitHubEventEmitter} beans.
     *
     * @return self
     */
    EventSenderOptions ignoreExceptions();

    EventSenderOptions installationId(long installationId);

    EventHandlingResponse eventFromPayload(String payload) throws IOException;

    EventHandlingResponse eventFromClassPath(String path) throws IOException;
}
