package org.nrg.containers.model;

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
    private Boolean writable;
    @JsonProperty("path") private String remotePath;

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public Boolean isWritable() {
        return writable;
    }

    public Boolean getWritable() {
        return writable;
    }

    public void setWritable(final Boolean writable) {
        this.writable = writable;
    }

    public String getRemotePath() {
        return remotePath;
    }

    public void setRemotePath(final String remotePath) {
        this.remotePath = remotePath;
    }


    @Transient
    @ApiModelProperty(hidden = true)
    public String toBindMountString(final String hostPath) {
        // return hostPath + ":" + remotePath + (isInput()?":ro":"");
        return hostPath + ":" + remotePath + (writable?"":":ro");
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final CommandMount that = (CommandMount) o;
        return Objects.equals(this.name, that.name) &&
                // Objects.equals(this.type, that.type) &&
                Objects.equals(this.writable, that.writable) &&
                Objects.equals(this.remotePath, that.remotePath); // &&
                // Objects.equals(this.fileInput, that.fileInput) &&
                // Objects.equals(this.resource, that.resource);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, writable, remotePath);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("name", name)
                // .add("type", type)
                .add("writable", writable)
                .add("remotePath", remotePath)
                // .add("fileInput", fileInput)
                // .add("resource", resource)
                .toString();
    }
}