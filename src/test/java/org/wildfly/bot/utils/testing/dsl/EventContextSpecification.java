package org.wildfly.bot.utils.testing.dsl;

import io.quarkiverse.githubapp.testing.dsl.GitHubMockSetup;

public interface EventContextSpecification {
    <T extends Throwable> EventContextSpecification github(GitHubMockSetup<T> gitHubMockSetup) throws T;

    EventSenderOptions when();
}
