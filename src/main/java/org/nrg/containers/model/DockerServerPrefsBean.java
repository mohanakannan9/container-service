package org.nrg.containers.model;

import com.google.common.base.MoreObjects;
import org.nrg.prefs.annotations.NrgPreference;
import org.nrg.prefs.annotations.NrgPreferenceBean;
import org.nrg.prefs.beans.AbstractPreferenceBean;
import org.nrg.prefs.exceptions.InvalidPreferenceName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

@NrgPreferenceBean(toolId = "docker-server",
    toolName = "Docker Server Prefs",
    description = "All the preferences that define a Docker Server")
public class DockerServerPrefsBean extends AbstractPreferenceBean {
    private static final Logger _log = LoggerFactory.getLogger(DockerServerPrefsBean.class);

    public void setFromDto(final DockerServer dockerServerDto) throws InvalidPreferenceName {
        setHost(dockerServerDto.getHost());
        setCertPath(dockerServerDto.getCertPath());
    }

    public DockerServer toDto() {
        return new DockerServer(this);
    }

    @NrgPreference
    public String getHost() {
        return getValue("host");
    }

    public void setHost(final String host) throws InvalidPreferenceName {
        _log.debug("Setting host: " + host);
        if (host != null) {
            try {
                set(host, "host");
            } catch (InvalidPreferenceName e) {
                _log.error("Error setting Docker server preference \"host\".", e.getMessage());
                throw e;
            }
        }
    }

    @NrgPreference
    public String getCertPath() {
        return getValue("certPath");
    }

    public void setCertPath(final String certPath) throws InvalidPreferenceName {
        _log.debug("Setting certPath: " + certPath);
        if (certPath != null) {
            try {
                set(certPath, "certPath");
            } catch (InvalidPreferenceName e) {
                _log.error("Error setting Docker server preference \"certPath\".", e.getMessage());
                throw e;
            }
        }
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("host", getHost())
            .add("certPath", getCertPath())
            .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DockerServerPrefsBean that = (DockerServerPrefsBean) o;

        return Objects.equals(this.getHost(), that.getHost()) &&
                Objects.equals(this.getCertPath(), that.getCertPath());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getHost(), getCertPath());
    }
}
