package org.nrg.containers.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ContainerServer {
    private String _host;

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
}
