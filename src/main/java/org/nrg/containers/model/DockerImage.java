package org.nrg.containers.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@ApiModel(description = "Properties that define an image.")
public class DockerImage {

    @JsonProperty("image-id") private String imageId;
    @JsonProperty("tags") private List<String> tags;
    private Map<String, String> labels;

    public DockerImage() {}

    public DockerImage(final String imageId,
                       final List<String> repoTags,
                       final Map<String, String> labels) {
        this.imageId = imageId;
        this.tags = repoTags;
        this.labels = labels;
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
    @ApiModelProperty(value = "The image's repo tags.")
    public List<String> getTags() { return tags; }

    public void setTags(final List<String> tags) {
        this.tags = tags;
    }

    public void addTag(final String tag) {
        if (StringUtils.isBlank(tag)) {
            return;
        }
        if (this.tags == null) {
            this.tags = Lists.newArrayList();
        }
        this.tags.add(tag);
    }

    /**
     * Image labels
     **/
    @ApiModelProperty(value = "Image labels")
    public Map<String, String> getLabels() { return labels; }

    public void setLabels(final Map<String, String> labels) {
        this.labels = labels;
    }

    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("imageId", imageId)
                .add("tags", tags)
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
                Objects.equals(this.tags, that.tags) &&
                Objects.equals(this.labels, that.labels);
    }

    @Override
    public int hashCode() {
        return Objects.hash(imageId, tags, labels);
    }
}
