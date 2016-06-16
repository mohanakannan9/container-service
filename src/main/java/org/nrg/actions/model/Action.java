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
    @JsonProperty("root-xsi-type") private String rootXsiType;
    @JsonProperty("root-matchers") private List<Matcher> rootMatchers = Lists.newArrayList();
    private List<ActionInput> inputs = Lists.newArrayList();
    @JsonProperty("resources-staged") private List<ActionResource> resourcesStaged = Lists.newArrayList();
    @JsonProperty("resources-created") private List<ActionResource> resourcesCreated = Lists.newArrayList();

    private Map<String, ActionInput> inputsByCommandVariableName = Maps.newHashMap();
    private int inputCachedHash = 0;
    private Map<String, ActionResource> stagedByMountName = Maps.newHashMap();
    private int stagedCacheHash = 0;
    private Map<String, ActionResource> createdByMountName = Maps.newHashMap();
    private int createdCacheHash = 0;

    public Action() {}

    public Action(final ActionDto dto, final Command command) {
        this.name = dto.getName();
        this.description = dto.getDescription();
        setInputs(Lists.newArrayList(dto.getInputs()));
        setResourcesStaged(Lists.newArrayList(dto.getResourcesStaged()));
        setResourcesCreated(Lists.newArrayList(dto.getResourcesCreated()));
        this.rootXsiType = dto.getRootXsiType();
        setRootMatchers(Lists.newArrayList(dto.getRootMatchers()));

        this.command = command;

        if (command != null) {
            // If there are any command variables that weren't referenced
            // explicitly as action inputs, create default inputs for them
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

            // If there are any command mounts that weren't referenced
            // explicitly as action resources, create default resources for them
            for (final String mountName : command.getMountsIn().keySet()) {
                if (getStagedByMountName(mountName) == null) {
                    addResourceStaged(new ActionResource(mountName));
                }
            }

            for (final String mountName : command.getMountsOut().keySet()) {
                if (getCreatedByMountName(mountName) == null) {
                    addResourceCreated(new ActionResource(mountName));
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
        this.inputs = inputs == null ?
                Lists.<ActionInput>newArrayList() :
                inputs;
    }

    public String getRootXsiType() {
        return rootXsiType;
    }

    public void setRootXsiType(final String rootXsiType) {
        this.rootXsiType = rootXsiType;
    }

    @ElementCollection
    public List<Matcher> getRootMatchers() {
        return rootMatchers;
    }

    public void setRootMatchers(final List<Matcher> rootMatchers) {
        this.rootMatchers = rootMatchers == null ?
                Lists.<Matcher>newArrayList() :
                rootMatchers;
    }

    @ElementCollection
    public List<ActionResource> getResourcesStaged() {
        return resourcesStaged;
    }

    public void setResourcesStaged(final List<ActionResource> resourcesStaged) {
        this.resourcesStaged = resourcesStaged == null ?
                Lists.<ActionResource>newArrayList() :
                resourcesStaged;
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
        this.resourcesCreated = resourcesCreated == null ?
                Lists.<ActionResource>newArrayList() :
                resourcesCreated;
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
    private Map<String, ActionInput> getInputsByCommandVariableName() {
        if (inputs == null) {
            inputCachedHash = 0;
            return Maps.newHashMap();
        }
        if (inputCachedHash != inputs.hashCode() || inputsByCommandVariableName == null || inputsByCommandVariableName.isEmpty()) {
            final Map<String, ActionInput> map = Maps.newHashMap();
            for (final ActionInput input : inputs) {
                map.put(input.getCommandVariableName(), input);
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

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        final Action that = (Action) o;
        return Objects.equals(this.name, that.name) &&
                Objects.equals(this.description, that.description) &&
                Objects.equals(this.command, that.command) &&
                Objects.equals(this.rootXsiType, that.rootXsiType) &&
                Objects.equals(this.rootMatchers, that.rootMatchers) &&
                Objects.equals(this.inputs, that.inputs) &&
                Objects.equals(this.resourcesStaged, that.resourcesStaged) &&
                Objects.equals(this.resourcesCreated, that.resourcesCreated);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), name, description, command, rootXsiType, rootMatchers, inputs, resourcesStaged, resourcesCreated);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("name", name)
                .add("description", description)
                .add("command", command)
                .add("rootXsiType", rootXsiType)
                .add("rootMatchers", rootMatchers)
                .add("inputs", inputs)
                .add("resourcesStaged", resourcesStaged)
                .add("resourcesCreated", resourcesCreated)
                .toString();
    }
}
