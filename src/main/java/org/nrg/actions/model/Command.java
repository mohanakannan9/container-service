package org.nrg.actions.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import org.hibernate.envers.Audited;
import org.nrg.containers.model.DockerImageCommand;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntity;

import javax.persistence.DiscriminatorColumn;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
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
    private List<CommandVariable> variables;
    @JsonProperty("mounts") private CommandMounts commandMounts;


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
    public List<CommandVariable> getVariables() {
        return variables;
    }

    void setVariables(final List<CommandVariable> commandVariables) {
        this.variables = commandVariables;
    }

    public CommandMounts getCommandMounts() {
        return commandMounts;
    }

    public void setCommandMounts(final CommandMounts commandMounts) {
        this.commandMounts = commandMounts;
    }

    public abstract void run();

    @Transient
    public CommandVariable getInputWithName(final String name) {
        for (final CommandVariable commandVariable : variables) {
            if (commandVariable.getName().equals(name)) {
                return commandVariable;
            }
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !super.equals(o) || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Command command = (Command) o;
        return Objects.equals(this.name, command.name) &&
                Objects.equals(this.description, command.description) &&
                Objects.equals(this.infoUrl, command.infoUrl) &&
                Objects.equals(this.template, command.template) &&
                Objects.equals(this.variables, command.variables) &&
                Objects.equals(this.commandMounts, command.commandMounts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), name, description, infoUrl, template, variables, commandMounts);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("name", name)
                .add("description", description)
                .add("infoUrl", infoUrl)
                .add("template", template)
                .add("variables", variables)
                .add("commandMounts", commandMounts)
                .toString();
    }

    @Override
    public ToStringHelper addParentPropertiesToString(final ToStringHelper helper) {
        return super.addParentPropertiesToString(helper)
                .add("name", name)
                .add("description", description)
                .add("infoUrl", infoUrl)
                .add("template", template)
                .add("variables", variables);
    }
}
