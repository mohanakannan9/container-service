package org.nrg.execution.model;

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
    @JsonProperty("run") private List<String> run;
    @JsonProperty("env") private Map<String, String> environmentVariables = Maps.newHashMap();
    @JsonProperty("mounts-in") private List<CommandMount> mountsIn = Lists.newArrayList();
    @JsonProperty("mounts-out") private List<CommandMount> mountsOut = Lists.newArrayList();

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

    @ElementCollection
    public List<String> getRun() {
        return run;
    }

    public void setRun(final List<String> run) {
        this.run = run == null ?
                Lists.<String>newArrayList() :
                run;
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
                environmentVariables;
    }

    @ElementCollection(fetch = FetchType.EAGER)
    public List<CommandMount> getMountsIn() {
        return mountsIn;
    }

    public void setMountsIn(final List<CommandMount> mountsIn) {
        this.mountsIn = mountsIn == null ?
                Lists.<CommandMount>newArrayList() :
                mountsIn;
        for (final CommandMount mountIn : this.mountsIn) {
            mountIn.setReadOnly(true);
        }
    }

    @ElementCollection(fetch = FetchType.EAGER)
    public List<CommandMount> getMountsOut() {
        return mountsOut;
    }

    public void setMountsOut(final List<CommandMount> mountsOut) {
        this.mountsOut = mountsOut == null ?
                Lists.<CommandMount>newArrayList() :
                mountsOut;
        for (final CommandMount mountOut : this.mountsIn) {
            mountOut.setReadOnly(false);
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final ResolvedCommand that = (ResolvedCommand) o;
        return Objects.equals(this.commandId, that.commandId) &&
                Objects.equals(this.run, that.run) &&
                Objects.equals(this.dockerImage, that.dockerImage) &&
                Objects.equals(this.environmentVariables, that.environmentVariables) &&
                Objects.equals(this.mountsIn, that.mountsIn) &&
                Objects.equals(this.mountsOut, that.mountsOut);
    }

    @Override
    public int hashCode() {
        return Objects.hash(commandId, run, dockerImage, environmentVariables, mountsIn, mountsOut);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("commandId", commandId)
                .add("run", run)
                .add("dockerImage", dockerImage)
                .add("environmentVariables", environmentVariables)
                .add("mountsIn", mountsIn)
                .add("mountsOut", mountsOut)
                .toString();
    }
}
