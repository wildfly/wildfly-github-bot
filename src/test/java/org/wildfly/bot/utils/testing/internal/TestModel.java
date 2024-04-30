package org.wildfly.bot.utils.testing.internal;

import io.quarkiverse.githubapp.testing.dsl.GitHubMockSetup;
import io.quarkus.test.junit.callback.QuarkusTestAfterAllCallback;
import io.quarkus.test.junit.callback.QuarkusTestBeforeEachCallback;
import io.quarkus.test.junit.callback.QuarkusTestContext;
import io.quarkus.test.junit.callback.QuarkusTestMethodContext;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.graphql.NonNull;
import org.wildfly.bot.utils.PullRequestJsonBuildable;
import org.wildfly.bot.utils.testing.CheckedFunction;
import org.wildfly.bot.utils.testing.PullRequestJson;
import org.wildfly.bot.utils.testing.PullRequestJsonBuilder;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

public class TestModel implements QuarkusTestBeforeEachCallback, QuarkusTestAfterAllCallback {

    private static final String POLLING_PROFILE = "polling";
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private static Optional<Callable<? extends PullRequestJsonBuilder>> pullRequestJsonBuilderCallableSse;
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private static Optional<Callable<? extends PullRequestJsonBuilder>> pullRequestJsonBuilderCallablePooling;
    private static CheckedFunction<PullRequestJsonBuilder, ? extends PullRequestJsonBuilder> pullRequestJsonBuilderFunction = builder -> builder;
    private static PullRequestJson pullRequestJson;

    /**
     * @implNote Between tests we clear generated Json, corresponding Building
     *           from {@code pullRequestJsonBuilderFunction} and triggers
     */
    @Override
    public void beforeEach(QuarkusTestMethodContext context) {
        pullRequestJson = null;
        TestModel.pullRequestJsonBuilderFunction = builder -> builder;
    }

    /**
     * @implNote We cache Builder providers for all tests. For individual tests
     *           you should change the behavior of the builder by {@code setPullRequestJsonBuilderBuild}
     */
    @Override
    public void afterAll(QuarkusTestContext context) {
        TestModel.pullRequestJsonBuilderCallableSse = Optional.empty();
        TestModel.pullRequestJsonBuilderCallablePooling = Optional.empty();
    }

    /**
     * Provides callables for initializing {@code PullRequestJsonBuilder}. This method is just a single
     * wrapper for individual calls to {@code sseJsonCallable} and {@code pollingJsonCallable}
     *
     * @param ssePullRequestJsonBuilderCallable
     * @param pollingPullRequestJsonBuilderCallable
     */
    public static void setAllCallables(
            @NonNull Callable<? extends PullRequestJsonBuilder> ssePullRequestJsonBuilderCallable,
            @NonNull Callable<? extends PullRequestJsonBuilder> pollingPullRequestJsonBuilderCallable) {
        sseJsonCallable(ssePullRequestJsonBuilderCallable);
        pollingJsonCallable(pollingPullRequestJsonBuilderCallable);
    }

    /**
     * Provides callable for initializing {@code PullRequestJsonBuilder} when tests are running
     * for sse mode.
     *
     * @param pullRequestJsonBuilderCallableSse
     */
    public static void sseJsonCallable(@NonNull Callable<? extends PullRequestJsonBuilder> pullRequestJsonBuilderCallableSse) {
        TestModel.pullRequestJsonBuilderCallableSse = Optional.of(pullRequestJsonBuilderCallableSse);
    }

    /**
     * Provides callable for initializing {@code PullRequestJsonBuilder} when tests are running
     * for pooling mode.
     *
     * @param pullRequestJsonBuilderCallablePooling
     */
    public static void pollingJsonCallable(
            @NonNull Callable<? extends PullRequestJsonBuilder> pullRequestJsonBuilderCallablePooling) {
        TestModel.pullRequestJsonBuilderCallablePooling = Optional.of(pullRequestJsonBuilderCallablePooling);
    }

    // We use generics here to help the user with intelli-sense
    public static <T extends PullRequestJsonBuilder> PullRequestJson setPullRequestJsonBuilderBuild(
            @NonNull CheckedFunction<T, PullRequestJsonBuilder> builder) throws Exception {
        //noinspection unchecked
        TestModel.pullRequestJsonBuilderFunction = (CheckedFunction<PullRequestJsonBuilder, ? extends PullRequestJsonBuilder>) builder;
        return getPullRequestJson();
    }

    public static GivenTest given(GitHubMockSetup<? extends Throwable> gitHubMockSetup) {
        return new GivenTest(gitHubMockSetup);
    }

    public static PullRequestJson getPullRequestJson() throws Exception {
        Callable<? extends PullRequestJsonBuilder> builderCallable;

        if (pollingProfile()) {
            if (TestModel.pullRequestJsonBuilderCallablePooling.isEmpty()) {
                throw new RuntimeException("Trying to call callable Pooling PullRequestJsonBuilder, but none was provided");
            }
            builderCallable = TestModel.pullRequestJsonBuilderCallablePooling.get();
        } else {
            if (TestModel.pullRequestJsonBuilderCallableSse.isEmpty()) {
                throw new RuntimeException("Trying to call callable Sse PullRequestJsonBuilder, but none was provided");
            }
            builderCallable = TestModel.pullRequestJsonBuilderCallableSse.get();
        }

        return supplyPullRequestJson(builderCallable);
    }

    static boolean pollingProfile() {
        List<String> profiles = ConfigProvider.getConfig().getValues("quarkus.test.profile", String.class);

        return profiles.contains(POLLING_PROFILE);
    }

    private static <T extends PullRequestJsonBuilder> PullRequestJson supplyPullRequestJson(Callable<T> callable)
            throws Exception {
        // we cache the built PullRequestJson in the same test
        if (pullRequestJson != null) {
            return pullRequestJson;
        }

        T builder = callable.call();
        PullRequestJsonBuilder builtBuilder = TestModel.pullRequestJsonBuilderFunction.apply(builder);

        if (!(builtBuilder instanceof PullRequestJsonBuildable)) {
            throw new RuntimeException(
                    "The provided PullRequestJsonBuilder should implement PullRequestJsonBuildable interface, such that we can instantiate objects. Currently [%s] does not support it"
                            .formatted(builder.getClass().getName()));
        }

        return ((PullRequestJsonBuildable) builtBuilder).build();
    }
}
