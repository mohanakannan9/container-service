package org.nrg.containers.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.FetchType;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Embeddable
public class ResolvedCommand implements Serializable {

    @JsonProperty("command-id") private Long commandId;
    @JsonProperty("docker-image") private String dockerImage;
    @JsonProperty("command-line") private String commandLine;
    @JsonProperty("env") private Map<String, String> environmentVariables;
    @JsonProperty("mounts-in") private List<CommandMount> mountsIn;
    @JsonProperty("mounts-out") private List<CommandMount> mountsOut;
    @JsonProperty("input-values") private Map<String, String> inputValues;
    private List<CommandOutput> outputs;

    public ResolvedCommand() {}

    public ResolvedCommand(final Command command) {
        this.commandId = command.getId();
        this.dockerImage = command.getDockerImage();
    }

    public Long getCommandId() {
        return commandId;
    }

    public void setCommandId(final Long commandId) {
        this.commandId = commandId;
    }

    public String getCommandLine() {
        return commandLine;
    }

    public void setCommandLine(final String commandLine) {
        this.commandLine = commandLine;
    }

    public String getDockerImage() {
        return dockerImage;
    }

    public void setDockerImage(final String dockerImage) {
        this.dockerImage = dockerImage;
    }

    @ElementCollection
    public Map<String, String> getEnvironmentVariables() {
        return environmentVariables;
    }

    public void setEnvironmentVariables(final Map<String, String> environmentVariables) {
        this.environmentVariables = environmentVariables == null ?
                Maps.<String, String>newHashMap() :
                Maps.newHashMap(environmentVariables);
    }

    @Transient
    public void addEnvironmentVariables(final Map<String, String> environmentVariables) {
        if (environmentVariables != null) {
            if (this.environmentVariables == null) {
                this.environmentVariables = Maps.newHashMap();
            }
            this.environmentVariables.putAll(environmentVariables);
        }
    }

    @ElementCollection(fetch = FetchType.EAGER)
    public List<CommandMount> getMountsIn() {
        return mountsIn;
    }

    public void setMountsIn(final List<CommandMount> mountsIn) {
        this.mountsIn = mountsIn == null ?
                Lists.<CommandMount>newArrayList() :
                Lists.newArrayList(mountsIn);
    }

    @ElementCollection(fetch = FetchType.EAGER)
    public List<CommandMount> getMountsOut() {
        return mountsOut;
    }

    public void setMountsOut(final List<CommandMount> mountsOut) {
        this.mountsOut = mountsOut == null ?
                Lists.<CommandMount>newArrayList() :
                Lists.newArrayList(mountsOut);
    }

    @Transient
    @JsonIgnore
    public void setMounts(final List<CommandMount> mounts) {
        final List<CommandMount> mountsIn = Lists.newArrayList();
        final List<CommandMount> mountsOut = Lists.newArrayList();
        for (final CommandMount mount : mounts) {
            if (mount.isInput()) {
                mountsIn.add(mount);
            } else {
                mountsOut.add(mount);
            }
        }
        setMountsIn(mountsIn);
        setMountsOut(mountsOut);
    }


    @ElementCollection
    public Map<String, String> getInputValues() {
        return inputValues;
    }

    public void setInputValues(final Map<String, String> inputValues) {
        this.inputValues = inputValues == null ?
                Maps.<String, String>newHashMap() :
                Maps.newHashMap(inputValues);
    }


    @ElementCollection
    public List<CommandOutput> getOutputs() {
        return outputs;
    }

    public void setOutputs(final List<CommandOutput> outputs) {
        this.outputs = outputs == null ?
                Lists.<CommandOutput>newArrayList() :
                Lists.newArrayList(outputs);
    }

    @Transient
    public void addOutput(final CommandOutput output) {
        if (this.outputs == null) {
            this.outputs = Lists.newArrayList();
        }

        this.outputs.add(output);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final ResolvedCommand that = (ResolvedCommand) o;
        return Objects.equals(this.commandId, that.commandId) &&
                Objects.equals(this.commandLine, that.commandLine) &&
                Objects.equals(this.dockerImage, that.dockerImage) &&
                Objects.equals(this.environmentVariables, that.environmentVariables) &&
                Objects.equals(this.mountsIn, that.mountsIn) &&
                Objects.equals(this.mountsOut, that.mountsOut) &&
                Objects.equals(this.inputValues, that.inputValues) &&
                Objects.equals(this.outputs, that.outputs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(commandId, commandLine, dockerImage, environmentVariables, mountsIn, mountsOut, inputValues, outputs);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("commandId", commandId)
                .add("commandLine", commandLine)
                .add("dockerImage", dockerImage)
                .add("environmentVariables", environmentVariables)
                .add("mountsIn", mountsIn)
                .add("mountsOut", mountsOut)
                .add("inputValues", inputValues)
                .add("outputs", outputs)
                .toString();
    }
}
