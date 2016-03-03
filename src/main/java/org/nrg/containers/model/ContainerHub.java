package org.nrg.containers.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

import java.util.List;
import java.util.Map;

public class ContainerHub {
    private String url;
    private String username;
    private String password;
    private String email;
    private String token;

    public ContainerHub() {}

    @JsonProperty("url")
    public String url() {
        return url;
    }

    public void setUrl(final String url) {
        this.url = url;
    }

    @JsonProperty("username")
    public String username() {
        return username;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    @JsonProperty("password")
    public String password() {
        return password;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    @JsonProperty("email")
    public String email() {
        return email;
    }

    public void setEmail(final String email) {
        this.email = email;
    }

    @JsonProperty("token")
    public String token() {
        return token;
    }

    public void setToken(final String token) {
        this.token = token;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ContainerHub that = (ContainerHub) o;

        return Objects.equal(this.url, that.url) &&
                Objects.equal(this.username, that.username) &&
                Objects.equal(this.password, that.password) &&
                Objects.equal(this.email, that.email) &&
                Objects.equal(this.token, that.token);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(url, username, password, email, token);
    }
}
