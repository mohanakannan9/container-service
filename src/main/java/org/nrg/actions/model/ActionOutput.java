package org.nrg.actions.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

import javax.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class ActionOutput implements Serializable {

    @JsonProperty("command-output-name") private String commandOutputName;
    @JsonProperty("resource-name") private String resourceName;

    public String getCommandOutputName() {
        return commandOutputName;
    }

    public void setCommandOutputName(String commandOutputName) {
        this.commandOutputName = commandOutputName;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ActionOutput that = (ActionOutput) o;
        return Objects.equals(commandOutputName, that.commandOutputName) &&
                Objects.equals(resourceName, that.resourceName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(commandOutputName, resourceName);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("commandOutputName", commandOutputName)
                .add("resourceName", resourceName)
                .toString();
    }
}
