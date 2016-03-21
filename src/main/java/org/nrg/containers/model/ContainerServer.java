package org.nrg.containers.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import org.nrg.prefs.annotations.NrgPreference;
import org.nrg.prefs.annotations.NrgPreferenceBean;
import org.nrg.prefs.beans.AbstractPreferenceBean;
import org.nrg.prefs.exceptions.InvalidPreferenceName;

@NrgPreferenceBean(toolId = "container", toolName = "Container Prefs", description = "All the preferences for the Container Service")
public class ContainerServer extends AbstractPreferenceBean {
    public ContainerServer() {}

    @NrgPreference
    @JsonProperty("host")
    public String getHost() {
        return getValue("host");
    }

    public void setHost(final String host) throws InvalidPreferenceName {
        set(host, "host");
    }

    @NrgPreference
    @JsonProperty("certPath")
    public String getCertPath() {
        return getValue("certPath");
    }

    public void setCertPath(final String certPath) throws InvalidPreferenceName {
        set(certPath, "certPath");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ContainerServer that = (ContainerServer) o;

        return Objects.equal(this.getHost(), that.getHost()) &&
                Objects.equal(this.getCertPath(), that.getCertPath());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getHost(), getCertPath());
    }
}
