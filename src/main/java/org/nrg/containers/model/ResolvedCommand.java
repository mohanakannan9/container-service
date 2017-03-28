package org.nrg.containers.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.nrg.containers.model.command.auto.Command;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@JsonTypeInfo(use = Id.NONE, include = As.PROPERTY, property = "type", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = ResolvedDockerCommand.class, name = "docker")
})
public abstract class ResolvedCommand implements Serializable {

    @JsonProperty("raw-input-values") private Map<String, String> rawInputValues;
    @JsonProperty("xnat-command-wrapper-id") private long xnatCommandWrapperId;
    @JsonProperty("xnat-input-values") private Map<String, String> xnatInputValues;
    @JsonProperty("command-id") private long commandId;
    @JsonProperty("command-input-values") private Map<String, String> commandInputValues;
    @JsonProperty("image") private String image;
    @JsonProperty("command-line") private String commandLine;
    @JsonProperty("env") private Map<String, String> environmentVariables;
    @JsonProperty("mounts") private List<ContainerEntityMount> mounts;
    private List<ContainerEntityOutput> outputs;
    @JsonProperty("working-directory") private String workingDirectory;

    public ResolvedCommand() {}

    public ResolvedCommand(final long xnatCommandWrapperId, final Command command) {
        this.xnatCommandWrapperId = xnatCommandWrapperId;
        this.commandId = command.id();
        this.image = command.image();
    }

    public abstract CommandType getType();

    public Map<String, String> getRawInputValues() {
        return rawInputValues;
    }

    public void setRawInputValues(final Map<String, String> rawInputValues) {
        this.rawInputValues = rawInputValues == null ?
                Maps.<String, String>newHashMap() :
                rawInputValues;
    }

    public Long getXnatCommandWrapperId() {
        return xnatCommandWrapperId;
    }

    public void setXnatCommandWrapperId(final long xnatCommandWrapperId) {
        this.xnatCommandWrapperId = xnatCommandWrapperId;
    }

    public Map<String, String> getXnatInputValues() {
        return xnatInputValues;
    }

    public void setXnatInputValues(final Map<String, String> xnatInputValues) {
        this.xnatInputValues = xnatInputValues;
    }

    public Long getCommandId() {
        return commandId;
    }

    public void setCommandId(final long commandId) {
        this.commandId = commandId;
    }

    public String getCommandLine() {
        return commandLine;
    }

    public void setCommandLine(final String commandLine) {
        this.commandLine = commandLine;
    }

    public String getImage() {
        return image;
    }

    public void setImage(final String image) {
        this.image = image;
    }

    public Map<String, String> getEnvironmentVariables() {
        return environmentVariables;
    }

    public void setEnvironmentVariables(final Map<String, String> environmentVariables) {
        this.environmentVariables = environmentVariables == null ?
                Maps.<String, String>newHashMap() :
                Maps.newHashMap(environmentVariables);
    }

    public void addEnvironmentVariables(final Map<String, String> environmentVariables) {
        if (environmentVariables != null) {
            if (this.environmentVariables == null) {
                this.environmentVariables = Maps.newHashMap();
            }
            this.environmentVariables.putAll(environmentVariables);
        }
    }

    public List<ContainerEntityMount> getMounts() {
        return mounts;
    }

    public void setMounts(final List<ContainerEntityMount> mounts) {
        this.mounts = mounts == null ?
                Lists.<ContainerEntityMount>newArrayList() :
                mounts;
    }

    public Map<String, String> getCommandInputValues() {
        return commandInputValues;
    }

    public void setCommandInputValues(final Map<String, String> commandInputValues) {
        this.commandInputValues = commandInputValues == null ?
                Maps.<String, String>newHashMap() :
                Maps.newHashMap(commandInputValues);
    }

    public List<ContainerEntityOutput> getOutputs() {
        return outputs;
    }

    public void setOutputs(final List<ContainerEntityOutput> outputs) {
        this.outputs = outputs == null ?
                Lists.<ContainerEntityOutput>newArrayList() :
                Lists.newArrayList(outputs);
    }

    public void addOutput(final ContainerEntityOutput output) {
        if (this.outputs == null) {
            this.outputs = Lists.newArrayList();
        }

        this.outputs.add(output);
    }

    public String getWorkingDirectory() {
        return workingDirectory;
    }

    public void setWorkingDirectory(final String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final ResolvedCommand that = (ResolvedCommand) o;
        return Objects.equals(this.xnatCommandWrapperId, that.xnatCommandWrapperId) &&
                Objects.equals(this.commandId, that.commandId) &&
                Objects.equals(this.rawInputValues, that.rawInputValues) &&
                Objects.equals(this.xnatInputValues, that.xnatInputValues) &&
                Objects.equals(this.commandInputValues, that.commandInputValues) &&
                Objects.equals(this.image, that.image) &&
                Objects.equals(this.commandLine, that.commandLine) &&
                Objects.equals(this.environmentVariables, that.environmentVariables) &&
                Objects.equals(this.mounts, that.mounts) &&
                Objects.equals(this.outputs, that.outputs) &&
                Objects.equals(this.workingDirectory, that.workingDirectory);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rawInputValues, xnatCommandWrapperId, xnatInputValues, commandId, commandInputValues,
                image, commandLine, environmentVariables, mounts, outputs, workingDirectory);
    }

    public MoreObjects.ToStringHelper addPropertiesToString(final MoreObjects.ToStringHelper helper) {
        return helper
                .add("xnatCommandWrapperId", xnatCommandWrapperId)
                .add("commandId", commandId)
                .add("image", image)
                .add("commandLine", commandLine)
                .add("rawInputValues", rawInputValues)
                .add("xnatInputValues", xnatInputValues)
                .add("commandInputValues", commandInputValues)
                .add("environmentVariables", environmentVariables)
                .add("mounts", mounts)
                .add("outputs", outputs)
                .add("workingDirectory", workingDirectory);
    }

    @Override
    public String toString() {
        return addPropertiesToString(MoreObjects.toStringHelper(this)).toString();
    }
}
