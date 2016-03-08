package org.nrg.containers.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

public class ContainerServer {
    private String _host;
    private String _certPath;

    public ContainerServer() {}

    public ContainerServer(final String host, final String certPath) {
        setHost(host);
        setCertPath(certPath);
    }

    @JsonProperty("host")
    public String host() {
        return _host;
    }

    public void setHost(final String host) {
        _host = host;
    }

    @JsonProperty("certPath")
    public String certPath() {
        return _certPath;
    }

    public void setCertPath(final String certPath) {
        _certPath = certPath;
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

        return Objects.equal(this._host, that._host) &&
                Objects.equal(this._certPath, that._certPath);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(_host, _certPath);
    }
}
