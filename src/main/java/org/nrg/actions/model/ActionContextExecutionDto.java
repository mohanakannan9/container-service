package org.nrg.actions.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.Objects;

public class ActionContextExecutionDto {
    private String name;
    private String description;
    @JsonProperty("action-id") private Long actionId;
    @JsonProperty("command-id") private Long commandId;
    @JsonProperty("project") private String project;
    @JsonProperty("root-id") private String rootId;
    private List<ActionInput> inputs;
    @JsonProperty("resources-staged") private List<ActionResource> resourcesStaged;
    @JsonProperty("resources-created") private List<ActionResource> resourcesCreated;

    public ActionContextExecutionDto() {}

    public ActionContextExecutionDto(final Action action, final String rootId) {
        if (action == null) {
            return;
        }
        this.name = action.getName();
        this.description = action.getDescription();
        this.actionId = action.getId();
        this.commandId = action.getCommand() != null ?
                action.getCommand().getId() : null;

        this.inputs = action.getInputs();
        this.resourcesCreated = action.getResourcesCreated();
        this.resourcesStaged = action.getResourcesStaged();

        this.rootId = rootId;
    }

    public ActionContextExecutionDto(final Action action) {
        this(action, null);
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public Long getActionId() {
        return actionId;
    }

    public void setActionId(final Long actionId) {
        this.actionId = actionId;
    }

    public Long getCommandId() {
        return commandId;
    }

    public void setCommandId(final Long commandId) {
        this.commandId = commandId;
    }

    public String getRootId() {
        return rootId;
    }

    public void setRootId(final String rootId) {
        this.rootId = rootId;
    }

    public List<ActionInput> getInputs() {
        return inputs;
    }

    public void setInputs(final List<ActionInput> inputs) {
        this.inputs = inputs;
    }

    public List<ActionResource> getResourcesStaged() {
        return resourcesStaged;
    }

    public void setResourcesStaged(final List<ActionResource> resourcesStaged) {
        this.resourcesStaged = resourcesStaged;
    }

    public void addStaged(final ActionResource staged) {
        if (this.resourcesStaged == null) {
            this.resourcesStaged = Lists.newArrayList();
        }
        this.resourcesStaged.add(staged);
    }

    public List<ActionResource> getResourcesCreated() {
        return resourcesCreated;
    }

    public void setResourcesCreated(final List<ActionResource> resourcesCreated) {
        this.resourcesCreated = resourcesCreated;
    }

    public void addCreated(final ActionResource created) {
        if (this.resourcesCreated == null) {
            this.resourcesCreated = Lists.newArrayList();
        }
        this.resourcesCreated.add(created);
    }

    public String getProject() {
        return project;
    }

    public void setProject(final String project) {
        this.project = project;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final ActionContextExecutionDto that = (ActionContextExecutionDto) o;
        return Objects.equals(this.name, that.name) &&
                Objects.equals(this.description, that.description) &&
                Objects.equals(this.actionId, that.actionId) &&
                Objects.equals(this.commandId, that.commandId) &&
                Objects.equals(this.project, that.project) &&
                Objects.equals(this.rootId, that.rootId) &&
                Objects.equals(this.inputs, that.inputs) &&
                Objects.equals(this.resourcesStaged, that.resourcesStaged) &&
                Objects.equals(this.resourcesCreated, that.resourcesCreated);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, actionId, commandId, project, rootId, inputs, resourcesStaged, resourcesCreated);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("name", name)
                .add("description", description)
                .add("actionId", actionId)
                .add("commandId", commandId)
                .add("project", project)
                .add("rootId", rootId)
                .add("inputs", inputs)
                .add("resourcesStaged", resourcesStaged)
                .add("resourcesCreated", resourcesCreated)
                .toString();
    }
}
