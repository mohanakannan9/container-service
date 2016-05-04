package org.nrg.actions.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.collect.Lists;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.envers.Audited;
import org.nrg.automation.model.ScriptCommand;
import org.nrg.containers.model.DockerImageCommand;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntity;

import javax.persistence.DiscriminatorColumn;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.OneToMany;
import javax.persistence.Transient;
import java.util.List;
import java.util.Objects;

@Entity
@Audited
@Inheritance
@DiscriminatorColumn(name = "COMMAND_TYPE")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = DockerImageCommand.class, name = "docker-image"),
        @JsonSubTypes.Type(value = ScriptCommand.class, name = "script")
})
@JsonTypeName("command")
public abstract class Command extends AbstractHibernateEntity {
    private String name;
    private String description;
    @JsonProperty("info-url") private String infoUrl;
    private String template;
    private List<CommandInput> inputs = Lists.newArrayList();
    private List<Output> outputs = Lists.newArrayList();

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

    public String getInfoUrl() {
        return infoUrl;
    }

    public void setInfoUrl(final String infoUrl) {
        this.infoUrl = infoUrl;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(final String template) {
        this.template = template;
    }

    @ElementCollection
    public List<CommandInput> getInputs() {
        return inputs;
    }

    void setInputs(final List<CommandInput> inputs) {
        this.inputs = inputs;
    }

    @ElementCollection
    public List<Output> getOutputs() {
        return outputs;
    }

    void setOutputs(final List<Output> outputs) {
        this.outputs = outputs;
    }

    public abstract void run();

    @Transient
    public CommandInput getInputWithName(final String name) {
        for (final CommandInput commandInput : inputs) {
            if (commandInput.getName().equals(name)) {
                return commandInput;
            }
        }
        return null;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || !super.equals(o) || getClass() != o.getClass()) return false;
        final Command that = (Command) o;
        return Objects.equals(this.name, that.name) &&
                Objects.equals(this.description, that.description) &&
                Objects.equals(this.infoUrl, that.infoUrl) &&
                Objects.equals(this.template, that.template) &&
                Objects.equals(this.inputs, that.inputs) &&
                Objects.equals(this.outputs, that.outputs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), name, description, infoUrl, template, inputs, outputs);
    }

    @Override
    public String toString() {
        return addParentPropertiesToString(MoreObjects.toStringHelper(this)).toString();
    }

    @Override
    public ToStringHelper addParentPropertiesToString(final ToStringHelper helper) {
        return super.addParentPropertiesToString(helper)
                .add("name", name)
                .add("description", description)
                .add("infoUrl", infoUrl)
                .add("template", template)
                .add("inputs", inputs)
                .add("outputs", outputs);
    }
}
