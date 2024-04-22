package org.wildfly.bot.utils.testing.reflection;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Charsets;
import io.quarkiverse.githubapp.testing.internal.GitHubAppTestingContext;
import io.quarkiverse.githubapp.testing.internal.GitHubMockContextImpl;
import org.apache.commons.io.IOUtils;
import org.kohsuke.github.GHEventInfo;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * This class exposes internal code of quarkus-github-app, where we extend
 *  functionality of it's testing framework. Thus, we can intercept mocking
 *  and reuse existing code, used internally in the quarkus-github-app's testing framework.
 */
public class ExposeGitHubAppTestingContext {

    public static void init(GitHubAppTestingContext testingContext)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method privateMethod = GitHubMockContextImpl.class.getDeclaredMethod("init");

        privateMethod.setAccessible(true);
        privateMethod.invoke(testingContext.mocks);
    }

    public static void initEventStubs(GitHubAppTestingContext testingContext, long installationId)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method privateMethod = GitHubMockContextImpl.class.getDeclaredMethod("initEventStubs", long.class);

        privateMethod.setAccessible(true);
        privateMethod.invoke(testingContext.mocks, installationId);
    }

    public static String getFromClassPath(GitHubAppTestingContext testingContext, String path)
            throws IOException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        try (InputStream stream = testingContext.testInstance.getClass().getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalArgumentException("No such file in classpath: '" + path + "'");
            }
            return IOUtils.toString(stream, Charsets.UTF_8);
        }
    }

    public static void setPayload(GHEventInfo ghEventInfo, JsonNode payload) throws NoSuchFieldException, IllegalAccessException {
        Field payloadField = GHEventInfo.class.getDeclaredField("payload");
        payloadField.setAccessible(true);

        payloadField.set(ghEventInfo, payload);
    }
}
