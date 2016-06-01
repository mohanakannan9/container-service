package org.nrg.actions.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.hibernate.envers.Audited;
import org.nrg.containers.model.DockerImageCommand;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntity;

import javax.persistence.DiscriminatorColumn;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Inheritance;
import java.util.List;
import java.util.Map;
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
    private List<CommandVariable> variables = Lists.newArrayList();
    @JsonProperty("run-template") private String runTemplate;
    @JsonProperty("mounts-in") private List<CommandMount> mountsIn = Lists.newArrayList();
    @JsonProperty("mounts-out") private List<CommandMount> mountsOut = Lists.newArrayList();
    @JsonProperty("env") private Map<String, String> environmentVariables = Maps.newHashMap();

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

    @ElementCollection
    public List<CommandVariable> getVariables() {
        return variables;
    }

    void setVariables(final List<CommandVariable> variables) {
        this.variables = variables == null ?
                Lists.<CommandVariable>newArrayList() :
                variables;
    }

    public String getRunTemplate() {
        return runTemplate;
    }

    public void setRunTemplate(final String runTemplate) {
        this.runTemplate = runTemplate;
    }

    @ElementCollection
    public List<CommandMount> getMountsIn() {
        return mountsIn;
    }

    public void setMountsIn(final List<CommandMount> mountsIn) {
        this.mountsIn = mountsIn == null ?
                Lists.<CommandMount>newArrayList() :
                mountsIn;
    }

    @ElementCollection
    public List<CommandMount> getMountsOut() {
        return mountsOut;
    }

    public void setMountsOut(final List<CommandMount> mountsOut) {
        this.mountsOut = mountsOut == null ?
                Lists.<CommandMount>newArrayList() :
                mountsOut;
    }

    @ElementCollection
    public Map<String, String> getEnvironmentVariables() {
        return environmentVariables;
    }

    public void setEnvironmentVariables(final Map<String, String> environmentVariables) {
        this.environmentVariables = environmentVariables == null ?
                Maps.<String, String>newHashMap() :
                environmentVariables;
    }

    @Override
    public ToStringHelper addParentPropertiesToString(final ToStringHelper helper) {
        return super.addParentPropertiesToString(helper)
                .add("name", name)
                .add("description", description)
                .add("infoUrl", infoUrl)
                .add("variables", variables)
                .add("runTemplate", runTemplate)
                .add("mountsIn", mountsIn)
                .add("mountsOut", mountsOut)
                .add("environmentVariables", environmentVariables);
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
                Objects.equals(this.runTemplate, that.runTemplate) &&
                Objects.equals(this.variables, that.variables) &&
                Objects.equals(this.mountsIn, that.mountsIn) &&
                Objects.equals(this.mountsOut, that.mountsOut) &&
                Objects.equals(this.environmentVariables, that.environmentVariables);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), name, description, infoUrl,
                variables, runTemplate, mountsIn, mountsOut, environmentVariables);
    }

    @Override
    public String toString() {
        return addParentPropertiesToString(MoreObjects.toStringHelper(this))
                .toString();
    }
}
