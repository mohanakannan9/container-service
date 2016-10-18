package org.nrg.containers.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import io.swagger.annotations.ApiModelProperty;

import javax.persistence.Embeddable;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class CommandOutput implements Serializable {
    private String name;
    private String description;
    private Type type;
    private String label;
    private Boolean required;
    private String parent;
    private CommandOutputFiles files;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public Type getType() {
        return type;
    }

    public void setType(final Type type) {
        this.type = type;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(final String label) {
        this.label = label;
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

    public String getParent() {
        return parent;
    }

    public void setParent(final String parent) {
        this.parent = parent;
    }

    public CommandOutputFiles getFiles() {
        return files;
    }

    public void setFiles(final CommandOutputFiles files) {
        this.files = files;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final CommandOutput that = (CommandOutput) o;
        return Objects.equals(this.name, that.name) &&
                Objects.equals(this.description, that.description) &&
                Objects.equals(this.label, that.label) &&
                Objects.equals(this.required, that.required) &&
                Objects.equals(this.parent, that.parent) &&
                Objects.equals(this.files, that.files);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, label, required, parent, files);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("name", name)
                .add("description", description)
                .add("type", label)
                .add("required", required)
                .add("parent", parent)
                .add("files", files)
                .toString();
    }

    public enum Type {
        @JsonProperty("Resource") RESOURCE,
        @JsonProperty("Assessor") ASSESSOR
    }
}