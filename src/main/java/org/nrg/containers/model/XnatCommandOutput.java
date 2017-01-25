package org.nrg.containers.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

import javax.persistence.Embeddable;
import java.util.Objects;

@Embeddable
public class XnatCommandOutput {
    @JsonProperty("command-output-name") private String commandOutputName;
    private String label;

    public String getCommandOutputName() {
        return commandOutputName;
    }

    public void setCommandOutputName(final String commandOutputName) {
        this.commandOutputName = commandOutputName;
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
                Objects.equals(this.label, that.label);
    }

    @Override
    public int hashCode() {
        return Objects.hash(commandOutputName, label);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("commandOutputName", commandOutputName)
                .add("label", label)
                .toString();
    }
}
