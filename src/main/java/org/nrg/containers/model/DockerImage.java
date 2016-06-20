package org.nrg.containers.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.persistence.ElementCollection;
import javax.persistence.FetchType;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@ApiModel(description = "Properties that define an image.")
public class DockerImage {

    @JsonProperty("image-id") private String imageId;
    @JsonProperty("repo-tags") private List<String> repoTags = Lists.newArrayList();
    private Map<String, String> labels = Maps.newHashMap();

    public DockerImage() {}

    public DockerImage(final String imageId,
                       final List<String> repoTags,
                       final Map<String, String> labels) {
        this.imageId = imageId;
        setRepoTags(repoTags);
        setLabels(labels);
    }

    /**
     * The image's docker id.
     **/
    @ApiModelProperty(value = "The image's id.")
    public String getImageId() { return imageId; }

    public void setImageId(final String imageId) { this.imageId = imageId; }

    /**
     * The image's repo tags.
     **/
    @ElementCollection(fetch = FetchType.EAGER)
    @ApiModelProperty(value = "The image's repo tags.")
    public List<String> getRepoTags() { return repoTags; }

    public void setRepoTags(final List<String> repoTags) {
        this.repoTags = repoTags == null ?
                Lists.<String>newArrayList() :
                repoTags;
    }

    /**
     * Image labels
     **/
    @ApiModelProperty(value = "Image labels")
    @ElementCollection(fetch = FetchType.EAGER)
    public Map<String, String> getLabels() { return labels; }

    public void setLabels(final Map<String, String> labels) {
        this.labels = labels == null ?
                Maps.<String, String>newHashMap() :
                labels;
    }

    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("imageId", imageId)
                .add("tags", repoTags)
                .add("labels", labels)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DockerImage that = (DockerImage) o;

        return Objects.equals(this.imageId, that.imageId) &&
                Objects.equals(this.repoTags, that.repoTags) &&
                Objects.equals(this.labels, that.labels);
    }

    @Override
    public int hashCode() {
        return Objects.hash(imageId, repoTags, labels);
    }
}
