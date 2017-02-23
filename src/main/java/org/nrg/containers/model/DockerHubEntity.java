package org.nrg.containers.model;

import com.google.common.base.MoreObjects;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.nrg.containers.model.auto.DockerHub;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import java.util.Objects;

@Entity
public class DockerHubEntity extends AbstractHibernateEntity {

    private String name;
    private String url;
    private String auth;
    private String email;

    public static DockerHubEntity fromPojo(final DockerHub pojo) {
        final DockerHubEntity dockerHubEntity = new DockerHubEntity();
        dockerHubEntity.setId(pojo.id());
        dockerHubEntity.name = pojo.name();
        dockerHubEntity.url = pojo.url();
        dockerHubEntity.email = pojo.email();
        if (StringUtils.isNotBlank(pojo.username()) && StringUtils.isNotBlank(pojo.password())) {
            final String usernameAndPassword = pojo.username() + ":" + pojo.password();
            dockerHubEntity.auth = Base64.encodeBase64String(usernameAndPassword.getBytes());
        }
        return dockerHubEntity;
    }

    public DockerHub toPojo(final long defaultId) {
        String username = "";
        String password = "";
        if (StringUtils.isNotBlank(this.auth)) {
            final String[] auth = new String(Base64.decodeBase64(this.auth)).split(":");
            if (auth.length == 2) {
                username = auth[0];
                password = auth[1];
            }
        }
        return DockerHub.create(this.getId(), this.name, this.url, username, password, this.email, this.getId() == defaultId);
    }

    @Column(unique = true)
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

    public String getAuth() {
        return auth;
    }

    public void setAuth(final String auth) {
        this.auth = auth;
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
        final DockerHubEntity that = (DockerHubEntity) o;
        return Objects.equals(this.name, that.name) &&
                Objects.equals(this.url, that.url) &&
                Objects.equals(this.auth, that.auth) &&
                Objects.equals(this.email, that.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), name, url, auth, email);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("name", name)
                .add("url", url)
                // .add("email", email)
                .toString();
    }
}
