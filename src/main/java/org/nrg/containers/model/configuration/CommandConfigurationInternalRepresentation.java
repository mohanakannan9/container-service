package org.nrg.containers.model.configuration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import javax.annotation.Nullable;

@AutoValue
public abstract class CommandConfigurationInternalRepresentation {
    @Nullable @JsonProperty("enabled") public abstract Boolean enabled();
    @Nullable @JsonProperty("configuration") public abstract CommandConfiguration configuration();

    @JsonCreator
    public static CommandConfigurationInternalRepresentation create(@JsonProperty("enabled") final Boolean enabled,
                                                                    @JsonProperty("configuration") final CommandConfiguration configuration) {
        return new AutoValue_CommandConfigurationInternalRepresentation(enabled, configuration);
    }
}
