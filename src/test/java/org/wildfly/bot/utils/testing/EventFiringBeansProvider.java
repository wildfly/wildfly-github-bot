package org.wildfly.bot.utils.testing;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.wildfly.bot.polling.GitHubEventEmitter;

@ApplicationScoped
public class EventFiringBeansProvider {

    @Any
    @Inject
    private Instance<GitHubEventEmitter<?>> beans;

    public Instance<GitHubEventEmitter<?>> getBeans() {
        return beans;
    }
}
