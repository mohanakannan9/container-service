package org.nrg.actions.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

import javax.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class ActionOutput implements Serializable {

    private String name;
    @JsonProperty("command-output-name") private String commandOutputName;
    @JsonProperty("resource-name") private String resourceName;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCommandOutputName() {
        return commandOutputName;
    }

    public void setCommandOutputName(String commandOutputName) {
        if (this.name == null) {
            this.name = commandOutputName;
        }
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
        return Objects.equals(this.name, that.name) &&
                Objects.equals(this.commandOutputName, that.commandOutputName) &&
                Objects.equals(this.resourceName, that.resourceName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, commandOutputName, resourceName);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("name", name)
                .add("commandOutputName", commandOutputName)
                .add("resourceName", resourceName)
                .toString();
    }
}
