package org.nrg.containers.model.command.auto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import org.nrg.containers.model.command.auto.ResolvedCommand.PartiallyResolvedCommand;
import org.nrg.containers.model.configuration.CommandConfiguration;
import org.nrg.containers.model.configuration.CommandConfiguration.CommandInputConfiguration;

import javax.annotation.Nullable;
import java.util.Map;

@AutoValue
public abstract class LaunchUi {
    @JsonProperty("command-id") public abstract long commandId();
    @JsonProperty("command-name") public abstract String commandName();
    @JsonProperty("command-description") public abstract String commandDescription();
    @JsonProperty("wrapper-id") public abstract long wrapperId();
    @JsonProperty("wrapper-name") public abstract String wrapperName();
    @JsonProperty("wrapper-description") public abstract String wrapperDescription();
    @JsonProperty("image-name") public abstract String imageName();
    @JsonProperty("image-type") public abstract String imageType();
    @JsonProperty("inputs") public abstract ImmutableMap<String, LaunchUiInput> inputs();

    public static LaunchUi create(final PartiallyResolvedCommand partiallyResolvedCommand,
                                  final CommandConfiguration commandConfiguration) {
        final LaunchUi.Builder builder = builder()
                .commandId(partiallyResolvedCommand.commandId())
                .commandName(partiallyResolvedCommand.commandName())
                .commandDescription(partiallyResolvedCommand.commandDescription())
                .wrapperId(partiallyResolvedCommand.wrapperId())
                .wrapperName(partiallyResolvedCommand.wrapperName())
                .wrapperDescription(partiallyResolvedCommand.wrapperDescription())
                .imageName(partiallyResolvedCommand.image())
                .imageType(partiallyResolvedCommand.type());

        for (final Map.Entry<String, CommandInputConfiguration> commandInputConfigurationEntry : commandConfiguration.inputs().entrySet()) {
            final String inputName = commandInputConfigurationEntry.getKey();
            final CommandInputConfiguration inputConfiguration = commandInputConfigurationEntry.getValue();

            final String resolvedValue = partiallyResolvedCommand.wrapperInputValues().containsKey(inputName) ?
                    partiallyResolvedCommand.wrapperInputValues().get(inputName) :
                    partiallyResolvedCommand.commandInputValues().containsKey(inputName) ?
                            partiallyResolvedCommand.commandInputValues().get(inputName) :
                            partiallyResolvedCommand.rawInputValues().containsKey(inputName) ?
                                    partiallyResolvedCommand.rawInputValues().get(inputName) :
                                    null;
            builder.addInput(inputName, LaunchUiInput.create(inputConfiguration, resolvedValue));
        }

        return builder.build();
    }

    public static Builder builder() {
        return new AutoValue_LaunchUi.Builder();
    }

    @AutoValue
    public static abstract class LaunchUiInput {
        @JsonProperty("description") public abstract String description();
        @JsonProperty("type") public abstract String type();
        @JsonProperty("user-settable") public abstract Boolean userSettable();
        @JsonProperty("advanced") public abstract Boolean advanced();
        @JsonProperty("required") public abstract Boolean required();
        @JsonProperty("value") public abstract String value();

        public static LaunchUiInput create(final CommandInputConfiguration inputConfiguration,
                                           final @Nullable String resolvedValue) {
            final String inputType = inputConfiguration.type(); // TODO figure out how to get UI types from this
            return new AutoValue_LaunchUi_LaunchUiInput(inputConfiguration.description(), inputType,
                    inputConfiguration.userSettable(), inputConfiguration.advanced(), inputConfiguration.required(),
                    resolvedValue == null ? inputConfiguration.defaultValue() : resolvedValue);
        }
    }

    @AutoValue.Builder
    public static abstract class Builder {
        public abstract Builder commandId(final long commandId);
        public abstract Builder commandName(final String commandName);
        public abstract Builder commandDescription(final String commandDescription);
        public abstract Builder wrapperId(final long wrapperId);
        public abstract Builder wrapperName(final String wrapperName);
        public abstract Builder wrapperDescription(final String wrapperDescription);
        public abstract Builder imageName(final String imageName);
        public abstract Builder imageType(final String imageType);
        public abstract Builder inputs(final Map<String, LaunchUiInput> inputs);
        abstract ImmutableMap.Builder<String, LaunchUiInput> inputsBuilder();
        public Builder addInput(final String name, final LaunchUiInput value) {
            inputsBuilder().put(name, value);
            return this;
        }

        public abstract LaunchUi build();
    }
}
