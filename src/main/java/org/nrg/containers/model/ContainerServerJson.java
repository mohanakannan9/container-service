package org.nrg.containers.model;

import com.google.common.base.Objects;
import org.nrg.prefs.exceptions.InvalidPreferenceName;

public class ContainerServerJson {
    private String host;
    private String certPath;

    public ContainerServerJson() {}

    private ContainerServerJson(final String host, final String certPath) {
        this.host = host;
        this.certPath = certPath;
    }

    public static ContainerServerJson fromPrefBean(final ContainerServer server) {
        return new ContainerServerJson(server.getHost(), server.getCertPath());
    }

    public ContainerServer toPrefBean() throws InvalidPreferenceName {
        return new ContainerServer(this);
    }

    public String getHost() {
        return host;
    }

    public void setHost(final String host) {
        this.host = host;
    }

    public String getCertPath() {
        return certPath;
    }

    public void setCertPath(final String certPath) {
        this.certPath = certPath;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ContainerServerJson that = (ContainerServerJson) o;

        return Objects.equal(this.host, that.host) &&
                Objects.equal(this.certPath, that.certPath);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(host, certPath);
    }
}
