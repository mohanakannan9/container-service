package org.nrg.containers.model.command.auto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

/**
 * This is a value class that will be returned when the UI requests
 * a list of commands that are available to be launched for a given context.
 */
@AutoValue
@JsonInclude(JsonInclude.Include.ALWAYS)
public abstract class CommandSummaryForContext {
    @JsonProperty("command-id") public abstract long commandId();
    @JsonProperty("command-name") public abstract String commandName();
    @JsonProperty("command-description") public abstract String commandDescription();
    @JsonProperty("wrapper-id") public abstract long wrapperId();
    @JsonProperty("wrapper-name") public abstract String wrapperName();
    @JsonProperty("wrapper-description") public abstract String wrapperDescription();
    @JsonProperty("image-name") public abstract String imageName();
    @JsonProperty("image-type") public abstract String imageType();
    @JsonProperty("enabled") public abstract boolean enabled();
    @JsonProperty("root-element-name") public abstract String externalInputName();

    public static CommandSummaryForContext create(final Command command,
                                                  final Command.CommandWrapper wrapper,
                                                  final boolean enabled,
                                                  final String externalInputName) {
        return new AutoValue_CommandSummaryForContext(
                command.id(),
                command.name(),
                command.description(),
                wrapper.id(),
                wrapper.name(),
                wrapper.description(),
                command.image(),
                command.type(),
                enabled,
                externalInputName
        );
    }
}
