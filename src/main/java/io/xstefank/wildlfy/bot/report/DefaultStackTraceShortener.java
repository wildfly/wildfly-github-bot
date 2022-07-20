package io.xstefank.wildlfy.bot.report;

import io.quarkus.arc.DefaultBean;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Singleton;

@Singleton
@DefaultBean
public class DefaultStackTraceShortener implements StackTraceShortener{

    @Override
    public String shorten(String stacktrace, int length) {
        if (StringUtils.isBlank(stacktrace)) {
            return null;
        }

        return StringUtils.abbreviate(stacktrace, length);
    }
}
