package org.nrg.containers.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import io.swagger.annotations.ApiModelProperty;
import org.hibernate.envers.Audited;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntity;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Transient;
import java.util.Set;
import java.util.Map;
import java.util.Objects;

@Entity
@Audited
@JsonTypeInfo(use = Id.NONE, include = As.PROPERTY, property = "type", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = DockerCommand.class, name = "docker")
})
public abstract class Command extends AbstractHibernateEntity {
    private String name;
    private String label;
    private String description;
    private String version;
    @JsonProperty("schema-version") private String schemaVersion;
    @JsonProperty("info-url") private String infoUrl;
    private String image;
    @JsonProperty("working-directory") private String workingDirectory;
    @JsonProperty("command-line") private String commandLine;
    @JsonProperty("mounts") private Set<CommandMount> mounts;
    @JsonProperty("environment-variables") private Map<String, String> environmentVariables;
    private Set<CommandInput> inputs;
    private Set<CommandOutput> outputs;
    @JsonProperty("xnat") private Set<XnatCommandWrapper> xnatCommandWrappers;

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(final String label) {
        this.label = label;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(final String version) {
        this.version = version;
    }

    public String getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(final String schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    @ApiModelProperty("A URL where users can get more information about the Command")
    public String getInfoUrl() {
        return infoUrl;
    }

    public void setInfoUrl(final String infoUrl) {
        this.infoUrl = infoUrl;
    }

    public String getImage() {
        return image;
    }

    public void setImage(final String image) {
        this.image = image;
    }

    public String getWorkingDirectory() {
        return workingDirectory;
    }

    public void setWorkingDirectory(final String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    @ApiModelProperty("The command that will be executed in the container when the Command is launched.")
    public String getCommandLine() {
        return commandLine;
    }

    public void setCommandLine(final String commandLine) {
        this.commandLine = commandLine;
    }

    @ElementCollection
    public Set<CommandMount> getMounts() {
        return mounts;
    }

    public void setMounts(final Set<CommandMount> mounts) {
        this.mounts = mounts == null ?
                Sets.<CommandMount>newHashSet() :
                mounts;
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

    @ElementCollection
    @ApiModelProperty("A list of inputs. " +
            "When the Command is launched, these inputs receive values; " +
            "those values will be used to fill in any template strings in the Command's run-template, mounts, or environment variables.")
    public Set<CommandInput> getInputs() {
        return inputs;
    }

    public void setInputs(final Set<CommandInput> inputs) {
        this.inputs = inputs == null ?
                Sets.<CommandInput>newHashSet() :
                inputs;
    }

    @ElementCollection
    @ApiModelProperty("A list of outputs.")
    public Set<CommandOutput> getOutputs() {
        return outputs;
    }

    public void setOutputs(final Set<CommandOutput> outputs) {
        this.outputs = outputs == null ?
                Sets.<CommandOutput>newHashSet() :
                outputs;
    }

    @OneToMany
    public Set<XnatCommandWrapper> getXnatCommandWrappers() {
        return xnatCommandWrappers;
    }

    public void setXnatCommandWrappers(final Set<XnatCommandWrapper> xnatCommandWrappers) {
        this.xnatCommandWrappers = xnatCommandWrappers == null ?
                Sets.<XnatCommandWrapper>newHashSet() :
                xnatCommandWrappers;
    }

    @Transient
    public void addXnatCommandWrapper(final XnatCommandWrapper xnatCommandWrapper) {
        if (xnatCommandWrapper == null) {
            return;
        }

        if (this.xnatCommandWrappers == null) {
            this.xnatCommandWrappers = Sets.newHashSet();
        }
        this.xnatCommandWrappers.add(xnatCommandWrapper);
    }

    @Transient
    public void update(final Command other, final Boolean ignoreNull) {
        // if (other == null) {
        //     return;
        // }
        //
        // if (other.description != null || !ignoreNull) {
        //     this.description = other.description;
        // }
        // if (other.infoUrl != null || !ignoreNull) {
        //     this.infoUrl = other.infoUrl;
        // }
        //
        // if (this.run == null || (other.run == null && !ignoreNull)) {
        //     this.run = other.run;
        // } else {
        //     this.run.update(other.run, ignoreNull);
        // }
        //
        // if (this.inputs == null || this.inputs.isEmpty()) {
        //     // If we have no inputs, just take what we are given.
        //     // No need to go digging through lists when we're comparing to nothing.
        //     // Even if the "update" is empty too, no big deal.
        //     this.inputs = other.inputs;
        // } else {
        //     final Map<String, CommandInput> inputsToUpdateMap = Maps.newHashMap();
        //     if (other.inputs != null) {
        //         for (final CommandInput otherOutput : other.inputs) {
        //             inputsToUpdateMap.put(otherOutput.getName(), otherOutput);
        //         }
        //     }
        //
        //     // Update any inputs that already exist
        //     final Iterator<CommandInput> iterator = this.inputs.iterator();
        //     while (iterator.hasNext()) {
        //         final CommandInput thisInput = iterator.next();
        //         if (inputsToUpdateMap.containsKey(thisInput.getName())) {
        //             // The mount we are looking at is in the list of inputs to update
        //             // So update it and keep it in the list.
        //             thisInput.update(inputsToUpdateMap.get(thisInput.getName()), ignoreNull);
        //
        //             // Now that we've updated that mount, remove it from the map of ones to update.
        //             inputsToUpdateMap.remove(thisInput.getName());
        //         } else {
        //             // The mount we are looking at is not in the list of inputs to update.
        //             // If we ignore nulls, than that's fine, the mount can stay. Do nothing.
        //             // If ignoreNull==false, then not seeing the mount means "remove it".
        //             if (!ignoreNull) {
        //                 iterator.remove();
        //             }
        //         }
        //     }
        //
        //     // We have now updated/removed all inputs we already knew about.
        //     // Any inputs still in the toUpdateMap are ones we didn't know about before,
        //     // so we can just add them.
        //     this.inputs.addAll(inputsToUpdateMap.values());
        // }
        //
        // if (this.outputs == null || this.outputs.isEmpty()) {
        //     // If we have no outputs, just take what we are given.
        //     // No need to go digging through lists when we're comparing to nothing.
        //     // Even if the "update" is empty too, no big deal.
        //     this.outputs = other.outputs;
        // } else {
        //     final Map<String, CommandOutput> outputsToUpdateMap = Maps.newHashMap();
        //     if (other.outputs != null) {
        //         for (final CommandOutput otherOutput : other.outputs) {
        //             outputsToUpdateMap.put(otherOutput.getName(), otherOutput);
        //         }
        //     }
        //
        //     // Update any outputs that already exist
        //     final Iterator<CommandOutput> iterator = this.outputs.iterator();
        //     while (iterator.hasNext()) {
        //         final CommandOutput thisOutput = iterator.next();
        //         if (outputsToUpdateMap.containsKey(thisOutput.getName())) {
        //             // The mount we are looking at is in the list of outputs to update
        //             // So update it and keep it in the list.
        //             thisOutput.update(outputsToUpdateMap.get(thisOutput.getName()), ignoreNull);
        //
        //             // Now that we've updated that mount, remove it from the map of ones to update.
        //             outputsToUpdateMap.remove(thisOutput.getName());
        //         } else {
        //             // The mount we are looking at is not in the list of outputs to update.
        //             // If we ignore nulls, than that's fine, the mount can stay. Do nothing.
        //             // If ignoreNull==false, then not seeing the mount means "remove it".
        //             if (!ignoreNull) {
        //                 iterator.remove();
        //             }
        //         }
        //     }
        //
        //     // We have now updated/removed all outputs we already knew about.
        //     // Any outputs still in the toUpdateMap are ones we didn't know about before,
        //     // so we can just add them.
        //     this.outputs.addAll(outputsToUpdateMap.values());
        // }
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        final Command that = (Command) o;
        return Objects.equals(this.name, that.name) &&
                Objects.equals(this.label, that.label) &&
                Objects.equals(this.description, that.description) &&
                Objects.equals(this.version, that.version) &&
                Objects.equals(this.schemaVersion, that.schemaVersion) &&
                Objects.equals(this.infoUrl, that.infoUrl) &&
                Objects.equals(this.image, that.image) &&
                Objects.equals(this.workingDirectory, that.workingDirectory) &&
                Objects.equals(this.commandLine, that.commandLine) &&
                Objects.equals(this.mounts, that.mounts) &&
                Objects.equals(this.environmentVariables, that.environmentVariables) &&
                Objects.equals(this.inputs, that.inputs) &&
                Objects.equals(this.outputs, that.outputs) &&
                Objects.equals(this.xnatCommandWrappers, that.xnatCommandWrappers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), name, label, description, version, schemaVersion, infoUrl, image,
                workingDirectory, commandLine, mounts, environmentVariables, inputs, outputs, xnatCommandWrappers);
    }

    @Override
    public ToStringHelper addParentPropertiesToString(final ToStringHelper helper) {
        return super.addParentPropertiesToString(helper)
                .add("name", name)
                .add("label", label)
                .add("description", description)
                .add("version", version)
                .add("schemaVersion", schemaVersion)
                .add("infoUrl", infoUrl)
                .add("image", image)
                .add("workingDirectory", workingDirectory)
                .add("commandLine", commandLine)
                .add("mounts", mounts)
                .add("environmentVariables", environmentVariables)
                .add("inputs", inputs)
                .add("outputs", outputs)
                .add("xnatCommandWrappers", xnatCommandWrappers);
    }

    @Override
    public String toString() {
        return addParentPropertiesToString(MoreObjects.toStringHelper(this))
                .toString();
    }

    public enum Type {
        DOCKER("docker");

        private final String name;

        @JsonCreator
        Type(final String name) {
            this.name = name;
        }

        @JsonValue
        public String getName() {
            return name;
        }
    }
}
