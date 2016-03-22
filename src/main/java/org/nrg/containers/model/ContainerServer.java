package org.nrg.containers.model;

import com.google.common.base.Objects;

public class ContainerServer {
    private String host;
    private String certPath;

    public ContainerServer() {}

    public ContainerServer(final String host, final String certPath) {
        this.host = host;
        this.certPath = certPath;
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

        ContainerServer that = (ContainerServer) o;

        return Objects.equal(this.host, that.host) &&
                Objects.equal(this.certPath, that.certPath);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(host, certPath);
    }
}
