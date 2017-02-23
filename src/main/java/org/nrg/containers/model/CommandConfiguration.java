package org.nrg.containers.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.Maps;

import javax.annotation.Nullable;
import java.util.Map;

@AutoValue
public abstract class CommandConfiguration {
    @JsonProperty("inputs") abstract Map<String, CommandInputConfiguration> inputs();
    @JsonProperty("outputs") abstract Map<String, CommandOutputConfiguration> outputs();

    @JsonCreator
    public static CommandConfiguration create(@JsonProperty("inputs") final Map<String, CommandInputConfiguration> inputs,
                                       @JsonProperty("outputs") final Map<String, CommandOutputConfiguration> outputs) {
        return new AutoValue_CommandConfiguration(inputs == null ? Maps.<String, CommandInputConfiguration>newHashMap() : inputs,
                outputs == null ? Maps.<String, CommandOutputConfiguration>newHashMap() : outputs);
    }

    public CommandConfiguration merge(final CommandConfiguration that) {
        if (that == null) {
            return this;
        }

        for (final Map.Entry<String, CommandInputConfiguration> otherInput : that.inputs().entrySet()) {
            final CommandInputConfiguration thisInputValue = this.inputs().get(otherInput.getKey());
            this.inputs().put(otherInput.getKey(),
                    thisInputValue == null ? otherInput.getValue() : thisInputValue.merge(otherInput.getValue()));
        }
        for (final Map.Entry<String, CommandOutputConfiguration> otherOutput : that.outputs().entrySet()) {
            final CommandOutputConfiguration thisOutputValue = this.outputs().get(otherOutput.getKey());
            this.outputs().put(otherOutput.getKey(),
                    thisOutputValue == null ? otherOutput.getValue() : thisOutputValue.merge(otherOutput.getValue()));
        }

        return this;
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

        @JsonCreator
        static CommandInputConfiguration create(@JsonProperty("default-value") final String defaultValue,
                                                @JsonProperty("matcher") final String matcher,
                                                @JsonProperty("user-settable") final Boolean userSettable) {
            return new AutoValue_CommandConfiguration_CommandInputConfiguration(defaultValue, matcher, userSettable);
        }

        CommandInputConfiguration merge(final CommandInputConfiguration that) {
            if (that == null) {
                return this;
            }
            return create(that.defaultValue() == null ? this.defaultValue() : that.defaultValue(),
                    that.matcher() == null ? this.matcher() : that.matcher(),
                    that.userSettable() == null ? this.userSettable() : that.userSettable());
        }
    }

    @AutoValue
    public static abstract class CommandOutputConfiguration {
        @Nullable @JsonProperty("label") public abstract String label();

        @JsonCreator
        public static CommandOutputConfiguration create(@JsonProperty("label") final String label) {
            return new AutoValue_CommandConfiguration_CommandOutputConfiguration(label);
        }

        CommandOutputConfiguration merge(final CommandOutputConfiguration that) {
            if (that == null) {
                return this;
            }
            return create(that.label() == null ? this.label() : that.label());
        }
    }
}
