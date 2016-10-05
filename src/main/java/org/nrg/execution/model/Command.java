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
    private CommandRun run;
    private List<CommandInput> inputs = Lists.newArrayList();
    private List<CommandOutput> outputs = Lists.newArrayList();

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

    public CommandRun getRun() {
        return run;
    }

    public void setRun(final CommandRun run) {
        this.run = run;
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
    @ApiModelProperty("A list of outputs.")
    public List<CommandOutput> getOutputs() {
        return outputs;
    }

    void setOutputs(final List<CommandOutput> outputs) {
        this.outputs = outputs == null ?
                Lists.<CommandOutput>newArrayList() :
                outputs;
    }

    @Override
    public ToStringHelper addParentPropertiesToString(final ToStringHelper helper) {
        return super.addParentPropertiesToString(helper)
                .add("name", name)
                .add("description", description)
                .add("infoUrl", infoUrl)
                .add("dockerImage", dockerImage)
                .add("run", run)
                .add("inputs", inputs)
                .add("outputs", outputs);
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
                Objects.equals(this.run, that.run) &&
                Objects.equals(this.inputs, that.inputs) &&
                Objects.equals(this.outputs, that.outputs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, infoUrl, dockerImage, run, inputs, outputs);
    }

    @Override
    public String toString() {
        return addParentPropertiesToString(MoreObjects.toStringHelper(this))
                .toString();
    }
}
