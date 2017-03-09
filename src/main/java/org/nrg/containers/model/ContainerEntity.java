package org.nrg.containers.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.envers.Audited;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntity;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Transient;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Entity
@Audited
public class ContainerEntity extends AbstractHibernateEntity {
    @JsonProperty("command-id") private long commandId;
    @JsonProperty("xnat-command-wrapper-id") private long xnatCommandWrapperId;
    @JsonProperty("docker-image") private String dockerImage;
    @JsonProperty("command-line") private String commandLine;
    @JsonProperty("env") private Map<String, String> environmentVariables = Maps.newHashMap();
    @JsonProperty("mounts") private List<ContainerEntityMount> mounts = Lists.newArrayList();
    @JsonProperty("container-id") private String containerId;
    @JsonProperty("user-id") private String userId;
    @JsonProperty("raw-input-values") private Map<String, String> rawInputValues;
    @JsonProperty("xnat-input-values") private Map<String, String> xnatInputValues;
    @JsonProperty("command-input-values") private Map<String, String> commandInputValues;
    private List<ContainerEntityOutput> outputs;
    private List<ContainerEntityHistory> history = Lists.newArrayList();
    @JsonProperty("log-paths") private Set<String> logPaths;

    public ContainerEntity() {}

    public ContainerEntity(final ResolvedCommand resolvedCommand,
                           final String containerId,
                           final String userId) {
        this.containerId = containerId;
        this.userId = userId;

        this.commandId = resolvedCommand.getCommandId();
        this.xnatCommandWrapperId = resolvedCommand.getXnatCommandWrapperId();
        this.dockerImage = resolvedCommand.getImage();
        this.commandLine = resolvedCommand.getCommandLine();
        setEnvironmentVariables(resolvedCommand.getEnvironmentVariables());
        setMounts(resolvedCommand.getMounts());
        setRawInputValues(resolvedCommand.getRawInputValues());
        setXnatInputValues(resolvedCommand.getXnatInputValues());
        setCommandInputValues(resolvedCommand.getCommandInputValues());
        setOutputs(resolvedCommand.getOutputs());
        setLogPaths(null);
    }

    public long getCommandId() {
        return commandId;
    }

    public void setCommandId(final long commandId) {
        this.commandId = commandId;
    }

    public long getXnatCommandWrapperId() {
        return xnatCommandWrapperId;
    }

    public void setXnatCommandWrapperId(final long xnatCommandWrapperId) {
        this.xnatCommandWrapperId = xnatCommandWrapperId;
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
        this.environmentVariables = environmentVariables == null ?
                Maps.<String, String>newHashMap() :
                environmentVariables;
    }

    @OneToMany(mappedBy = "containerEntity", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<ContainerEntityMount> getMounts() {
        return mounts;
    }

    public void setMounts(final List<ContainerEntityMount> mounts) {
        this.mounts = mounts == null ?
                Lists.<ContainerEntityMount>newArrayList() :
                mounts;
        for (final ContainerEntityMount mount : this.mounts) {
            mount.setContainerEntity(this);
        }
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
    public Map<String, String> getRawInputValues() {
        return rawInputValues;
    }

    public void setRawInputValues(final Map<String, String> rawInputValues) {
        this.rawInputValues = rawInputValues == null ?
                Maps.<String, String>newHashMap() :
                rawInputValues;
    }

    @ElementCollection
    @Column(columnDefinition = "TEXT")
    public Map<String, String> getXnatInputValues() {
        return xnatInputValues;
    }

    public void setXnatInputValues(final Map<String, String> xnatInputValues) {
        this.xnatInputValues = xnatInputValues == null ?
                Maps.<String, String>newHashMap() :
                xnatInputValues;
    }

    @ElementCollection
    @Column(columnDefinition = "TEXT")
    public Map<String, String> getCommandInputValues() {
        return commandInputValues;
    }

    public void setCommandInputValues(final Map<String, String> commandInputValues) {
        this.commandInputValues = commandInputValues == null ?
                Maps.<String, String>newHashMap() :
                commandInputValues;
    }

    @OneToMany(mappedBy = "containerEntity", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<ContainerEntityOutput> getOutputs() {
        return outputs;
    }

    public void setOutputs(final List<ContainerEntityOutput> outputs) {
        this.outputs = outputs == null ?
                Lists.<ContainerEntityOutput>newArrayList() :
                outputs;
        for (final ContainerEntityOutput output : this.outputs) {
            output.setContainerEntity(this);
        }
    }

    @OneToMany(mappedBy = "containerEntity", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<ContainerEntityHistory> getHistory() {
        return history;
    }

    public void setHistory(final List<ContainerEntityHistory> history) {
        this.history = history == null ?
                Lists.<ContainerEntityHistory>newArrayList() :
                history;
        for (final ContainerEntityHistory historyItem : this.history) {
            addToHistory(historyItem);
        }
    }

    @Transient
    public void addToHistory(final ContainerEntityHistory historyItem) {
        if (historyItem == null) {
            return;
        }
        historyItem.setContainerEntity(this);
        if (this.history == null) {
            this.history = Lists.newArrayList();
        }
        this.history.add(historyItem);
    }

    @ElementCollection
    public Set<String> getLogPaths() {
        return logPaths;
    }

    public void setLogPaths(final Set<String> logPaths) {
        this.logPaths = logPaths;
    }

    @Transient
    public void addLogPath(final String logPath) {
        if (StringUtils.isBlank(logPath)) {
            return;
        }

        if (this.logPaths == null) {
            this.logPaths = Sets.newHashSet();
        }
        this.logPaths.add(logPath);
    }

    @Transient
    public void addLogPaths(final Set<String> logPaths) {
        if (logPaths == null || logPaths.isEmpty()) {
            return;
        }

        for (final String logPath : logPaths) {
            addLogPath(logPath);
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final ContainerEntity that = (ContainerEntity) o;
        return Objects.equals(this.getId(), that.getId()) &&
                Objects.equals(this.containerId, that.containerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.getId(), containerId);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("containerId", containerId)
                .add("commandId", commandId)
                .add("xnatCommandWrapperId", xnatCommandWrapperId)
                .add("dockerImage", dockerImage)
                .add("commandLine", commandLine)
                .add("environmentVariables", environmentVariables)
                .add("mounts", mounts)
                .add("userId", userId)
                .add("rawInputValues", rawInputValues)
                .add("xnatInputValues", xnatInputValues)
                .add("commandInputValues", commandInputValues)
                .add("outputs", outputs)
                .add("history", history)
                .add("logPaths", logPaths)
                .toString();
    }
}
