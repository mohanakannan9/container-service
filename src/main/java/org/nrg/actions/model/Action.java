package org.nrg.actions.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.envers.Audited;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntity;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Audited
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "nrg")
public class Action extends AbstractHibernateEntity {
    private String name;
    private String description;
    private Command command;
    private ActionRoot root;
    private List<ActionInput> inputs;
    @JsonProperty("resources-staged") private List<ActionResource> resourcesStaged;
    @JsonProperty("resources-created") private List<ActionResource> resourcesCreated;

    private Map<String, ActionInput> inputsByCommandVariableName = Maps.newHashMap();
    private int inputCachedHash;
    private Map<String, ActionResource> stagedByMountName = Maps.newHashMap();
    private int stagedCacheHash;
    private Map<String, ActionResource> createdByMountName = Maps.newHashMap();
    private int createdCacheHash;

    public Action() {}

    public Action(final ActionDto dto, final Command command) {
        this.name = dto.getName();
        this.description = dto.getDescription();
        this.inputs = dto.getInputs() == null ? Lists.<ActionInput>newArrayList() : dto.getInputs();
        this.resourcesStaged = dto.getResourcesStaged();
        this.resourcesCreated = dto.getResourcesCreated();
        this.root = dto.getRoot();

        this.command = command;

        // If there are any command variables that weren't referenced
        // explicitly as action inputs, create default inputs for them
        if (command != null && command.getVariables() != null) {
            final Map<String, ActionInput> inputMap =
                    getInputsByCommandVariableName() != null ?
                            getInputsByCommandVariableName() :
                            Maps.<String, ActionInput>newHashMap();
            for (final CommandVariable variable : command.getVariables()) {
                if (!inputMap.containsKey(variable.getName())) {
                    final ActionInput input = new ActionInput(variable);
                    this.inputs.add(input);
                    inputMap.put(variable.getName(), input);
                }
            }
        }

        // If there are any command mounts that weren't referenced
        // explicitly as action resources, create default resources for them
        if (command != null && command.getMountsIn() != null) {
            for (final CommandMount input : command.getMountsIn()) {
                if (getStagedByMountName(input.getName()) == null) {
                    final ActionResource staged = new ActionResource(input);
                    addResourceStaged(staged);
                }
            }
        }
        if (command != null && command.getMountsOut() != null) {
            for (final CommandMount output : command.getMountsOut()) {
                if (getCreatedByMountName(output.getName()) == null) {
                    final ActionResource created = new ActionResource(output);
                    addResourceCreated(created);
                }
            }
        }
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

    @ManyToOne
    public Command getCommand() {
        return command;
    }

    public void setCommand(final Command command) {
        this.command = command;
    }

    @ElementCollection
    public List<ActionInput> getInputs() {
        return inputs;
    }

    public void setInputs(final List<ActionInput> inputs) {
        this.inputs = inputs;
    }

    public ActionRoot getRoot() {
        return root;
    }

    public void setRoot(final ActionRoot root) {
        this.root = root;
    }

    @ElementCollection
    public List<ActionResource> getResourcesStaged() {
        return resourcesStaged;
    }

    public void setResourcesStaged(final List<ActionResource> resourcesStaged) {
        this.resourcesStaged = resourcesStaged;
    }

    @Transient
    public void addResourceStaged(final ActionResource staged) {
        if (this.resourcesStaged == null) {
            this.resourcesStaged = Lists.newArrayList();
        }
        this.resourcesStaged.add(staged);

        this.stagedByMountName.put(staged.getMountName(), staged);
        this.stagedCacheHash = staged.hashCode();
    }


    @ElementCollection
    public List<ActionResource> getResourcesCreated() {
        return resourcesCreated;
    }

    public void setResourcesCreated(final List<ActionResource> resourcesCreated) {
        this.resourcesCreated = resourcesCreated;
    }

    @Transient
    public void addResourceCreated(final ActionResource created) {
        if (this.resourcesCreated == null) {
            this.resourcesCreated = Lists.newArrayList();
        }
        this.resourcesCreated.add(created);

        this.createdByMountName.put(created.getMountName(), created);
        this.createdCacheHash = created.hashCode();
    }

    @Transient
    public CommandVariable getCommandInput(final ActionInput actionInput) {
        return getCommandVariableByName(actionInput.getCommandVariableName());
    }

    @Transient
    public CommandVariable getCommandVariableByName(final String name) {
        for (final CommandVariable commandVariable : command.getVariables()) {
            if (commandVariable.getName().equals(name)) {
                return commandVariable;
            }
        }
        return null;
    }

    @Transient
    private Map<String, ActionInput> getInputsByCommandVariableName() {
        if (inputs == null) {
            inputCachedHash = 0;
            return Maps.newHashMap();
        }
        if (inputCachedHash != inputs.hashCode() || inputsByCommandVariableName == null || inputsByCommandVariableName.isEmpty()) {
            final Map<String, ActionInput> map = Maps.newHashMap();
            for (final ActionInput input : inputs) {
                map.put(input.getInputName(), input);
            }
            inputsByCommandVariableName = map;
            inputCachedHash = inputs.hashCode();
        }
        return inputsByCommandVariableName;
    }

    @Transient
    private Map<String, ActionResource> getCreatedByMountNameMap() {
        if (resourcesCreated == null) {
            return Maps.newHashMap();
        }
        if (createdCacheHash != resourcesCreated.hashCode() ||
                createdByMountName == null ||
                createdByMountName.isEmpty()) {
            final Map<String, ActionResource> map = Maps.newHashMap();
            for (final ActionResource resource : resourcesCreated) {
                map.put(resource.getMountName(), resource);
            }
            createdByMountName = map;
            createdCacheHash = resourcesCreated.hashCode();
        }
        return createdByMountName;
    }

    @Transient
    private Map<String, ActionResource> getStagedByMountNameMap() {
        if (resourcesStaged == null) {
            return Maps.newHashMap();
        }
        if (stagedCacheHash!= resourcesStaged.hashCode() ||
                stagedByMountName == null ||
                stagedByMountName.isEmpty()) {
            final Map<String, ActionResource> map = Maps.newHashMap();
            for (final ActionResource resource : resourcesStaged) {
                map.put(resource.getMountName(), resource);
            }
            stagedByMountName = map;
            stagedCacheHash = resourcesStaged.hashCode();
        }
        return stagedByMountName;
    }

    @Transient
    public ActionResource getCreatedByMountName(final String mountName) {
        return getCreatedByMountNameMap().containsKey(mountName) ?
                getCreatedByMountNameMap().get(mountName) :
                null;
    }

    @Transient
    public ActionResource getStagedByMountName(final String mountName) {
        return getStagedByMountNameMap().containsKey(mountName) ?
                getStagedByMountNameMap().get(mountName) :
                null;
    }

    public void run() {
        // TODO
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        final Action action = (Action) o;
        return Objects.equals(this.name, action.name) &&
                Objects.equals(this.description, action.description) &&
                Objects.equals(this.command, action.command) &&
                Objects.equals(this.root, action.root) &&
                Objects.equals(this.inputs, action.inputs) &&
                Objects.equals(this.resourcesStaged, action.resourcesStaged) &&
                Objects.equals(this.resourcesCreated, action.resourcesCreated);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), name, description, command, root, inputs, resourcesStaged, resourcesCreated);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("name", name)
                .add("description", description)
                .add("command", command)
                .add("root", root)
                .add("inputs", inputs)
                .add("resourcesStaged", resourcesStaged)
                .add("resourcesCreated", resourcesCreated)
                .toString();
    }
}
