package org.nrg.execution.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class DockerServer {
    @JsonProperty("host") private String host;
    @JsonProperty("cert-path") private String certPath;

    public DockerServer() {}

    public DockerServer(final String host, final String certPath) {
        this.host = host;
        this.certPath = certPath;
    }

    public DockerServer(final DockerServerPrefsBean dockerServerPrefsBean) {
        this.host = dockerServerPrefsBean.getHost();
        this.certPath = dockerServerPrefsBean.getCertPath();
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

        return Objects.equals(this.host, that.host) &&
                Objects.equals(this.certPath, that.certPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, certPath);
    }
}
