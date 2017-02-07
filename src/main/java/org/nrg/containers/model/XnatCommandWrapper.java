package org.nrg.containers.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import org.hibernate.envers.Audited;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntity;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import java.util.List;
import java.util.Objects;

@Entity
public class XnatCommandWrapper extends AbstractHibernateEntity {
    private String name;
    private String description;
    private Command command;
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
    @JoinColumn(name = "command_id", insertable = false, updatable = false)
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

    @ElementCollection
    public List<XnatCommandInput> getDerivedInputs() {
        return derivedInputs;
    }

    public void setDerivedInputs(final List<XnatCommandInput> derivedInputs) {
        this.derivedInputs = derivedInputs == null ?
                Lists.<XnatCommandInput>newArrayList() :
                derivedInputs;
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

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        final XnatCommandWrapper that = (XnatCommandWrapper) o;
        return Objects.equals(this.name, that.name) &&
                Objects.equals(this.description, that.description) &&
                Objects.equals(this.command, that.command) &&
                Objects.equals(this.externalInputs, that.externalInputs) &&
                Objects.equals(this.derivedInputs, that.derivedInputs) &&
                Objects.equals(this.outputHandlers, that.outputHandlers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), name, description, command, externalInputs, derivedInputs, outputHandlers);
    }

    @Override
    public MoreObjects.ToStringHelper addParentPropertiesToString(final MoreObjects.ToStringHelper helper) {
        return super.addParentPropertiesToString(helper)
                .add("name", name)
                .add("description", description)
                .add("command", command)
                .add("externalInputs", externalInputs)
                .add("derivedInputs", derivedInputs)
                .add("outputHandlers", outputHandlers);
    }

    @Override
    public String toString() {
        return addParentPropertiesToString(MoreObjects.toStringHelper(this))
                .toString();
    }

}
