package org.nrg.actions.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import org.nrg.containers.model.DockerImage;

import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Embeddable
public class ResolvedCommand implements Serializable {

    @JsonProperty("command-id") private Long commandId;
    @JsonProperty("run") private String run;
    @JsonProperty("docker-image-id") private String dockerImageId;
    @JsonProperty("env") private Map<String, String> environmentVariables;
    @JsonProperty("mounts-in") private List<ResolvedCommandMount> mountsIn;
    @JsonProperty("mounts-out") private List<ResolvedCommandMount> mountsOut;

    public Long getCommandId() {
        return commandId;
    }

    public void setCommandId(final Long commandId) {
        this.commandId = commandId;
    }

    public String getRun() {
        return run;
    }

    public void setRun(final String run) {
        this.run = run;
    }

    public String getDockerImageId() {
        return dockerImageId;
    }

    public void setDockerImageId(final String dockerImageId) {
        this.dockerImageId = dockerImageId;
    }

    @ElementCollection
    public Map<String, String> getEnvironmentVariables() {
        return environmentVariables;
    }

    public void setEnvironmentVariables(final Map<String, String> environmentVariables) {
        this.environmentVariables = environmentVariables;
    }

    @ElementCollection
    public List<ResolvedCommandMount> getMountsIn() {
        return mountsIn;
    }

    public void setMountsIn(final List<ResolvedCommandMount> mountsIn) {
        this.mountsIn = mountsIn;
    }

    public void setMountsInFromCommandMounts(final List<CommandMount> commandMounts) {
        this.mountsIn = Lists.newArrayList();
        for (final CommandMount mount : commandMounts) {
            this.mountsIn.add(new ResolvedCommandMount(mount, true));
        }
    }

    @ElementCollection
    public List<ResolvedCommandMount> getMountsOut() {
        return mountsOut;
    }

    public void setMountsOut(final List<ResolvedCommandMount> mountsOut) {
        this.mountsOut = mountsOut;
    }

    @Transient
    public void setMountsOutFromCommandMounts(final List<CommandMount> commandMounts) {
        this.mountsOut = Lists.newArrayList();
        for (final CommandMount mount : commandMounts) {
            this.mountsOut.add(new ResolvedCommandMount(mount, false));
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        final ResolvedCommand that = (ResolvedCommand) o;
        return Objects.equals(this.commandId, that.commandId) &&
                Objects.equals(this.run, that.run) &&
                Objects.equals(this.dockerImageId, that.dockerImageId) &&
                Objects.equals(this.environmentVariables, that.environmentVariables) &&
                Objects.equals(this.mountsIn, that.mountsIn) &&
                Objects.equals(this.mountsOut, that.mountsOut);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), commandId, run, dockerImageId, environmentVariables, mountsIn, mountsOut);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("commandId", commandId)
                .add("run", run)
                .add("dockerImageId", dockerImageId)
                .add("environmentVariables", environmentVariables)
                .add("mountsIn", mountsIn)
                .add("mountsOut", mountsOut)
                .toString();
    }
}
