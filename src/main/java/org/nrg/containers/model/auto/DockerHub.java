package org.nrg.containers.model.auto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.base.MoreObjects;

import javax.annotation.Nullable;

@AutoValue
public abstract class DockerHub {
    public static final String DEFAULT_NAME = "Docker Hub";
    public static final String DEFAULT_URL = "https://index.docker.io/v1/";
    public static final DockerHub DEFAULT = DockerHub.create(0L, DEFAULT_NAME, DEFAULT_URL, "", "", "");

    @JsonProperty("id") public abstract long id();
    @Nullable @JsonProperty("name") public abstract String name();
    @Nullable @JsonProperty("url") public abstract String url();
    @Nullable @JsonIgnore public abstract String username();
    @Nullable @JsonIgnore public abstract String password();
    @Nullable @JsonProperty("email") public abstract String email();

    @JsonProperty("username")
    public String obscuredUsername() {
        return obscuredString();
    }

    @JsonProperty("password")
    public String obscuredPassword() {
        return obscuredString();
    }

    private String obscuredString() {
        return "*****";
    }

    @JsonCreator
    public static DockerHub create(@JsonProperty("id") final Long id,
                                   @JsonProperty("name") final String name,
                                   @JsonProperty("url") final String url,
                                   @JsonProperty("username") final String username,
                                   @JsonProperty("password") final String password,
                                   @JsonProperty("email") final String email) {
        return new AutoValue_DockerHub(id == null ? 0L : id, name, url, username, password, email);
    }

    @Override
    public final String toString() {
        return MoreObjects.toStringHelper("DockerHub")
                .add("id", id())
                .add("name", name())
                .add("email", email())
                .add("username", obscuredUsername())
                .add("password", obscuredPassword())
                .toString();
    }
}
