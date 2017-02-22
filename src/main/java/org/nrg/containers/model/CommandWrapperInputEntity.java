package org.nrg.containers.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang3.StringUtils;
import org.nrg.containers.model.auto.CommandPojo;

import javax.annotation.Nullable;
import javax.persistence.Embeddable;
import javax.persistence.Transient;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Embeddable
public class CommandWrapperInputEntity {
    public static final Type DEFAULT_TYPE = Type.STRING;
    private String name;
    private String description;
    private Type type;
    @JsonProperty("derived-from-xnat-input") private String derivedFromXnatInput;
    @JsonProperty("derived-from-xnat-object-property") private String derivedFromXnatObjectProperty;
    private String matcher;
    @JsonProperty("provides-value-for-command-input") private String providesValueForCommandInput;
    @JsonProperty("provides-files-for-command-mount") private String providesFilesForCommandMount;
    @JsonProperty("default-value") private String defaultValue;
    @JsonProperty("user-settable") private Boolean userSettable = true;
    @JsonProperty("replacement-key") private String rawReplacementKey;
    private boolean required = false;
    private String value;
    private String jsonRepresentation;

    public static CommandWrapperInputEntity passthrough(final CommandInput commandInput) {
        final CommandWrapperInputEntity identityInput = new CommandWrapperInputEntity();
        identityInput.setName(commandInput.getName());
        identityInput.setType(CommandWrapperInputEntity.Type.valueOf(commandInput.getType().getName().toUpperCase()));
        identityInput.setMatcher(commandInput.getMatcher());
        identityInput.setProvidesValueForCommandInput(commandInput.getName());
        identityInput.setDefaultValue(commandInput.getDefaultValue());
        identityInput.setUserSettable(true);
        identityInput.setRequired(commandInput.isRequired());

        return identityInput;
    }

    public static CommandWrapperInputEntity fromPojo(final CommandPojo.CommandWrapperInputPojo commandWrapperInputPojo) {
        final CommandWrapperInputEntity commandWrapperInputEntity = new CommandWrapperInputEntity();
        commandWrapperInputEntity.name = commandWrapperInputPojo.name();
        commandWrapperInputEntity.description = commandWrapperInputPojo.description();
        commandWrapperInputEntity.derivedFromXnatInput = commandWrapperInputPojo.derivedFromXnatInput();
        commandWrapperInputEntity.derivedFromXnatObjectProperty = commandWrapperInputPojo.derivedFromXnatObjectProperty();
        commandWrapperInputEntity.matcher = commandWrapperInputPojo.matcher();
        commandWrapperInputEntity.providesValueForCommandInput = commandWrapperInputPojo.providesValueForCommandInput();
        commandWrapperInputEntity.providesFilesForCommandMount = commandWrapperInputPojo.providesFilesForCommandMount();
        commandWrapperInputEntity.defaultValue = commandWrapperInputPojo.defaultValue();
        commandWrapperInputEntity.userSettable = commandWrapperInputPojo.userSettable();
        commandWrapperInputEntity.rawReplacementKey = commandWrapperInputPojo.rawReplacementKey();
        commandWrapperInputEntity.required = commandWrapperInputPojo.required();
        switch (commandWrapperInputPojo.type()) {
            case "string":
                commandWrapperInputEntity.type = Type.STRING;
                break;
            case "boolean":
                commandWrapperInputEntity.type = Type.BOOLEAN;
                break;
            case "number":
                commandWrapperInputEntity.type = Type.NUMBER;
                break;
            case "Directory":
                commandWrapperInputEntity.type = Type.DIRECTORY;
                break;
            case "File[]":
                commandWrapperInputEntity.type = Type.FILES;
                break;
            case "File":
                commandWrapperInputEntity.type = Type.FILE;
                break;
            case "Project":
                commandWrapperInputEntity.type = Type.PROJECT;
                break;
            case "Subject":
                commandWrapperInputEntity.type = Type.SUBJECT;
                break;
            case "Session":
                commandWrapperInputEntity.type = Type.SESSION;
                break;
            case "Scan":
                commandWrapperInputEntity.type = Type.SCAN;
                break;
            case "Assessor":
                commandWrapperInputEntity.type = Type.ASSESSOR;
                break;
            case "Resource":
                commandWrapperInputEntity.type = Type.RESOURCE;
                break;
            case "Config":
                commandWrapperInputEntity.type = Type.CONFIG;
                break;
            default:
                commandWrapperInputEntity.type = DEFAULT_TYPE;
        }
        return commandWrapperInputEntity;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public Type getType() {
        return type;
    }

    public void setType(final Type type) {
        this.type = type;
    }

    public String getDerivedFromXnatInput() {
        return derivedFromXnatInput;
    }

    public void setDerivedFromXnatInput(final String derivedFromXnatInput) {
        this.derivedFromXnatInput = derivedFromXnatInput;
    }

    public String getDerivedFromXnatObjectProperty() {
        return derivedFromXnatObjectProperty;
    }

    public void setDerivedFromXnatObjectProperty(final String derivedFromXnatObjectProperty) {
        this.derivedFromXnatObjectProperty = derivedFromXnatObjectProperty;
    }

    public String getMatcher() {
        return matcher;
    }

    public void setMatcher(final String matcher) {
        this.matcher = matcher;
    }

    public String getProvidesValueForCommandInput() {
        return providesValueForCommandInput;
    }

    public void setProvidesValueForCommandInput(final String providesValueForCommandInput) {
        this.providesValueForCommandInput = providesValueForCommandInput;
    }

    public String getProvidesFilesForCommandMount() {
        return providesFilesForCommandMount;
    }

    public void setProvidesFilesForCommandMount(final String providesFilesForCommandMount) {
        this.providesFilesForCommandMount = providesFilesForCommandMount;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(final String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public Boolean getUserSettable() {
        return userSettable;
    }

    public void setUserSettable(final Boolean userSettable) {
        this.userSettable = userSettable;
    }

    @ApiModelProperty(value = "String in the command-line or elsewhere that will be replaced by this input's value. Default: #input_name#", example = "[MY_INPUT]")
    public String getRawReplacementKey() {
        return rawReplacementKey;
    }

    public void setRawReplacementKey(final String rawReplacementKey) {
        this.rawReplacementKey = rawReplacementKey;
    }

    @Transient
    @JsonIgnore
    public String getReplacementKey() {
        return StringUtils.isNotBlank(rawReplacementKey) ? rawReplacementKey : "#" + getName() + "#";
    }

    public Boolean getRequired() {
        return required;
    }

    public Boolean isRequired() {
        return required;
    }

    public void setRequired(final Boolean required) {
        this.required = required;
    }

    @Transient
    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
    }

    @Transient
    public String getJsonRepresentation() {
        return jsonRepresentation;
    }

    public void setJsonRepresentation(final String jsonRepresentation) {
        this.jsonRepresentation = jsonRepresentation;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final CommandWrapperInputEntity that = (CommandWrapperInputEntity) o;
        return Objects.equals(this.name, that.name) &&
                Objects.equals(this.description, that.description) &&
                type == that.type &&
                Objects.equals(this.derivedFromXnatInput, that.derivedFromXnatInput) &&
                Objects.equals(this.derivedFromXnatObjectProperty, that.derivedFromXnatObjectProperty) &&
                Objects.equals(this.matcher, that.matcher) &&
                Objects.equals(this.providesValueForCommandInput, that.providesValueForCommandInput) &&
                Objects.equals(this.providesFilesForCommandMount, that.providesFilesForCommandMount) &&
                Objects.equals(this.defaultValue, that.defaultValue) &&
                Objects.equals(this.userSettable, that.userSettable) &&
                Objects.equals(this.rawReplacementKey, that.rawReplacementKey) &&
                Objects.equals(this.required, that.required);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, type, derivedFromXnatInput, derivedFromXnatObjectProperty, matcher,
                providesValueForCommandInput, providesFilesForCommandMount, defaultValue, userSettable, rawReplacementKey, required);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("name", name)
                .add("description", description)
                .add("type", type)
                .add("derivedFromXnatInput", derivedFromXnatInput)
                .add("derivedFromXnatObjectProperty", derivedFromXnatObjectProperty)
                .add("matcher", matcher)
                .add("providesValueForCommandInput", providesValueForCommandInput)
                .add("providesFilesForCommandMount", providesFilesForCommandMount)
                .add("defaultValue", defaultValue)
                .add("userSettable", userSettable)
                .add("rawReplacementKey", rawReplacementKey)
                .add("required", required)
                .add("value", value)
                .add("jsonRepresentation", jsonRepresentation)
                .toString();
    }

    public enum Type {
        STRING("string"),
        BOOLEAN("boolean"),
        NUMBER("number"),
        DIRECTORY("Directory"),
        FILES("File[]"),
        FILE("File"),
        PROJECT("Project"),
        SUBJECT("Subject"),
        SESSION("Session"),
        SCAN("Scan"),
        ASSESSOR("Assessor"),
        RESOURCE("Resource"),
        CONFIG("Config");

        private final String name;

        @JsonCreator
        Type(final String name) {
            this.name = name;
        }

        @JsonValue
        public String getName() {
            return name;
        }

        public static List<String> names() {
            return Lists.transform(Arrays.asList(Type.values()), new Function<Type, String>() {
                @Nullable
                @Override
                public String apply(@Nullable final Type type) {
                    return type != null ? type.getName() : "";
                }
            });
        }
    }
}
