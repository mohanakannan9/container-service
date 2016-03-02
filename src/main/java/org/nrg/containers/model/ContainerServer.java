package org.nrg.containers.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

public class ContainerServer {
    private String _host;

    public ContainerServer() {}

    public ContainerServer(final String host) {
        setHost(host);
    }

    @JsonProperty("host")
    public String host() {
        return _host;
    }

    public void setHost(final String host) {
        _host = host;
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

        return Objects.equal(this._host, that._host);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(_host);
    }
}
