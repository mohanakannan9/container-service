package org.nrg.execution.model;

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
    @JsonProperty("root-xsi-type") private String rootXsiType;
    @JsonProperty("root-matchers") private List<Matcher> rootMatchers = Lists.newArrayList();
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

    public String getRootXsiType() {
        return rootXsiType;
    }

    public void setRootXsiType(final String rootXsiType) {
        this.rootXsiType = rootXsiType;
    }

    public List<Matcher> getRootMatchers() {
        return rootMatchers;
    }

    public void setRootMatchers(final List<Matcher> rootMatchers) {
        this.rootMatchers = rootMatchers == null ?
                Lists.<Matcher>newArrayList() :
                Lists.newArrayList(rootMatchers);
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
        final ActionDto that = (ActionDto) o;
        return Objects.equals(this.id, that.id) &&
                Objects.equals(this.name, that.name) &&
                Objects.equals(this.description, that.description) &&
                Objects.equals(this.commandId, that.commandId) &&
                Objects.equals(this.rootXsiType, that.rootXsiType) &&
                Objects.equals(this.rootMatchers, that.rootMatchers) &&
                Objects.equals(this.inputs, that.inputs) &&
                Objects.equals(this.resourcesStaged, that.resourcesStaged) &&
                Objects.equals(this.resourcesCreated, that.resourcesCreated);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, description, commandId, rootXsiType, rootMatchers, inputs, resourcesStaged, resourcesCreated);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("name", name)
                .add("description", description)
                .add("commandId", commandId)
                .add("rootXiType", rootXsiType)
                .add("rootMatchers", rootMatchers)
                .add("inputs", inputs)
                .add("staged", resourcesStaged)
                .add("created", resourcesCreated)
                .toString();
    }
}
