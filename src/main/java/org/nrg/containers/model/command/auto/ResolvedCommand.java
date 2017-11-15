package org.nrg.containers.model.command.auto;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.nrg.containers.model.command.entity.CommandEntity;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

@AutoValue
public abstract class ResolvedCommand {
    private ImmutableMap<String, String> externalWrapperInputValues;
    private ImmutableMap<String, String> derivedWrapperInputValues;
    private ImmutableMap<String, String> commandInputValues;

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
    public abstract String commandLine();
    public abstract ImmutableMap<String, String> environmentVariables();
    public abstract ImmutableMap<String, String> ports();
    public abstract ImmutableList<ResolvedCommandMount> mounts();
    public abstract ImmutableList<ResolvedCommandOutput> outputs();
    @Nullable public abstract String workingDirectory();

    public ImmutableMap<String, String> externalWrapperInputValues() {
        if (externalWrapperInputValues == null) {
            setUpLegacyInputLists();
        }
        return externalWrapperInputValues;
    }

    public ImmutableMap<String, String> derivedWrapperInputValues() {
        if (derivedWrapperInputValues == null) {
            setUpLegacyInputLists();
        }
        return derivedWrapperInputValues;
    }

    public ImmutableMap<String, String> commandInputValues() {
        if (commandInputValues == null) {
            setUpLegacyInputLists();
        }
        return commandInputValues;
    }

    public ImmutableMap<String, String> wrapperInputValues() {
        final ImmutableMap.Builder<String, String> wrapperValuesBuilder = ImmutableMap.builder();
        wrapperValuesBuilder.putAll(externalWrapperInputValues());
        wrapperValuesBuilder.putAll(derivedWrapperInputValues());
        return wrapperValuesBuilder.build();
    }

    private void setUpLegacyInputLists() {
        // Read out all the input trees into Map<String, String>s
        final List<ResolvedInputTreeNode<? extends Command.Input>> flatTrees = flattenInputTrees();
        final ImmutableMap.Builder<String, String> externalWrapperInputValuesBuilder = ImmutableMap.builder();
        final ImmutableMap.Builder<String, String> derivedWrapperInputValuesBuilder = ImmutableMap.builder();
        final ImmutableMap.Builder<String, String> commandInputValuesBuilder = ImmutableMap.builder();
        for (final ResolvedInputTreeNode<? extends Command.Input> node : flatTrees) {
            final List<ResolvedInputTreeNode.ResolvedInputTreeValueAndChildren> valuesAndChildren = node.valuesAndChildren();
            final String value = (valuesAndChildren != null && !valuesAndChildren.isEmpty()) ?
                    valuesAndChildren.get(0).resolvedValue().value() :
                    null;
            if (node.input() instanceof Command.CommandWrapperExternalInput) {
                externalWrapperInputValuesBuilder.put(node.input().name(), value == null ? "null" : value);
            } else if (node.input() instanceof Command.CommandWrapperDerivedInput) {
                derivedWrapperInputValuesBuilder.put(node.input().name(), value == null ? "null" : value);
            } else {
                commandInputValuesBuilder.put(node.input().name(), value == null ? "null" : value);
            }
        }
        externalWrapperInputValues = externalWrapperInputValuesBuilder.build();
        derivedWrapperInputValues = derivedWrapperInputValuesBuilder.build();
        commandInputValues = commandInputValuesBuilder.build();
    }

    public List<ResolvedInputTreeNode<? extends Command.Input>> flattenInputTrees() {
        final List<ResolvedInputTreeNode<? extends Command.Input>> flatTree = Lists.newArrayList();
        for (final ResolvedInputTreeNode<? extends Command.Input> rootNode : resolvedInputTrees()) {
            flatTree.addAll(flattenTree(rootNode));
        }
        return flatTree;
    }

    private List<ResolvedInputTreeNode<? extends Command.Input>> flattenTree(final ResolvedInputTreeNode<? extends Command.Input> node) {
        final List<ResolvedInputTreeNode<? extends Command.Input>> flatTree = Lists.newArrayList();
        flatTree.add(node);

        final List<ResolvedInputTreeNode.ResolvedInputTreeValueAndChildren> resolvedValueAndChildren = node.valuesAndChildren();
        if (resolvedValueAndChildren.size() == 1) {
            // This node has a single value, so we can attempt to flatten its children
            final ResolvedInputTreeNode.ResolvedInputTreeValueAndChildren singleValue = resolvedValueAndChildren.get(0);
            final List<ResolvedInputTreeNode<? extends Command.Input>> children = singleValue.children();
            if (!(children == null || children.isEmpty())) {
                for (final ResolvedInputTreeNode<? extends Command.Input> child : children) {
                    flatTree.addAll(flattenTree(child));
                }
            } else {
                // Input has a uniquely resolved value, but no children.
            }
        } else {
            // This node has multiple values, so we can't flatten its children
        }
        return flatTree;
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
        public abstract Builder resolvedInputTrees(List<ResolvedInputTreeNode<? extends Command.Input>> resolvedInputTrees);
        public abstract ImmutableList.Builder<ResolvedInputTreeNode<? extends Command.Input>> resolvedInputTreesBuilder();
        public Builder addResolvedInputTree(final ResolvedInputTreeNode<? extends Command.Input> resolvedInputTree) {
            resolvedInputTreesBuilder().add(resolvedInputTree);
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
    public abstract static class PartiallyResolvedCommandMount {
        public abstract String name();
        public abstract Boolean writable();
        public abstract String containerPath();
        public abstract ImmutableList<ResolvedCommandMount.ResolvedCommandMountFiles> inputFiles();

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
            public abstract Builder inputFiles(List<ResolvedCommandMount.ResolvedCommandMountFiles> inputFiles);
            public abstract ImmutableList.Builder<ResolvedCommandMount.ResolvedCommandMountFiles> inputFilesBuilder();
            public Builder addInputFiles(final ResolvedCommandMount.ResolvedCommandMountFiles inputFiles) {
                inputFilesBuilder().add(inputFiles);
                return this;
            }

            public abstract PartiallyResolvedCommandMount build();
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
