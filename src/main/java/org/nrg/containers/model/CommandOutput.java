package org.nrg.containers.model;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.nrg.containers.model.auto.CommandPojo;

import javax.persistence.Embeddable;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

@Embeddable
public class CommandOutput implements Serializable {
    private String name;
    private String description;
    private Boolean required;
    private String mount;
    private String path;
    private String glob;

    public static CommandOutput fromPojo(final CommandPojo.CommandOutputPojo commandOutputPojo) {
        final CommandOutput commandOutput = new CommandOutput();
        commandOutput.name = commandOutputPojo.name();
        commandOutput.description = commandOutputPojo.description();
        commandOutput.required = commandOutputPojo.required();
        commandOutput.mount = commandOutputPojo.mount();
        commandOutput.path = commandOutputPojo.path();
        commandOutput.glob = commandOutputPojo.glob();
        return commandOutput;
    }

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

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final CommandOutput that = (CommandOutput) o;
        return Objects.equals(this.name, that.name) &&
                Objects.equals(this.description, that.description) &&
                Objects.equals(this.required, that.required) &&
                Objects.equals(this.mount, that.mount) &&
                Objects.equals(this.path, that.path) &&
                Objects.equals(this.glob, that.glob);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, required, mount, path, glob);

    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("name", name)
                .add("description", description)
                .add("required", required)
                .add("mount", mount)
                .add("path", path)
                .add("glob", glob)
                .toString();
    }
}