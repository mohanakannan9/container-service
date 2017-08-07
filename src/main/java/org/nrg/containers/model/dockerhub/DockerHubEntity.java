package org.nrg.containers.model.dockerhub;

import com.google.common.base.MoreObjects;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import java.util.Objects;

@Entity
public class DockerHubEntity extends AbstractHibernateEntity {

    private String name;
    private String url;

    public static DockerHubEntity fromPojo(final DockerHubBase.DockerHub pojo) {
        return fromPojoWithTemplate(pojo, new DockerHubEntity());
    }

    public static DockerHubEntity fromPojoWithTemplate(final DockerHubBase.DockerHub pojo, final DockerHubEntity template) {
        if (template == null) {
            return fromPojo(pojo);
        }
        template.setId(pojo.id());
        template.name = pojo.name();
        template.url = pojo.url();
        return template;
    }

    public DockerHubBase.DockerHub toPojo(final long defaultId) {
        return DockerHubBase.DockerHub.create(this.getId(), this.name, this.url, this.getId() == defaultId);
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

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        final DockerHubEntity that = (DockerHubEntity) o;
        return Objects.equals(this.name, that.name) &&
                Objects.equals(this.url, that.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, url);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("name", name)
                .add("url", url)
                .toString();
    }
}
