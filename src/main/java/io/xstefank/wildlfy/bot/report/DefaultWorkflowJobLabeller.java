package io.xstefank.wildlfy.bot.report;

import io.quarkus.arc.DefaultBean;

import javax.inject.Singleton;

@Singleton
@DefaultBean
public class DefaultWorkflowJobLabeller implements WorkflowJobLabeller {

    @Override
    public String label(String name) {
        return name;
    }
}
