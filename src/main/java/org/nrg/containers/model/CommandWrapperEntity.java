package org.nrg.containers.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import org.nrg.containers.model.auto.CommandPojo;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

@Entity
public class CommandWrapperEntity implements Serializable {
    private long id;
    private String name;
    private String description;
    @JsonIgnore private CommandEntity commandEntity;
    @JsonProperty("external-inputs") private List<CommandWrapperInputEntity> externalInputs;
    @JsonProperty("derived-inputs") private List<CommandWrapperInputEntity> derivedInputs;
    @JsonProperty("output-handlers") private List<CommandWrapperOutputEntity> outputHandlers;

    public static CommandWrapperEntity passthrough(final CommandEntity commandEntity) {
        final CommandWrapperEntity identity = new CommandWrapperEntity();
        identity.commandEntity = commandEntity;

        final List<CommandWrapperInputEntity> externalInputs = Lists.newArrayList();
        if (commandEntity.getInputs() != null) {
            for (final CommandInput commandInput : commandEntity.getInputs()) {
                externalInputs.add(CommandWrapperInputEntity.passthrough(commandInput));
            }
        }
        identity.setExternalInputs(externalInputs);

        return identity;
    }

    public static CommandWrapperEntity fromPojo(final CommandPojo.CommandWrapperPojo commandWrapperPojo) {
        final CommandWrapperEntity commandWrapperEntity = new CommandWrapperEntity();
        commandWrapperEntity.name = commandWrapperPojo.name();
        commandWrapperEntity.description = commandWrapperPojo.description();
        for (final CommandPojo.CommandWrapperInputPojo externalCommandWrapperInputPojo : commandWrapperPojo.externalInputs()) {
            commandWrapperEntity.addExternalInput(CommandWrapperInputEntity.fromPojo(externalCommandWrapperInputPojo));
        }
        for (final CommandPojo.CommandWrapperInputPojo derivedCommandWrapperInputPojo : commandWrapperPojo.derivedInputs()) {
            commandWrapperEntity.addDerivedInput(CommandWrapperInputEntity.fromPojo(derivedCommandWrapperInputPojo));
        }
        for (final CommandPojo.CommandWrapperOutputPojo commandWrapperOutputPojo : commandWrapperPojo.outputHandlers()) {
            commandWrapperEntity.addOutputHandler(CommandWrapperOutputEntity.fromPojo(commandWrapperOutputPojo));
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
    public List<CommandWrapperInputEntity> getExternalInputs() {
        return externalInputs;
    }

    public void setExternalInputs(final List<CommandWrapperInputEntity> externalInputs) {
        this.externalInputs = externalInputs == null ?
                Lists.<CommandWrapperInputEntity>newArrayList() :
                externalInputs;
    }

    public void addExternalInput(final CommandWrapperInputEntity externalInput) {
        if (externalInput == null) {
            return;
        }

        if (this.externalInputs == null) {
            this.externalInputs = Lists.newArrayList();
        }
        this.externalInputs.add(externalInput);
    }

    @ElementCollection
    public List<CommandWrapperInputEntity> getDerivedInputs() {
        return derivedInputs;
    }

    public void setDerivedInputs(final List<CommandWrapperInputEntity> derivedInputs) {
        this.derivedInputs = derivedInputs == null ?
                Lists.<CommandWrapperInputEntity>newArrayList() :
                derivedInputs;
    }

    public void addDerivedInput(final CommandWrapperInputEntity derivedInput) {
        if (derivedInput == null) {
            return;
        }

        if (this.derivedInputs == null) {
            this.derivedInputs = Lists.newArrayList();
        }
        this.derivedInputs.add(derivedInput);
    }

    @ElementCollection
    public List<CommandWrapperOutputEntity> getOutputHandlers() {
        return outputHandlers;
    }

    public void setOutputHandlers(final List<CommandWrapperOutputEntity> outputHandlers) {
        this.outputHandlers = outputHandlers == null ?
                Lists.<CommandWrapperOutputEntity>newArrayList() :
                outputHandlers;
    }

    public void addOutputHandler(final CommandWrapperOutputEntity outputHandler) {
        if (outputHandler == null) {
            return;
        }

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
        return Objects.equals(this.id, that.id) &&
                Objects.equals(this.name, that.name) &&
                Objects.equals(this.description, that.description) &&
                Objects.equals(this.externalInputs, that.externalInputs) &&
                Objects.equals(this.derivedInputs, that.derivedInputs) &&
                Objects.equals(this.outputHandlers, that.outputHandlers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, description, externalInputs, derivedInputs, outputHandlers);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("name", name)
                .add("description", description)
                .add("externalInputs", externalInputs)
                .add("derivedInputs", derivedInputs)
                .add("outputHandlers", outputHandlers)
                .toString();
    }

}
