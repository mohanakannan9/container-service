package org.nrg.containers.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@ApiModel(description = "Properties that define an image.")
public class DockerImageDto {

    private Long id;
    private Boolean enabled;
    private Date created;
    private Date updated;
    private Date disabled;
    private String name;
    @JsonProperty("image-id") private String imageId;
    @JsonProperty("repo-tags") private List<String> repoTags = Lists.newArrayList();
    private Map<String, String> labels = Maps.newHashMap();
    @JsonProperty("in-database") private Boolean inDatabase;
    @JsonProperty("on-docker-server") private Boolean onDockerServer;

    private DockerImageDto() {}

    private DockerImageDto(final Builder builder) {
        this.id = builder.id;
        this.enabled = builder.enabled;
        this.created = builder.created;
        this.updated = builder.updated;
        this.disabled = builder.disabled;
        this.name = builder.name;
        this.imageId = builder.imageId;
        this.repoTags = builder.repoTags;
        this.labels = builder.labels;
        this.inDatabase = builder.inDatabase;
        this.onDockerServer = builder.onDockerServer;
    }

    private DockerImageDto(final DockerImage dockerImage, final Boolean inDatabase, final Boolean onDockerServer) {
        this.id = dockerImage.getId();
        this.enabled = dockerImage.isEnabled();
        this.created = dockerImage.getCreated();
        this.updated = dockerImage.getTimestamp();
        this.disabled = dockerImage.getDisabled();
        this.name = dockerImage.getName();
        this.imageId = dockerImage.getImageId();
        setRepoTags(dockerImage.getRepoTags());
        setLabels(dockerImage.getLabels());
        this.inDatabase = inDatabase;
        this.onDockerServer = onDockerServer;
    }

    public static DockerImageDto fromDbImage(final DockerImage dockerImage) {
        return new DockerImageDto(dockerImage, true, null);
    }

    public static DockerImageDto fromDbImage(final DockerImage dockerImage, final Boolean onDockerServer) {
        return new DockerImageDto(dockerImage, true, onDockerServer);
    }

    public DockerImage toDbImage() {
        return new DockerImage(name, imageId, repoTags, labels);
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(final Boolean enabled) {
        this.enabled = enabled;
    }

    public Date getCreated() {
        return created == null ? null : new Date(created.getTime());
    }

    public void setCreated(final Date created) {
        this.created = created;
    }

    public Date getUpdated() {
        return updated == null ? null : new Date(updated.getTime()) ;
    }

    public void setUpdated(final Date updated) {
        this.updated = updated;
    }

    public Date getDisabled() {
        return disabled == null ? null : new Date(disabled.getTime());
    }

    public void setDisabled(final Date disabled) {
        this.disabled = disabled;
    }


    /**
     * The image's XNAT name.
     **/
    @ApiModelProperty(value = "The image's name.")
    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    /**
     * The image's docker id.
     **/
    @ApiModelProperty(value = "The image's id.")
    public String getImageId() { return imageId; }

    public void setImageId(final String imageId) {
        this.imageId = imageId;
    }

    /**
     * The image's repo tags.
     **/
    @ApiModelProperty(value = "The image's repo tags.")
    public List<String> getRepoTags() { return repoTags; }

    public void setRepoTags(final List<String> repoTags) {
        this.repoTags = repoTags == null ?
                Lists.<String>newArrayList() :
                Lists.newArrayList(repoTags);
    }

    /**
     * Image labels
     **/
    @ApiModelProperty(value = "Image labels")
    public Map<String, String> getLabels() { return labels; }

    public void setLabels(final Map<String, String> labels) {
        this.labels = labels == null ?
                Maps.<String, String>newHashMap() :
                Maps.newHashMap(labels);
    }

    public Boolean getInDatabase() {
        return inDatabase;
    }


    public void setInDatabase(final Boolean inDatabase) {
        this.inDatabase = inDatabase;
    }

    public Boolean getOnDockerServer() {
        return onDockerServer;
    }

    public void setOnDockerServer(final Boolean onDockerServer) {
        this.onDockerServer = onDockerServer;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final DockerImageDto that = (DockerImageDto) o;

        return Objects.equals(id, that.id) &&
                Objects.equals(enabled, that.enabled) &&
                Objects.equals(created, that.created) &&
                Objects.equals(updated, that.updated) &&
                Objects.equals(disabled, that.disabled) &&
                Objects.equals(name, that.name) &&
                Objects.equals(imageId, that.imageId) &&
                Objects.equals(repoTags, that.repoTags) &&
                Objects.equals(labels, that.labels) &&
                Objects.equals(inDatabase, that.inDatabase) &&
                Objects.equals(onDockerServer, that.onDockerServer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, enabled, created, updated, disabled, name, imageId,
                repoTags, labels, inDatabase, onDockerServer);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("enabled", enabled)
                .add("created", created)
                .add("updated", updated)
                .add("disabled", disabled)
                .add("name", name)
                .add("imageId", imageId)
                .add("repoTags", repoTags)
                .add("labels", labels)
                .add("inDatabase", inDatabase)
                .add("onDockerServer", onDockerServer)
                .toString();
    }


    public static class Builder {
        private Long id;
        private Boolean enabled;
        private Date created;
        private Date updated;
        private Date disabled;
        private String name;
        private String imageId;
        private List<String> repoTags;
        private Map<String, String> labels;
        private Boolean inDatabase;
        private Boolean onDockerServer;

        private Builder() {}

        private Builder(final DockerImageDto dockerImageDto) {
            this.id = dockerImageDto.id;
            this.enabled = dockerImageDto.enabled;
            this.created = dockerImageDto.created;
            this.updated = dockerImageDto.updated;
            this.disabled = dockerImageDto.disabled;
            this.name = dockerImageDto.name;
            this.imageId = dockerImageDto.imageId;
            this.repoTags = dockerImageDto.repoTags;
            this.labels = dockerImageDto.labels;
            this.inDatabase = dockerImageDto.inDatabase;
            this.onDockerServer = dockerImageDto.onDockerServer;
        }

        public DockerImageDto build() {
            return new DockerImageDto(this);
        }

        public Builder setId(final Long id) {
            this.id = id;
            return this;
        }

        public Builder setEnabled(final Boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder setCreated(final Date created) {
            this.created = created;
            return this;
        }

        public Builder setUpdated(final Date updated) {
            this.updated = updated;
            return this;
        }

        public Builder setDisabled(final Date disabled) {
            this.disabled = disabled;
            return this;
        }

        public Builder setName(final String name) {
            this.name = name;
            return this;
        }

        public Builder setImageId(final String imageId) {
            this.imageId = imageId;
            return this;
        }

        public Builder setRepoTags(final List<String> repoTags) {
            this.repoTags = repoTags == null ?
                    Lists.<String>newArrayList():
                    repoTags;
            return this;
        }

        public Builder setLabels(final Map<String, String> labels) {
            this.labels = labels == null ?
                    Maps.<String, String>newHashMap() :
                    labels;
            return this;
        }

        public Builder setInDatabase(final Boolean inDatabase) {
            this.inDatabase = inDatabase;
            return this;
        }

        public Builder setOnDockerServer(final Boolean onDockerServer) {
            this.onDockerServer = onDockerServer;
            return this;
        }

    }
}
