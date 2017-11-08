package org.nrg.containers.model.command.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.base.MoreObjects;
import io.swagger.annotations.ApiModelProperty;
import org.hibernate.envers.Audited;
import org.nrg.containers.model.command.auto.Command;

import javax.annotation.Nonnull;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Audited
public class CommandInputEntity implements Serializable {
    public static Type DEFAULT_TYPE = Type.STRING;

    private long id;
    @JsonIgnore private CommandEntity commandEntity;
    private String name;
    private String description;
    private Type type = DEFAULT_TYPE;
    private Boolean required;
    private String matcher;
    @JsonProperty("default-value") private String defaultValue;
    @JsonProperty("replacement-key") private String rawReplacementKey;
    @JsonProperty("command-line-flag") private String commandLineFlag = "";
    @JsonProperty("command-line-separator") private String commandLineSeparator = " ";
    @JsonProperty("true-value") private String trueValue;
    @JsonProperty("false-value") private String falseValue;
    private String value;

    public static CommandInputEntity fromPojo(final Command.CommandInput commandInput) {
        return new CommandInputEntity().update(commandInput);
    }

    @Nonnull
    public CommandInputEntity update(final Command.CommandInput commandInput) {
        if (this.id == 0L || commandInput.id() != 0L) {
            this.setId(commandInput.id());
        }
        this.setName(commandInput.name());
        this.setDescription(commandInput.description());
        this.setRequired(commandInput.required());
        this.setMatcher(commandInput.matcher());
        this.setDefaultValue(commandInput.defaultValue());
        this.setRawReplacementKey(commandInput.rawReplacementKey());
        this.setCommandLineFlag(commandInput.commandLineFlag());
        this.setCommandLineSeparator(commandInput.commandLineSeparator());
        this.setTrueValue(commandInput.trueValue());
        this.setFalseValue(commandInput.falseValue());

        switch (commandInput.type()) {
            case "string":
                this.setType(Type.STRING);
                break;
            case "boolean":
                this.setType(Type.BOOLEAN);
                break;
            case "number":
                this.setType(Type.NUMBER);
                break;
            default:
                this.setType(DEFAULT_TYPE);
        }

        return this;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public long getId() {
        return id;
    }

    public void setId(final long id) {
        this.id = id;
    }

    @ManyToOne
    public CommandEntity getCommandEntity() {
        return commandEntity;
    }

    public void setCommandEntity(final CommandEntity commandEntity) {
        this.commandEntity = commandEntity;
    }

    @ApiModelProperty(value = "Name of the command input", required = true)
    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    @ApiModelProperty("Description of the command input")
    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    @Enumerated(EnumType.STRING)
    @ApiModelProperty(value = "Type of the command input", allowableValues = "string, boolean, number, Project, Subject, Session, Scan, Assessor, Resource, Config")
    public Type getType() {
        return type;
    }

    public void setType(final Type type) {
        this.type = type;
    }

    @ApiModelProperty("Whether the argument is required. If true, and no value is given as a default or at command launch time, an error is thrown.")
    @JsonGetter
    public Boolean getRequired() {
        return required;
    }

    @Transient
    @JsonIgnore
    public boolean isRequired() {
        return required != null && required;
    }

    public void setRequired(final Boolean required) {
        this.required = required;
    }

    public String getMatcher() {
        return matcher;
    }

    public void setMatcher(final String matcher) {
        this.matcher = matcher;
    }

    @ApiModelProperty("Default value of the input")
    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(final String value) {
        this.defaultValue = value;
    }

    @ApiModelProperty(value = "String in the command-line or elsewhere that will be replaced by this input's value. Default: #input_name#", example = "[MY_INPUT]")
    public String getRawReplacementKey() {
        return rawReplacementKey;
    }

    public void setRawReplacementKey(final String rawReplacementKey) {
        this.rawReplacementKey = rawReplacementKey;
    }

    @ApiModelProperty(value = "Flag to use for this input when substituting the value on the command line", example = "--input-flag")
    public String getCommandLineFlag() {
        return commandLineFlag;
    }

    public void setCommandLineFlag(final String commandLineFlag) {
        this.commandLineFlag = commandLineFlag;
    }

    @ApiModelProperty(value = "Separator between command-line-flag and value. Default \" \" (space).", example = " ")
    public String getCommandLineSeparator() {
        return commandLineSeparator;
    }

    public void setCommandLineSeparator(final String commandLineSeparator) {
        this.commandLineSeparator = commandLineSeparator;
    }

    @ApiModelProperty(value = "If the input is a boolean, this string will be used when the value is \"true\".", example = "1")
    public String getTrueValue() {
        return trueValue;
    }

    public void setTrueValue(final String trueValue) {
        this.trueValue = trueValue;
    }

    @ApiModelProperty(value = "If the input is a boolean, this string will be used when the value is \"false\".", example = "0")
    public String getFalseValue() {
        return falseValue;
    }

    public void setFalseValue(final String falseValue) {
        this.falseValue = falseValue;
    }

    @Transient
    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final CommandInputEntity that = (CommandInputEntity) o;
        return Objects.equals(this.commandEntity, that.commandEntity) &&
                Objects.equals(this.name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(commandEntity, name);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("name", name)
                .add("description", description)
                .add("type", type)
                .add("required", required)
                .add("matcher", matcher)
                .add("defaultValue", defaultValue)
                .add("rawReplacementKey", rawReplacementKey)
                .add("commandLineFlag", commandLineFlag)
                .add("commandLineSeparator", commandLineSeparator)
                .add("trueValue", trueValue)
                .add("falseValue", falseValue)
                .add("value", value)
                .toString();
    }

    public enum Type {
        STRING("string"),
        BOOLEAN("boolean"),
        NUMBER("number");

        public final String name;

        @JsonCreator
        Type(final String name) {
            this.name = name;
        }

        @JsonValue
        public String getName() {
            return name;
        }
    }
}