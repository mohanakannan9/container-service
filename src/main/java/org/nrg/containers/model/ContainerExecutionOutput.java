package org.nrg.containers.model;

import com.google.common.base.MoreObjects;
import org.nrg.containers.model.auto.Command;
import org.nrg.containers.model.auto.Command.CommandOutput;
import org.nrg.containers.model.auto.Command.CommandWrapperOutput;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class ContainerExecutionOutput implements Serializable {
    private String name;
    private String type;
    private Boolean required;
    private String mount;
    private String path;
    private String glob;
    private String label;
    private String created;
    private String handledByXnatCommandInput;

    public ContainerExecutionOutput() {}

    public ContainerExecutionOutput(final CommandOutput commandOutput, final CommandWrapperOutput commandOutputHandler) {
        this.name = commandOutput.name();
        this.required = commandOutput.required();
        this.mount = commandOutput.mount();
        this.path = commandOutput.path();
        this.glob = commandOutput.glob();
        this.label = commandOutputHandler.label();
        this.type = commandOutputHandler.type();
        this.handledByXnatCommandInput = commandOutputHandler.xnatInputName();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
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

    public String getHandledByXnatCommandInput() {
        return handledByXnatCommandInput;
    }

    public void setHandledByXnatCommandInput(final String handledByXnatCommandInput) {
        this.handledByXnatCommandInput = handledByXnatCommandInput;
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
                Objects.equals(this.type, that.type) &&
                Objects.equals(this.required, that.required) &&
                Objects.equals(this.mount, that.mount) &&
                Objects.equals(this.path, that.path) &&
                Objects.equals(this.glob, that.glob) &&
                Objects.equals(this.label, that.label) &&
                Objects.equals(this.handledByXnatCommandInput, that.handledByXnatCommandInput) &&
                Objects.equals(this.created, that.created);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type, required, mount, path, glob, label, handledByXnatCommandInput, created);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("name", name)
                .add("type", type)
                .add("required", required)
                .add("mount", mount)
                .add("path", path)
                .add("glob", glob)
                .add("label", label)
                .add("handledByxnatInput", handledByXnatCommandInput)
                .add("created", created)
                .toString();
    }
}