package org.nrg.actions.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.hibernate.envers.Audited;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntity;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Entity
@Audited
public class Command extends AbstractHibernateEntity {
    private String name;
    private String description;
    @JsonProperty("info-url") private String infoUrl;
    @JsonProperty("docker-image") private String dockerImage;
    @JsonProperty("script-id") private Long scriptId;
    private List<CommandVariable> variables = Lists.newArrayList();
    @JsonProperty("run-template") private List<String> runTemplate;
    @JsonProperty("mounts-in") private Map<String, String> mountsIn = Maps.newHashMap();
    @JsonProperty("mounts-out") private Map<String, String> mountsOut = Maps.newHashMap();
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

    public String getDockerImage() {
        return dockerImage;
    }

    public void setDockerImage(String dockerImage) {
        this.dockerImage = dockerImage;
    }

    public Long getScriptId() {
        return scriptId;
    }

    public void setScriptId(Long scriptId) {
        this.scriptId = scriptId;
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

    @ElementCollection
    public List<String> getRunTemplate() {
        return runTemplate;
    }

    public void setRunTemplate(final List<String> runTemplate) {
        this.runTemplate = runTemplate == null ?
                Lists.<String>newArrayList() :
                runTemplate;
    }

    @ElementCollection
    public Map<String, String> getMountsIn() {
        return mountsIn;
    }

    public void setMountsIn(final Map<String, String> mountsIn) {
        this.mountsIn = mountsIn == null ?
                Maps.<String, String>newHashMap() :
                mountsIn;
    }

    @ElementCollection
    public Map<String, String> getMountsOut() {
        return mountsOut;
    }

    public void setMountsOut(final Map<String, String> mountsOut) {
        this.mountsOut = mountsOut == null ?
                Maps.<String, String>newHashMap() :
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
                .add("dockerImage", dockerImage)
                .add("scriptId", scriptId)
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
                Objects.equals(this.dockerImage, that.dockerImage) &&
                Objects.equals(this.scriptId, that.scriptId) &&
                Objects.equals(this.runTemplate, that.runTemplate) &&
                Objects.equals(this.variables, that.variables) &&
                Objects.equals(this.mountsIn, that.mountsIn) &&
                Objects.equals(this.mountsOut, that.mountsOut) &&
                Objects.equals(this.environmentVariables, that.environmentVariables);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, infoUrl, dockerImage, scriptId,
                variables, runTemplate, mountsIn, mountsOut, environmentVariables);
    }

    @Override
    public String toString() {
        return addParentPropertiesToString(MoreObjects.toStringHelper(this))
                .toString();
    }
}
