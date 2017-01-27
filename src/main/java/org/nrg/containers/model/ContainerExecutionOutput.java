package org.nrg.containers.model;

import com.google.common.base.MoreObjects;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class ContainerExecutionOutput implements Serializable {
    private String name;
    private Boolean required;
    private String mount;
    private String path;
    private String glob;
    private String label;
    private String created;

    public ContainerExecutionOutput() {}

    public ContainerExecutionOutput(final CommandOutput commandOutput, final XnatCommandOutput commandOutputHandler) {
        this.name = commandOutput.getName();
        this.required = commandOutput.getRequired();
        this.mount = commandOutput.getMount();
        this.path = commandOutput.getPath();
        this.glob = commandOutput.getGlob();
        this.label = commandOutputHandler.getLabel();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getRequired() {
        return required;
    }

    @Transient
    public boolean isRequired() {
        return required != null && required;
    }

    public void setRequired(final Boolean required) {
        this.required = required;
    }

    public String getMount() {
        return mount;
    }

    public void setMount(final String mount) {
        this.mount = mount;
    }

    public String getPath() {
        return path;
    }

    public void setPath(final String path) {
        this.path = path;
    }

    public String getGlob() {
        return glob;
    }

    public void setGlob(final String glob) {
        this.glob = glob;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(final String label) {
        this.label = label;
    }

    @Column(columnDefinition = "TEXT")
    public String getCreated() {
        return created;
    }

    public void setCreated(final String created) {
        this.created = created;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final ContainerExecutionOutput that = (ContainerExecutionOutput) o;
        return Objects.equals(this.name, that.name) &&
                Objects.equals(this.required, that.required) &&
                Objects.equals(this.mount, that.mount) &&
                Objects.equals(this.path, that.path) &&
                Objects.equals(this.glob, that.glob) &&
                Objects.equals(this.label, that.label) &&
                Objects.equals(this.created, that.created);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, required, mount, path, glob, label, created);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("name", name)
                .add("required", required)
                .add("mount", mount)
                .add("path", path)
                .add("glob", glob)
                .add("label", label)
                .add("created", created)
                .toString();
    }
}