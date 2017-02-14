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
public class XnatCommandWrapper implements Serializable {
    private long id;
    private String name;
    private String description;
    @JsonIgnore private Command command;
    @JsonProperty("external-inputs") private List<XnatCommandInput> externalInputs;
    @JsonProperty("derived-inputs") private List<XnatCommandInput> derivedInputs;
    @JsonProperty("output-handlers") private List<XnatCommandOutput> outputHandlers;

    public static XnatCommandWrapper passthrough(final Command command) {
        final XnatCommandWrapper identity = new XnatCommandWrapper();
        identity.command = command;

        final List<XnatCommandInput> externalInputs = Lists.newArrayList();
        if (command.getInputs() != null) {
            for (final CommandInput commandInput : command.getInputs()) {
                externalInputs.add(XnatCommandInput.passthrough(commandInput));
            }
        }
        identity.setExternalInputs(externalInputs);

        return identity;
    }

    public static XnatCommandWrapper fromPojo(final CommandPojo.CommandWrapperPojo commandWrapperPojo) {
        final XnatCommandWrapper xnatCommandWrapper = new XnatCommandWrapper();
        xnatCommandWrapper.name = commandWrapperPojo.name();
        xnatCommandWrapper.description = commandWrapperPojo.description();
        for (final CommandPojo.CommandWrapperInputPojo externalCommandWrapperInputPojo : commandWrapperPojo.externalInputs()) {
            xnatCommandWrapper.addExternalInput(XnatCommandInput.fromPojo(externalCommandWrapperInputPojo));
        }
        for (final CommandPojo.CommandWrapperInputPojo derivedCommandWrapperInputPojo : commandWrapperPojo.derivedInputs()) {
            xnatCommandWrapper.addDerivedInput(XnatCommandInput.fromPojo(derivedCommandWrapperInputPojo));
        }
        for (final CommandPojo.CommandWrapperOutputPojo commandWrapperOutputPojo : commandWrapperPojo.outputHandlers()) {
            xnatCommandWrapper.addOutputHandler(XnatCommandOutput.fromPojo(commandWrapperOutputPojo));
        }
        return xnatCommandWrapper;
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
    public Command getCommand() {
        return command;
    }

    public void setCommand(final Command command) {
        this.command = command;
    }

    @ElementCollection
    public List<XnatCommandInput> getExternalInputs() {
        return externalInputs;
    }

    public void setExternalInputs(final List<XnatCommandInput> externalInputs) {
        this.externalInputs = externalInputs == null ?
                Lists.<XnatCommandInput>newArrayList() :
                externalInputs;
    }

    public void addExternalInput(final XnatCommandInput externalInput) {
        if (externalInput == null) {
            return;
        }

        if (this.externalInputs == null) {
            this.externalInputs = Lists.newArrayList();
        }
        this.externalInputs.add(externalInput);
    }

    @ElementCollection
    public List<XnatCommandInput> getDerivedInputs() {
        return derivedInputs;
    }

    public void setDerivedInputs(final List<XnatCommandInput> derivedInputs) {
        this.derivedInputs = derivedInputs == null ?
                Lists.<XnatCommandInput>newArrayList() :
                derivedInputs;
    }

    public void addDerivedInput(final XnatCommandInput derivedInput) {
        if (derivedInput == null) {
            return;
        }

        if (this.derivedInputs == null) {
            this.derivedInputs = Lists.newArrayList();
        }
        this.derivedInputs.add(derivedInput);
    }

    @ElementCollection
    public List<XnatCommandOutput> getOutputHandlers() {
        return outputHandlers;
    }

    public void setOutputHandlers(final List<XnatCommandOutput> outputHandlers) {
        this.outputHandlers = outputHandlers == null ?
                Lists.<XnatCommandOutput>newArrayList() :
                outputHandlers;
    }

    public void addOutputHandler(final XnatCommandOutput outputHandler) {
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
        final XnatCommandWrapper that = (XnatCommandWrapper) o;
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
