package org.nrg.execution.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.swagger.annotations.ApiModelProperty;
import org.hibernate.envers.Audited;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntity;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Entity
@Audited
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"name", "dockerImage"})})
public class Command extends AbstractHibernateEntity {
    private String name;
    private String description;
    @JsonProperty("info-url") private String infoUrl;
    @JsonProperty("docker-image") private String dockerImage;
    private List<CommandVariable> variables = Lists.newArrayList();
    @JsonProperty("run-template") private List<String> runTemplate;
    @JsonProperty("mounts-in") private Map<String, String> mountsIn = Maps.newHashMap();
    @JsonProperty("mounts-out") private Map<String, String> mountsOut = Maps.newHashMap();
    @JsonProperty("env") private Map<String, String> environmentVariables = Maps.newHashMap();

    @ApiModelProperty(value = "The Command's user-readable name. Must be unique for a given docker image.", required = true)
    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    @ApiModelProperty("A brief description of the Command")
    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    @ApiModelProperty("A URL where users can get more information about the Command")
    public String getInfoUrl() {
        return infoUrl;
    }

    public void setInfoUrl(final String infoUrl) {
        this.infoUrl = infoUrl;
    }

    @ApiModelProperty(value = "The ID of the docker image where this Command will run", required = true)
    public String getDockerImage() {
        return dockerImage;
    }

    public void setDockerImage(final String dockerImage) {
        this.dockerImage = dockerImage;
    }

    @ElementCollection
    @ApiModelProperty("A list of variables. " +
            "When the Command is launched, these variables receive values; " +
            "those values will be used to fill in any template strings in the Command's run-template, mounts, or environment variables.")
    public List<CommandVariable> getVariables() {
        return variables;
    }

    void setVariables(final List<CommandVariable> variables) {
        this.variables = variables == null ?
                Lists.<CommandVariable>newArrayList() :
                variables;
    }

    @ElementCollection
    @ApiModelProperty("The command that will be executed in the container when the Command is launched. " +
            "Can use template strings, e.g. #variable-name#, which will be resolved into a value when the Command is launched.")
    public List<String> getRunTemplate() {
        return runTemplate;
    }

    public void setRunTemplate(final List<String> runTemplate) {
        this.runTemplate = runTemplate == null ?
                Lists.<String>newArrayList() :
                runTemplate;
    }

    @ElementCollection
    @ApiModelProperty(value = "A Map of input mounts. " +
            "Each key is a unique name for the mount, and each value is a path that will be mounted in the container when the Command is launched. " +
            "The paths can use template strings, e.g. #variable-name#, which will be resolved into a value when the Command is launched. " +
            "Input mounts are mounted read-only, and are intended to hold the raw input data your container will expect.",
            example = "{\"in\":\"/input\"}")
    public Map<String, String> getMountsIn() {
        return mountsIn;
    }

    public void setMountsIn(final Map<String, String> mountsIn) {
        this.mountsIn = mountsIn == null ?
                Maps.<String, String>newHashMap() :
                mountsIn;
    }

    @ElementCollection
    @ApiModelProperty(value = "A Map of output mounts. " +
            "Each key is a unique name for the mount, and each value is a path that will be mounted in the container when the Command is launched. " +
            "The paths can use template strings, e.g. #variable-name#, which will be resolved into a value when the Command is launched." +
            "Output mounts are writable, and are intended to hold any outputs your container produces.",
            example = "{\"out\":\"/output\"}")
    public Map<String, String> getMountsOut() {
        return mountsOut;
    }

    public void setMountsOut(final Map<String, String> mountsOut) {
        this.mountsOut = mountsOut == null ?
                Maps.<String, String>newHashMap() :
                mountsOut;
    }

    @ElementCollection
    @ApiModelProperty("A Map of environment variables. Each kay is the environment variable's name, and each value is the environment variable's value." +
            "Both the names and values can use template strings, e.g. #variable-name#, which will be resolved into a value when the Command is launched.")
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
        final Command that = (Command) o;
        return Objects.equals(this.getId(), that.getId()) &&
                Objects.equals(this.name, that.name) &&
                Objects.equals(this.description, that.description) &&
                Objects.equals(this.infoUrl, that.infoUrl) &&
                Objects.equals(this.dockerImage, that.dockerImage) &&
                Objects.equals(this.runTemplate, that.runTemplate) &&
                Objects.equals(this.variables, that.variables) &&
                Objects.equals(this.mountsIn, that.mountsIn) &&
                Objects.equals(this.mountsOut, that.mountsOut) &&
                Objects.equals(this.environmentVariables, that.environmentVariables);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, infoUrl, dockerImage,
                variables, runTemplate, mountsIn, mountsOut, environmentVariables);
    }

    @Override
    public String toString() {
        return addParentPropertiesToString(MoreObjects.toStringHelper(this))
                .toString();
    }
}
