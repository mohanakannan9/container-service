package org.nrg.containers.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.nrg.containers.model.auto.Command;
import org.hibernate.envers.Audited;

import javax.persistence.CascadeType;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Entity
@Audited
public class CommandWrapperEntity implements Serializable {
    private long id;
    private String name;
    private String description;
    @JsonIgnore private CommandEntity commandEntity;
    private Set<String> contexts;
    @JsonProperty("external-inputs") private List<CommandWrapperExternalInputEntity> externalInputs;
    @JsonProperty("derived-inputs") private List<CommandWrapperDerivedInputEntity> derivedInputs;
    @JsonProperty("output-handlers") private List<CommandWrapperOutputEntity> outputHandlers;

    public static CommandWrapperEntity fromPojo(final Command.CommandWrapper commandWrapper) {
        final CommandWrapperEntity commandWrapperEntity = new CommandWrapperEntity();
        commandWrapperEntity.name = commandWrapper.name();
        commandWrapperEntity.description = commandWrapper.description();
        for (final Command.CommandWrapperInput externalCommandWrapperInput : commandWrapper.externalInputs()) {
            commandWrapperEntity.addExternalInput(CommandWrapperExternalInputEntity.fromPojo(externalCommandWrapperInput));
        }
        for (final Command.CommandWrapperDerivedInput derivedCommandWrapperInput : commandWrapper.derivedInputs()) {
            commandWrapperEntity.addDerivedInput(CommandWrapperDerivedInputEntity.fromPojo(derivedCommandWrapperInput));
        }
        for (final Command.CommandWrapperOutput commandWrapperOutput : commandWrapper.outputHandlers()) {
            commandWrapperEntity.addOutputHandler(CommandWrapperOutputEntity.fromPojo(commandWrapperOutput));
        }
        return commandWrapperEntity;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public long getId() {
        return id;
    }

    public void setId(final long id) {
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

    @ManyToOne
    public CommandEntity getCommandEntity() {
        return commandEntity;
    }

    public void setCommandEntity(final CommandEntity commandEntity) {
        this.commandEntity = commandEntity;
    }

    @ElementCollection
    public Set<String> getContexts() {
        return contexts;
    }

    public void setContexts(final Set<String> contexts) {
        this.contexts = contexts == null ?
                Sets.<String>newHashSet() :
                contexts;
    }

    public void addContext(final String context) {
        if (StringUtils.isBlank(context)) {
            return;
        }
        if (this.contexts == null) {
            this.contexts = Sets.newHashSet();
        }
        this.contexts.add(context);
    }

    @OneToMany(mappedBy = "commandWrapperEntity", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<CommandWrapperExternalInputEntity> getExternalInputs() {
        return externalInputs;
    }

    public void setExternalInputs(final List<CommandWrapperExternalInputEntity> externalInputs) {
        this.externalInputs = externalInputs == null ?
                Lists.<CommandWrapperExternalInputEntity>newArrayList() :
                externalInputs;
        for (final CommandWrapperExternalInputEntity externalInput : this.externalInputs) {
            externalInput.setCommandWrapperEntity(this);
        }
    }

    public void addExternalInput(final CommandWrapperExternalInputEntity externalInput) {
        if (externalInput == null) {
            return;
        }
        externalInput.setCommandWrapperEntity(this);

        if (this.externalInputs == null) {
            this.externalInputs = Lists.newArrayList();
        }
        this.externalInputs.add(externalInput);
    }

    @OneToMany(mappedBy = "commandWrapperEntity", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<CommandWrapperDerivedInputEntity> getDerivedInputs() {
        return derivedInputs;
    }

    public void setDerivedInputs(final List<CommandWrapperDerivedInputEntity> derivedInputs) {
        this.derivedInputs = derivedInputs == null ?
                Lists.<CommandWrapperDerivedInputEntity>newArrayList() :
                derivedInputs;

        for (final CommandWrapperDerivedInputEntity derivedInput : this.derivedInputs) {
            derivedInput.setCommandWrapperEntity(this);
        }
    }

    public void addDerivedInput(final CommandWrapperDerivedInputEntity derivedInput) {
        if (derivedInput == null) {
            return;
        }
        derivedInput.setCommandWrapperEntity(this);

        if (this.derivedInputs == null) {
            this.derivedInputs = Lists.newArrayList();
        }
        this.derivedInputs.add(derivedInput);
    }

    @OneToMany(mappedBy = "commandWrapperEntity", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<CommandWrapperOutputEntity> getOutputHandlers() {
        return outputHandlers;
    }

    public void setOutputHandlers(final List<CommandWrapperOutputEntity> outputHandlers) {
        this.outputHandlers = outputHandlers == null ?
                Lists.<CommandWrapperOutputEntity>newArrayList() :
                outputHandlers;

        for (final CommandWrapperOutputEntity commandWrapperOutputEntity : this.outputHandlers) {
            commandWrapperOutputEntity.setCommandWrapperEntity(this);
        }
    }

    public void addOutputHandler(final CommandWrapperOutputEntity outputHandler) {
        if (outputHandler == null) {
            return;
        }
        outputHandler.setCommandWrapperEntity(this);

        if (this.outputHandlers == null) {
            this.outputHandlers = Lists.newArrayList();
        }
        this.outputHandlers.add(outputHandler);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final CommandWrapperEntity that = (CommandWrapperEntity) o;
        return Objects.equals(this.commandEntity, that.commandEntity) &&
                Objects.equals(this.name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, description, contexts, externalInputs, derivedInputs, outputHandlers);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("name", name)
                .add("description", description)
                .add("contexts", contexts)
                .add("externalInputs", externalInputs)
                .add("derivedInputs", derivedInputs)
                .add("outputHandlers", outputHandlers)
                .toString();
    }

}
