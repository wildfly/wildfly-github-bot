package org.wildfly.bot.utils.testing;

import java.io.IOException;

/**
 * We create this functional interface in order to be able to throw
 * {@code IOException} inside the {@code java.util.function.Function}.
 * This way we don't bother user with writing unnecessary try-catch blocks.
 */
@FunctionalInterface
public interface IOExceptionFunction<T, R> {
    R apply(T t) throws IOException;
}
