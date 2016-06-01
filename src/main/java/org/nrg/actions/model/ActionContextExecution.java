package org.nrg.actions.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntity;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import java.util.List;
import java.util.Objects;

@Entity
public class ActionContextExecution extends AbstractHibernateEntity {
    private String name;
    private String description;
    @JsonProperty("action-id") private Long actionId;
    @JsonProperty("project") private String project;
    @JsonProperty("root-id") private String rootId;
    private List<ActionInput> inputs = Lists.newArrayList();
    @JsonProperty("resources-staged") private List<ActionResource> resourcesStaged = Lists.newArrayList();
    @JsonProperty("resources-created") private List<ActionResource> resourcesCreated = Lists.newArrayList();
    @JsonProperty("resolved-command") private ResolvedCommand resolvedCommand;
    @JsonProperty("container-id") private String containerId;

    public ActionContextExecution() {}

    public ActionContextExecution(final ActionContextExecutionDto aceDto,
                                  final ResolvedCommand resolvedCommand) {
        if (aceDto == null) {
            return;
        }
        this.name = aceDto.getName();
        this.description = aceDto.getDescription();
        this.actionId = aceDto.getActionId();
        this.rootId = aceDto.getRootId();
        setInputs(aceDto.getInputs());
        setResourcesCreated(aceDto.getResourcesCreated());
        setResourcesStaged(aceDto.getResourcesStaged());

        this.resolvedCommand = resolvedCommand;
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

    public String getRootId() {
        return rootId;
    }

    public void setRootId(final String rootId) {
        this.rootId = rootId;
    }

    @ElementCollection(fetch = FetchType.EAGER)
    public List<ActionInput> getInputs() {
        return inputs;
    }

    public void setInputs(final List<ActionInput> inputs) {
        this.inputs = inputs == null ?
                Lists.<ActionInput>newArrayList() :
                Lists.newArrayList(inputs);
    }

    @ElementCollection(fetch = FetchType.EAGER)
    public List<ActionResource> getResourcesStaged() {
        return resourcesStaged;
    }

    public void setResourcesStaged(final List<ActionResource> resourcesStaged) {
        this.resourcesStaged = resourcesStaged == null ?
                Lists.<ActionResource>newArrayList() :
                Lists.newArrayList(resourcesStaged);
    }

    @ElementCollection(fetch = FetchType.EAGER)
    public List<ActionResource> getResourcesCreated() {
        return resourcesCreated;
    }

    public void setResourcesCreated(final List<ActionResource> resourcesCreated) {
        this.resourcesCreated = resourcesCreated == null ?
                Lists.<ActionResource>newArrayList() :
                Lists.newArrayList(resourcesCreated);
    }

    public ResolvedCommand getResolvedCommand() {
        return resolvedCommand;
    }

    public void setResolvedCommand(final ResolvedCommand resolvedCommand) {
        this.resolvedCommand = resolvedCommand;
    }

    public String getProject() {
        return project;
    }

    public void setProject(final String project) {
        this.project = project;
    }

    public String getContainerId() {
        return containerId;
    }

    public void setContainerId(final String containerId) {
        this.containerId = containerId;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        final ActionContextExecution that = (ActionContextExecution) o;
        return Objects.equals(this.name, that.name) &&
                Objects.equals(this.description, that.description) &&
                Objects.equals(this.actionId, that.actionId) &&
                Objects.equals(this.project, that.project) &&
                Objects.equals(this.rootId, that.rootId) &&
                Objects.equals(this.inputs, that.inputs) &&
                Objects.equals(this.resourcesStaged, that.resourcesStaged) &&
                Objects.equals(this.resourcesCreated, that.resourcesCreated) &&
                Objects.equals(this.resolvedCommand, that.resolvedCommand) &&
                Objects.equals(this.containerId, that.containerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), name, description, actionId, project, rootId, inputs, resourcesStaged, resourcesCreated, resolvedCommand, containerId);
    }

    @Override
    public String toString() {
        return addParentPropertiesToString(MoreObjects.toStringHelper(this))
                .add("name", name)
                .add("description", description)
                .add("actionId", actionId)
                .add("project", project)
                .add("rootId", rootId)
                .add("inputs", inputs)
                .add("resourcesStaged", resourcesStaged)
                .add("resourcesCreated", resourcesCreated)
                .add("resolvedCommand", resolvedCommand)
                .add("containerId", containerId)
                .toString();
    }
}
