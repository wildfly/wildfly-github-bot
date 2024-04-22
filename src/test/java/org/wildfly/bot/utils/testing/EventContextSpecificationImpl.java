package org.wildfly.bot.utils.testing;

import io.quarkiverse.githubapp.testing.dsl.GitHubMockSetup;
import org.wildfly.bot.utils.testing.dsl.EventContextSpecification;
import org.wildfly.bot.utils.testing.dsl.EventSenderOptions;

public class EventContextSpecificationImpl implements EventContextSpecification {

    private final ExtendedGitHubAppTestingContext testingContext;

    public EventContextSpecificationImpl(ExtendedGitHubAppTestingContext testingContext) {
        this.testingContext = testingContext;
        testingContext.init();
    }

    @Override
    public <T extends Throwable> EventContextSpecification github(GitHubMockSetup<T> gitHubMockSetup) throws T {
        gitHubMockSetup.setup(testingContext.getTestingContext().mocks);
        return this;
    }

    @Override
    public EventSenderOptions when() {
        return new EventSenderOptionsImpl(testingContext);
    }
}
