package org.nrg.containers.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.swagger.annotations.ApiModelProperty;

import javax.annotation.Nullable;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Embeddable
public class CommandRun implements Serializable {
    @JsonProperty("command-line") private String commandLine;
    @JsonProperty("mounts") private List<CommandMount> mounts = Lists.newArrayList();
    @JsonProperty("environment-variables") private Map<String, String> environmentVariables = Maps.newHashMap();
    private Map<String, String> ports = Maps.newHashMap();

    @Nullable
    @ApiModelProperty("The command that will be executed in the container when the Command is launched.")
    public String getCommandLine() {
        return commandLine;
    }

    public void setCommandLine(final String commandLine) {
        this.commandLine = commandLine;
    }

    @Nullable
    @ElementCollection
    public List<CommandMount> getMounts() {
        return mounts;
    }

    public void setMounts(final List<CommandMount> mounts) {
        this.mounts = mounts == null ?
                Lists.<CommandMount>newArrayList() :
                mounts;
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

    @ElementCollection
    public Map<String, String> getPorts() {
        return ports;
    }

    public void setPorts(final Map<String, String> ports) {
        this.ports = ports == null ?
                Maps.<String, String>newHashMap() :
                ports;

    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final CommandRun that = (CommandRun) o;
        return Objects.equals(this.commandLine, that.commandLine) &&
                Objects.equals(this.mounts, that.mounts) &&
                Objects.equals(this.environmentVariables, that.environmentVariables) &&
                Objects.equals(this.ports, that.ports);
    }

    @Override
    public int hashCode() {
        return Objects.hash(commandLine, mounts, environmentVariables, ports);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("commandLine", commandLine)
                .add("mounts", mounts)
                .add("environmentVariables", environmentVariables)
                .add("ports", ports)
                .toString();
    }
}
