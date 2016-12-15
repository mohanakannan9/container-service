package org.nrg.containers.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import io.swagger.annotations.ApiModelProperty;

import javax.persistence.Embeddable;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class CommandMount implements Serializable {

    @JsonProperty(required = true) private String name;
    @JsonProperty(required = true) private Type type = Type.INPUT;
    @JsonProperty("path") private String remotePath;
    @JsonProperty("file-input") private String fileInput;
    private String resource;

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    @ApiModelProperty(value = "Type of mount: input or output.", allowableValues = "input, output")
    public Type getType() {
        return type;
    }

    public void setType(final Type type) {
        this.type = type;
    }

    public String getRemotePath() {
        return remotePath;
    }

    public void setRemotePath(final String remotePath) {
        this.remotePath = remotePath;
    }

    @Transient
    @JsonIgnore
    public boolean isInput() {
        return type.equals(Type.INPUT);
    }

    public String getFileInput() {
        return fileInput;
    }

    public void setFileInput(final String fileInput) {
        this.fileInput = fileInput;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(final String resource) {
        this.resource = resource;
    }

    @Transient
    @ApiModelProperty(hidden = true)
    public String toBindMountString(final String hostPath) {
        return hostPath + ":" + remotePath + (isInput()?":ro":"");
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final CommandMount that = (CommandMount) o;
        return Objects.equals(this.name, that.name) &&
                Objects.equals(this.type, that.type) &&
                Objects.equals(this.remotePath, that.remotePath) &&
                Objects.equals(this.fileInput, that.fileInput) &&
                Objects.equals(this.resource, that.resource);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type, remotePath, fileInput, resource);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("name", name)
                .add("type", type)
                .add("remotePath", remotePath)
                .add("fileInput", fileInput)
                .add("resource", resource)
                .toString();
    }

    public enum Type {
        @JsonProperty("input") INPUT,
        @JsonProperty("output") OUTPUT
    }
}