package org.nrg.containers.model;

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

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    private DockerServer(final Builder builder) {
        this.host = builder.host;
        this.certPath = builder.certPath;
    }

    public String host() {
        return host;
    }

    public void host(final String host) {
        this.host = host;
    }

    public String certPath() {
        return certPath;
    }

    public void certPath(final String certPath) {
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

    public static class Builder {
        private String host;
        private String certPath;

        private Builder() {}

        private Builder(final DockerServer server) {
            this.host = server.host;
            this.certPath = server.certPath;
        }

        public DockerServer build() {
            return new DockerServer(this);
        }

        public Builder host(final String host) {
            this.host = host;
            return this;
        }

        public String host() {
            return this.host;
        }

        public Builder certPath(final String certPath) {
            this.certPath = certPath;
            return this;
        }

        public String certPath() {
            return this.certPath;
        }
    }
}
