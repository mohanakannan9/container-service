package org.nrg.containers.model.container.auto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.nrg.containers.model.container.ContainerInputType;
import org.nrg.containers.model.container.entity.ContainerEntity;
import org.nrg.containers.model.container.entity.ContainerEntityHistory;
import org.nrg.containers.model.container.entity.ContainerEntityInput;
import org.nrg.containers.model.container.entity.ContainerEntityMount;
import org.nrg.containers.model.container.entity.ContainerEntityOutput;
import org.nrg.containers.model.container.entity.ContainerMountFilesEntity;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

@AutoValue
public abstract class Container {
    @JsonProperty("command-id") public abstract long commandId();
    @JsonProperty("wrapper-id") public abstract long wrapperId();
    @JsonProperty("container-id") public abstract String containerId();
    @JsonProperty("user-id") public abstract String userId();
    @JsonProperty("docker-image") public abstract String dockerImage();
    @JsonProperty("command-line") public abstract String commandLine();
    @JsonProperty("env") public abstract ImmutableMap<String, String> environmentVariables();
    @JsonProperty("mounts") public abstract ImmutableList<ContainerMount> mounts();
    @JsonProperty("inputs") public abstract ImmutableList<ContainerInput> inputs();
    @JsonProperty("outputs") public abstract ImmutableList<ContainerOutput> outputs();
    @JsonProperty("history") public abstract ImmutableList<ContainerHistory> history();
    @JsonProperty("log-paths") public abstract ImmutableList<String> logPaths();

    @JsonCreator
    public static Container create(@JsonProperty("command-id") final long commandId,
                                   @JsonProperty("wrapper-id") final long wrapperId,
                                   @JsonProperty("container-id") final String containerId,
                                   @JsonProperty("user-id") final String userId,
                                   @JsonProperty("docker-image") final String dockerImage,
                                   @JsonProperty("command-line") final String commandLine,
                                   @JsonProperty("env") final Map<String, String> environmentVariables,
                                   @JsonProperty("mounts") final List<ContainerMount> mounts,
                                   @JsonProperty("inputs") final List<ContainerInput> inputs,
                                   @JsonProperty("outputs") final List<ContainerOutput> outputs,
                                   @JsonProperty("history") final List<ContainerHistory> history,
                                   @JsonProperty("log-paths") final List<String> logPaths) {

        return builder()
                .commandId(commandId)
                .wrapperId(wrapperId)
                .containerId(containerId)
                .userId(userId)
                .dockerImage(dockerImage)
                .commandLine(commandLine)
                .environmentVariables(environmentVariables == null ? Collections.<String, String>emptyMap() : environmentVariables)
                .mounts(mounts == null ? Collections.<ContainerMount>emptyList() : mounts)
                .inputs(inputs == null ? Collections.<ContainerInput>emptyList() : inputs)
                .outputs(outputs == null ? Collections.<ContainerOutput>emptyList() : outputs)
                .history(history == null ? Collections.<ContainerHistory>emptyList() : history)
                .logPaths(logPaths == null ? Collections.<String>emptyList() : logPaths)
                .build();
    }

    public static Container create(final ContainerEntity containerEntity) {
        return builder()
                .commandId(containerEntity.getCommandId())
                .wrapperId(containerEntity.getXnatCommandWrapperId())
                .containerId(containerEntity.getContainerId())
                .userId(containerEntity.getUserId())
                .dockerImage(containerEntity.getDockerImage())
                .commandLine(containerEntity.getCommandLine())
                .environmentVariables(containerEntity.getEnvironmentVariables() == null ? Collections.<String, String>emptyMap() : containerEntity.getEnvironmentVariables())
                .logPaths(containerEntity.getLogPaths() == null ? Collections.<String>emptyList() : containerEntity.getLogPaths())
                .mounts(containerEntity.getMounts() == null ?
                        Collections.<ContainerMount>emptyList() :
                        Lists.transform(containerEntity.getMounts(), new Function<ContainerEntityMount, ContainerMount>() {
                            @Override
                            public ContainerMount apply(final ContainerEntityMount input) {
                                return ContainerMount.create(input);
                            }
                        })
                )
                .inputs(containerEntity.getInputs() == null ?
                        Collections.<ContainerInput>emptyList() :
                        Lists.transform(containerEntity.getInputs(), new Function<ContainerEntityInput, ContainerInput>() {
                            @Override
                            public ContainerInput apply(final ContainerEntityInput input) {
                                return ContainerInput.create(input);
                            }
                        })
                )
                .outputs(containerEntity.getOutputs() == null ?
                        Collections.<ContainerOutput>emptyList() :
                        Lists.transform(containerEntity.getOutputs(), new Function<ContainerEntityOutput, ContainerOutput>() {
                            @Override
                            public ContainerOutput apply(final ContainerEntityOutput input) {
                                return ContainerOutput.create(input);
                            }
                        })
                )
                .history(containerEntity.getHistory() == null ?
                        Collections.<ContainerHistory>emptyList() :
                        Lists.transform(containerEntity.getHistory(), new Function<ContainerEntityHistory, ContainerHistory>() {
                            @Override
                            public ContainerHistory apply(final ContainerEntityHistory input) {
                                return ContainerHistory.create(input);
                            }
                        })
                )
                .build();
    }

    public static Builder builder() {
        return new AutoValue_Container.Builder();
    }

    @AutoValue.Builder
    public static abstract class Builder {
        public abstract Builder commandId(long commandId);
        public abstract Builder wrapperId(long wrapperId);
        public abstract Builder containerId(String containerId);
        public abstract Builder userId(String userId);
        public abstract Builder dockerImage(String dockerImage);
        public abstract Builder commandLine(String commandLine);

        public abstract Builder environmentVariables(Map<String, String> environmentVariables);
        abstract ImmutableMap.Builder<String, String> environmentVariablesBuilder();
        public Builder addEnvironmentVariable(final String envKey, final String envValue) {
            environmentVariablesBuilder().put(envKey, envValue);
            return this;
        }

        public abstract Builder mounts(List<ContainerMount> mounts);
        abstract ImmutableList.Builder<ContainerMount> mountsBuilder();
        public Builder addMount(final ContainerMount mounts) {
            mountsBuilder().add(mounts);
            return this;
        }

        public abstract Builder inputs(List<ContainerInput> inputs);
        abstract ImmutableList.Builder<ContainerInput> inputsBuilder();
        public Builder addInput(final ContainerInput inputs) {
            inputsBuilder().add(inputs);
            return this;
        }

        public abstract Builder outputs(List<ContainerOutput> outputs);
        abstract ImmutableList.Builder<ContainerOutput> outputsBuilder();
        public Builder addOutput(final ContainerOutput outputs) {
            outputsBuilder().add(outputs);
            return this;
        }

        public abstract Builder history(List<ContainerHistory> history);
        abstract ImmutableList.Builder<ContainerHistory> historyBuilder();
        public Builder addHistoryItem(final ContainerHistory history) {
            historyBuilder().add(history);
            return this;
        }

        public abstract Builder logPaths(List<String> logPaths);
        abstract ImmutableList.Builder<String> logPathsBuilder();
        public Builder addLogPath(final String logPaths) {
            logPathsBuilder().add(logPaths);
            return this;
        }

        public abstract Container build();
    }

    @AutoValue
    public static abstract class ContainerMount {
        @JsonProperty("name") public abstract String name();
        @JsonProperty("writable") public abstract boolean writable();
        @JsonProperty("xnat-host-path") public abstract String xnatHostPath();
        @JsonProperty("container-host-path") public abstract String containerHostPath();
        @JsonProperty("container-path") public abstract String containerPath();
        @JsonProperty("input-files") public abstract ImmutableList<ContainerMountFiles> inputFiles();

        @JsonCreator
        public static ContainerMount create(@JsonProperty("name") final String name,
                                            @JsonProperty("writable") final boolean writable,
                                            @JsonProperty("xnat-host-path") final String xnatHostPath,
                                            @JsonProperty("container-host-path") final String containerHostPath,
                                            @JsonProperty("container-path") final String containerPath,
                                            @JsonProperty("input-files") final List<ContainerMountFiles> inputFiles) {
            final ImmutableList<ContainerMountFiles> inputFilesCopy = inputFiles == null ?
                    ImmutableList.<ContainerMountFiles>of() :
                    ImmutableList.copyOf(inputFiles);
            return new AutoValue_Container_ContainerMount(name, writable, xnatHostPath, containerHostPath,
                    containerPath, inputFilesCopy);
        }

        public static ContainerMount create(final ContainerEntityMount containerEntityMount) {
            final List<ContainerMountFiles> containerMountFiles = containerEntityMount.getInputFiles() == null ? null :
                    Lists.transform(containerEntityMount.getInputFiles(), new Function<ContainerMountFilesEntity, ContainerMountFiles>() {
                        @Override
                        public ContainerMountFiles apply(final ContainerMountFilesEntity input) {
                            return ContainerMountFiles.create(input);
                        }
                    });
            return create(containerEntityMount.getName(), containerEntityMount.isWritable(), containerEntityMount.getXnatHostPath(),
                    containerEntityMount.getContainerHostPath(), containerEntityMount.getContainerPath(), containerMountFiles);
        }
    }

    @AutoValue
    public static abstract class ContainerMountFiles {
        @JsonProperty("from-xnat-input") public abstract String fromXnatInput();
        @Nullable @JsonProperty("from-uri") public abstract String fromUri();
        @JsonProperty("root-directory") public abstract String rootDirectory();
        @JsonProperty("path") public abstract String path();

        @JsonCreator
        public static ContainerMountFiles create(@JsonProperty("from-xnat-input") final String fromXnatInput,
                                                 @JsonProperty("from-uri") final String fromUri,
                                                 @JsonProperty("root-directory") final String rootDirectory,
                                                 @JsonProperty("path") final String path) {
            return new AutoValue_Container_ContainerMountFiles(fromXnatInput, fromUri, rootDirectory, path);
        }

        public static ContainerMountFiles create(final ContainerMountFilesEntity containerMountFilesEntity) {
            return create(containerMountFilesEntity.getFromXnatInput(), containerMountFilesEntity.getFromUri(),
                    containerMountFilesEntity.getRootDirectory(), containerMountFilesEntity.getPath());
        }
    }

    @AutoValue
    public static abstract class ContainerInput {
        @JsonProperty("type") public abstract ContainerInputType type();
        @JsonProperty("name") public abstract String name();
        @JsonProperty("value") public abstract String value();

        @JsonCreator
        public static ContainerInput create(@JsonProperty("type") final ContainerInputType type,
                                            @JsonProperty("name") final String name,
                                            @JsonProperty("value") final String value) {
            return new AutoValue_Container_ContainerInput(type, name, value);
        }

        public static ContainerInput create(final ContainerEntityInput containerEntityInput) {
            return create(containerEntityInput.getType(), containerEntityInput.getName(), containerEntityInput.getValue());
        }
    }

    @AutoValue
    public static abstract class ContainerOutput {
        @JsonProperty("name") public abstract String name();
        @JsonProperty("type") public abstract String type();
        @JsonProperty("required") public abstract Boolean required();
        @JsonProperty("mount") public abstract String mount();
        @Nullable @JsonProperty("path") public abstract String path();
        @Nullable @JsonProperty("glob") public abstract String glob();
        @JsonProperty("label") public abstract String label();
        @JsonProperty("created") public abstract String created();
        @JsonProperty("handled-by-wrapper-input") public abstract String handledByWrapperInput();

        @JsonCreator
        public static ContainerOutput create(@JsonProperty("name") final String name,
                                             @JsonProperty("type") final String type,
                                             @JsonProperty("required") final Boolean required,
                                             @JsonProperty("mount") final String mount,
                                             @JsonProperty("path") final String path,
                                             @JsonProperty("glob") final String glob,
                                             @JsonProperty("label") final String label,
                                             @JsonProperty("created") final String created,
                                             @JsonProperty("handled-by-wrapper-input") final String handledByWrapperInput) {
            return new AutoValue_Container_ContainerOutput(name, type, required, mount, path, glob, label, created, handledByWrapperInput);
        }

        public static ContainerOutput create(final ContainerEntityOutput containerEntityOutput) {
            return create(containerEntityOutput.getName(), containerEntityOutput.getType(), containerEntityOutput.isRequired(),
                    containerEntityOutput.getMount(), containerEntityOutput.getPath(), containerEntityOutput.getGlob(),
                    containerEntityOutput.getLabel(), containerEntityOutput.getCreated(), containerEntityOutput.getHandledByXnatCommandInput());
        }
    }

    @AutoValue
    public static abstract class ContainerHistory {
        @JsonProperty("status") public abstract String status();
        @JsonProperty("entity-type") public abstract String entityType();
        @JsonProperty("entity-id") public abstract String entityId();
        @JsonProperty("time-recorded") public abstract Date timeRecorded();
        @JsonProperty("external-timestamp") public abstract String externalTimestamp();

        @JsonCreator
        public static ContainerHistory create(@JsonProperty("status") final String status,
                                              @JsonProperty("entity-type") final String entityType,
                                              @JsonProperty("entity-id") final String entityId,
                                              @JsonProperty("time-recorded") final Date timeRecorded,
                                              @JsonProperty("external-timestamp") final String externalTimestamp) {
            return new AutoValue_Container_ContainerHistory(status, entityType, entityId, timeRecorded, externalTimestamp);
        }

        public static ContainerHistory create(final ContainerEntityHistory containerEntityHistory) {
            return create(containerEntityHistory.getStatus(), containerEntityHistory.getEntityType(),
                    containerEntityHistory.getEntityId(), containerEntityHistory.getTimeRecorded(), containerEntityHistory.getExternalTimestamp());
        }
    }
}
