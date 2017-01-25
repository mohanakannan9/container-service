package org.nrg.containers.model;

import com.google.common.base.MoreObjects;

import javax.persistence.Embeddable;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class CommandOutput implements Serializable {
    private String name;
    private String description;
    // private OutputType type;
    // private String label;
    private Boolean required;
    // private String parent;
    // private CommandOutputFiles files;
    private String mount;
    private String path;
    private String glob;

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

    // public OutputType getType() {
    //     return type;
    // }
    //
    // public void setType(final OutputType type) {
    //     this.type = type;
    // }
    //
    // public String getLabel() {
    //     return label;
    // }
    //
    // public void setLabel(final String label) {
    //     this.label = label;
    // }

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

    // public String getParent() {
    //     return parent;
    // }
    //
    // public void setParent(final String parent) {
    //     this.parent = parent;
    // }
    //
    // public CommandOutputFiles getFiles() {
    //     return files;
    // }
    //
    // public void setFiles(final CommandOutputFiles files) {
    //     this.files = files;
    // }

    @Transient
    void update(final CommandOutput other, final Boolean ignoreNull) {
        // if (other == null) {
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
        //     // intentional change to Type.STRING and an attempt to not change.
        //     return;
        // }
        //
        // if (other.description != null || !ignoreNull) {
        //     this.description = other.description;
        // }
        // if (other.label != null || !ignoreNull) {
        //     this.label = other.label;
        // }
        // if (other.required != null || !ignoreNull) {
        //     this.required = other.required;
        // }
        // if (other.parent != null || !ignoreNull) {
        //     this.parent = other.parent;
        // }
        //
        // if (this.files == null || (other.files == null && !ignoreNull)) {
        //     this.files = other.files;
        // } else {
        //     this.files.update(other.files, ignoreNull);
        // }
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final CommandOutput that = (CommandOutput) o;
        return Objects.equals(this.name, that.name) &&
                Objects.equals(this.description, that.description) &&
                // Objects.equals(this.type, that.type) &&
                // Objects.equals(this.label, that.label) &&
                Objects.equals(this.required, that.required) &&
                // Objects.equals(this.parent, that.parent) &&
                // Objects.equals(this.files, that.files);
                Objects.equals(this.mount, that.mount) &&
                Objects.equals(this.path, that.path) &&
                Objects.equals(this.glob, that.glob);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, required, //type, label, required, parent, files);
                mount, path, glob);

    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("name", name)
                .add("description", description)
                // .add("type", type)
                // .add("label", label)
                .add("required", required)
                // .add("parent", parent)
                // .add("files", files)
                .add("mount", mount)
                .add("path", path)
                .add("glob", glob)
                .toString();
    }
}