package org.nrg.containers.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.base.Function;
import com.google.common.collect.Lists;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;

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

    public static List<String> names() {
        return Lists.transform(Arrays.asList(CommandType.values()), new Function<CommandType, String>() {
            @Nullable
            @Override
            public String apply(@Nullable final CommandType type) {
                return type != null ? type.getName() : "";
            }
        });
    }

}
