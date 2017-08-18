package org.nrg.containers.model.command.auto;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.nrg.containers.model.command.entity.CommandEntity;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

@AutoValue
public abstract class ResolvedCommand {

    public abstract Long wrapperId();
    public abstract String wrapperName();
    @Nullable public abstract String wrapperDescription();
    public abstract Long commandId();
    public abstract String commandName();
    @Nullable public abstract String commandDescription();
    public abstract String image();
    public abstract String type();
    public abstract ImmutableMap<String, String> rawInputValues();
    public abstract ImmutableMap<String, String> externalWrapperInputValues();
    public abstract ImmutableMap<String, String> derivedWrapperInputValues();
    public abstract ImmutableMap<String, String> commandInputValues();
    public abstract String commandLine();
    public abstract ImmutableMap<String, String> environmentVariables();
    public abstract ImmutableMap<String, String> ports();
    public abstract ImmutableList<ResolvedCommandMount> mounts();
    public abstract ImmutableList<ResolvedCommandOutput> outputs();
    @Nullable public abstract String workingDirectory();

    public ImmutableMap<String, String> wrapperInputValues() {
        final ImmutableMap.Builder<String, String> wrapperValuesBuilder = ImmutableMap.builder();
        wrapperValuesBuilder.putAll(externalWrapperInputValues());
        wrapperValuesBuilder.putAll(derivedWrapperInputValues());
        return wrapperValuesBuilder.build();
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
        public abstract Builder externalWrapperInputValues(Map<String, String> inputValues);
        public abstract ImmutableMap.Builder<String, String> externalWrapperInputValuesBuilder();
        public Builder addExternalWrapperInputValue(final String inputName, final String inputValue) {
            externalWrapperInputValuesBuilder().put(inputName, inputValue);
            return this;
        }
        public abstract Builder derivedWrapperInputValues(Map<String, String> inputValues);
        public abstract ImmutableMap.Builder<String, String> derivedWrapperInputValuesBuilder();
        public Builder addDerivedWrapperInputValue(final String inputName, final String inputValue) {
            derivedWrapperInputValuesBuilder().put(inputName, inputValue);
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
        public abstract Long wrapperId();
        public abstract String wrapperName();
        @Nullable public abstract String wrapperDescription();
        public abstract Long commandId();
        public abstract String commandName();
        @Nullable public abstract String commandDescription();
        public abstract String image();
        public abstract String type();
        public abstract ImmutableMap<String, String> rawInputValues();
        public abstract ImmutableList<ResolvedInputTreeNode<? extends Command.Input>> resolvedInputTrees();

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
            public abstract Builder resolvedInputTrees(List<ResolvedInputTreeNode<? extends Command.Input>> resolvedInputTrees);
            abstract ImmutableList.Builder<ResolvedInputTreeNode<? extends Command.Input>> resolvedInputTreesBuilder();
            public Builder addResolvedInputTree(final ResolvedInputTreeNode<? extends Command.Input> root) {
                resolvedInputTreesBuilder().add(root);
                return this;
            }

            public abstract PartiallyResolvedCommand build();
        }
    }

    @AutoValue
    public abstract static class ResolvedCommandMount {
        public abstract String name();
        public abstract Boolean writable();
        public abstract String containerPath();
        public abstract ImmutableList<ResolvedCommandMountFiles> inputFiles();
        public abstract String xnatHostPath();
        public abstract String containerHostPath();

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
        public abstract String name();
        public abstract Boolean writable();
        public abstract String containerPath();
        public abstract ImmutableList<ResolvedCommandMountFiles> inputFiles();

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
        public abstract String fromWrapperInput();
        @Nullable public abstract String fromUri();
        @Nullable public abstract String rootDirectory();
        @Nullable public abstract String path();

        public static ResolvedCommandMountFiles create(final String fromWrapperInput,
                                                       final String fromUri,
                                                       final String rootDirectory,
                                                       final String path) {
            return new AutoValue_ResolvedCommand_ResolvedCommandMountFiles(fromWrapperInput, fromUri, rootDirectory, path);
        }
    }

    @AutoValue
    public abstract static class ResolvedCommandOutput {
        public abstract String name();
        public abstract String type();
        public abstract Boolean required();
        public abstract String mount();
        @Nullable public abstract String path();
        @Nullable public abstract String glob();
        public abstract String label();
        public abstract String handledByWrapperInput();

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
            public abstract Builder handledByWrapperInput(String handledByWrapperInput);

            public abstract ResolvedCommandOutput build();
        }
    }
}
