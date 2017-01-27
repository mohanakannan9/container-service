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
    // @JsonProperty(required = true) private Type type = Type.INPUT;
    private Boolean writable;
    @JsonProperty("path") private String remotePath;
    // @JsonProperty("file-input") private String fileInput;
    // private String resource;

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    // @ApiModelProperty(value = "Type of mount: input or output.", allowableValues = "input, output")
    // public Type getType() {
    //     return type;
    // }
    //
    // public void setType(final Type type) {
    //     this.type = type;
    // }

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

    // @Transient
    // @JsonIgnore
    // public boolean isInput() {
    //     return type.equals(Type.INPUT);
    // }
    //
    // public String getFileInput() {
    //     return fileInput;
    // }
    //
    // public void setFileInput(final String fileInput) {
    //     this.fileInput = fileInput;
    // }

    // public String getResource() {
    //     return resource;
    // }
    //
    // public void setResource(final String resource) {
    //     this.resource = resource;
    // }

    @Transient
    @ApiModelProperty(hidden = true)
    public String toBindMountString(final String hostPath) {
        // return hostPath + ":" + remotePath + (isInput()?":ro":"");
        return hostPath + ":" + remotePath + (writable?"":":ro");
    }

    @Transient
    void update(final CommandMount other, final Boolean ignoreNull) {
        // if (other == null) {
        //     // This should not happen. Caller should check for null before calling.
        //     return;
        // }
        //
        // if (!(StringUtils.isNotBlank(other.name) && this.name.equals(other.name))) {
        //     // We can't change the name. That's the identifier.
        //     // How did you even get here with differently-named objects?
        //     return;
        // }
        //
        // if (!this.type.equals(other.type)) {
        //     // We can't change the type.
        //     // It has a non-null default, so there is no good way to discriminate between an
        //     // intentional change to Type.INPUT and an attempt to not change.
        //     return;
        // }
        //
        // if (!(other.remotePath == null && ignoreNull)) {
        //     this.remotePath = other.remotePath;
        // }
        // if (!(other.fileInput == null && ignoreNull)) {
        //     this.fileInput = other.fileInput;
        // }
        // // if (!(other.resource == null && ignoreNull)) {
        // //     this.resource = other.resource;
        // // }
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
        // return Objects.hash(name, type, remotePath, fileInput, resource);
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

    // public enum Type {
    //     @JsonProperty("input") INPUT,
    //     @JsonProperty("output") OUTPUT
    // }
}