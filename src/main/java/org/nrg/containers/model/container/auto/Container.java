package org.nrg.containers.model.container.auto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import javax.annotation.Nullable;

@AutoValue
@JsonInclude(JsonInclude.Include.ALWAYS)
public abstract class Container {
    @JsonProperty("id") public abstract String id();
    @Nullable @JsonProperty("status") public abstract String status();

    @JsonCreator
    public static Container create(@JsonProperty("id") final String id,
                                  @JsonProperty("status") final String status) {
        return new AutoValue_Container(id, status);
    }
}
