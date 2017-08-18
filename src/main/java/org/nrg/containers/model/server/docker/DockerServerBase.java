package org.nrg.containers.model.server.docker;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;

public abstract class DockerServerBase {
    @JsonProperty("name") public abstract String name();
    @JsonProperty("host") public abstract String host();
    @Nullable @JsonProperty("cert-path") public abstract String certPath();

    @AutoValue
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public abstract static class DockerServer extends DockerServerBase {
        @JsonCreator
        public static DockerServer create(@JsonProperty("name") final String name,
                                          @JsonProperty("host") final String host,
                                          @JsonProperty("cert-path") final String certPath) {
            return new AutoValue_DockerServerBase_DockerServer(StringUtils.isBlank(name) ? host : name, host, certPath);
        }
        public static DockerServer create(final DockerServerPrefsBean dockerServerPrefsBean) {
            return create(dockerServerPrefsBean.getName(),
                    dockerServerPrefsBean.getHost(),
                    dockerServerPrefsBean.getCertPath());
        }
    }

    @AutoValue
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public static abstract class DockerServerWithPing extends DockerServerBase {
        @Nullable @JsonProperty("ping") public abstract Boolean ping();

        @JsonCreator
        public static DockerServerWithPing create(@JsonProperty("name") final String name,
                                                  @JsonProperty("host") final String host,
                                                  @JsonProperty("cert-path") final String certPath,
                                                  @JsonProperty("ping") final Boolean ping) {
            return new AutoValue_DockerServerBase_DockerServerWithPing(name, host, certPath, ping);
        }

        public static DockerServerWithPing create(final DockerServer dockerServer,
                                                  final Boolean ping) {
            return create(
                    dockerServer.name(),
                    dockerServer.host(),
                    dockerServer.certPath(),
                    ping
            );
        }
    }
}
