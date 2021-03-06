package org.nrg.containers.model.container.entity;

import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.hibernate.envers.Audited;
import org.nrg.containers.model.container.ContainerInputType;
import org.nrg.containers.model.container.auto.Container;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntity;

import javax.persistence.CascadeType;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Transient;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Entity
@Audited
public class ContainerEntity extends AbstractHibernateEntity {
    public static Map<String, String> STANDARD_STATUS_MAP = ImmutableMap.<String, String>builder()
            .put("complete", "Complete")
            .put("created", "Created")
            .put("rejected", "Failed")
            .put("failed", "Failed")
            .put("start", "Running")
            .put("started", "Running")
            .put("running", "Running")
            .put("kill", "Killed")
            .put("oom", "Killed (Out of Memory)")
            .put("starting", "Starting")
            .build();
    private static final Set<String> TERMINAL_STATI = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "Complete", "Failed", "Killed"
    )));

    private long commandId;
    private long wrapperId;
    private String status;
    private Date statusTime;
    private String dockerImage;
    private String commandLine;
    private Boolean overrideEntrypoint;
    private String workingDirectory;
    private Map<String, String> environmentVariables = Maps.newHashMap();
    private Map<String, String> ports = new HashMap<>();
    private List<ContainerEntityMount> mounts = Lists.newArrayList();
    private String containerId;
    private String workflowId;
    private String userId;
    private Boolean swarm;
    private String serviceId;
    private String taskId;
    private String nodeId;
    private String subtype;
    private ContainerEntity parentContainerEntity;
    private String parentSourceObjectName;
    private List<ContainerEntityInput> inputs;
    private List<ContainerEntityOutput> outputs;
    private List<ContainerEntityHistory> history = Lists.newArrayList();
    private List<String> logPaths;
    private Long reserveMemory;
    private Long limitMemory;
    private Double limitCpu;

    public ContainerEntity() {}

    public static ContainerEntity fromPojo(final Container containerPojo) {
        if (containerPojo == null) {
            return null; // This is for the setup container parent, which may be null
        }
        final ContainerEntity containerEntity = new ContainerEntity();
        containerEntity.update(containerPojo);
        return containerEntity;
    }

    public ContainerEntity update(final Container containerPojo) {
        this.setId(containerPojo.databaseId());
        this.setStatus(containerPojo.status());
        this.setStatusTime(containerPojo.statusTime());
        this.setCommandId(containerPojo.commandId());
        this.setWrapperId(containerPojo.wrapperId());
        this.setContainerId(containerPojo.containerId());
        this.setWorkflowId(containerPojo.workflowId());
        this.setUserId(containerPojo.userId());
        this.setServiceId(containerPojo.serviceId());
        this.setTaskId(containerPojo.taskId());
        this.setNodeId(containerPojo.nodeId());
        this.setSwarm(containerPojo.swarm());
        this.setDockerImage(containerPojo.dockerImage());
        this.setCommandLine(containerPojo.commandLine());
        this.setWorkingDirectory(containerPojo.workingDirectory());
        this.setSubtype(containerPojo.subtype());
        this.setParentContainerEntity(fromPojo(containerPojo.parent()));
        this.setParentSourceObjectName(containerPojo.parentSourceObjectName());
        this.setEnvironmentVariables(containerPojo.environmentVariables());
        this.setPorts(containerPojo.ports());
        this.setLogPaths(containerPojo.logPaths());
        this.setMounts(Lists.newArrayList(Lists.transform(
                containerPojo.mounts(), new Function<Container.ContainerMount, ContainerEntityMount>() {
                    @Override
                    public ContainerEntityMount apply(final Container.ContainerMount input) {
                        return ContainerEntityMount.fromPojo(input);
                    }
                }))
        );
        this.setInputs(Lists.newArrayList(Lists.transform(
                containerPojo.inputs(), new Function<Container.ContainerInput, ContainerEntityInput>() {
                    @Override
                    public ContainerEntityInput apply(final Container.ContainerInput input) {
                        return ContainerEntityInput.fromPojo(input);
                    }
                }))
        );
        this.setOutputs(Lists.newArrayList(Lists.transform(
                containerPojo.outputs(), new Function<Container.ContainerOutput, ContainerEntityOutput>() {
                    @Override
                    public ContainerEntityOutput apply(final Container.ContainerOutput input) {
                        return ContainerEntityOutput.fromPojo(input);
                    }
                }))
        );
        this.setHistory(Lists.newArrayList(Lists.transform(
                containerPojo.history(), new Function<Container.ContainerHistory, ContainerEntityHistory>() {
                    @Override
                    public ContainerEntityHistory apply(final Container.ContainerHistory input) {
                        return ContainerEntityHistory.fromPojo(input);
                    }
                }))
        );
        this.setReserveMemory(containerPojo.reserveMemory());
        this.setLimitMemory(containerPojo.limitMemory());
        this.setLimitCpu(containerPojo.limitCpu());

        return this;
    }

    public long getCommandId() {
        return commandId;
    }

    public void setCommandId(final long commandId) {
        this.commandId = commandId;
    }

    public long getWrapperId() {
        return wrapperId;
    }

    public void setWrapperId(final long wrapperId) {
        this.wrapperId = wrapperId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = STANDARD_STATUS_MAP.containsKey(status) ? STANDARD_STATUS_MAP.get(status) : status;
    }

    @Transient
    public boolean statusIsTerminal() {
        if (status != null) {
            for (final String terminalStatus : TERMINAL_STATI) {
                if (status.contains(terminalStatus)) {
                    return true;
                }
            }
        }
        return false;
    }

    public Date getStatusTime() {
        return statusTime;
    }

    public void setStatusTime(final Date statusTime) {
        this.statusTime = statusTime == null ? null : new Date(statusTime.getTime());
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

    public Boolean getOverrideEntrypoint() {
        return overrideEntrypoint;
    }

    public void setOverrideEntrypoint(final Boolean overrideEntrypoint) {
        this.overrideEntrypoint = overrideEntrypoint;
    }

    public String getWorkingDirectory() {
        return workingDirectory;
    }

    public void setWorkingDirectory(final String workingDirectory) {
        this.workingDirectory = workingDirectory;
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

    @ElementCollection
    public Map<String, String> getPorts() {
        return ports;
    }

    public void setPorts(final Map<String, String> ports) {
        this.ports = ports == null ? new HashMap<String, String>() : ports;
    }

    public String getSubtype() {
        return subtype;
    }

    public void setSubtype(final String subtype) {
        this.subtype = subtype;
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

    public String getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(final String workflowId) {
        this.workflowId = workflowId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(final String user) {
        this.userId = user;
    }

    public Boolean getSwarm() {
        return swarm;
    }

    public void setSwarm(final Boolean swarm) {
        this.swarm = swarm != null && swarm;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(final String serviceId) {
        this.serviceId = serviceId;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(final String taskId) {
        this.taskId = taskId;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(final String nodeId) {
        this.nodeId = nodeId;
    }

    public Long getReserveMemory() {
        return reserveMemory;
    }

    public void setReserveMemory(final Long reserveMemory) {
        this.reserveMemory = reserveMemory;
    }

    public Long getLimitMemory() {
        return limitMemory;
    }

    public void setLimitMemory(final Long limitMemory) {
        this.limitMemory = limitMemory;
    }

    public Double getLimitCpu() {
        return limitCpu;
    }

    public void setLimitCpu(final Double limitCpu) {
        this.limitCpu = limitCpu;
    }

    @ManyToOne
    public ContainerEntity getParentContainerEntity() {
        return parentContainerEntity;
    }

    public void setParentContainerEntity(final ContainerEntity parentContainerEntity) {
        this.parentContainerEntity = parentContainerEntity;
    }

    public String getParentSourceObjectName() {
        return parentSourceObjectName;
    }

    public void setParentSourceObjectName(final String parentSourceObjectName) {
        this.parentSourceObjectName = parentSourceObjectName;
    }

    @OneToMany(mappedBy = "containerEntity", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<ContainerEntityInput> getInputs() {
        return inputs;
    }

    public void setInputs(final List<ContainerEntityInput> inputs) {
        this.inputs = inputs == null ?
                Lists.<ContainerEntityInput>newArrayList() :
                inputs;
        for (final ContainerEntityInput input : this.inputs) {
            input.setContainerEntity(this);
        }
    }

    public void addInput(final ContainerEntityInput input) {
        if (input == null) {
            return;
        }
        input.setContainerEntity(this);

        if (this.inputs == null) {
            this.inputs = Lists.newArrayList();
        }
        this.inputs.add(input);
    }

    @Transient
    public Map<String, String> getRawInputs() {
        return getInputs(ContainerInputType.RAW);
    }

    public void addRawInputs(final Map<String, String> rawInputValues) {
        addInputs(ContainerInputType.RAW, rawInputValues);
    }

    @Transient
    @SuppressWarnings("deprecation")
    public Map<String, String> getWrapperInputs() {
        final Map<String, String> wrapperInputs = Maps.newHashMap();
        wrapperInputs.putAll(getLegacyWrapperInputs());
        wrapperInputs.putAll(getExternalWrapperInputs());
        wrapperInputs.putAll(getDerivedWrapperInputs());
        return wrapperInputs;
    }

    @Transient
    public Map<String, String> getExternalWrapperInputs() {
        return getInputs(ContainerInputType.WRAPPER_EXTERNAL);
    }

    public void addExternalWrapperInputs(final Map<String, String> xnatInputValues) {
        addInputs(ContainerInputType.WRAPPER_EXTERNAL, xnatInputValues);
    }

    @Transient
    public Map<String, String> getDerivedWrapperInputs() {
        return getInputs(ContainerInputType.WRAPPER_DERIVED);
    }

    public void addDerivedWrapperInputs(final Map<String, String> xnatInputValues) {
        addInputs(ContainerInputType.WRAPPER_DERIVED, xnatInputValues);
    }

    /**
     * Get inputs of type "wrapper".
     * We no longer save inputs of this type. Now the wrapper inputs are separately saved
     * as type "wrapper_external" or "wrapper_derived". But we keep this here for legacy containers.
     * @return A map of wrapper input names to values.
     * @since 1.2
     */
    @Transient
    @Deprecated
    public Map<String, String> getLegacyWrapperInputs() {
        return getInputs(ContainerInputType.WRAPPER_DEPRECATED);
    }

    @Transient
    public Map<String, String> getCommandInputs() {
        return getInputs(ContainerInputType.COMMAND);
    }

    public void addCommandInputs(final Map<String, String> commandInputValues) {
        addInputs(ContainerInputType.COMMAND, commandInputValues);
    }

    private Map<String, String> getInputs(final ContainerInputType type) {
        if (this.inputs == null) {
            return null;
        }
        final Map<String, String> inputs = Maps.newHashMap();
        for (final ContainerEntityInput input : this.inputs) {
            if (input.getType() == type) {
                inputs.put(input.getName(), input.getValue());
            }
        }
        return inputs;
    }

    private void addInputs(final ContainerInputType type,
                           final Map<String, String> inputs) {
        if (inputs == null) {
            return;
        }
        for (final Map.Entry<String, String> inputEntry : inputs.entrySet()) {
            addInput(ContainerEntityInput.create(inputEntry.getKey(), inputEntry.getValue(), type));
        }
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
            historyItem.setContainerEntity(this);
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

    @Transient
    public boolean isItemInHistory(final ContainerEntityHistory historyItem) {
        return this.history != null && this.history.contains(historyItem);

    }

    @ElementCollection
    public List<String> getLogPaths() {
        return logPaths;
    }

    public void setLogPaths(final List<String> logPaths) {
        this.logPaths = logPaths;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final ContainerEntity that = (ContainerEntity) o;
        return Objects.equals(this.containerId, that.containerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(containerId);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("swarm", swarm)
                .add("containerId", containerId)
                .add("serviceId", serviceId)
                .add("taskId", taskId)
                .add("nodeId", nodeId)
                .add("userId", userId)
                .add("subtype", subtype)
                .add("parentContainerEntityId", parentContainerEntity == null ? null : parentContainerEntity.getId())
                .add("parentContainerEntityContainerId", parentContainerEntity == null ? null : parentContainerEntity.getContainerId())
                .add("parentSourceObjectName", parentSourceObjectName)
                .add("workflowId", workflowId)
                .add("commandId", commandId)
                .add("wrapperId", wrapperId)
                .add("status", status)
                .add("statusTime", statusTime)
                .add("dockerImage", dockerImage)
                .add("commandLine", commandLine)
                .add("overrideEntrypoint", overrideEntrypoint)
                .add("workingDirectory", workingDirectory)
                .add("environmentVariables", environmentVariables)
                .add("ports", ports)
                .add("mounts", mounts)
                .add("inputs", inputs)
                .add("outputs", outputs)
                .add("history", history)
                .add("logPaths", logPaths)
                .add("reserveMemory", reserveMemory)
                .add("limitMemory", limitMemory)
                .add("limitCpu", limitCpu)
                .toString();
    }
}
