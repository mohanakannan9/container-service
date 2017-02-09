package org.nrg.containers.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntity;

import javax.persistence.Entity;
import java.util.Objects;

@Entity
public class DockerHub extends AbstractHibernateEntity {

    @JsonProperty("name") private String name;
    @JsonProperty("url") private String url;
    @JsonProperty("username") private String username;
    @JsonProperty("password") private String password;
    @JsonProperty("email") private String email;

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(final String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(final String email) {
        this.email = email;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        final DockerHub dockerHub = (DockerHub) o;
        return Objects.equals(this.name, dockerHub.name) &&
                Objects.equals(this.url, dockerHub.url) &&
                Objects.equals(this.username, dockerHub.username) &&
                Objects.equals(this.password, dockerHub.password) &&
                Objects.equals(this.email, dockerHub.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), name, url, username, password, email);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("name", name)
                .add("url", url)
                .add("username", username)
                .add("password", "******")
                .add("email", email)
                .toString();
    }
}
