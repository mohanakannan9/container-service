package org.nrg.containers.model.auto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.nrg.containers.model.CommandEntity;
import org.nrg.containers.model.CommandInputEntity;
import org.nrg.containers.model.CommandMountEntity;
import org.nrg.containers.model.CommandOutputEntity;
import org.nrg.containers.model.CommandType;
import org.nrg.containers.model.CommandWrapperDerivedInputEntity;
import org.nrg.containers.model.CommandWrapperExternalInputEntity;
import org.nrg.containers.model.CommandWrapperInputType;
import org.nrg.containers.model.DockerCommandEntity;
import org.nrg.containers.model.CommandWrapperOutputEntity;
import org.nrg.containers.model.CommandWrapperEntity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Set;

@AutoValue
public abstract class Command {
    @JsonProperty("id") public abstract long id();
    @JsonProperty("name") public abstract String name();
    @Nullable @JsonProperty("label") public abstract String label();
    @Nullable @JsonProperty("description") public abstract String description();
    @Nullable @JsonProperty("version") public abstract String version();
    @Nullable @JsonProperty("schema-version") public abstract String schemaVersion();
    @Nullable @JsonProperty("info-url") public abstract String infoUrl();
    @Nullable @JsonProperty("image") public abstract String image();
    @JsonProperty("type") public abstract String type();
    @Nullable @JsonProperty("index") public abstract String index();
    @Nullable @JsonProperty("hash") public abstract String hash();
    @Nullable @JsonProperty("working-directory") public abstract String workingDirectory();
    @Nullable @JsonProperty("command-line") public abstract String commandLine();
    @JsonProperty("mounts") public abstract List<CommandMount> mounts();
    @JsonProperty("environment-variables") public abstract Map<String, String> environmentVariables();
    @JsonProperty("ports") public abstract Map<String, String> ports();
    @JsonProperty("inputs") public abstract List<CommandInput> inputs();
    @JsonProperty("outputs") public abstract List<CommandOutput> outputs();
    @JsonProperty("xnat") public abstract List<CommandWrapper> xnatCommandWrappers();

    @JsonCreator
    static Command create(@JsonProperty("id") final long id,
                          @JsonProperty("name") final String name,
                          @JsonProperty("label") final String label,
                          @JsonProperty("description") final String description,
                          @JsonProperty("version") final String version,
                          @JsonProperty("schema-version") final String schemaVersion,
                          @JsonProperty("info-url") final String infoUrl,
                          @JsonProperty("image") final String image,
                          @JsonProperty("type") final String type,
                          @JsonProperty("index") final String index,
                          @JsonProperty("hash") final String hash,
                          @JsonProperty("working-directory") final String workingDirectory,
                          @JsonProperty("command-line") final String commandLine,
                          @JsonProperty("mounts") final List<CommandMount> mounts,
                          @JsonProperty("environment-variables") final Map<String, String> environmentVariables,
                          @JsonProperty("ports") final Map<String, String> ports,
                          @JsonProperty("inputs") final List<CommandInput> inputs,
                          @JsonProperty("outputs") final List<CommandOutput> outputs,
                          @JsonProperty("xnat") final List<CommandWrapper> xnatCommandWrappers) {
        return builder()
                .id(id)
                .name(name == null ? "" : name)
                .label(label)
                .description(description)
                .version(version)
                .schemaVersion(schemaVersion)
                .infoUrl(infoUrl)
                .image(image)
                .type(type == null ? CommandEntity.DEFAULT_TYPE.getName() : type)
                .index(index)
                .hash(hash)
                .workingDirectory(workingDirectory)
                .commandLine(commandLine)
                .mounts(mounts == null ? Lists.<CommandMount>newArrayList() : mounts)
                .environmentVariables(environmentVariables == null ? Maps.<String, String>newHashMap() : environmentVariables)
                .ports(ports == null ? Maps.<String, String>newHashMap() : ports)
                .inputs(inputs == null ? Lists.<CommandInput>newArrayList() : inputs)
                .outputs(outputs == null ? Lists.<CommandOutput>newArrayList() : outputs)
                .xnatCommandWrappers(xnatCommandWrappers == null ? Lists.<CommandWrapper>newArrayList() : xnatCommandWrappers)
                .build();
    }

    public static Command create(final CommandEntity commandEntity) {
        if (commandEntity == null) {
            return null;
        }
        Command.Builder builder = builder()
                .id(commandEntity.getId())
                .name(commandEntity.getName())
                .label(commandEntity.getLabel())
                .description(commandEntity.getDescription())
                .version(commandEntity.getVersion())
                .schemaVersion(commandEntity.getSchemaVersion())
                .infoUrl(commandEntity.getInfoUrl())
                .image(commandEntity.getImage())
                .type(commandEntity.getType().getName())
                .workingDirectory(commandEntity.getWorkingDirectory())
                .commandLine(commandEntity.getCommandLine())
                .environmentVariables(commandEntity.getEnvironmentVariables() == null ?
                        Maps.<String, String>newHashMap() :
                        Maps.newHashMap(commandEntity.getEnvironmentVariables()))
                .mounts(commandEntity.getMounts() == null ?
                        Lists.<CommandMount>newArrayList() :
                        Lists.newArrayList(Lists.transform(commandEntity.getMounts(), new Function<CommandMountEntity, CommandMount>() {
                            @Nullable
                            @Override
                            public CommandMount apply(@Nullable final CommandMountEntity mount) {
                                return mount == null ? null : CommandMount.create(mount);
                            }
                        })))
                .inputs(commandEntity.getInputs() == null ?
                        Lists.<CommandInput>newArrayList() :
                        Lists.newArrayList(Lists.transform(commandEntity.getInputs(), new Function<CommandInputEntity, CommandInput>() {
                            @Nullable
                            @Override
                            public CommandInput apply(@Nullable final CommandInputEntity commandInput) {
                                return commandInput == null ? null : CommandInput.create(commandInput);
                            }
                        })))
                .outputs(commandEntity.getOutputs() == null ?
                        Lists.<CommandOutput>newArrayList() :
                        Lists.newArrayList(Lists.transform(commandEntity.getOutputs(), new Function<CommandOutputEntity, CommandOutput>() {
                            @Nullable
                            @Override
                            public CommandOutput apply(@Nullable final CommandOutputEntity commandOutput) {
                                return commandOutput == null ? null : CommandOutput.create(commandOutput);
                            }
                        })))
                .xnatCommandWrappers(commandEntity.getCommandWrapperEntities() == null ?
                        Lists.<CommandWrapper>newArrayList() :
                        Lists.newArrayList(Lists.transform(commandEntity.getCommandWrapperEntities(), new Function<CommandWrapperEntity, CommandWrapper>() {
                            @Nullable
                            @Override
                            public CommandWrapper apply(@Nullable final CommandWrapperEntity xnatCommandWrapper) {
                                return xnatCommandWrapper == null ? null : CommandWrapper.create(xnatCommandWrapper);
                            }
                        })));

        switch (commandEntity.getType()) {
            case DOCKER:
                builder = builder.index(((DockerCommandEntity) commandEntity).getIndex())
                        .hash(((DockerCommandEntity) commandEntity).getHash())
                        .ports(((DockerCommandEntity) commandEntity).getPorts() == null ?
                                Maps.<String, String>newHashMap() :
                                Maps.newHashMap(((DockerCommandEntity) commandEntity).getPorts()));
                break;
        }

        return builder.build();
    }

    public void addWrapper(final CommandWrapper commandWrapper) {
        xnatCommandWrappers().add(commandWrapper);
    }

    public abstract Builder toBuilder();

    public static Builder builder() {
        return new AutoValue_Command.Builder()
                .id(0L)
                .name("")
                .type(CommandEntity.DEFAULT_TYPE.getName())
                .mounts(Lists.<CommandMount>newArrayList())
                .environmentVariables(Maps.<String, String>newHashMap())
                .ports(Maps.<String, String>newHashMap())
                .inputs(Lists.<CommandInput>newArrayList())
                .outputs(Lists.<CommandOutput>newArrayList())
                .xnatCommandWrappers(Lists.<CommandWrapper>newArrayList());
    }

    @Nonnull
    public List<String> validate() {
        final List<String> errors = Lists.newArrayList();
        final List<String> commandTypeNames = CommandType.names();

        if (!commandTypeNames.contains(type())) {
            errors.add("Cannot instantiate command of type \"" + type() + "\". Known types: " + StringUtils.join(commandTypeNames, ", "));
        }

        if (StringUtils.isBlank(name())) {
            errors.add("Command name cannot be blank.");
        }
        final String commandName = "Command \"" + name() + "\" - ";

        if (StringUtils.isBlank(image())) {
            errors.add(commandName + "image name cannot be blank.");
        }

        final Function<String, String> addCommandNameToError = new Function<String, String>() {
            @Nullable
            @Override
            public String apply(@Nullable final String input) {
                return commandName + input;
            }
        };

        final Set<String> mountNames = Sets.newHashSet();
        for (final CommandMount mount : mounts()) {
            final List<String> mountErrors = Lists.newArrayList();
            mountErrors.addAll(Lists.transform(mount.validate(), addCommandNameToError));

            if (mountNames.contains(mount.name())) {
                errors.add(commandName + "mount name \"" + mount.name() + "\" is not unique.");
            } else {
                mountNames.add(mount.name());
            }

            if (!mountErrors.isEmpty()) {
                errors.addAll(mountErrors);
            }
        }
        final String knownMounts = StringUtils.join(mountNames, ", ");

        final Set<String> inputNames = Sets.newHashSet();
        for (final CommandInput input : inputs()) {
            final List<String> inputErrors = Lists.newArrayList();
            inputErrors.addAll(Lists.transform(input.validate(), addCommandNameToError));

            if (inputNames.contains(input.name())) {
                errors.add(commandName + "input name \"" + input.name() + "\" is not unique.");
            } else {
                inputNames.add(input.name());
            }

            if (!inputErrors.isEmpty()) {
                errors.addAll(inputErrors);
            }
        }

        final Set<String> outputNames = Sets.newHashSet();
        for (final CommandOutput output : outputs()) {
            final List<String> outputErrors = Lists.newArrayList();
            outputErrors.addAll(Lists.transform(output.validate(), addCommandNameToError));

            if (outputNames.contains(output.name())) {
                errors.add(commandName + "output name \"" + output.name() + "\" is not unique.");
            } else {
                outputNames.add(output.name());
            }

            if (!mountNames.contains(output.mount())) {
                errors.add(commandName + "output \"" + output.name() + "\" references unknown mount \"" + output.mount() + "\". Known mounts: " + knownMounts);
            }

            if (!outputErrors.isEmpty()) {
                errors.addAll(outputErrors);
            }
        }
        final String knownOutputs = StringUtils.join(outputNames, ", ");

        final Set<String> wrapperNames = Sets.newHashSet();
        for (final CommandWrapper commandWrapper : xnatCommandWrappers()) {
            final List<String> wrapperErrors = Lists.newArrayList();
            wrapperErrors.addAll(Lists.transform(commandWrapper.validate(), addCommandNameToError));

            if (wrapperNames.contains(commandWrapper.name())) {
                errors.add(commandName + "wrapper name \"" + commandWrapper.name() + "\" is not unique.");
            } else {
                wrapperNames.add(commandWrapper.name());
            }
            final String wrapperName = commandName + "wrapper \"" + commandWrapper.name() + "\" - ";
            final Function<String, String> addWrapperNameToError = new Function<String, String>() {
                @Nullable
                @Override
                public String apply(@Nullable final String input) {
                    return wrapperName + input;
                }
            };

            final Set<String> wrapperInputNames = Sets.newHashSet();
            for (final CommandWrapperInput external : commandWrapper.externalInputs()) {
                final List<String> inputErrors = Lists.newArrayList();
                inputErrors.addAll(Lists.transform(external.validate(), addWrapperNameToError));

                if (wrapperInputNames.contains(external.name())) {
                    errors.add(wrapperName + "external input name \"" + external.name() + "\" is not unique.");
                } else {
                    wrapperInputNames.add(external.name());
                }


                if (!inputErrors.isEmpty()) {
                    errors.addAll(inputErrors);
                }
            }

            for (final CommandWrapperDerivedInput derived : commandWrapper.derivedInputs()) {
                final List<String> inputErrors = Lists.newArrayList();
                inputErrors.addAll(Lists.transform(derived.validate(), addWrapperNameToError));

                if (wrapperInputNames.contains(derived.name())) {
                    errors.add(wrapperName + "derived input name \"" + derived.name() + "\" is not unique.");
                } else if (!wrapperInputNames.contains(derived.derivedFromXnatInput())) {
                    errors.add(wrapperName + "derived input \"" + derived.name() + "\" is derived from an unknown XNAT input \"" + derived.derivedFromXnatInput() + "\". Known inputs: " + StringUtils.join(wrapperInputNames, ", "));
                } else {
                    wrapperInputNames.add(derived.name());
                }


                if (!inputErrors.isEmpty()) {
                    errors.addAll(inputErrors);
                }
            }
            final String knownWrapperInputs = StringUtils.join(wrapperInputNames, ", ");

            final Set<String> handledOutputs = Sets.newHashSet();
            for (final CommandWrapperOutput output : commandWrapper.outputHandlers()) {
                final List<String> outputErrors = Lists.newArrayList();
                outputErrors.addAll(Lists.transform(output.validate(), addWrapperNameToError));

                if (!outputNames.contains(output.commandOutputName())) {
                    errors.add(wrapperName + "output handler refers to unknown command output \"" + output.commandOutputName() + "\". Known outputs: " + knownOutputs + ".");
                } else {
                    handledOutputs.add(output.commandOutputName());
                }

                if (!wrapperInputNames.contains(output.xnatInputName())) {
                    errors.add(wrapperName + "output handler refers to unknown XNAT input \"" + output.xnatInputName() + "\". Known inputs: " + knownWrapperInputs + ".");
                }

                if (!outputErrors.isEmpty()) {
                    errors.addAll(outputErrors);
                }
            }

            // Check that all command outputs are handled by some output handler
            if (!handledOutputs.containsAll(outputNames)) {
                // We know at least one output is not handled. Now find out which.
                for (final String commandOutput : outputNames) {
                    if (!handledOutputs.contains(commandOutput)) {
                        errors.add(commandName + "command output \"" + commandOutput + "\" is not handled by any output handler.");
                    }
                }
            }


            if (!wrapperErrors.isEmpty()) {
                errors.addAll(wrapperErrors);
            }
        }

        return errors;
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder id(final long id);
        public abstract Builder name(final String name);
        public abstract Builder label(final String label);
        public abstract Builder description(final String description);
        public abstract Builder version(final String version);
        public abstract Builder schemaVersion(final String schemaVersion);
        public abstract Builder infoUrl(final String infoUrl);
        public abstract Builder image(final String image);
        public abstract Builder type(final String type);
        public abstract Builder index(final String index);
        public abstract Builder hash(final String hash);
        public abstract Builder workingDirectory(final String workingDirectory);
        public abstract Builder commandLine(final String commandLine);
        public abstract Builder mounts(final List<CommandMount> mounts);
        public abstract Builder environmentVariables(final Map<String, String> environmentVariables);
        public abstract Builder ports(final Map<String, String> ports);
        public abstract Builder inputs(final List<CommandInput> inputs);
        public abstract Builder outputs(final List<CommandOutput> outputs);
        public abstract Builder xnatCommandWrappers(final List<CommandWrapper> xnatCommandWrappers);

        public abstract Command build();
    }

    @AutoValue
    public static abstract class CommandMount {
        @JsonIgnore public abstract long id();
        @JsonProperty("name") public abstract String name();
        @JsonProperty("writable") public abstract boolean writable();
        @Nullable @JsonProperty("path") public abstract String path();

        @JsonCreator
        static CommandMount create(@JsonProperty("name") final String name,
                                   @JsonProperty("writable") final Boolean writable,
                                   @JsonProperty("path") final String path) {
            return create(0L, name == null ? "" : name, writable == null ? false : writable, path);
        }

        static CommandMount create(final long id,
                                   final String name,
                                   final Boolean writable,
                                   final String path) {
            return new AutoValue_Command_CommandMount(id, name == null ? "" : name, writable == null ? false : writable, path);
        }

        static CommandMount create(final CommandMountEntity mount) {
            if (mount == null) {
                return null;
            }
            return CommandMount.create(mount.getName(), mount.isWritable(), mount.getContainerPath());
        }

        List<String> validate() {
            final List<String> errors = Lists.newArrayList();
            if (StringUtils.isBlank(name())) {
                errors.add("Mount name cannot be blank.");
            }
            if (StringUtils.isBlank(path())) {
                errors.add("Mount \"" + name() + "\" path cannot be blank.");
            }

            return errors;
        }
    }

    @AutoValue
    public static abstract class CommandInput {
        @JsonIgnore public abstract long id();
        @JsonProperty("name") public abstract String name();
        @Nullable @JsonProperty("description") public abstract String description();
        @JsonProperty("type") public abstract String type();
        @JsonProperty("required") public abstract boolean required();
        @Nullable @JsonProperty("matcher") public abstract String matcher();
        @Nullable @JsonProperty("default-value") public abstract String defaultValue();
        @Nullable @JsonProperty("replacement-key") public abstract String rawReplacementKey();
        @Nullable @JsonProperty("command-line-flag") public abstract String commandLineFlag();
        @Nullable @JsonProperty("command-line-separator") public abstract String commandLineSeparator();
        @Nullable @JsonProperty("true-value") public abstract String trueValue();
        @Nullable @JsonProperty("false-value") public abstract String falseValue();

        @JsonCreator
        static CommandInput create(@JsonProperty("name") final String name,
                                   @JsonProperty("description") final String description,
                                   @JsonProperty("type") final String type,
                                   @JsonProperty("required") final Boolean required,
                                   @JsonProperty("matcher") final String matcher,
                                   @JsonProperty("default-value") final String defaultValue,
                                   @JsonProperty("replacement-key") final String rawReplacementKey,
                                   @JsonProperty("command-line-flag") final String commandLineFlag,
                                   @JsonProperty("command-line-separator") final String commandLineSeparator,
                                   @JsonProperty("true-value") final String trueValue,
                                   @JsonProperty("false-value") final String falseValue) {
            return builder()
                    .name(name == null ? "" : name)
                    .description(description)
                    .type(type == null ? CommandInputEntity.DEFAULT_TYPE.getName() : type)
                    .required(required == null ? false : required)
                    .matcher(matcher)
                    .defaultValue(defaultValue)
                    .rawReplacementKey(rawReplacementKey)
                    .commandLineFlag(commandLineFlag)
                    .commandLineSeparator(commandLineSeparator)
                    .trueValue(trueValue)
                    .falseValue(falseValue)
                    .build();
        }

        public static CommandInput create(final CommandInputEntity commandInputEntity) {
            if (commandInputEntity == null) {
                return null;
            }
            return builder()
                    .id(commandInputEntity.getId())
                    .name(commandInputEntity.getName())
                    .description(commandInputEntity.getDescription())
                    .type(commandInputEntity.getType().getName())
                    .required(commandInputEntity.isRequired())
                    .matcher(commandInputEntity.getMatcher())
                    .defaultValue(commandInputEntity.getDefaultValue())
                    .rawReplacementKey(commandInputEntity.getRawReplacementKey())
                    .commandLineFlag(commandInputEntity.getCommandLineFlag())
                    .commandLineSeparator(commandInputEntity.getCommandLineSeparator())
                    .trueValue(commandInputEntity.getTrueValue())
                    .falseValue(commandInputEntity.getFalseValue())
                    .build();
        }

        public static Builder builder() {
            return new AutoValue_Command_CommandInput.Builder()
                    .id(0L)
                    .name("")
                    .type(CommandInputEntity.DEFAULT_TYPE.getName())
                    .required(false);
        }

        @Nonnull
        List<String> validate() {
            final List<String> errors = Lists.newArrayList();
            if (StringUtils.isBlank(name())) {
                errors.add("Command input name cannot be blank");
            }
            return errors;
        }

        public String replacementKey() {
            return StringUtils.isNotBlank(rawReplacementKey()) ? rawReplacementKey() : "#" + name() + "#";
        }

        @AutoValue.Builder
        public abstract static class Builder {
            public abstract Builder id(final long id);
            public abstract Builder name(final String name);
            public abstract Builder description(final String description);
            public abstract Builder type(final String type);
            public abstract Builder required(final boolean required);
            public abstract Builder matcher(final String matcher);
            public abstract Builder defaultValue(final String defaultValue);
            public abstract Builder rawReplacementKey(final String rawReplacementKey);
            public abstract Builder commandLineFlag(final String commandLineFlag);
            public abstract Builder commandLineSeparator(final String commandLineSeparator);
            public abstract Builder trueValue(final String trueValue);
            public abstract Builder falseValue(final String falseValue);

            public abstract CommandInput build();
        }
    }

    @AutoValue
    public static abstract class CommandOutput {
        @JsonIgnore public abstract long id();
        @JsonProperty("name") public abstract String name();
        @Nullable @JsonProperty("description") public abstract String description();
        @Nullable @JsonProperty("required") public abstract Boolean required();
        @Nullable @JsonProperty("mount") public abstract String mount();
        @Nullable @JsonProperty("path") public abstract String path();
        @Nullable @JsonProperty("glob") public abstract String glob();

        @JsonCreator
        static CommandOutput create(@JsonProperty("name") final String name,
                                    @JsonProperty("description") final String description,
                                    @JsonProperty("required") final Boolean required,
                                    @JsonProperty("mount") final String mount,
                                    @JsonProperty("path") final String path,
                                    @JsonProperty("glob") final String glob) {
            return create(0L, name == null ? "" : name, description, required, mount, path, glob);
        }

        static CommandOutput create(final long id,
                                    final String name,
                                    final String description,
                                    final Boolean required,
                                    final String mount,
                                    final String path,
                                    final String glob) {
            return new AutoValue_Command_CommandOutput(id, name == null ? "" : name, description, required, mount, path, glob);
        }

        static CommandOutput create(final CommandOutputEntity commandOutputEntity) {
            if (commandOutputEntity == null) {
                return null;
            }
            return create(commandOutputEntity.getId(), commandOutputEntity.getName(), commandOutputEntity.getDescription(), commandOutputEntity.isRequired(), commandOutputEntity.getMount(),
                    commandOutputEntity.getPath(), commandOutputEntity.getGlob());
        }

        @Nonnull
        List<String> validate() {
            final List<String> errors = Lists.newArrayList();
            if (StringUtils.isBlank(name())) {
                errors.add("Output name cannot be blank.");
            }
            if (StringUtils.isBlank(mount())) {
                errors.add("Output \"" + name() + "\" - mount cannot be blank.");
            }
            return errors;
        }
    }

    @AutoValue
    public static abstract class CommandWrapper {
        @JsonProperty("id") public abstract long id();
        @JsonProperty("name") public abstract String name();
        @Nullable @JsonProperty("description") public abstract String description();
        @JsonProperty("contexts") public abstract Set<String> contexts();
        @JsonProperty("external-inputs") public abstract List<CommandWrapperExternalInput> externalInputs();
        @JsonProperty("derived-inputs") public abstract List<CommandWrapperDerivedInput> derivedInputs();
        @JsonProperty("output-handlers") public abstract List<CommandWrapperOutput> outputHandlers();

        @JsonCreator
        static CommandWrapper create(@JsonProperty("id") final long id,
                                     @JsonProperty("name") final String name,
                                     @JsonProperty("description") final String description,
                                     @JsonProperty("contexts") final Set<String> contexts,
                                     @JsonProperty("external-inputs") final List<CommandWrapperExternalInput> externalInputs,
                                     @JsonProperty("derived-inputs") final List<CommandWrapperDerivedInput> derivedInputs,
                                     @JsonProperty("output-handlers") final List<CommandWrapperOutput> outputHandlers) {
            return builder()
                    .id(id)
                    .name(name == null ? "" : name)
                    .description(description)
                    .contexts(contexts == null ? Sets.<String>newHashSet() : contexts)
                    .externalInputs(externalInputs == null ? Lists.<CommandWrapperExternalInput>newArrayList() : externalInputs)
                    .derivedInputs(derivedInputs == null ? Lists.<CommandWrapperDerivedInput>newArrayList() : derivedInputs)
                    .outputHandlers(outputHandlers == null ? Lists.<CommandWrapperOutput>newArrayList() : outputHandlers)
                    .build();
        }

        public static CommandWrapper passthrough(final Command command) {
            if (command == null) {
                return null;
            }
            return builder()
                    .externalInputs(Lists.transform(command.inputs(), new Function<CommandInput, CommandWrapperExternalInput>() {
                        @Nullable
                        @Override
                        public CommandWrapperExternalInput apply(@Nullable final CommandInput commandInput) {
                            return CommandWrapperExternalInput.passthrough(commandInput);
                        }
                    }))
                    .build();
        }

        public static Builder builder() {
            return new AutoValue_Command_CommandWrapper.Builder()
                    .id(0L)
                    .name("")
                    .contexts(Sets.<String>newHashSet())
                    .externalInputs(Lists.<CommandWrapperExternalInput>newArrayList())
                    .derivedInputs(Lists.<CommandWrapperDerivedInput>newArrayList())
                    .outputHandlers(Lists.<CommandWrapperOutput>newArrayList());
        }

        public Builder toBuilder() {
            return builder()
                    .id(this.id())
                    .name(this.name())
                    .contexts(this.contexts())
                    .externalInputs(this.externalInputs())
                    .derivedInputs(this.derivedInputs())
                    .outputHandlers(this.outputHandlers());
        }

        public static CommandWrapper create(final CommandWrapperEntity commandWrapperEntity) {
            if (commandWrapperEntity == null) {
                return null;
            }
            final Set<String> contexts = commandWrapperEntity.getContexts() == null ?
                    Sets.<String>newHashSet() :
                    Sets.newHashSet(commandWrapperEntity.getContexts());
            final List<CommandWrapperExternalInput> external = commandWrapperEntity.getExternalInputs() == null ?
                    Lists.<CommandWrapperExternalInput>newArrayList() :
                    Lists.newArrayList(Lists.transform(commandWrapperEntity.getExternalInputs(), new Function<CommandWrapperExternalInputEntity, CommandWrapperExternalInput>() {
                        @Nullable
                        @Override
                        public CommandWrapperExternalInput apply(@Nullable final CommandWrapperExternalInputEntity xnatCommandInput) {
                            return xnatCommandInput == null ? null : CommandWrapperExternalInput.create(xnatCommandInput);
                        }
                    }));
            final List<CommandWrapperDerivedInput> derived = commandWrapperEntity.getDerivedInputs() == null ?
                    Lists.<CommandWrapperDerivedInput>newArrayList() :
                    Lists.newArrayList(Lists.transform(commandWrapperEntity.getDerivedInputs(), new Function<CommandWrapperDerivedInputEntity, CommandWrapperDerivedInput>() {
                        @Nullable
                        @Override
                        public CommandWrapperDerivedInput apply(@Nullable final CommandWrapperDerivedInputEntity xnatCommandInput) {
                            return xnatCommandInput == null ? null : CommandWrapperDerivedInput.create(xnatCommandInput);
                        }
                    }));
            final List<CommandWrapperOutput> outputs = commandWrapperEntity.getOutputHandlers() == null ?
                    Lists.<CommandWrapperOutput>newArrayList() :
                    Lists.newArrayList(Lists.transform(commandWrapperEntity.getOutputHandlers(), new Function<CommandWrapperOutputEntity, CommandWrapperOutput>() {
                        @Nullable
                        @Override
                        public CommandWrapperOutput apply(@Nullable final CommandWrapperOutputEntity xnatCommandOutput) {
                            return xnatCommandOutput == null ? null : CommandWrapperOutput.create(xnatCommandOutput);
                        }
                    }));
            return create(commandWrapperEntity.getId(), commandWrapperEntity.getName(), commandWrapperEntity.getDescription(),
                    contexts, external, derived, outputs);
        }

        @Nonnull
        List<String> validate() {
            final List<String> errors = Lists.newArrayList();
            if (StringUtils.isBlank(name())) {
                errors.add("Command wrapper name cannot be blank.");
            }

            return errors;
        }

        @AutoValue.Builder
        public abstract static class Builder {
            public abstract Builder id(final long id);
            public abstract Builder name(final String name);
            public abstract Builder description(final String description);
            public abstract Builder contexts(final Set<String> contexts);
            public abstract Builder externalInputs(final List<CommandWrapperExternalInput> externalInputs);
            public abstract Builder derivedInputs(final List<CommandWrapperDerivedInput> derivedInputs);
            public abstract Builder outputHandlers(final List<CommandWrapperOutput> outputHandlers);

            public abstract CommandWrapper build();
        }
    }

    public static abstract class CommandWrapperInput {
        @JsonIgnore public abstract long id();
        @JsonProperty("name") public abstract String name();
        @Nullable @JsonProperty("description") public abstract String description();
        @JsonProperty("type") public abstract String type();
        @Nullable @JsonProperty("matcher") public abstract String matcher();
        @Nullable @JsonProperty("provides-value-for-command-input") public abstract String providesValueForCommandInput();
        @Nullable @JsonProperty("provides-files-for-command-mount") public abstract String providesFilesForCommandMount();
        @Nullable @JsonProperty("default-value") public abstract String defaultValue();
        @Nullable @JsonProperty("user-settable") public abstract Boolean userSettable();
        @Nullable @JsonProperty("replacement-key") public abstract String rawReplacementKey();
        @JsonProperty("required") public abstract boolean required();

        @Nonnull
        List<String> validate() {
            final List<String> errors = Lists.newArrayList();
            if (StringUtils.isBlank(name())) {
                errors.add("Command wrapper input name cannot be blank.");
            }

            final List<String> types = CommandWrapperInputType.names();
            if (!types.contains(type())) {
                errors.add("Command wrapper input \"" + name() + "\" - Unknown type \"" + type() + "\". Known types: " + StringUtils.join(types, ", "));
            }

            return errors;
        }

        public String replacementKey() {
            return StringUtils.isNotBlank(rawReplacementKey()) ? rawReplacementKey() : "#" + name() + "#";
        }


    }

    @AutoValue
    public static abstract class CommandWrapperExternalInput extends CommandWrapperInput {
        @JsonCreator
        static CommandWrapperExternalInput create(@JsonProperty("name") final String name,
                                                  @JsonProperty("description") final String description,
                                                  @JsonProperty("type") final String type,
                                                  @JsonProperty("matcher") final String matcher,
                                                  @JsonProperty("provides-value-for-command-input") final String providesValueForCommandInput,
                                                  @JsonProperty("provides-files-for-command-mount") final String providesFilesForCommandMount,
                                                  @JsonProperty("default-value") final String defaultValue,
                                                  @JsonProperty("user-settable") final Boolean userSettable,
                                                  @JsonProperty("replacement-key") final String rawReplacementKey,
                                                  @JsonProperty("required") final Boolean required) {
            return builder()
                    .name(name == null ? "" : name)
                    .description(description)
                    .type(type == null ? CommandWrapperExternalInputEntity.DEFAULT_TYPE.getName() : type)
                    .matcher(matcher)
                    .providesValueForCommandInput(providesValueForCommandInput)
                    .providesFilesForCommandMount(providesFilesForCommandMount)
                    .defaultValue(defaultValue)
                    .userSettable(userSettable)
                    .rawReplacementKey(rawReplacementKey)
                    .required(required == null ? Boolean.FALSE : required)
                    .build();
        }

        static CommandWrapperExternalInput create(final CommandWrapperExternalInputEntity wrapperInput) {
            if (wrapperInput == null) {
                return null;
            }

            return builder()
                    .id(wrapperInput.getId())
                    .name(wrapperInput.getName())
                    .description(wrapperInput.getDescription())
                    .type(wrapperInput.getType().getName())
                    .matcher(wrapperInput.getMatcher())
                    .providesValueForCommandInput(wrapperInput.getProvidesValueForCommandInput())
                    .providesFilesForCommandMount(wrapperInput.getProvidesFilesForCommandMount())
                    .defaultValue(wrapperInput.getDefaultValue())
                    .userSettable(wrapperInput.getUserSettable())
                    .rawReplacementKey(wrapperInput.getRawReplacementKey())
                    .required(wrapperInput.isRequired() == null ? false : wrapperInput.isRequired())
                    .build();
        }

        static CommandWrapperExternalInput passthrough(final CommandInput commandInput) {
            if (commandInput == null) {
                return null;
            }
            return builder()
                    .name(commandInput.name())
                    .type(commandInput.type())
                    .matcher(commandInput.matcher())
                    .providesValueForCommandInput(commandInput.name())
                    .defaultValue(commandInput.defaultValue())
                    .userSettable(true)
                    .required(commandInput.required())
                    .build();
        }

        public static Builder builder() {
            return new AutoValue_Command_CommandWrapperExternalInput.Builder()
                    .id(0L)
                    .name("")
                    .type(CommandWrapperExternalInputEntity.DEFAULT_TYPE.getName())
                    .required(false);
        }

        @AutoValue.Builder
        public abstract static class Builder {
            public abstract Builder id(final long id);
            public abstract Builder name(final String name);
            public abstract Builder description(final String description);
            public abstract Builder type(final String type);
            public abstract Builder matcher(final String matcher);
            public abstract Builder providesValueForCommandInput(final String providesValueForCommandInput);
            public abstract Builder providesFilesForCommandMount(final String providesFilesForCommandMount);
            public abstract Builder defaultValue(final String defaultValue);
            public abstract Builder userSettable(final Boolean userSettable);
            public abstract Builder rawReplacementKey(final String rawReplacementKey);
            public abstract Builder required(final boolean required);

            public abstract CommandWrapperExternalInput build();
        }
    }

    @AutoValue
    public static abstract class CommandWrapperDerivedInput extends CommandWrapperInput {
        @Nullable @JsonProperty("derived-from-xnat-input") public abstract String derivedFromXnatInput();
        @Nullable @JsonProperty("derived-from-xnat-object-property") public abstract String derivedFromXnatObjectProperty();

        @JsonCreator
        static CommandWrapperDerivedInput create(@JsonProperty("name") final String name,
                                                 @JsonProperty("description") final String description,
                                                 @JsonProperty("type") final String type,
                                                 @JsonProperty("derived-from-xnat-input") final String derivedFromXnatInput,
                                                 @JsonProperty("derived-from-xnat-object-property") final String derivedFromXnatObjectProperty,
                                                 @JsonProperty("matcher") final String matcher,
                                                 @JsonProperty("provides-value-for-command-input") final String providesValueForCommandInput,
                                                 @JsonProperty("provides-files-for-command-mount") final String providesFilesForCommandMount,
                                                 @JsonProperty("default-value") final String defaultValue,
                                                 @JsonProperty("user-settable") final Boolean userSettable,
                                                 @JsonProperty("replacement-key") final String rawReplacementKey,
                                                 @JsonProperty("required") final Boolean required) {
            return builder()
                    .name(name == null ? "" : name)
                    .description(description)
                    .type(type == null ? CommandWrapperDerivedInputEntity.DEFAULT_TYPE.getName() : type)
                    .derivedFromXnatInput(derivedFromXnatInput)
                    .derivedFromXnatObjectProperty(derivedFromXnatObjectProperty)
                    .matcher(matcher)
                    .providesValueForCommandInput(providesValueForCommandInput)
                    .providesFilesForCommandMount(providesFilesForCommandMount)
                    .defaultValue(defaultValue)
                    .userSettable(userSettable)
                    .rawReplacementKey(rawReplacementKey)
                    .required(required == null ? Boolean.FALSE : required)
                    .build();
        }

        static CommandWrapperDerivedInput create(final CommandWrapperDerivedInputEntity wrapperInput) {
            if (wrapperInput == null) {
                return null;
            }

            return builder()
                    .id(wrapperInput.getId())
                    .name(wrapperInput.getName())
                    .description(wrapperInput.getDescription())
                    .type(wrapperInput.getType().getName())
                    .derivedFromXnatInput(wrapperInput.getDerivedFromXnatInput())
                    .derivedFromXnatObjectProperty(wrapperInput.getDerivedFromXnatObjectProperty())
                    .matcher(wrapperInput.getMatcher())
                    .providesValueForCommandInput(wrapperInput.getProvidesValueForCommandInput())
                    .providesFilesForCommandMount(wrapperInput.getProvidesFilesForCommandMount())
                    .defaultValue(wrapperInput.getDefaultValue())
                    .userSettable(wrapperInput.getUserSettable())
                    .rawReplacementKey(wrapperInput.getRawReplacementKey())
                    .required(wrapperInput.isRequired() == null ? false : wrapperInput.isRequired())
                    .build();
        }

        public static Builder builder() {
            return new AutoValue_Command_CommandWrapperDerivedInput.Builder()
                    .id(0L)
                    .name("")
                    .type(CommandWrapperDerivedInputEntity.DEFAULT_TYPE.getName())
                    .required(false);
        }

        @Nonnull
        List<String> validate() {
            // Derived inputs have all the same constraints as external inputs, plus more
            final List<String> errors = super.validate();

            if (StringUtils.isBlank(derivedFromXnatInput())) {
                errors.add("Command wrapper input \"" + name() + "\" - \"derived-from-xnat-input\" cannot be blank.");
            }

            return errors;
        }

        public String replacementKey() {
            return StringUtils.isNotBlank(rawReplacementKey()) ? rawReplacementKey() : "#" + name() + "#";
        }

        @AutoValue.Builder
        public abstract static class Builder {
            public abstract Builder id(final long id);
            public abstract Builder name(final String name);
            public abstract Builder description(final String description);
            public abstract Builder type(final String type);
            public abstract Builder matcher(final String matcher);
            public abstract Builder providesValueForCommandInput(final String providesValueForCommandInput);
            public abstract Builder providesFilesForCommandMount(final String providesFilesForCommandMount);
            public abstract Builder defaultValue(final String defaultValue);
            public abstract Builder userSettable(final Boolean userSettable);
            public abstract Builder rawReplacementKey(final String rawReplacementKey);
            public abstract Builder required(final boolean required);
            public abstract Builder derivedFromXnatInput(final String derivedFromXnatInput);
            public abstract Builder derivedFromXnatObjectProperty(final String derivedFromXnatObjectProperty);

            public abstract CommandWrapperDerivedInput build();
        }
    }

    @AutoValue
    public static abstract class CommandWrapperOutput {
        @JsonIgnore public abstract long id();
        @JsonProperty("accepts-command-output") public abstract String commandOutputName();
        @JsonProperty("as-a-child-of-xnat-input") public abstract String xnatInputName();
        @JsonProperty("type") public abstract String type();
        @Nullable @JsonProperty("label") public abstract String label();

        @JsonCreator
        static CommandWrapperOutput create(@JsonProperty("accepts-command-output") final String commandOutputName,
                                           @JsonProperty("as-a-child-of-xnat-input") final String xnatInputName,
                                           @JsonProperty("type") final String type,
                                           @JsonProperty("label") final String label) {
            return create(
                    0L,
                    commandOutputName == null ? "" : commandOutputName,
                    xnatInputName == null ? "" : xnatInputName,
                    type == null ? CommandWrapperOutputEntity.DEFAULT_TYPE.getName() : type,
                    label);
        }

        static CommandWrapperOutput create(final long id,
                                           final String commandOutputName,
                                           final String xnatInputName,
                                           final String type,
                                           final String label) {
            return new AutoValue_Command_CommandWrapperOutput(
                    0L,
                    commandOutputName == null ? "" : commandOutputName,
                    xnatInputName == null ? "" : xnatInputName,
                    type == null ? CommandWrapperOutputEntity.DEFAULT_TYPE.getName() : type,
                    label);
        }

        static CommandWrapperOutput create(final CommandWrapperOutputEntity wrapperOutput) {
            if (wrapperOutput == null) {
                return null;
            }
            return create(wrapperOutput.getId(), wrapperOutput.getCommandOutputName(), wrapperOutput.getXnatInputName(), wrapperOutput.getType().getName(), wrapperOutput.getLabel());
        }

        @Nonnull
        List<String> validate() {
            final List<String> errors = Lists.newArrayList();

            if (StringUtils.isBlank(commandOutputName())) {
                errors.add("Command wrapper output - command output name cannot be blank.");
            }
            if (StringUtils.isBlank(xnatInputName())) {
                errors.add("Command wrapper output - xnat input name cannot be blank.");
            }
            final List<String> types = CommandWrapperOutputEntity.Type.names();
            if (!types.contains(type())) {
                errors.add("Command wrapper output - Unknown type \"" + type() + "\". Known types: " + StringUtils.join(types, ", "));
            }

            return errors;
        }
    }

}
