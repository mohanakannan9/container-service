package org.nrg.actions.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.Objects;

public class ActionDto {
    private Long id;
    private String name;
    private String description;
    @JsonProperty("command-id") private Long commandId;
    private ActionRoot root;
    private List<ActionInput> inputs = Lists.newArrayList();
    @JsonProperty("resources-staged") private List<ActionResource> resourcesStaged = Lists.newArrayList();
    @JsonProperty("resources-created") private List<ActionResource> resourcesCreated = Lists.newArrayList();

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
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

    public Long getCommandId() {
        return commandId;
    }

    public void setCommandId(final Long commandId) {
        this.commandId = commandId;
    }

    public ActionRoot getRoot() {
        return root;
    }

    public void setRoot(final ActionRoot root) {
        this.root = root;
    }

    public List<ActionInput> getInputs() {
        return inputs;
    }

    public void setInputs(final List<ActionInput> inputs) {
        this.inputs = inputs == null ?
                Lists.<ActionInput>newArrayList() :
                Lists.newArrayList(inputs);
    }

    public List<ActionResource> getResourcesStaged() {
        return resourcesStaged;
    }

    public void setResourcesStaged(final List<ActionResource> resourcesStaged) {
        this.resourcesStaged = resourcesStaged == null ?
                Lists.<ActionResource>newArrayList() :
                Lists.newArrayList(resourcesStaged);
    }

    public void addResourceStaged(final ActionResource staged) {
        if (this.resourcesStaged == null) {
            this.resourcesStaged = Lists.newArrayList();
        }
        this.resourcesStaged.add(staged);
    }

    public List<ActionResource> getResourcesCreated() {
        return resourcesCreated;
    }

    public void setResourcesCreated(final List<ActionResource> resourcesCreated) {
        this.resourcesCreated = resourcesCreated == null ?
                Lists.<ActionResource>newArrayList() :
                Lists.newArrayList(resourcesCreated);
    }

    public void addResourceCreated(final ActionResource created) {
        if (this.resourcesCreated == null) {
            this.resourcesCreated = Lists.newArrayList();
        }
        this.resourcesCreated.add(created);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final ActionDto actionDto = (ActionDto) o;
        return Objects.equals(this.id, actionDto.id) &&
                Objects.equals(this.name, actionDto.name) &&
                Objects.equals(this.description, actionDto.description) &&
                Objects.equals(this.commandId, actionDto.commandId) &&
                Objects.equals(this.root, actionDto.root) &&
                Objects.equals(this.inputs, actionDto.inputs) &&
                Objects.equals(this.resourcesStaged, actionDto.resourcesStaged) &&
                Objects.equals(this.resourcesCreated, actionDto.resourcesCreated);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, description, commandId, root, inputs, resourcesStaged, resourcesCreated);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("name", name)
                .add("description", description)
                .add("commandId", commandId)
                .add("root", root)
                .add("inputs", inputs)
                .add("staged", resourcesStaged)
                .add("created", resourcesCreated)
                .toString();
    }
}
