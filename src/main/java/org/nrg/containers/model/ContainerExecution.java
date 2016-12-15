package org.nrg.containers.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntity;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Entity
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"containerId"})})
public class ContainerExecution extends AbstractHibernateEntity {
    @JsonProperty("command-id") private Long commandId;
    @JsonProperty("docker-image") private String dockerImage;
    @JsonProperty("command-line") private String commandLine;
    @JsonProperty("env") private Map<String, String> environmentVariables = Maps.newHashMap();
    @JsonProperty("mounts-in") private List<ContainerExecutionMount> mountsIn = Lists.newArrayList();
    @JsonProperty("mounts-out") private List<ContainerExecutionMount> mountsOut = Lists.newArrayList();
    @JsonProperty("container-id") private String containerId;
    @JsonProperty("user-id") private String userId;
    @JsonProperty("input-values") private Map<String, String> inputValues = Maps.newHashMap();
    private List<ContainerExecutionOutput> outputs;
    private List<ContainerExecutionHistory> history = Lists.newArrayList();

    public ContainerExecution() {}

    public ContainerExecution(final ResolvedCommand resolvedCommand,
                              final String containerId,
                              final String userId) {
        this.containerId = containerId;
        this.userId = userId;

        this.commandId = resolvedCommand.getCommandId();
        this.dockerImage = resolvedCommand.getDockerImage();
        this.commandLine = resolvedCommand.getCommandLine();
        this.environmentVariables = resolvedCommand.getEnvironmentVariables() == null ?
                Maps.<String, String>newHashMap() :
                Maps.newHashMap(resolvedCommand.getEnvironmentVariables());
        this.mountsIn = resolvedCommand.getMountsIn() == null ?
                Lists.<ContainerExecutionMount>newArrayList() :
                Lists.newArrayList(resolvedCommand.getMountsIn());
        this.mountsOut = resolvedCommand.getMountsOut() == null ?
                Lists.<ContainerExecutionMount>newArrayList() :
                Lists.newArrayList(resolvedCommand.getMountsOut());
        this.inputValues = resolvedCommand.getInputValues() == null ?
                Maps.<String, String>newHashMap() :
                Maps.newHashMap(resolvedCommand.getInputValues());
        this.outputs = resolvedCommand.getOutputs() == null ?
                Lists.<ContainerExecutionOutput>newArrayList() :
                Lists.newArrayList(resolvedCommand.getOutputs());
    }

    public Long getCommandId() {
        return commandId;
    }

    public void setCommandId(final Long commandId) {
        this.commandId = commandId;
    }

    public String getDockerImage() {
        return dockerImage;
    }

    public void setDockerImage(final String dockerImage) {
        this.dockerImage = dockerImage;
    }

    public String getCommandLine() {
        return commandLine;
    }

    public void setCommandLine(final String commandLine) {
        this.commandLine = commandLine;
    }

    @ElementCollection
    public Map<String, String> getEnvironmentVariables() {
        return environmentVariables;
    }

    public void setEnvironmentVariables(final Map<String, String> environmentVariables) {
        this.environmentVariables = environmentVariables;
    }

    @ElementCollection
    public List<ContainerExecutionMount> getMountsIn() {
        return mountsIn;
    }

    public void setMountsIn(final List<ContainerExecutionMount> mountsIn) {
        this.mountsIn = mountsIn;
    }

    @ElementCollection
    public List<ContainerExecutionMount> getMountsOut() {
        return mountsOut;
    }

    public void setMountsOut(final List<ContainerExecutionMount> mountsOut) {
        this.mountsOut = mountsOut;
    }

    public String getContainerId() {
        return containerId;
    }

    public void setContainerId(final String containerId) {
        this.containerId = containerId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(final String user) {
        this.userId = user;
    }

    @ElementCollection
    @Column(columnDefinition = "TEXT")
    public Map<String, String> getInputValues() {
        return inputValues;
    }

    public void setInputValues(final Map<String, String> inputValues) {
        this.inputValues = inputValues;
    }

    @ElementCollection
    public List<ContainerExecutionOutput> getOutputs() {
        return outputs;
    }

    public void setOutputs(final List<ContainerExecutionOutput> outputs) {
        this.outputs = outputs;
    }

    @ElementCollection
    public List<ContainerExecutionHistory> getHistory() {
        return history;
    }

    public void setHistory(final List<ContainerExecutionHistory> history) {
        this.history = history;
    }

    @Transient
    public void addToHistory(final ContainerExecutionHistory historyItem) {
        if (this.history == null) {
            this.history = Lists.newArrayList();
        }
        this.history.add(historyItem);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        final ContainerExecution that = (ContainerExecution) o;
        return Objects.equals(this.commandId, that.commandId) &&
                Objects.equals(this.dockerImage, that.dockerImage) &&
                Objects.equals(this.commandLine, that.commandLine) &&
                Objects.equals(this.environmentVariables, that.environmentVariables) &&
                Objects.equals(this.mountsIn, that.mountsIn) &&
                Objects.equals(this.mountsOut, that.mountsOut) &&
                Objects.equals(this.containerId, that.containerId) &&
                Objects.equals(this.userId, that.userId) &&
                Objects.equals(this.inputValues, that.inputValues) &&
                Objects.equals(this.outputs, that.outputs) &&
                Objects.equals(this.history, that.history);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.getId(), commandId, dockerImage, commandLine, environmentVariables,
                mountsIn, mountsOut, containerId, userId, inputValues, outputs, history);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("commandId", commandId)
                .add("dockerImage", dockerImage)
                .add("commandLine", commandLine)
                .add("environmentVariables", environmentVariables)
                .add("mountsIn", mountsIn)
                .add("mountsOut", mountsOut)
                .add("containerId", containerId)
                .add("userId", userId)
                .add("inputValues", inputValues)
                .add("outputs", outputs)
                .add("history", history)
                .toString();
    }
}
