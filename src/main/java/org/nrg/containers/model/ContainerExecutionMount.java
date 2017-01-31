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
public class ContainerExecutionMount implements Serializable {

    @JsonProperty(required = true) private String name;
    @JsonProperty("writable") private boolean writable;
    @JsonProperty("host-path") private String hostPath;
    @JsonProperty("path") private String remotePath;
    @JsonProperty("file-input") private String fileInput;
    private String resource;

    public ContainerExecutionMount() {}

    public ContainerExecutionMount(final CommandMount commandMount) {
        this.name = commandMount.getName();
        this.writable = commandMount.getWritable();
        this.hostPath = null; // Intentionally blank. Will be set later.
        this.remotePath = commandMount.getRemotePath();

        // TODO pass an XnatCommandOutput to this constructor, pull these properties from there
        // this.fileInput = commandMount.getFileInput();
        // this.resource = commandMount.getResource();
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public boolean getWritable() {
        return writable;
    }

    public void setWritable(final boolean writable) {
        this.writable = writable;
    }

    public String getHostPath() {
        return hostPath;
    }

    public void setHostPath(final String localPath) {
        this.hostPath = localPath;
    }

    public String getRemotePath() {
        return remotePath;
    }

    public void setRemotePath(final String remotePath) {
        this.remotePath = remotePath;
    }

    @Transient
    @JsonIgnore
    public boolean isWritable() {
        return writable;
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
    public String toBindMountString() {
        return hostPath + ":" + remotePath + (writable ? "" : ":ro");
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final ContainerExecutionMount that = (ContainerExecutionMount) o;
        return Objects.equals(this.name, that.name) &&
                Objects.equals(this.writable, that.writable) &&
                Objects.equals(this.hostPath, that.hostPath) &&
                Objects.equals(this.remotePath, that.remotePath) &&
                Objects.equals(this.fileInput, that.fileInput) &&
                Objects.equals(this.resource, that.resource);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, writable, hostPath, remotePath, fileInput, resource);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("name", name)
                .add("writable", writable)
                .add("hostPath", hostPath)
                .add("remotePath", remotePath)
                .add("fileInput", fileInput)
                .add("resource", resource)
                .toString();
    }
}