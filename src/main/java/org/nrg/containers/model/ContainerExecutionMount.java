package org.nrg.containers.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import io.swagger.annotations.ApiModelProperty;
import org.nrg.containers.model.auto.Command;
import org.nrg.containers.model.auto.Command.CommandMount;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

@Entity
public class ContainerExecutionMount implements Serializable {
    private long id;
    @JsonProperty(required = true) private String name;
    @JsonProperty("writable") private boolean writable;
    @JsonProperty("xnat-host-path") private String xnatHostPath;
    @JsonProperty("container-host-path") private String containerHostPath;
    @JsonProperty("container-path") private String containerPath;
    @JsonProperty("input-files") private List<ContainerMountFiles> inputFiles;

    public ContainerExecutionMount() {}

    public ContainerExecutionMount(final CommandMount commandMount) {
        this.name = commandMount.name();
        this.writable = commandMount.writable();
        this.xnatHostPath = null;        // Intentionally blank. Will be set later.
        this.containerHostPath = null;   // Intentionally blank. Will be set later.
        this.containerPath = null;       // Intentionally blank. Will be set later.
        this.inputFiles = null;          // Intentionally blank. Will be set later.
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public long getId() {
        return id;
    }

    public void setId(final long id) {
        this.id = id;
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

    public String getXnatHostPath() {
        return xnatHostPath;
    }

    public void setXnatHostPath(final String xnatHostPath) {
        this.xnatHostPath = xnatHostPath;
    }

    public String getContainerHostPath() {
        return containerHostPath;
    }

    public void setContainerHostPath(final String containerHostPath) {
        this.containerHostPath = containerHostPath;
    }

    public String getContainerPath() {
        return containerPath;
    }

    public void setContainerPath(final String containerPath) {
        this.containerPath = containerPath;
    }

    @Transient
    @JsonIgnore
    public boolean isWritable() {
        return writable;
    }

    @ElementCollection
    public List<ContainerMountFiles> getInputFiles() {
        return inputFiles;
    }

    public void setInputFiles(final List<ContainerMountFiles> inputFiles) {
        this.inputFiles = inputFiles == null ?
                Lists.<ContainerMountFiles>newArrayList() :
                inputFiles;
    }

    @Transient
    @ApiModelProperty(hidden = true)
    public String toBindMountString() {
        return containerHostPath + ":" + containerPath + (writable ? "" : ":ro");
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final ContainerExecutionMount that = (ContainerExecutionMount) o;
        return Objects.equals(this.id, that.id) &&
                Objects.equals(this.name, that.name) &&
                Objects.equals(this.writable, that.writable) &&
                Objects.equals(this.xnatHostPath, that.xnatHostPath) &&
                Objects.equals(this.containerHostPath, that.containerHostPath) &&
                Objects.equals(this.containerPath, that.containerPath) &&
                Objects.equals(this.inputFiles, that.inputFiles);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, writable, xnatHostPath, containerHostPath, containerPath, inputFiles);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("name", name)
                .add("writable", writable)
                .add("xnatHostPath", xnatHostPath)
                .add("containerHostPath", containerHostPath)
                .add("remotePath", containerPath)
                .add("inputFiles", inputFiles)
                .toString();
    }
}