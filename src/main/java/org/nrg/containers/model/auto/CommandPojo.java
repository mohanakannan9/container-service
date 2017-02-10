package org.nrg.containers.model.auto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

@AutoValue
public abstract class CommandPojo {
    @Nullable @JsonProperty("name") public abstract String name();
    @Nullable @JsonProperty("label") public abstract String label();
    @Nullable @JsonProperty("description") public abstract String description();
    @Nullable @JsonProperty("version") public abstract String version();
    @Nullable @JsonProperty("schema-version") public abstract String schemaVersion();
    @Nullable @JsonProperty("info-url") public abstract String infoUrl();
    @Nullable @JsonProperty("image") public abstract String image();
    @Nullable @JsonProperty("type") public abstract String type();
    @Nullable @JsonProperty("index") public abstract String index();
    @Nullable @JsonProperty("hash") public abstract String hash();
    @Nullable @JsonProperty("working-directory") public abstract String workingDirectory();
    @Nullable @JsonProperty("command-line") public abstract String commandLine();
    @Nullable @JsonProperty("mounts") public abstract List<CommandMountPojo> mounts();
    @Nullable @JsonProperty("environment-variables") public abstract Map<String, String> environmentVariables();
    @Nullable @JsonProperty("ports") public abstract Map<String, String> ports();
    @Nullable @JsonProperty("inputs") public abstract List<CommandInputPojo> inputs();
    @Nullable @JsonProperty("outputs") public abstract List<CommandOutputPojo> outputs();
    @Nullable @JsonProperty("xnat") public abstract List<CommandWrapperPojo> xnatCommandWrappers();

    @JsonCreator
    static CommandPojo create(@JsonProperty("name") String name,
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
        return new AutoValue_CommandPojo(name, label, description, version, schemaVersion, infoUrl, image, type, index, hash, workingDirectory, commandLine, mounts, environmentVariables, ports, inputs, outputs, xnatCommandWrappers);
    }

    @AutoValue
    static abstract class CommandMountPojo {
        @Nullable @JsonProperty("name") public abstract String name();
        @Nullable @JsonProperty("writable") public abstract Boolean writable();
        @Nullable @JsonProperty("path") public abstract String path();

        @JsonCreator
        static CommandMountPojo create(@JsonProperty("name") String name,
                                       @JsonProperty("writable") Boolean writable,
                                       @JsonProperty("path") String path) {
            return new AutoValue_CommandPojo_CommandMountPojo(name, writable, path);
        }
    }
    @AutoValue
    static abstract class CommandInputPojo {
        @Nullable @JsonProperty("name") public abstract String name();
        @Nullable @JsonProperty("description") public abstract String description();
        @Nullable @JsonProperty("type") public abstract String type();
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
            return new AutoValue_CommandPojo_CommandInputPojo(name, description, type, required, matcher, defaultValue, rawReplacementKey, commandLineFlag, commandLineSeparator, trueValue, falseValue, value);
        }
    }
    @AutoValue
    static abstract class CommandOutputPojo {
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
    }

    @AutoValue
    static abstract class CommandWrapperPojo {
        @Nullable @JsonProperty("name") public abstract String name();
        @Nullable @JsonProperty("description") public abstract String description();
        @Nullable @JsonProperty("external-inputs") public abstract List<CommandWrapperInputPojo> externalInputs();
        @Nullable @JsonProperty("derived-inputs") public abstract List<CommandWrapperInputPojo> derivedInputs();
        @Nullable @JsonProperty("output-handlers") public abstract List<CommandWrapperOutputPojo> outputHandlers();

        @JsonCreator
        static CommandWrapperPojo create(@JsonProperty("name") String name,
                                         @JsonProperty("description") String description,
                                         @JsonProperty("external-inputs") List<CommandWrapperInputPojo> externalInputs,
                                         @JsonProperty("derived-inputs") List<CommandWrapperInputPojo> derivedInputs,
                                         @JsonProperty("output-handlers") List<CommandWrapperOutputPojo> outputHandlers) {
            return new AutoValue_CommandPojo_CommandWrapperPojo(name, description, externalInputs, derivedInputs, outputHandlers);
        }
    }

    @AutoValue
    static abstract class CommandWrapperInputPojo {
        @Nullable @JsonProperty("name") public abstract String name();
        @Nullable @JsonProperty("description") public abstract String description();
        @Nullable @JsonProperty("type") public abstract String type();
        @Nullable @JsonProperty("derived-from-xnat-input") public abstract String derivedFromXnatInput();
        @Nullable @JsonProperty("derived-from-xnat-object-property") public abstract String derivedFromXnatObjectProperty();
        @Nullable @JsonProperty("matcher") public abstract String matcher();
        @Nullable @JsonProperty("provides-value-for-command-input") public abstract String providesValueForCommandInput();
        @Nullable @JsonProperty("provides-files-for-command-mount") public abstract String providesFilesForCommandMount();
        @Nullable @JsonProperty("default-value") public abstract String defaultValue();
        @Nullable @JsonProperty("user-settable") public abstract Boolean userSettable();
        @Nullable @JsonProperty("replacement-key") public abstract String rawReplacementKey();
        @Nullable @JsonProperty("required") public abstract Boolean required();
        @Nullable @JsonProperty("value") public abstract String value();
        @Nullable @JsonProperty("jsonRepresentation") public abstract String jsonRepresentation();

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
                                              @JsonProperty("required") boolean required,
                                              @JsonProperty("value") String value,
                                              @JsonProperty("jsonRepresentation") String jsonRepresentation) {
            return new AutoValue_CommandPojo_CommandWrapperInputPojo(name, description, type, derivedFromXnatInput, derivedFromXnatObjectProperty, matcher, providesValueForCommandInput, providesFilesForCommandMount, defaultValue, userSettable, rawReplacementKey, required, value, jsonRepresentation);
        }
    }

    @AutoValue
    static abstract class CommandWrapperOutputPojo {
        @Nullable @JsonProperty("accepts-command-output") public abstract String commandOutputName();
        @Nullable @JsonProperty("as-a-child-of-xnat-input") public abstract String xnatInputName();
        @Nullable @JsonProperty("type") public abstract String type();
        @Nullable @JsonProperty("label") public abstract String label();

        @JsonCreator
        static CommandWrapperOutputPojo create(@JsonProperty("accepts-command-output") String commandOutputName,
                                               @JsonProperty("as-a-child-of-xnat-input") String xnatInputName,
                                               @JsonProperty("type") String type,
                                               @JsonProperty("label") String label) {
            return new AutoValue_CommandPojo_CommandWrapperOutputPojo(commandOutputName, xnatInputName, type, label);
        }
    }

}
