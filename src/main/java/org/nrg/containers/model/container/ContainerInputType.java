package org.nrg.containers.model.container;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ContainerInputType {
    RAW("raw"),
    WRAPPER_DEPRECATED("wrapper"),
    COMMAND("command"),
    WRAPPER_EXTERNAL("wrapper-external"),
    WRAPPER_DERIVED("wrapper-derived");

    private final String name;

    @JsonCreator
    ContainerInputType(final String name) {
        this.name = name;
    }

    @JsonValue
    public String getName() {
        return name;
    }
}
