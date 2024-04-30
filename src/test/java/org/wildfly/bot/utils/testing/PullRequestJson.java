package org.wildfly.bot.utils.testing;

import com.fasterxml.jackson.databind.JsonNode;

public interface PullRequestJson {

    String ACTION = "action";
    String BODY = "body";
    String HEAD = "head";
    String ID = "id";
    String PULL_REQUEST = "pull_request";
    String SHA = "sha";
    String TITLE = "title";
    String USER = "user";
    String LOGIN = "login";
    String NUMBER = "number";

    default String commitSHA() {
        return payload().get(PULL_REQUEST).get(HEAD).get(SHA).textValue();
    }

    default String jsonString() {
        return payload().toPrettyString();
    }

    default long id() {
        return payload().get(PULL_REQUEST).get(ID).longValue();
    }

    default long number() {
        return payload().get(NUMBER).longValue();
    }

    default String status() {
        return payload().get(ACTION).textValue();
    }

    JsonNode payload();
}
