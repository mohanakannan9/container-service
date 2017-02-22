package org.nrg.containers.model.auto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.nrg.containers.model.CommandEntity;
import org.nrg.containers.model.CommandInput;
import org.nrg.containers.model.CommandMount;
import org.nrg.containers.model.CommandOutput;
import org.nrg.containers.model.CommandType;
import org.nrg.containers.model.DockerCommandEntity;
import org.nrg.containers.model.CommandWrapperInputEntity;
import org.nrg.containers.model.XnatCommandOutput;
import org.nrg.containers.model.CommandWrapperEntity;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Set;

@AutoValue
public abstract class CommandPojo {
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
    @JsonProperty("mounts") public abstract List<CommandMountPojo> mounts();
    @JsonProperty("environment-variables") public abstract Map<String, String> environmentVariables();
    @JsonProperty("ports") public abstract Map<String, String> ports();
    @JsonProperty("inputs") public abstract List<CommandInputPojo> inputs();
    @JsonProperty("outputs") public abstract List<CommandOutputPojo> outputs();
    @JsonProperty("xnat") public abstract List<CommandWrapperPojo> xnatCommandWrappers();

    @JsonCreator
    static CommandPojo create(@JsonProperty("id") long id,
                              @JsonProperty("name") String name,
                              @JsonProperty("label") String label,
                              @JsonProperty("description") String description,
                              @JsonProperty("version") String version,
                              @JsonProperty("schema-version") String schemaVersion,
                              @JsonProperty("info-url") String infoUrl,
                              @JsonProperty("image") String image,
                              @JsonProperty("type") String type,
                              @JsonProperty("index") String index,
                              @JsonProperty("hash") String hash,
                              @JsonProperty("working-directory") String workingDirectory,
                              @JsonProperty("command-line") String commandLine,
                              @JsonProperty("mounts") List<CommandMountPojo> mounts,
                              @JsonProperty("environment-variables") Map<String, String> environmentVariables,
                              @JsonProperty("ports") Map<String, String> ports,
                              @JsonProperty("inputs") List<CommandInputPojo> inputs,
                              @JsonProperty("outputs") List<CommandOutputPojo> outputs,
                              @JsonProperty("xnat") List<CommandWrapperPojo> xnatCommandWrappers) {
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
                .mounts(mounts == null ? Lists.<CommandMountPojo>newArrayList() : mounts)
                .environmentVariables(environmentVariables == null ? Maps.<String, String>newHashMap() : environmentVariables)
                .ports(ports == null ? Maps.<String, String>newHashMap() : ports)
                .inputs(inputs == null ? Lists.<CommandInputPojo>newArrayList() : inputs)
                .outputs(outputs == null ? Lists.<CommandOutputPojo>newArrayList() : outputs)
                .xnatCommandWrappers(xnatCommandWrappers == null ? Lists.<CommandWrapperPojo>newArrayList() : xnatCommandWrappers)
                .build();
    }

    static CommandPojo create(final CommandEntity commandEntity) {
        CommandPojo.Builder builder = builder()
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
                .environmentVariables(commandEntity.getEnvironmentVariables() == null ? Maps.<String, String>newHashMap() : commandEntity.getEnvironmentVariables())
                .mounts(commandEntity.getMounts() == null ? Lists.<CommandMountPojo>newArrayList() :
                        Lists.transform(commandEntity.getMounts(), new Function<CommandMount, CommandMountPojo>() {
                            @Nullable
                            @Override
                            public CommandMountPojo apply(@Nullable final CommandMount mount) {
                                return mount == null ? null : CommandMountPojo.create(mount);
                            }
                        }))
                .inputs(commandEntity.getInputs() == null ? Lists.<CommandInputPojo>newArrayList() :
                        Lists.transform(commandEntity.getInputs(), new Function<CommandInput, CommandInputPojo>() {
                            @Nullable
                            @Override
                            public CommandInputPojo apply(@Nullable final CommandInput commandInput) {
                                return commandInput == null ? null : CommandInputPojo.create(commandInput);
                            }
                        }))
                .outputs(commandEntity.getOutputs() == null ? Lists.<CommandOutputPojo>newArrayList() :
                        Lists.transform(commandEntity.getOutputs(), new Function<CommandOutput, CommandOutputPojo>() {
                            @Nullable
                            @Override
                            public CommandOutputPojo apply(@Nullable final CommandOutput commandOutput) {
                                return commandOutput == null ? null : CommandOutputPojo.create(commandOutput);
                            }
                        }))
                .xnatCommandWrappers(commandEntity.getCommandWrapperEntities() == null ? Lists.<CommandWrapperPojo>newArrayList() :
                        Lists.transform(commandEntity.getCommandWrapperEntities(), new Function<CommandWrapperEntity, CommandWrapperPojo>() {
                            @Nullable
                            @Override
                            public CommandWrapperPojo apply(@Nullable final CommandWrapperEntity xnatCommandWrapper) {
                                return xnatCommandWrapper == null ? null : CommandWrapperPojo.create(xnatCommandWrapper);
                            }
                        }));

        switch (commandEntity.getType()) {
            case DOCKER:
                builder = builder.index(((DockerCommandEntity) commandEntity).getIndex())
                        .hash(((DockerCommandEntity) commandEntity).getHash())
                        .ports(((DockerCommandEntity) commandEntity).getPorts() == null ? Maps.<String, String>newHashMap() : ((DockerCommandEntity) commandEntity).getPorts());
                break;
        }

        return builder.build();
    }

    public abstract Builder toBuilder();

    public static Builder builder() {
        return new AutoValue_CommandPojo.Builder()
                .id(0L)
                .name("")
                .type(CommandEntity.DEFAULT_TYPE.getName())
                .mounts(Lists.<CommandMountPojo>newArrayList())
                .environmentVariables(Maps.<String, String>newHashMap())
                .ports(Maps.<String, String>newHashMap())
                .inputs(Lists.<CommandInputPojo>newArrayList())
                .outputs(Lists.<CommandOutputPojo>newArrayList())
                .xnatCommandWrappers(Lists.<CommandWrapperPojo>newArrayList());
    }

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

        final Set<String> mountNames = Sets.newHashSet();
        for (final CommandMountPojo mount : mounts()) {
            final List<String> mountErrors = mount.validate();

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
        for (final CommandInputPojo input : inputs()) {
            final List<String> inputErrors = input.validate();

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
        for (final CommandOutputPojo output : outputs()) {
            final List<String> outputErrors = output.validate();

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
        for (final CommandWrapperPojo commandWrapperPojo : xnatCommandWrappers()) {
            final List<String> wrapperErrors = commandWrapperPojo.validate();

            if (wrapperNames.contains(commandWrapperPojo.name())) {
                errors.add(commandName + "wrapper name \"" + commandWrapperPojo.name() + "\" is not unique.");
            } else {
                wrapperNames.add(commandWrapperPojo.name());
            }
            final String wrapperName = commandName + "wrapper \"" + commandWrapperPojo.name() + "\" - ";

            final Set<String> wrapperInputNames = Sets.newHashSet();
            for (final CommandWrapperInputPojo external : commandWrapperPojo.externalInputs()) {
                final List<String> inputErrors = external.validateExternal();

                if (wrapperInputNames.contains(external.name())) {
                    errors.add(wrapperName + "external input name \"" + external.name() + "\" is not unique.");
                } else {
                    wrapperInputNames.add(external.name());
                }


                if (!inputErrors.isEmpty()) {
                    errors.addAll(inputErrors);
                }
            }

            for (final CommandWrapperInputPojo derived : commandWrapperPojo.derivedInputs()) {
                final List<String> inputErrors = derived.validateDerived();

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
            for (final CommandWrapperOutputPojo output : commandWrapperPojo.outputHandlers()) {
                final List<String> outputErrors = output.validate();

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

    public void addCommandWrapper(final CommandWrapperPojo commandWrapperPojo) {
        if (commandWrapperPojo == null) {
            return;
        }
        this.xnatCommandWrappers().add(commandWrapperPojo);
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
        public abstract Builder mounts(final List<CommandMountPojo> mounts);
        public abstract Builder environmentVariables(final Map<String, String> environmentVariables);
        public abstract Builder ports(final Map<String, String> ports);
        public abstract Builder inputs(final List<CommandInputPojo> inputs);
        public abstract Builder outputs(final List<CommandOutputPojo> outputs);
        public abstract Builder xnatCommandWrappers(final List<CommandWrapperPojo> xnatCommandWrappers);

        public abstract CommandPojo build();
    }

    @AutoValue
    public static abstract class CommandMountPojo {
        @Nullable @JsonProperty("name") public abstract String name();
        @Nullable @JsonProperty("writable") public abstract Boolean writable();
        @Nullable @JsonProperty("path") public abstract String path();

        @JsonCreator
        static CommandMountPojo create(@JsonProperty("name") String name,
                                       @JsonProperty("writable") Boolean writable,
                                       @JsonProperty("path") String path) {
            return new AutoValue_CommandPojo_CommandMountPojo(name, writable, path);
        }

        static CommandMountPojo create(final CommandMount mount) {
            return CommandMountPojo.create(mount.getName(), mount.isWritable(), mount.getContainerPath());
        }

        List<String> validate() {
            final List<String> errors = Lists.newArrayList();
            if (StringUtils.isBlank(name())) {
                errors.add("Mount name cannot be blank.");
            }
            if (StringUtils.isBlank(path())) {
                errors.add("Mount path cannot be blank.");
            }

            return errors;
        }
    }

    @AutoValue
    public static abstract class CommandInputPojo {
        @Nullable @JsonProperty("name") public abstract String name();
        @Nullable @JsonProperty("description") public abstract String description();
        @JsonProperty("type") public abstract String type();
        @Nullable @JsonProperty("required") public abstract Boolean required();
        @Nullable @JsonProperty("matcher") public abstract String matcher();
        @Nullable @JsonProperty("default-value") public abstract String defaultValue();
        @Nullable @JsonProperty("replacement-key") public abstract String rawReplacementKey();
        @Nullable @JsonProperty("command-line-flag") public abstract String commandLineFlag();
        @Nullable @JsonProperty("command-line-separator") public abstract String commandLineSeparator();
        @Nullable @JsonProperty("true-value") public abstract String trueValue();
        @Nullable @JsonProperty("false-value") public abstract String falseValue();
        @Nullable @JsonProperty("value") public abstract String value();

        @JsonCreator
        static CommandInputPojo create(@JsonProperty("name") String name,
                                       @JsonProperty("description") String description,
                                       @JsonProperty("type") String type,
                                       @JsonProperty("required") Boolean required,
                                       @JsonProperty("matcher") String matcher,
                                       @JsonProperty("default-value") String defaultValue,
                                       @JsonProperty("replacement-key") String rawReplacementKey,
                                       @JsonProperty("command-line-flag") String commandLineFlag,
                                       @JsonProperty("command-line-separator") String commandLineSeparator,
                                       @JsonProperty("true-value") String trueValue,
                                       @JsonProperty("false-value") String falseValue,
                                       @JsonProperty("value") String value) {
            return new AutoValue_CommandPojo_CommandInputPojo(name, description, type == null ? CommandInput.DEFAULT_TYPE.getName() : type, required, matcher, defaultValue, rawReplacementKey, commandLineFlag, commandLineSeparator, trueValue, falseValue, value);
        }

        static CommandInputPojo create(final CommandInput commandInput) {
            return create(commandInput.getName(), commandInput.getDescription(), commandInput.getType().getName(), commandInput.isRequired(), commandInput.getMatcher(),
                    commandInput.getDefaultValue(), commandInput.getRawReplacementKey(), commandInput.getCommandLineFlag(), commandInput.getCommandLineSeparator(),
                    commandInput.getTrueValue(), commandInput.getFalseValue(), commandInput.getValue());
        }

        List<String> validate() {
            final List<String> errors = Lists.newArrayList();
            if (StringUtils.isBlank(name())) {
                errors.add("Name cannot be blank");
            }
            return errors;
        }
    }
    @AutoValue
    public static abstract class CommandOutputPojo {
        @Nullable @JsonProperty("name") public abstract String name();
        @Nullable @JsonProperty("description") public abstract String description();
        @Nullable @JsonProperty("required") public abstract Boolean required();
        @Nullable @JsonProperty("mount") public abstract String mount();
        @Nullable @JsonProperty("path") public abstract String path();
        @Nullable @JsonProperty("glob") public abstract String glob();

        @JsonCreator
        static CommandOutputPojo create(@JsonProperty("name") String name,
                                        @JsonProperty("description") String description,
                                        @JsonProperty("required") Boolean required,
                                        @JsonProperty("mount") String mount,
                                        @JsonProperty("path") String path,
                                        @JsonProperty("glob") String glob) {
            return new AutoValue_CommandPojo_CommandOutputPojo(name, description, required, mount, path, glob);
        }

        static CommandOutputPojo create(final CommandOutput commandOutput) {
            return create(commandOutput.getName(), commandOutput.getDescription(), commandOutput.isRequired(), commandOutput.getMount(),
                    commandOutput.getPath(), commandOutput.getGlob());
        }

        List<String> validate() {
            final List<String> errors = Lists.newArrayList();
            if (StringUtils.isBlank(name())) {
                errors.add("Name cannot be blank.");
            }
            if (StringUtils.isBlank(mount())) {
                errors.add("Mount cannot be blank.");
            }
            return errors;
        }
    }

    @AutoValue
    public static abstract class CommandWrapperPojo {
        @JsonProperty("id") public abstract long id();
        @Nullable @JsonProperty("name") public abstract String name();
        @Nullable @JsonProperty("description") public abstract String description();
        @JsonProperty("external-inputs") public abstract List<CommandWrapperInputPojo> externalInputs();
        @JsonProperty("derived-inputs") public abstract List<CommandWrapperInputPojo> derivedInputs();
        @JsonProperty("output-handlers") public abstract List<CommandWrapperOutputPojo> outputHandlers();

        @JsonCreator
        static CommandWrapperPojo create(@JsonProperty("id") long id,
                                         @JsonProperty("name") String name,
                                         @JsonProperty("description") String description,
                                         @JsonProperty("external-inputs") List<CommandWrapperInputPojo> externalInputs,
                                         @JsonProperty("derived-inputs") List<CommandWrapperInputPojo> derivedInputs,
                                         @JsonProperty("output-handlers") List<CommandWrapperOutputPojo> outputHandlers) {
            return builder()
                    .id(id)
                    .name(name)
                    .description(description)
                    .externalInputs(externalInputs == null ? Lists.<CommandWrapperInputPojo>newArrayList() : externalInputs)
                    .derivedInputs(derivedInputs == null ? Lists.<CommandWrapperInputPojo>newArrayList() : derivedInputs)
                    .outputHandlers(outputHandlers == null ? Lists.<CommandWrapperOutputPojo>newArrayList() : outputHandlers)
                    .build();
        }

        public static Builder builder() {
            return new AutoValue_CommandPojo_CommandWrapperPojo.Builder()
                    .id(0L)
                    .externalInputs(Lists.<CommandWrapperInputPojo>newArrayList())
                    .derivedInputs(Lists.<CommandWrapperInputPojo>newArrayList())
                    .outputHandlers(Lists.<CommandWrapperOutputPojo>newArrayList());
        }

        static CommandWrapperPojo create(final CommandWrapperEntity commandWrapperEntity) {
            final List<CommandWrapperInputPojo> external = commandWrapperEntity.getExternalInputs() == null ?
                    Lists.<CommandWrapperInputPojo>newArrayList() :
                    Lists.transform(commandWrapperEntity.getExternalInputs(), new Function<CommandWrapperInputEntity, CommandWrapperInputPojo>() {
                        @Nullable
                        @Override
                        public CommandWrapperInputPojo apply(@Nullable final CommandWrapperInputEntity xnatCommandInput) {
                            return xnatCommandInput == null ? null : CommandWrapperInputPojo.create(xnatCommandInput);
                        }
                    });
            final List<CommandWrapperInputPojo> derived = commandWrapperEntity.getDerivedInputs() == null ?
                    Lists.<CommandWrapperInputPojo>newArrayList() :
                    Lists.transform(commandWrapperEntity.getDerivedInputs(), new Function<CommandWrapperInputEntity, CommandWrapperInputPojo>() {
                        @Nullable
                        @Override
                        public CommandWrapperInputPojo apply(@Nullable final CommandWrapperInputEntity xnatCommandInput) {
                            return xnatCommandInput == null ? null : CommandWrapperInputPojo.create(xnatCommandInput);
                        }
                    });
            final List<CommandWrapperOutputPojo> outputs = commandWrapperEntity.getOutputHandlers() == null ?
                    Lists.<CommandWrapperOutputPojo>newArrayList() :
                    Lists.transform(commandWrapperEntity.getOutputHandlers(), new Function<XnatCommandOutput, CommandWrapperOutputPojo>() {
                        @Nullable
                        @Override
                        public CommandWrapperOutputPojo apply(@Nullable final XnatCommandOutput xnatCommandOutput) {
                            return xnatCommandOutput == null ? null : CommandWrapperOutputPojo.create(xnatCommandOutput);
                        }
                    });
            return create(commandWrapperEntity.getId(), commandWrapperEntity.getName(), commandWrapperEntity.getDescription(),
                    external, derived, outputs);
        }

        List<String> validate() {
            final List<String> errors = Lists.newArrayList();
            if (StringUtils.isBlank(name())) {
                errors.add("Name cannot be blank.");
            }

            return errors;
        }

        @AutoValue.Builder
        public abstract static class Builder {
            public abstract Builder id(final long id);
            public abstract Builder name(final String name);
            public abstract Builder description(final String description);
            public abstract Builder externalInputs(final List<CommandWrapperInputPojo> externalInputs);
            public abstract Builder derivedInputs(final List<CommandWrapperInputPojo> derivedInputs);
            public abstract Builder outputHandlers(final List<CommandWrapperOutputPojo> outputHandlers);

            public abstract CommandWrapperPojo build();
        }
    }

    @AutoValue
    public static abstract class CommandWrapperInputPojo {
        @Nullable @JsonProperty("name") public abstract String name();
        @Nullable @JsonProperty("description") public abstract String description();
        @JsonProperty("type") public abstract String type();
        @Nullable @JsonProperty("derived-from-xnat-input") public abstract String derivedFromXnatInput();
        @Nullable @JsonProperty("derived-from-xnat-object-property") public abstract String derivedFromXnatObjectProperty();
        @Nullable @JsonProperty("matcher") public abstract String matcher();
        @Nullable @JsonProperty("provides-value-for-command-input") public abstract String providesValueForCommandInput();
        @Nullable @JsonProperty("provides-files-for-command-mount") public abstract String providesFilesForCommandMount();
        @Nullable @JsonProperty("default-value") public abstract String defaultValue();
        @Nullable @JsonProperty("user-settable") public abstract Boolean userSettable();
        @Nullable @JsonProperty("replacement-key") public abstract String rawReplacementKey();
        @JsonProperty("required") public abstract boolean required();
        @Nullable @JsonProperty("value") public abstract String value();

        @JsonCreator
        static CommandWrapperInputPojo create(@JsonProperty("name") String name,
                                              @JsonProperty("description") String description,
                                              @JsonProperty("type") String type,
                                              @JsonProperty("derived-from-xnat-input") String derivedFromXnatInput,
                                              @JsonProperty("derived-from-xnat-object-property") String derivedFromXnatObjectProperty,
                                              @JsonProperty("matcher") String matcher,
                                              @JsonProperty("provides-value-for-command-input") String providesValueForCommandInput,
                                              @JsonProperty("provides-files-for-command-mount") String providesFilesForCommandMount,
                                              @JsonProperty("default-value") String defaultValue,
                                              @JsonProperty("user-settable") Boolean userSettable,
                                              @JsonProperty("replacement-key") String rawReplacementKey,
                                              @JsonProperty("required") Boolean required,
                                              @JsonProperty("value") String value) {
            return new AutoValue_CommandPojo_CommandWrapperInputPojo(
                    name,
                    description,
                    type == null ? CommandWrapperInputEntity.DEFAULT_TYPE.getName() : type,
                    derivedFromXnatInput,
                    derivedFromXnatObjectProperty,
                    matcher,
                    providesValueForCommandInput,
                    providesFilesForCommandMount,
                    defaultValue,
                    userSettable,
                    rawReplacementKey,
                    required == null ? Boolean.FALSE : required,
                    value);
        }

        static CommandWrapperInputPojo create(final CommandWrapperInputEntity wrapperInput) {
            return create(wrapperInput.getName(), wrapperInput.getDescription(), wrapperInput.getType().getName(), wrapperInput.getDerivedFromXnatInput(),
                    wrapperInput.getDerivedFromXnatObjectProperty(), wrapperInput.getMatcher(), wrapperInput.getProvidesValueForCommandInput(),
                    wrapperInput.getProvidesFilesForCommandMount(), wrapperInput.getDefaultValue(), wrapperInput.getUserSettable(), wrapperInput.getRawReplacementKey(),
                    wrapperInput.isRequired() == null ? false : wrapperInput.isRequired(), wrapperInput.getValue());
        }

        List<String> validateExternal() {
            final List<String> errors = Lists.newArrayList();
            if (StringUtils.isBlank(name())) {
                errors.add("Name cannot be blank.");
            }

            final List<String> types = CommandWrapperInputEntity.Type.names();
            if (!types.contains(type())) {
                errors.add("Unknown type \"" + type() + "\". Known types: " + StringUtils.join(types, ", "));
            }

            return errors;
        }

        List<String> validateDerived() {
            // Derived inputs have all the same constraints as external inputs, plus more
            final List<String> errors = validateExternal();

            if (StringUtils.isBlank(derivedFromXnatInput())) {
                errors.add("\"Derived from\" cannot be blank.");
            }

            return errors;
        }
    }

    @AutoValue
    public static abstract class CommandWrapperOutputPojo {
        @Nullable @JsonProperty("accepts-command-output") public abstract String commandOutputName();
        @Nullable @JsonProperty("as-a-child-of-xnat-input") public abstract String xnatInputName();
        @JsonProperty("type") public abstract String type();
        @Nullable @JsonProperty("label") public abstract String label();

        @JsonCreator
        static CommandWrapperOutputPojo create(@JsonProperty("accepts-command-output") String commandOutputName,
                                               @JsonProperty("as-a-child-of-xnat-input") String xnatInputName,
                                               @JsonProperty("type") String type,
                                               @JsonProperty("label") String label) {
            return new AutoValue_CommandPojo_CommandWrapperOutputPojo(
                    commandOutputName,
                    xnatInputName,
                    type == null ? XnatCommandOutput.DEFAULT_TYPE.getName() : type,
                    label);
        }

        static CommandWrapperOutputPojo create(final XnatCommandOutput wrapperOutput) {
            return create(wrapperOutput.getCommandOutputName(), wrapperOutput.getXnatInputName(), wrapperOutput.getType().getName(), wrapperOutput.getLabel());
        }

        List<String> validate() {
            final List<String> errors = Lists.newArrayList();

            final List<String> types = XnatCommandOutput.Type.names();
            if (!types.contains(type())) {
                errors.add("Unknown type \"" + type() + "\". Known types: " + StringUtils.join(types, ", "));
            }

            if (StringUtils.isBlank(commandOutputName())) {
                errors.add("Command output name cannot be blank.");
            }
            if (StringUtils.isBlank(xnatInputName())) {
                errors.add("Xnat input name cannot be blank.");
            }

            return errors;
        }
    }

}
