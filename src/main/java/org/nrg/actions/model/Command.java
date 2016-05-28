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
    @JsonProperty("mounts-in") private List<CommandMount> mountsIn;
    @JsonProperty("mounts-out") private List<CommandMount> mountsOut;

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

    @ElementCollection
    public List<CommandMount> getMountsIn() {
        return mountsIn;
    }

    public void setMountsIn(final List<CommandMount> mountsIn) {
        this.mountsIn = mountsIn;
    }

    @ElementCollection
    public List<CommandMount> getMountsOut() {
        return mountsOut;
    }

    public void setMountsOut(final List<CommandMount> mountsOut) {
        this.mountsOut = mountsOut;
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
    public ToStringHelper addParentPropertiesToString(final ToStringHelper helper) {
        return super.addParentPropertiesToString(helper)
                .add("name", name)
                .add("description", description)
                .add("infoUrl", infoUrl)
                .add("template", template)
                .add("variables", variables)
                .add("mountsIn", mountsIn)
                .add("mountsOut", mountsOut);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        final Command that = (Command) o;
        return Objects.equals(this.name, that.name) &&
                Objects.equals(this.description, that.description) &&
                Objects.equals(this.infoUrl, that.infoUrl) &&
                Objects.equals(this.template, that.template) &&
                Objects.equals(this.variables, that.variables) &&
                Objects.equals(this.mountsIn, that.mountsIn) &&
                Objects.equals(this.mountsOut, that.mountsOut);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), name, description, infoUrl, template, variables, mountsIn, mountsOut);
    }

    @Override
    public String toString() {
        return addParentPropertiesToString(MoreObjects.toStringHelper(this))
                .toString();
    }
}
