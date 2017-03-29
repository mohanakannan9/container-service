package org.nrg.containers.model.configuration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import org.nrg.containers.model.command.auto.Command;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

@AutoValue
public abstract class CommandConfiguration {
    @JsonProperty("inputs") public abstract ImmutableMap<String, CommandInputConfiguration> inputs();
    @JsonProperty("outputs") public abstract ImmutableMap<String, CommandOutputConfiguration> outputs();

    @JsonCreator
    public static CommandConfiguration create(@JsonProperty("inputs") final Map<String, CommandInputConfiguration> inputs,
                                              @JsonProperty("outputs") final Map<String, CommandOutputConfiguration> outputs) {
        return builder()
                .inputs(inputs == null ? Collections.<String, CommandInputConfiguration>emptyMap() : inputs)
                .outputs(outputs == null ? Collections.<String, CommandOutputConfiguration>emptyMap() : outputs)
                .build();
    }

    public static CommandConfiguration create(final @Nonnull Command command,
                                              final CommandConfigurationInternal commandConfigurationInternal,
                                              final String wrapperName) {
        Builder builder = builder();
        final Set<String> handledCommandInputs = Sets.newHashSet();

        // TODO do something with the configuration

        Command.CommandWrapper commandWrapper = null;
        for (final Command.CommandWrapper commandWrapperLoop : command.xnatCommandWrappers()) {
            if (commandWrapperLoop.name().equals(wrapperName)) {
                commandWrapper = commandWrapperLoop;
                break;
            }
        }
        if (commandWrapper != null) {
            for (final Command.CommandWrapperExternalInput externalInput : commandWrapper.externalInputs()) {
                builder = builder.addInput(externalInput.name(), CommandInputConfiguration.create(externalInput));
                handledCommandInputs.add(externalInput.providesValueForCommandInput());
            }
            for (final Command.CommandWrapperDerivedInput derivedInput : commandWrapper.derivedInputs()) {
                builder = builder.addInput(derivedInput.name(), CommandInputConfiguration.create(derivedInput));
                handledCommandInputs.add(derivedInput.providesValueForCommandInput());
            }
            for (final Command.CommandWrapperOutput wrapperOutput : commandWrapper.outputHandlers()) {
                builder = builder.addOutput(wrapperOutput.name(), CommandOutputConfiguration.create(wrapperOutput));
            }
        }

        for (final Command.CommandInput commandInput : command.inputs()) {
            if (!handledCommandInputs.contains(commandInput.name())) {
                builder = builder.addInput(commandInput.name(), CommandInputConfiguration.create(commandInput));
            }
        }

        return builder.build();
    }

    public static Builder builder() {
        return new AutoValue_CommandConfiguration.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder inputs(@Nonnull Map<String, CommandInputConfiguration> inputs);
        abstract ImmutableMap.Builder<String, CommandInputConfiguration> inputsBuilder();
        public Builder addInput(final @Nonnull String inputName, final CommandInputConfiguration commandInputConfiguration) {
            inputsBuilder().put(inputName, commandInputConfiguration);
            return this;
        }

        public abstract Builder outputs(@Nonnull Map<String, CommandOutputConfiguration> outputs);
        abstract ImmutableMap.Builder<String, CommandOutputConfiguration> outputsBuilder();
        public Builder addOutput(final @Nonnull String outputName, final CommandOutputConfiguration commandOutputConfiguration) {
            outputsBuilder().put(outputName, commandOutputConfiguration);
            return this;
        }

        public abstract CommandConfiguration build();
    }

    @Nullable
    public String getInputDefaultValue(final String inputName) {
        final CommandInputConfiguration input = inputs().get(inputName);
        return input == null ? null : input.defaultValue();
    }

    @Nullable
    public String getInputMatcher(final String inputName) {
        final CommandInputConfiguration input = inputs().get(inputName);
        return input == null ? null : input.matcher();
    }

    @Nullable
    public Boolean isInputUserSettable(final String inputName) {
        final CommandInputConfiguration input = inputs().get(inputName);
        return input == null ? null : input.userSettable();
    }

    @Nullable
    public String getOutputLabel(final String outputName) {
        final CommandOutputConfiguration output = outputs().get(outputName);
        return output == null ? null : output.label();
    }

    @AutoValue
    public static abstract class CommandInputConfiguration {
        @Nullable @JsonProperty("default-value") public abstract String defaultValue();
        @Nullable @JsonProperty("matcher") public abstract String matcher();
        @Nullable @JsonProperty("user-settable") public abstract Boolean userSettable();
        @Nullable @JsonProperty("advanced") public abstract Boolean advanced();

        @JsonCreator
        static CommandInputConfiguration create(@JsonProperty("default-value") final String defaultValue,
                                                @JsonProperty("matcher") final String matcher,
                                                @JsonProperty("user-settable") final Boolean userSettable,
                                                @JsonProperty("advanced") final Boolean advanced) {
            return builder()
                    .defaultValue(defaultValue)
                    .matcher(matcher)
                    .userSettable(userSettable)
                    .advanced(advanced)
                    .build();
        }

        static CommandInputConfiguration create(final Command.CommandInput commandInput) {
            return create(commandInput.defaultValue(), commandInput.matcher(), true, false);
        }

        static CommandInputConfiguration create(final Command.CommandWrapperInput commandWrapperInput) {
            return create(commandWrapperInput.defaultValue(), commandWrapperInput.matcher(), true, false);
        }

        public static Builder builder() {
            return new AutoValue_CommandConfiguration_CommandInputConfiguration.Builder();
        }

        @AutoValue.Builder
        public abstract static class Builder {
            public abstract Builder defaultValue(final String defaultValue);
            public abstract Builder matcher(final String matcher);
            public abstract Builder userSettable(final Boolean userSettable);
            public abstract Builder advanced(final Boolean advanced);

            public abstract CommandInputConfiguration build();
        }
    }

    @AutoValue
    public static abstract class CommandOutputConfiguration {
        @Nullable @JsonProperty("label") public abstract String label();

        @JsonCreator
        public static CommandOutputConfiguration create(@JsonProperty("label") final String label) {
            return new AutoValue_CommandConfiguration_CommandOutputConfiguration(label);
        }

        static CommandOutputConfiguration create(final Command.CommandWrapperOutput commandWrapperOutput) {
            return create(commandWrapperOutput.label());
        }
    }
}
