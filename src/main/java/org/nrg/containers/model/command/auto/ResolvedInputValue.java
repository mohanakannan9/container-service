package org.nrg.containers.model.command.auto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.nrg.containers.model.xnat.XnatModelObject;

import javax.annotation.Nullable;

@AutoValue
public abstract class ResolvedInputValue {
    @JsonProperty("type") public abstract String type();
    @Nullable
    @JsonProperty("value") public abstract String value();
    @JsonIgnore
    @Nullable public abstract XnatModelObject xnatModelObject();
    @Nullable @JsonProperty("json-value") public abstract String jsonValue();

    @JsonCreator
    public static ResolvedInputValue create(@JsonProperty("type") final String type,
                                            @JsonProperty("value") final String value,
                                            @JsonProperty("json-value") final String jsonValue) {
        return builder()
                .type(type)
                .value(value)
                .jsonValue(jsonValue == null ? value : jsonValue)
                .build();
    }

    public static Builder builder() {
        return new AutoValue_ResolvedInputValue.Builder();
    }

    @AutoValue.Builder
    public static abstract class Builder {
        public abstract Builder type(String type);
        public abstract Builder value(String value);
        public abstract Builder xnatModelObject(XnatModelObject xnatModelObject);
        public abstract Builder jsonValue(String jsonValue);

        public abstract ResolvedInputValue build();
    }
}
