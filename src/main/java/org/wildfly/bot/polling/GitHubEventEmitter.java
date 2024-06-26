package org.wildfly.bot.polling;

@FunctionalInterface
public interface GitHubEventEmitter<T extends Throwable> {

    void fire() throws T;
}
