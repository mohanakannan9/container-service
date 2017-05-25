package org.nrg.containers.model.command.auto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.nrg.containers.model.command.auto.Command.Input;
import org.nrg.containers.model.command.auto.ResolvedCommand.PartiallyResolvedCommand;
import org.nrg.containers.model.command.auto.ResolvedInputTreeNode.ResolvedInputTreeValueAndChildren;
import org.nrg.containers.model.command.entity.CommandInputEntity.Type;
import org.nrg.containers.model.configuration.CommandConfiguration;
import org.nrg.containers.model.configuration.CommandConfiguration.CommandInputConfiguration;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
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
        // We have to go through the resolved input trees and get their values into the flat structure needed by the UI.
        final Map<String, LaunchUiInput.Builder> inputBuilderMap = Maps.newHashMap();
        for (final ResolvedInputTreeNode<? extends Input> rootNode : partiallyResolvedCommand.resolvedInputTrees()) {
            addNodesToInputMap(rootNode, null, null, commandConfiguration.inputs(), inputBuilderMap);
        }

        return builder()
                .commandId(partiallyResolvedCommand.commandId())
                .commandName(partiallyResolvedCommand.commandName())
                .commandDescription(partiallyResolvedCommand.commandDescription())
                .wrapperId(partiallyResolvedCommand.wrapperId())
                .wrapperName(partiallyResolvedCommand.wrapperName())
                .wrapperDescription(partiallyResolvedCommand.wrapperDescription())
                .imageName(partiallyResolvedCommand.image())
                .imageType(partiallyResolvedCommand.type())
                .addInputsFromBuilders(inputBuilderMap)
                .build();
    }

    private static void addNodesToInputMap(final @Nonnull ResolvedInputTreeNode<? extends Input> node,
                                           final @Nullable String parentName,
                                           final @Nullable String parentValue,
                                           final @Nonnull Map<String, CommandInputConfiguration> inputConfigurationMap,
                                           final @Nonnull Map<String, LaunchUiInput.Builder> inputMap) {
        final Input commandInput = node.input();
        final String inputName = commandInput.name();
        final CommandInputConfiguration inputConfiguration =
                inputConfigurationMap.containsKey(inputName) && inputConfigurationMap.get(inputName) != null ?
                        inputConfigurationMap.get(inputName) :
                        CommandInputConfiguration.builder().build();

        final List<LaunchUiInputValue> valueList = Lists.newArrayList();
        for (final ResolvedInputTreeValueAndChildren valueAndChildren : node.valuesAndChildren()) {
            final ResolvedInputValue resolvedValue = valueAndChildren.resolvedValue();
            final String value = resolvedValue.value();

            // Add all children to the map, using this node's value as their parent value
            for (final ResolvedInputTreeNode<? extends Input> child : valueAndChildren.children()) {
                addNodesToInputMap(child, commandInput.name(), value, inputConfigurationMap, inputMap);
            }

            // Now add this node's value to the uiInput's values
            if (value == null) {
                valueList.add(LaunchUiInputValue.createNull());
            } else {
                valueList.add(LaunchUiInputValue.create(value, resolvedValue.valueLabel()));
            }
        }
        // TODO figure this out from... I don't know from what
        final UiType uiType;
        final String inputType = commandInput.type();
        final boolean inputIsRequired = commandInput.required() || (inputConfiguration.required() != null && inputConfiguration.required());
        final boolean multipleValues = valueList.size() > 1;
        final boolean noValues = !multipleValues && (valueList.size() == 0 || valueList.get(0).equals(LaunchUiInputValue.createNull()));
        final boolean userSettable = inputConfiguration.userSettable() == null || inputConfiguration.userSettable(); // default: true
        final boolean advanced = inputConfiguration.advanced() != null && inputConfiguration.advanced(); // default: false

        if (!userSettable) {
            uiType = UiType.STATIC;
        } else {
            // The user should be able to set this input
            if (noValues) {
                uiType = UiType.TEXT;
            } else if (multipleValues) {
                uiType = UiType.SELECT;
            } else {
                // We have a single value
                // If it is a boolean, number, or string, apply those types.
                if (inputType.equals(Type.BOOLEAN.getName())) {
                    uiType = UiType.BOOLEAN;
                } else if (inputType.equals(Type.NUMBER.getName())) {
                    uiType = UiType.NUMBER;
                } else if (inputType.equals(Type.STRING.getName())) {
                    uiType = UiType.TEXT;
                } else {
                    // If the input is not one of the simple types above,
                    // it is some XNAT type. But if we only have one value, that
                    // implies either it was given to us (therefore don't change it)
                    // or it was derived (therefore don't change it). 
                    uiType = UiType.STATIC;
                }
            }
        }

        // Get the input builder if it exists, or make a new one if it doesn't
        final LaunchUiInput.Builder uiInputBuilder;
        if (inputMap.containsKey(commandInput.name())) {
            uiInputBuilder = inputMap.get(commandInput.name());
        } else {
            uiInputBuilder = LaunchUiInput.builder()
                    .label(commandInput.name()) // TODO add label to commandInput (pojo, hibernate, tests, and examples)
                    .description(commandInput.description())
                    .required(inputIsRequired)
                    .advanced(advanced)
                    .parent(parentName);
            inputMap.put(commandInput.name(), uiInputBuilder);
        }
        uiInputBuilder.addUi(parentValue == null ? "default" : parentValue,
                LaunchUiInputValuesAndType.create(valueList, uiType));
    }

    public static Builder builder() {
        return new AutoValue_LaunchUi.Builder();
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
        public Builder addInputsFromBuilders(final @Nonnull Map<String, LaunchUiInput.Builder> inputBuilders) {
            for (final Map.Entry<String, LaunchUiInput.Builder> inputBuilderEntry : inputBuilders.entrySet()) {
                inputsBuilder().put(inputBuilderEntry.getKey(), inputBuilderEntry.getValue().build());
            }
            return this;
        }
        public abstract LaunchUi build();
    }

    @AutoValue
    public static abstract class LaunchUiInput {
        @JsonProperty("label") public abstract String label();
        @Nullable @JsonProperty("description") public abstract String description();
        @JsonProperty("advanced") public abstract Boolean advanced();
        @JsonProperty("required") public abstract Boolean required();
        @JsonProperty("parent") @Nullable public abstract String parent();
        @JsonProperty("ui") public abstract ImmutableMap<String, LaunchUiInputValuesAndType> ui();

        public static Builder builder() {
            return new AutoValue_LaunchUi_LaunchUiInput.Builder();
        }

        @AutoValue.Builder
        public static abstract class Builder {
            public abstract Builder label(String label);
            public abstract Builder description(String description);
            public abstract Builder advanced(Boolean advanced);
            public abstract Builder required(Boolean required);
            public abstract Builder parent(String parent);
            public abstract Builder ui(Map<String, LaunchUiInputValuesAndType> ui);
            abstract ImmutableMap.Builder<String, LaunchUiInputValuesAndType> uiBuilder();
            public Builder addUi(final @Nonnull String parentValue,
                                 final @Nonnull LaunchUiInputValuesAndType valuesAndType) {
                uiBuilder().put(parentValue, valuesAndType);
                return this;
            }

            public abstract LaunchUiInput build();
        }
    }

    @AutoValue
    public static abstract class LaunchUiInputValuesAndType {
        @JsonProperty("values") public abstract ImmutableList<LaunchUiInputValue> values();
        @JsonProperty("type") public abstract UiType type();

        public static LaunchUiInputValuesAndType create(final List<LaunchUiInputValue> values,
                                                        final UiType type) {
            final ImmutableList<LaunchUiInputValue> valuesCopy =
                    values == null ?
                            ImmutableList.<LaunchUiInputValue>of() :
                            ImmutableList.copyOf(values);
            return new AutoValue_LaunchUi_LaunchUiInputValuesAndType(valuesCopy, type);
        }
    }

    @AutoValue
    public static abstract class LaunchUiInputValue {
        private static String NULL_VALUE = "";
        private static String NULL_LABEL = "null";

        @JsonProperty("value") public abstract String value();
        @JsonProperty("label") public abstract String label();

        @JsonIgnore
        public boolean isNull() {
            return value().equals(NULL_VALUE) && label().equals(NULL_LABEL);
        }

        public static LaunchUiInputValue create(final @Nonnull String value,
                                                final String label) {
            return new AutoValue_LaunchUi_LaunchUiInputValue(
                    value, label == null ? value : label);
        }

        public static LaunchUiInputValue createNull() {
            return new AutoValue_LaunchUi_LaunchUiInputValue(NULL_VALUE, NULL_LABEL);
        }
    }

    public enum UiType {
        DEFAULT("default"),
        TEXT("text"),
        NUMBER("number"),
        BOOLEAN("boolean"),
        SELECT("select"),
        HIDDEN("hidden"),
        STATIC("static");

        public final String name;

        @JsonCreator
        UiType(final String name) {
            this.name = name;
        }

        @JsonValue
        public String getName() {
            return name;
        }
    }
}
