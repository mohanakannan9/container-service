package org.nrg.containers.model.server.docker;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.util.Date;

public abstract class DockerServerBase {
    @JsonProperty("id") public abstract Long id();
    @JsonProperty("name") public abstract String name();
    @JsonProperty("host") public abstract String host();
    @Nullable @JsonProperty("cert-path") public abstract String certPath();
    @JsonProperty("swarm-mode") public abstract boolean swarmMode();
    @JsonIgnore public abstract Date lastEventCheckTime();

    @AutoValue
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public abstract static class DockerServer extends DockerServerBase {
        public static final DockerServer DEFAULT_SOCKET = DockerServer.create(0L, "Local socket", "unix:///var/run/docker.sock", null, false);

        @JsonCreator
        public static DockerServer create(@JsonProperty("id") final Long id,
                                          @JsonProperty("name") final String name,
                                          @JsonProperty("host") final String host,
                                          @JsonProperty("cert-path") final String certPath,
                                          @JsonProperty("swarm-mode") final Boolean swarmMode) {
            return create(id, name, host, certPath, swarmMode, null);
        }

        public static DockerServer create(final Long id,
                                          final String name,
                                          final String host,
                                          final String certPath,
                                          final Boolean swarmMode,
                                          final Date lastEventCheckTime) {
            return new AutoValue_DockerServerBase_DockerServer(
                    id == null ? 0L : id,
                    StringUtils.isBlank(name) ? host : name,
                    host,
                    certPath,
                    swarmMode != null && swarmMode,
                    lastEventCheckTime != null ? lastEventCheckTime : new Date()
            );
        }

        public static DockerServer create(final DockerServerEntity dockerServerEntity) {
            return create(
                    dockerServerEntity.getId(),
                    dockerServerEntity.getName(),
                    dockerServerEntity.getHost(),
                    dockerServerEntity.getCertPath(),
                    dockerServerEntity.getSwarmMode(),
                    dockerServerEntity.getLastEventCheckTime()
            );
        }

        @SuppressWarnings("deprecation")
        public static DockerServer create(final DockerServerPrefsBean dockerServerPrefsBean) {
            return create(
                    0L,
                    dockerServerPrefsBean.getName(),
                    dockerServerPrefsBean.getHost(),
                    dockerServerPrefsBean.getCertPath(),
                    false,
                    dockerServerPrefsBean.getLastEventCheckTime());
        }

        public DockerServer updateEventCheckTime(final Date newLastEventCheckTime) {

            return newLastEventCheckTime == null ? this :
                    create(
                            this.id(),
                            this.name(),
                            this.host(),
                            this.certPath(),
                            this.swarmMode(),
                            newLastEventCheckTime
                    );
        }
    }

    @AutoValue
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public static abstract class DockerServerWithPing extends DockerServerBase {
        @Nullable @JsonProperty("ping") public abstract Boolean ping();

        @JsonCreator
        public static DockerServerWithPing create(@JsonProperty("id") final Long id,
                                                  @JsonProperty("name") final String name,
                                                  @JsonProperty("host") final String host,
                                                  @JsonProperty("cert-path") final String certPath,
                                                  @JsonProperty("swarm-mode") final Boolean swarmMode,
                                                  @JsonProperty("ping") final Boolean ping) {
            return create(id == null ? 0L : id, name, host, certPath, swarmMode, new Date(0), ping);
        }

        public static DockerServerWithPing create(final Long id,
                                                  final String name,
                                                  final String host,
                                                  final String certPath,
                                                  final Boolean swarmMode,
                                                  final Date lastEventCheckTime,
                                                  final Boolean ping) {
            return new AutoValue_DockerServerBase_DockerServerWithPing(
                    id == null ? 0L : id,
                    StringUtils.isBlank(name) ? host : name,
                    host,
                    certPath,
                    swarmMode != null && swarmMode,
                    lastEventCheckTime != null ? lastEventCheckTime : new Date(0),
                    ping != null && ping);
        }

        public static DockerServerWithPing create(final DockerServer dockerServer,
                                                  final Boolean ping) {
            return create(
                    dockerServer.id(),
                    dockerServer.name(),
                    dockerServer.host(),
                    dockerServer.certPath(),
                    dockerServer.swarmMode(),
                    dockerServer.lastEventCheckTime(),
                    ping
            );
        }
    }
}
