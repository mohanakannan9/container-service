package org.nrg.containers.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class DockerServer {
    private String name;
    private String host;
    @JsonProperty("cert-path") private String certPath;

    public DockerServer() {}

    public DockerServer(final String name, final String host, final String certPath) {
        this.name = name;
        this.host = host;
        this.certPath = certPath;
    }

    public DockerServer(final DockerServerPrefsBean dockerServerPrefsBean) {
        this.name = dockerServerPrefsBean.getName();
        this.host = dockerServerPrefsBean.getHost();
        this.certPath = dockerServerPrefsBean.getCertPath();
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
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
        DockerServer that = (DockerServer) o;
        return Objects.equals(this.name, that.name) &&
                Objects.equals(this.host, that.host) &&
                Objects.equals(this.certPath, that.certPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, host, certPath);
    }
}
