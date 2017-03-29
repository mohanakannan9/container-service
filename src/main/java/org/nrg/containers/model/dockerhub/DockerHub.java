package org.nrg.containers.model.dockerhub;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import javax.annotation.Nullable;

@AutoValue
public abstract class DockerHub {
    public static final String DEFAULT_NAME = "Docker Hub";
    public static final String DEFAULT_URL = "https://index.docker.io/v1/";
    public static final DockerHub DEFAULT = DockerHub.create(0L, DEFAULT_NAME, DEFAULT_URL, false);

    @JsonProperty("id") public abstract long id();
    @Nullable @JsonProperty("name") public abstract String name();
    @Nullable @JsonProperty("url") public abstract String url();
    @JsonProperty("default") public abstract boolean isDefault();

    @JsonCreator
    public static DockerHub create(@JsonProperty("id") final Long id,
                                   @JsonProperty("name") final String name,
                                   @JsonProperty("url") final String url,
                                   @JsonProperty("default") final Boolean isDefault) {
        return new AutoValue_DockerHub(id == null ? 0L : id, name, url, isDefault == null ? false : isDefault);
    }
}
