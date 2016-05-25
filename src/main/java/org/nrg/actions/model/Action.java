package org.nrg.actions.model;

import com.google.common.base.MoreObjects;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.envers.Audited;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntity;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;
import java.util.List;
import java.util.Objects;

@Audited
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "nrg")
public class Action extends AbstractHibernateEntity {
    private String name;
    private String description;
    private Command command;
    private List<ActionInput> inputs;
    private List<ActionOutput> outputs;

    public Action() {}

    public Action(final ActionDto dto, final Command command) {
        this.name = dto.getName();
        this.description = dto.getDescription();
        this.inputs = dto.getInputs();
        this.command = command;
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

    @ElementCollection
    public List<ActionOutput> getOutputs() {
        return outputs;
    }

    public void setOutputs(final List<ActionOutput> outputs) {
        this.outputs = outputs;
    }

    @Transient
    public CommandLineInput getCommandInput(final ActionInput actionInput) {
        return getCommandInputByName(actionInput.getCommandInputName());
    }

    @Transient
    public CommandLineInput getCommandInputByName(final String name) {
        for (final CommandLineInput commandLineInput : command.getCommandLineInputs()) {
            if (commandLineInput.getName().equals(name)) {
                return commandLineInput;
            }
        }
        return null;
    }

    public void run() {
        // TODO
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || !super.equals(o) || getClass() != o.getClass()) return false;
        final Action that = (Action) o;
        return Objects.equals(this.name, that.name) &&
                Objects.equals(this.description, that.description) &&
                Objects.equals(this.command, that.command) &&
                Objects.equals(this.inputs, that.inputs) &&
                Objects.equals(this.outputs, that.outputs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), name, description, command, inputs, outputs);
    }

    @Override
    public String toString() {
        return addParentPropertiesToString(MoreObjects.toStringHelper(this))
                .add("name", name)
                .add("description", description)
                .add("command", command)
                .add("inputs", inputs)
                .add("outputs", outputs)
                .toString();
    }
}
