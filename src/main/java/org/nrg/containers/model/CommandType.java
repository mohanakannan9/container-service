package org.nrg.containers.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum CommandType {

    DOCKER("docker");

    private final String name;

    @JsonCreator
    CommandType(final String name) {
        this.name = name;
    }

    @JsonValue
    public String getName() {
        return name;
    }

}
