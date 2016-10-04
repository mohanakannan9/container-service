package org.nrg.execution.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.swagger.annotations.ApiModelProperty;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Entity
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"name", "dockerImage"})})
public class Command extends AbstractHibernateEntity {
    private String name;
    private String description;
    @JsonProperty("info-url") private String infoUrl;
    @JsonProperty("docker-image") private String dockerImage;
    private List<CommandInput> inputs = Lists.newArrayList();
    @JsonProperty("run-template") private List<String> runTemplate;
    @JsonProperty("mounts-in") private List<CommandMount> mountsIn = Lists.newArrayList();
    @JsonProperty("mounts-out") private List<CommandMount> mountsOut = Lists.newArrayList();
    @JsonProperty("env") private Map<String, String> environmentVariables = Maps.newHashMap();

    @Nonnull
    @ApiModelProperty(value = "The Command's user-readable name. Must be unique for a given docker image.", required = true)
    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    @Nullable
    @ApiModelProperty("A brief description of the Command")
    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    @Nullable
    @ApiModelProperty("A URL where users can get more information about the Command")
    public String getInfoUrl() {
        return infoUrl;
    }

    public void setInfoUrl(final String infoUrl) {
        this.infoUrl = infoUrl;
    }

    @Nonnull
    @ApiModelProperty(value = "The ID of the docker image where this Command will run", required = true)
    public String getDockerImage() {
        return dockerImage;
    }

    public void setDockerImage(final String dockerImage) {
        this.dockerImage = dockerImage;
    }

    @Nullable
    @ElementCollection
    @ApiModelProperty("A list of inputs. " +
            "When the Command is launched, these inputs receive values; " +
            "those values will be used to fill in any template strings in the Command's run-template, mounts, or environment variables.")
    public List<CommandInput> getInputs() {
        return inputs;
    }

    void setInputs(final List<CommandInput> inputs) {
        this.inputs = inputs == null ?
                Lists.<CommandInput>newArrayList() :
                inputs;
    }

    @Nullable
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

    @Nullable
    @ElementCollection
    public List<CommandMount> getMountsIn() {
        return mountsIn;
    }

    public void setMountsIn(final List<CommandMount> mountsIn) {
        this.mountsIn = mountsIn == null ?
                Lists.<CommandMount>newArrayList() :
                mountsIn;
        for (final CommandMount mount : this.mountsIn) {
            mount.setReadOnly(true);
        }
    }

    @Nullable
    @ElementCollection
    public List<CommandMount> getMountsOut() {
        return mountsOut;
    }

    public void setMountsOut(final List<CommandMount> mountsOut) {
        this.mountsOut = mountsOut == null ?
                Lists.<CommandMount>newArrayList() :
                mountsOut;
        for (final CommandMount mount : this.mountsOut) {
            mount.setReadOnly(false);
        }
    }

    @Nullable
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
                .add("inputs", inputs)
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
                Objects.equals(this.inputs, that.inputs) &&
                Objects.equals(this.mountsIn, that.mountsIn) &&
                Objects.equals(this.mountsOut, that.mountsOut) &&
                Objects.equals(this.environmentVariables, that.environmentVariables);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, infoUrl, dockerImage,
                inputs, runTemplate, mountsIn, mountsOut, environmentVariables);
    }

    @Override
    public String toString() {
        return addParentPropertiesToString(MoreObjects.toStringHelper(this))
                .toString();
    }
}
