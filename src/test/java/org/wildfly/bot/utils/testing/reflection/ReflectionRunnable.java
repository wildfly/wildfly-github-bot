package org.wildfly.bot.utils.testing.reflection;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

/**
 * We create this functional interface in order to wrap invocation of methods
 * retrieved using reflection.
 * This way we don't clutter code writing unnecessary try-catch blocks.
 */
@FunctionalInterface
public interface ReflectionRunnable {

    void run()
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException, IOException;
}
