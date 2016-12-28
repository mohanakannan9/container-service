package org.nrg.containers.model;

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
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import java.util.Iterator;
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

    public void setInputs(final List<CommandInput> inputs) {
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

    public void setOutputs(final List<CommandOutput> outputs) {
        this.outputs = outputs == null ?
                Lists.<CommandOutput>newArrayList() :
                outputs;
    }

    @Transient
    public void update(final Command other, final Boolean ignoreNull) {
        if (other == null) {
            return;
        }

        if (other.description != null || !ignoreNull) {
            this.description = other.description;
        }
        if (other.infoUrl != null || !ignoreNull) {
            this.infoUrl = other.infoUrl;
        }

        if (this.run == null || (other.run == null && !ignoreNull)) {
            this.run = other.run;
        } else {
            this.run.update(other.run, ignoreNull);
        }

        if (this.inputs == null || this.inputs.isEmpty()) {
            // If we have no inputs, just take what we are given.
            // No need to go digging through lists when we're comparing to nothing.
            // Even if the "update" is empty too, no big deal.
            this.inputs = other.inputs;
        } else {
            final Map<String, CommandInput> inputsToUpdateMap = Maps.newHashMap();
            if (other.inputs != null) {
                for (final CommandInput otherOutput : other.inputs) {
                    inputsToUpdateMap.put(otherOutput.getName(), otherOutput);
                }
            }

            // Update any inputs that already exist
            final Iterator<CommandInput> iterator = this.inputs.iterator();
            while (iterator.hasNext()) {
                final CommandInput thisInput = iterator.next();
                if (inputsToUpdateMap.containsKey(thisInput.getName())) {
                    // The mount we are looking at is in the list of inputs to update
                    // So update it and keep it in the list.
                    thisInput.update(inputsToUpdateMap.get(thisInput.getName()), ignoreNull);

                    // Now that we've updated that mount, remove it from the map of ones to update.
                    inputsToUpdateMap.remove(thisInput.getName());
                } else {
                    // The mount we are looking at is not in the list of inputs to update.
                    // If we ignore nulls, than that's fine, the mount can stay. Do nothing.
                    // If ignoreNull==false, then not seeing the mount means "remove it".
                    if (!ignoreNull) {
                        iterator.remove();
                    }
                }
            }

            // We have now updated/removed all inputs we already knew about.
            // Any inputs still in the toUpdateMap are ones we didn't know about before,
            // so we can just add them.
            this.inputs.addAll(inputsToUpdateMap.values());
        }

        if (this.outputs == null || this.outputs.isEmpty()) {
            // If we have no outputs, just take what we are given.
            // No need to go digging through lists when we're comparing to nothing.
            // Even if the "update" is empty too, no big deal.
            this.outputs = other.outputs;
        } else {
            final Map<String, CommandOutput> outputsToUpdateMap = Maps.newHashMap();
            if (other.outputs != null) {
                for (final CommandOutput otherOutput : other.outputs) {
                    outputsToUpdateMap.put(otherOutput.getName(), otherOutput);
                }
            }

            // Update any outputs that already exist
            final Iterator<CommandOutput> iterator = this.outputs.iterator();
            while (iterator.hasNext()) {
                final CommandOutput thisOutput = iterator.next();
                if (outputsToUpdateMap.containsKey(thisOutput.getName())) {
                    // The mount we are looking at is in the list of outputs to update
                    // So update it and keep it in the list.
                    thisOutput.update(outputsToUpdateMap.get(thisOutput.getName()), ignoreNull);

                    // Now that we've updated that mount, remove it from the map of ones to update.
                    outputsToUpdateMap.remove(thisOutput.getName());
                } else {
                    // The mount we are looking at is not in the list of outputs to update.
                    // If we ignore nulls, than that's fine, the mount can stay. Do nothing.
                    // If ignoreNull==false, then not seeing the mount means "remove it".
                    if (!ignoreNull) {
                        iterator.remove();
                    }
                }
            }

            // We have now updated/removed all outputs we already knew about.
            // Any outputs still in the toUpdateMap are ones we didn't know about before,
            // so we can just add them.
            this.outputs.addAll(outputsToUpdateMap.values());
        }
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
