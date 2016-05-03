package org.nrg.containers.image;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.envers.Audited;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntity;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Audited
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "nrg")
@ApiModel(description = "Properties that define an image.")
public class Image extends AbstractHibernateEntity implements Serializable {

    private String name;
    @JsonProperty("image-id") private String imageId;
    @JsonProperty("repo-tags") private List<String> repoTags;
    private Long size;
    private Map<String, String> labels;

    public Image() {}

    public Image(final String name, final String imageId, Long size, List<String> repoTags, Map<String, String> labels){
        this.name = name;
        this.imageId = imageId;
        this.size = size;
        this.repoTags = repoTags;
        this.labels = labels;
    }

    /**
     * The image's XNAT name.
     **/
    @ApiModelProperty(value = "The image's name.")
    public String getName() {
        return name;
    }

    public void setName(final String name) { this.name = name; }

    /**
     * The image's docker id.
     **/
    @ApiModelProperty(value = "The image's id.")
    public String getImageId() { return imageId; }

    public void setImageId(final String imageId) { this.imageId = imageId; }

    /**
     * The image's size.
     **/
    @ApiModelProperty(value = "The image's size.")
    public Long getSize() { return size; }

    public void setSize(final Long size) { this.size = size; }

    /**
     * The image's repo tags.
     **/
    @ElementCollection
    @ApiModelProperty(value = "The image's repo tags.")
    @JsonProperty("repotags")
    public List<String> getRepoTags() { return repoTags; }

    public void setRepoTags(final List<String> repoTags) { this.repoTags = repoTags; }

    /**
     * Image labels
     **/
    @ApiModelProperty(value = "Image labels")
    @JsonProperty("labels")
    @ElementCollection
    public Map<String, String> getLabels() { return labels; }

    public void setLabels(final Map<String, String> labels) { this.labels = labels; }

    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("imageId", imageId)
                .add("name", name)
                .add("size", size)
                .add("tags", repoTags)
                .add("labels", labels)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !super.equals(o) || getClass() != o.getClass()) {
            return false;
        }

        Image that = (Image) o;

        return Objects.equals(this.name, that.name) &&
                Objects.equals(this.imageId, that.imageId) &&
                Objects.equals(this.repoTags, that.repoTags) &&
                Objects.equals(this.size, that.size) &&
                Objects.equals(this.labels, that.labels);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), name, imageId, repoTags, size, labels);
    }
}
