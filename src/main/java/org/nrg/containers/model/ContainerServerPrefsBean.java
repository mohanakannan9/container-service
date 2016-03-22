package org.nrg.containers.model;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import org.nrg.prefs.annotations.NrgPreference;
import org.nrg.prefs.annotations.NrgPreferenceBean;
import org.nrg.prefs.beans.AbstractPreferenceBean;
import org.nrg.prefs.exceptions.InvalidPreferenceName;

@NrgPreferenceBean(toolId = "container", toolName = "Container Prefs", description = "All the preferences for the Container Service")
public class ContainerServerPrefsBean extends AbstractPreferenceBean {

    public void setFromBean(final ContainerServer csBean) throws InvalidPreferenceName {
        setHost(csBean.getHost());
        setCertPath(csBean.getCertPath());
    }

    public ContainerServer toBean() {
        return new ContainerServer(getHost(), getCertPath());
    }

    @NrgPreference
    @JsonGetter("host")
    public String getHost() {
        return getValue("host");
    }

    @JsonSetter("host")
    public void setHost(final String host) throws InvalidPreferenceName {
        set(host, "host");
    }

    @NrgPreference
    @JsonProperty("certPath")
    public String getCertPath() {
        return getValue("certPath");
    }

    @JsonSetter("certPath")
    public void setCertPath(final String certPath) throws InvalidPreferenceName {
        set(certPath, "certPath");
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

        ContainerServerPrefsBean that = (ContainerServerPrefsBean) o;

        return Objects.equal(this.getHost(), that.getHost()) &&
                Objects.equal(this.getCertPath(), that.getCertPath());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getHost(), getCertPath());
    }
}
