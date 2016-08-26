package org.nrg.execution.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntity;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
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
    @JsonProperty("run") private List<String> run;
    @JsonProperty("env") private Map<String, String> environmentVariables = Maps.newHashMap();
    @JsonProperty("mounts-in") private List<CommandMount> mountsIn = Lists.newArrayList();
    @JsonProperty("mounts-out") private List<CommandMount> mountsOut = Lists.newArrayList();
    @JsonProperty("container-id") private String containerId;
    @JsonProperty("user-id") private String userId;
    @JsonProperty("root-id") private String rootObjectId;
    @JsonProperty("root-xsi-type") private String rootObjectXsiType;
    private List<ContainerExecutionHistory> history = Lists.newArrayList();

    public ContainerExecution() {}

    public ContainerExecution(final ResolvedCommand resolvedCommand,
                              final String containerId,
                              final String rootObjectId,
                              final String rootObjectXsiType,
                              final String userId) {
        this.containerId = containerId;
        this.userId = userId;
        this.rootObjectId = rootObjectId;
        this.rootObjectXsiType = rootObjectXsiType;

        this.commandId = resolvedCommand.getCommandId();
        this.dockerImage = resolvedCommand.getDockerImage();
        this.run = resolvedCommand.getRun() == null ?
                Lists.<String>newArrayList() :
                Lists.newArrayList(resolvedCommand.getRun());
        this.environmentVariables = resolvedCommand.getEnvironmentVariables() == null ?
                Maps.<String, String>newHashMap() :
                Maps.newHashMap(resolvedCommand.getEnvironmentVariables());
        this.mountsIn = resolvedCommand.getMountsIn() == null ?
                Lists.<CommandMount>newArrayList() :
                Lists.newArrayList(resolvedCommand.getMountsIn());
        this.mountsOut = resolvedCommand.getMountsOut() == null ?
                Lists.<CommandMount>newArrayList() :
                Lists.newArrayList(resolvedCommand.getMountsOut());
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

    @ElementCollection
    public List<String> getRun() {
        return run;
    }

    public void setRun(final List<String> run) {
        this.run = run;
    }

    @ElementCollection
    public Map<String, String> getEnvironmentVariables() {
        return environmentVariables;
    }

    public void setEnvironmentVariables(final Map<String, String> environmentVariables) {
        this.environmentVariables = environmentVariables;
    }

    @ElementCollection
    public List<CommandMount> getMountsIn() {
        return mountsIn;
    }

    public void setMountsIn(final List<CommandMount> mountsIn) {
        this.mountsIn = mountsIn;
    }

    @ElementCollection
    public List<CommandMount> getMountsOut() {
        return mountsOut;
    }

    public void setMountsOut(final List<CommandMount> mountsOut) {
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

    public String getRootObjectId() {
        return rootObjectId;
    }

    public void setRootObjectId(final String rootObjectId) {
        this.rootObjectId = rootObjectId;
    }

    public String getRootObjectXsiType() {
        return rootObjectXsiType;
    }

    public void setRootObjectXsiType(final String rootObjectXsiType) {
        this.rootObjectXsiType = rootObjectXsiType;
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
        return Objects.equals(commandId, that.commandId) &&
                Objects.equals(dockerImage, that.dockerImage) &&
                Objects.equals(run, that.run) &&
                Objects.equals(environmentVariables, that.environmentVariables) &&
                Objects.equals(mountsIn, that.mountsIn) &&
                Objects.equals(mountsOut, that.mountsOut) &&
                Objects.equals(containerId, that.containerId) &&
                Objects.equals(userId, that.userId) &&
                Objects.equals(rootObjectId, that.rootObjectId) &&
                Objects.equals(rootObjectXsiType, that.rootObjectXsiType) &&
                Objects.equals(history, that.history);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.getId(), commandId, dockerImage, run, environmentVariables,
                mountsIn, mountsOut, containerId, userId, rootObjectId, rootObjectXsiType, history);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("commandId", commandId)
                .add("dockerImage", dockerImage)
                .add("run", run)
                .add("environmentVariables", environmentVariables)
                .add("mountsIn", mountsIn)
                .add("mountsOut", mountsOut)
                .add("containerId", containerId)
                .add("userId", userId)
                .add("rootObjectId", rootObjectId)
                .add("rootObjectXsiType", rootObjectXsiType)
                .add("history", history)
                .toString();
    }
}
