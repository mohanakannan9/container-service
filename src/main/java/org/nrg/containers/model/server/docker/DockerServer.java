package org.nrg.containers.model.server.docker;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@AutoValue
@JsonInclude(JsonInclude.Include.ALWAYS)
public abstract class DockerServer {
    @JsonProperty("name") public abstract String name();
    @JsonProperty("host") public abstract String host();
    @Nullable @JsonProperty("cert-path") public abstract String certPath();

    @JsonCreator
    public static DockerServer create(@JsonProperty("name") final String name,
                                      @JsonProperty("host") final String host,
                                      @JsonProperty("cert-path") final String certPath) {
        return builder()
                .host(host)
                .name(StringUtils.isBlank(name) ? host : name)
                .certPath(certPath)
                .build();
    }

    public static DockerServer create(final DockerServerPrefsBean dockerServerPrefsBean) {
        return create(dockerServerPrefsBean.getName(),
                dockerServerPrefsBean.getHost(),
                dockerServerPrefsBean.getCertPath());
    }

    public static Builder builder() {
        return new AutoValue_DockerServer.Builder();
    }

    @AutoValue.Builder
    public static abstract class Builder {
        public abstract Builder name(@Nonnull String name);

        public abstract Builder host(@Nonnull String host);

        public abstract Builder certPath(String certPath);

        public abstract DockerServer build();
    }
}
