package org.nrg.containers.model.command.auto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.nrg.containers.model.command.entity.CommandEntity;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@AutoValue
public abstract class ResolvedCommand {

    @JsonProperty("wrapper-id") public abstract Long wrapperId();
    @JsonProperty("wrapper-name") public abstract String wrapperName();
    @Nullable @JsonProperty("wrapper-description") public abstract String wrapperDescription();
    @JsonProperty("command-id") public abstract Long commandId();
    @JsonProperty("command-name") public abstract String commandName();
    @Nullable @JsonProperty("command-description") public abstract String commandDescription();
    @JsonProperty("image") public abstract String image();
    @JsonProperty("type") public abstract String type();
    @JsonProperty("raw-input-values") public abstract ImmutableMap<String, String> rawInputValues();
    @JsonProperty("xwrapper-input-values") public abstract ImmutableMap<String, String> wrapperInputValues();
    @JsonProperty("command-input-values") public abstract ImmutableMap<String, String> commandInputValues();
    @JsonProperty("command-line") public abstract String commandLine();
    @JsonProperty("env") public abstract ImmutableMap<String, String> environmentVariables();
    @JsonProperty("ports") public abstract ImmutableMap<String, String> ports();
    @JsonProperty("mounts") public abstract ImmutableList<ResolvedCommandMount> mounts();
    @JsonProperty("outputs") public abstract ImmutableList<ResolvedCommandOutput> outputs();
    @Nullable @JsonProperty("working-directory") public abstract String workingDirectory();

    @JsonCreator
    public static ResolvedCommand create(@JsonProperty("wrapper-id") final Long wrapperId,
                                         @JsonProperty("wrapper-name") final String wrapperName,
                                         @JsonProperty("wrapper-description") final String wrapperDescription,
                                         @JsonProperty("command-id") final Long commandId,
                                         @JsonProperty("command-name") final String commandName,
                                         @JsonProperty("command-description") final String commandDescription,
                                         @JsonProperty("image") final String image,
                                         @JsonProperty("type") final String type,
                                         @JsonProperty("raw-input-values") final Map<String, String> rawInputValues,
                                         @JsonProperty("wrapper-input-values") final Map<String, String> wrapperInputValues,
                                         @JsonProperty("command-input-values") final Map<String, String> commandInputValues,
                                         @JsonProperty("command-line") final String commandLine,
                                         @JsonProperty("env") final Map<String, String> environmentVariables,
                                         @JsonProperty("ports")final Map<String, String> ports,
                                         @JsonProperty("mounts") final List<ResolvedCommandMount> mounts,
                                         @JsonProperty("outputs") final List<ResolvedCommandOutput> outputs,
                                         @JsonProperty("working-directory") final String workingDirectory) {
        return builder()
                .wrapperId(wrapperId)
                .wrapperName(wrapperName)
                .wrapperDescription(wrapperDescription)
                .commandId(commandId)
                .commandName(commandName)
                .commandDescription(commandDescription)
                .image(image)
                .type(type == null ? CommandEntity.DEFAULT_TYPE.getName() : type)
                .rawInputValues(rawInputValues == null ? Collections.<String, String>emptyMap() : rawInputValues)
                .wrapperInputValues(wrapperInputValues == null ? Collections.<String, String>emptyMap() : wrapperInputValues)
                .commandInputValues(commandInputValues == null ? Collections.<String, String>emptyMap() : commandInputValues)
                .commandLine(commandLine)
                .environmentVariables(environmentVariables == null ? Collections.<String, String>emptyMap() : environmentVariables)
                .ports(ports == null ? Collections.<String, String>emptyMap() : ports)
                .mounts(mounts == null ? Collections.<ResolvedCommandMount>emptyList() : mounts)
                .outputs(outputs == null ? Collections.<ResolvedCommandOutput>emptyList() : outputs)
                .workingDirectory(workingDirectory)
                .build();
    }

    public static Builder builder() {
        return new AutoValue_ResolvedCommand.Builder()
                .type(CommandEntity.DEFAULT_TYPE.getName());
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder {
        public abstract Builder wrapperId(Long wrapperId);
        public abstract Builder wrapperName(String wrapperDescription);
        public abstract Builder wrapperDescription(String wrapperDescription);
        public abstract Builder commandId(Long commandId);
        public abstract Builder commandName(String commandDescription);
        public abstract Builder commandDescription(String commandDescription);
        public abstract Builder image(String image);
        public abstract Builder type(String type);
        public abstract Builder rawInputValues(Map<String, String> rawInputValues);
        public abstract ImmutableMap.Builder<String, String> rawInputValuesBuilder();
        public Builder addRawInputValue(final String inputName, final String inputValue) {
            rawInputValuesBuilder().put(inputName, inputValue);
            return this;
        }
        public abstract Builder wrapperInputValues(Map<String, String> xnatInputValues);
        public abstract ImmutableMap.Builder<String, String> wrapperInputValuesBuilder();
        public Builder addWrapperInputValue(final String inputName, final String inputValue) {
            wrapperInputValuesBuilder().put(inputName, inputValue);
            return this;
        }
        public abstract Builder commandInputValues(Map<String, String> commandInputValues);
        public abstract ImmutableMap.Builder<String, String> commandInputValuesBuilder();
        public Builder addCommandInputValue(final String inputName, final String inputValue) {
            commandInputValuesBuilder().put(inputName, inputValue);
            return this;
        }
        public abstract Builder commandLine(String commandLine);
        public abstract Builder environmentVariables(Map<String, String> environmentVariables);
        public abstract ImmutableMap.Builder<String, String> environmentVariablesBuilder();
        public Builder addEnvironmentVariable(final String name, final String value) {
            environmentVariablesBuilder().put(name, value);
            return this;
        }
        public abstract Builder ports(Map<String, String> ports);
        public abstract ImmutableMap.Builder<String, String> portsBuilder();
        public Builder addPort(final String name, final String value) {
            portsBuilder().put(name, value);
            return this;
        }
        public abstract Builder mounts(List<ResolvedCommandMount> mounts);
        public abstract ImmutableList.Builder<ResolvedCommandMount> mountsBuilder();
        public Builder addMount(final ResolvedCommandMount mount) {
            mountsBuilder().add(mount);
            return this;
        }
        public abstract Builder outputs(List<ResolvedCommandOutput> outputs);
        public abstract ImmutableList.Builder<ResolvedCommandOutput> outputsBuilder();
        public Builder addOutput(final ResolvedCommandOutput output) {
            outputsBuilder().add(output);
            return this;
        }
        public abstract Builder workingDirectory(String workingDirectory);

        public abstract ResolvedCommand build();
    }

    @AutoValue
    public abstract static class PartiallyResolvedCommand {
        @JsonProperty("wrapper-id") public abstract Long wrapperId();
        @JsonProperty("wrapper-name") public abstract String wrapperName();
        @Nullable @JsonProperty("wrapper-description") public abstract String wrapperDescription();
        @JsonProperty("command-id") public abstract Long commandId();
        @JsonProperty("command-name") public abstract String commandName();
        @Nullable @JsonProperty("command-description") public abstract String commandDescription();
        @JsonProperty("image") public abstract String image();
        @JsonProperty("type") public abstract String type();
        @JsonProperty("raw-input-values") public abstract ImmutableMap<String, String> rawInputValues();
        @JsonProperty("wrapper-input-values") public abstract ImmutableMap<String, String> wrapperInputValues();
        @JsonProperty("command-input-values") public abstract ImmutableMap<String, String> commandInputValues();

        @JsonCreator
        public static PartiallyResolvedCommand create(@JsonProperty("wrapper-id") final Long wrapperId,
                                                      @JsonProperty("wrapper-name") final String wrapperName,
                                                      @JsonProperty("wrapper-description") final String wrapperDescription,
                                                      @JsonProperty("command-id") final Long commandId,
                                                      @JsonProperty("command-name") final String commandName,
                                                      @JsonProperty("command-description") final String commandDescription,
                                                      @JsonProperty("image") final String image,
                                                      @JsonProperty("type") final String type,
                                                      @JsonProperty("raw-input-values") final Map<String, String> rawInputValues,
                                                      @JsonProperty("wrapper-input-values") final Map<String, String> wrapperInputValues,
                                                      @JsonProperty("command-input-values") final Map<String, String> commandInputValues) {
            return builder()
                    .wrapperId(wrapperId)
                    .wrapperName(wrapperName)
                    .wrapperDescription(wrapperDescription)
                    .commandId(commandId)
                    .commandName(commandName)
                    .commandDescription(commandDescription)
                    .image(image)
                    .type(type == null ? CommandEntity.DEFAULT_TYPE.getName() : type)
                    .rawInputValues(rawInputValues == null ? Collections.<String, String>emptyMap() : rawInputValues)
                    .wrapperInputValues(wrapperInputValues == null ? Collections.<String, String>emptyMap() : wrapperInputValues)
                    .commandInputValues(commandInputValues == null ? Collections.<String, String>emptyMap() : commandInputValues)
                    .build();
        }

        public static Builder builder() {
            return new AutoValue_ResolvedCommand_PartiallyResolvedCommand.Builder()
                    .type(CommandEntity.DEFAULT_TYPE.getName());
        }

        @AutoValue.Builder
        public static abstract class Builder {
            public abstract Builder wrapperId(Long wrapperId);
            public abstract Builder wrapperName(String wrapperDescription);
            public abstract Builder wrapperDescription(String wrapperDescription);
            public abstract Builder commandId(Long commandId);
            public abstract Builder commandName(String commandDescription);
            public abstract Builder commandDescription(String commandDescription);
            public abstract Builder image(String image);
            public abstract Builder type(String type);
            public abstract Builder rawInputValues(Map<String, String> rawInputValues);
            public abstract ImmutableMap.Builder<String, String> rawInputValuesBuilder();
            public Builder addRawInputValue(final String inputName, final String inputValue) {
                rawInputValuesBuilder().put(inputName, inputValue);
                return this;
            }
            public abstract Builder wrapperInputValues(Map<String, String> wrapperInputValues);
            public abstract ImmutableMap.Builder<String, String> wrapperInputValuesBuilder();
            public Builder addWrapperInputValue(final String inputName, final String inputValue) {
                wrapperInputValuesBuilder().put(inputName, inputValue);
                return this;
            }
            public abstract Builder commandInputValues(Map<String, String> commandInputValues);
            public abstract ImmutableMap.Builder<String, String> commandInputValuesBuilder();
            public Builder addCommandInputValue(final String inputName, final String inputValue) {
                commandInputValuesBuilder().put(inputName, inputValue);
                return this;
            }

            public abstract PartiallyResolvedCommand build();
        }
    }

    @AutoValue
    public abstract static class ResolvedCommandMount {
        @JsonProperty("name") public abstract String name();
        @JsonProperty("writable") public abstract Boolean writable();
        @JsonProperty("container-path") public abstract String containerPath();
        @JsonProperty("input-files") public abstract ImmutableList<ResolvedCommandMountFiles> inputFiles();
        @JsonProperty("xnat-host-path") public abstract String xnatHostPath();
        @JsonProperty("container-host-path") public abstract String containerHostPath();

        @JsonCreator
        public static ResolvedCommandMount create(@JsonProperty("name") final String name,
                                                  @JsonProperty("writable") final Boolean writable,
                                                  @JsonProperty("container-path") final String containerPath,
                                                  @JsonProperty("input-files") final List<ResolvedCommandMountFiles> inputFiles,
                                                  @JsonProperty("xnat-host-path") final String xnatHostPath,
                                                  @JsonProperty("container-host-path") final String containerHostPath) {
            return builder()
                    .name(name)
                    .writable(writable)
                    .containerPath(containerPath)
                    .inputFiles(inputFiles == null ? Collections.<ResolvedCommandMountFiles>emptyList() : inputFiles)
                    .xnatHostPath(xnatHostPath)
                    .containerHostPath(containerHostPath)
                    .build();
        }

        public static Builder builder() {
            return new AutoValue_ResolvedCommand_ResolvedCommandMount.Builder();
        }

        public abstract Builder toBuilder();

        public String toBindMountString() {
            return containerHostPath() + ":" + containerPath() + (writable() ? "" : ":ro");
        }

        @AutoValue.Builder
        public abstract static class Builder {
            public abstract Builder name(String name);
            public abstract Builder writable(Boolean writable);
            public abstract Builder xnatHostPath(String xnatHostPath);
            public abstract Builder containerHostPath(String containerHostPath);
            public abstract Builder containerPath(String containerPath);
            public abstract Builder inputFiles(List<ResolvedCommandMountFiles> inputFiles);
            public abstract ImmutableList.Builder<ResolvedCommandMountFiles> inputFilesBuilder();
            public Builder addInputFiles(final ResolvedCommandMountFiles inputFiles) {
                inputFilesBuilder().add(inputFiles);
                return this;
            }

            public abstract ResolvedCommandMount build();
        }
    }

    @AutoValue
    public abstract static class PartiallyResolvedCommandMount {
        @JsonProperty("name") public abstract String name();
        @JsonProperty("writable") public abstract Boolean writable();
        @JsonProperty("container-path") public abstract String containerPath();
        @JsonProperty("input-files") public abstract ImmutableList<ResolvedCommandMountFiles> inputFiles();

        @JsonCreator
        public static PartiallyResolvedCommandMount create(@JsonProperty("name") final String name,
                                                           @JsonProperty("writable") final Boolean writable,
                                                           @JsonProperty("container-path") final String containerPath,
                                                           @JsonProperty("input-files") final List<ResolvedCommandMountFiles> inputFiles) {
            return builder()
                    .name(name)
                    .writable(writable)
                    .containerPath(containerPath)
                    .inputFiles(inputFiles == null ? Collections.<ResolvedCommandMountFiles>emptyList() : inputFiles)
                    .build();
        }

        public static Builder builder() {
            return new AutoValue_ResolvedCommand_PartiallyResolvedCommandMount.Builder();
        }

        public ResolvedCommandMount.Builder toResolvedCommandMountBuilder() {
            return ResolvedCommandMount.builder()
                    .name(this.name())
                    .writable(this.writable())
                    .containerPath(this.containerPath())
                    .inputFiles(this.inputFiles());
        }

        @AutoValue.Builder
        public abstract static class Builder {
            public abstract Builder name(String name);
            public abstract Builder writable(Boolean writable);
            public abstract Builder containerPath(String containerPath);
            public abstract Builder inputFiles(List<ResolvedCommandMountFiles> inputFiles);
            public abstract ImmutableList.Builder<ResolvedCommandMountFiles> inputFilesBuilder();
            public Builder addInputFiles(final ResolvedCommandMountFiles inputFiles) {
                inputFilesBuilder().add(inputFiles);
                return this;
            }

            public abstract PartiallyResolvedCommandMount build();
        }
    }

    @AutoValue
    public abstract static class ResolvedCommandMountFiles {
        @JsonProperty("from-xnat-input") public abstract String fromXnatInput();
        @Nullable @JsonProperty("from-uri") public abstract String fromUri();
        @JsonProperty("root-directory") public abstract String rootDirectory();
        @Nullable @JsonProperty("path") public abstract String path();

        public static ResolvedCommandMountFiles create(@JsonProperty("from-xnat-input") final String fromXnatInput,
                                                       @JsonProperty("from-uri") final String fromUri,
                                                       @JsonProperty("root-directory") final String rootDirectory,
                                                       @JsonProperty("path") final String path) {
            return new AutoValue_ResolvedCommand_ResolvedCommandMountFiles(fromXnatInput, fromUri, rootDirectory, path);
        }
    }

    @AutoValue
    public abstract static class ResolvedCommandOutput {
        @JsonProperty("name") public abstract String name();
        @JsonProperty("type") public abstract String type();
        @JsonProperty("required") public abstract Boolean required();
        @JsonProperty("mount") public abstract String mount();
        @Nullable @JsonProperty("path") public abstract String path();
        @Nullable @JsonProperty("glob") public abstract String glob();
        @JsonProperty("label") public abstract String label();
        @JsonProperty("handled-by-xnat-command-input") public abstract String handledByXnatCommandInput();

        @JsonCreator
        public static ResolvedCommandOutput create(@JsonProperty("name") final String name,
                                                   @JsonProperty("type") final String type,
                                                   @JsonProperty("required") final Boolean required,
                                                   @JsonProperty("mount") final String mount,
                                                   @JsonProperty("path") final String path,
                                                   @JsonProperty("glob") final String glob,
                                                   @JsonProperty("label") final String label,
                                                   @JsonProperty("handled-by-xnat-command-input") final String xnatInputName) {
            return builder()
                    .name(name)
                    .type(type)
                    .required(required)
                    .mount(mount)
                    .path(path)
                    .glob(glob)
                    .label(label)
                    .handledByXnatCommandInput(xnatInputName)
                    .build();
        }

        public static Builder builder() {
            return new AutoValue_ResolvedCommand_ResolvedCommandOutput.Builder();
        }

        @AutoValue.Builder
        public static abstract class Builder {
            public abstract Builder name(String name);
            public abstract Builder type(String type);
            public abstract Builder required(Boolean required);
            public abstract Builder mount(String mount);
            public abstract Builder path(String path);
            public abstract Builder glob(String glob);
            public abstract Builder label(String label);
            public abstract Builder handledByXnatCommandInput(String handledByXnatCommandInput);

            public abstract ResolvedCommandOutput build();
        }
    }
}
