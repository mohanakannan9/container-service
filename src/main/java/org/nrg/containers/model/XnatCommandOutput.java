package org.nrg.containers.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.base.MoreObjects;

import javax.persistence.Embeddable;
import java.util.Objects;

@Embeddable
public class XnatCommandOutput {
    @JsonProperty("accepts-command-output") private String commandOutputName;
    @JsonProperty("as-child-of-xnat-input-object") private String xnatInputName;
    private Type type;
    private String label;

    public Type getType() {
        return type;
    }

    public void setType(final Type type) {
        this.type = type;
    }

    public String getCommandOutputName() {
        return commandOutputName;
    }

    public void setCommandOutputName(final String commandOutputName) {
        this.commandOutputName = commandOutputName;
    }

    public String getXnatInputName() {
        return xnatInputName;
    }

    public void setXnatInputName(final String xnatInputName) {
        this.xnatInputName = xnatInputName;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(final String label) {
        this.label = label;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final XnatCommandOutput that = (XnatCommandOutput) o;
        return Objects.equals(this.commandOutputName, that.commandOutputName) &&
                Objects.equals(this.type, that.type) &&
                Objects.equals(this.xnatInputName, that.xnatInputName) &&
                Objects.equals(this.label, that.label);
    }

    @Override
    public int hashCode() {
        return Objects.hash(commandOutputName, type, xnatInputName, label);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("commandOutputName", commandOutputName)
                .add("xnatInputName", xnatInputName)
                .add("type", type)
                .add("label", label)
                .toString();
    }

    public enum Type {
        RESOURCE("Resource"),
        ASSESSOR("Assessor");

        private final String name;

        @JsonCreator
        Type(final String name) {
            this.name = name;
        }

        @JsonValue
        public String getName() {
            return name;
        }
    }
}
