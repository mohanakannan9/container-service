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
    @JsonProperty("run") private List<String> run;
    @JsonProperty("docker-image-id") private String dockerImageId;
    @JsonProperty("env") private Map<String, String> environmentVariables = Maps.newHashMap();
    @JsonProperty("mounts-in") private List<ResolvedCommandMount> mountsIn = Lists.newArrayList();
    @JsonProperty("mounts-out") private List<ResolvedCommandMount> mountsOut = Lists.newArrayList();

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
        this.environmentVariables = environmentVariables == null ?
                Maps.<String, String>newHashMap() :
                environmentVariables;
    }

    @ElementCollection(fetch = FetchType.EAGER)
    public List<ResolvedCommandMount> getMountsIn() {
        return mountsIn;
    }

    public void setMountsIn(final List<ResolvedCommandMount> mountsIn) {
        this.mountsIn = mountsIn == null ?
                Lists.<ResolvedCommandMount>newArrayList() :
                mountsIn;
    }

    @Transient
    @JsonIgnore
    public void setMountsIn(final Map<String, String> commandMounts) {
        setMountsIn(resolveCommandMounts(commandMounts));
    }

    @ElementCollection(fetch = FetchType.EAGER)
    public List<ResolvedCommandMount> getMountsOut() {
        return mountsOut;
    }

    public void setMountsOut(final List<ResolvedCommandMount> mountsOut) {
        this.mountsOut = mountsOut == null ?
                Lists.<ResolvedCommandMount>newArrayList() :
                mountsOut;
    }

    @Transient
    @JsonIgnore
    public void setMountsOut(final Map<String, String> commandMounts) {
        setMountsOut(resolveCommandMounts(commandMounts));
    }

    @Transient
    @JsonIgnore
    private List<ResolvedCommandMount> resolveCommandMounts(final Map<String, String> commandMounts) {
        final List<ResolvedCommandMount> resolvedMounts = Lists.newArrayList();
        for (final Map.Entry<String, String> mount : commandMounts.entrySet()) {
            resolvedMounts.add(new ResolvedCommandMount(mount.getKey(), mount.getValue(), true));
        }
        return resolvedMounts;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
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
        return Objects.hash(commandId, run, dockerImageId, environmentVariables, mountsIn, mountsOut);
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
