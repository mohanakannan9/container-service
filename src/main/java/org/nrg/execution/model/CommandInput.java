package org.nrg.execution.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang3.StringUtils;

import javax.persistence.Embeddable;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class CommandInput implements Serializable {
    private String name;
    private String description;
    private String type;
    private Boolean required;
    @JsonProperty("root-property") private String rootProperty;
    @JsonProperty("default-value") private String defaultValue;
    @JsonProperty("replacement-key") private String rawReplacementKey;
    @JsonProperty("command-line-flag") private String commandLineFlag = "";
    @JsonProperty("command-line-separator") private String commandLineSeparator = " ";
    @JsonProperty("true-value") private String trueValue;
    @JsonProperty("false-value") private String falseValue;
    @JsonIgnore private String value;

    @ApiModelProperty(value = "Name of the command input", required = true)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ApiModelProperty("Description of the command input")
    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    @ApiModelProperty(value = "Type of the command input", allowableValues = "string, boolean, number")
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @ApiModelProperty("Whether the argument is required. If true, and no value is given as a default or at command launch time, an error is thrown.")
    public Boolean getRequired() {
        return required;
    }

    @Transient
    public boolean isRequired() {
        return required != null && required;
    }

    public void setRequired(final Boolean required) {
        this.required = required;
    }

    public String getRootProperty() {
        return rootProperty;
    }

    public void setRootProperty(final String rootProperty) {
        this.rootProperty = rootProperty;
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

    @Transient
    @JsonIgnore
    public String getReplacementKey() {
        return StringUtils.isNotBlank(rawReplacementKey) ? rawReplacementKey : "#" + getName() + "#";
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

    @Transient
    @JsonIgnore
    public String getValueOrDefaultValue() {
        if (value != null) {
            return value;
        } else if (defaultValue != null) {
            return defaultValue;
        } else {
            return null;
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final CommandInput that = (CommandInput) o;
        return Objects.equals(this.name, that.name) &&
                Objects.equals(this.description, that.description) &&
                Objects.equals(this.type, that.type) &&
                Objects.equals(this.required, that.required) &&
                Objects.equals(this.rootProperty, that.rootProperty) &&
                Objects.equals(this.defaultValue, that.defaultValue) &&
                Objects.equals(this.rawReplacementKey, that.rawReplacementKey) &&
                Objects.equals(this.commandLineFlag, that.commandLineFlag) &&
                Objects.equals(this.commandLineSeparator, that.commandLineSeparator) &&
                Objects.equals(this.trueValue, that.trueValue) &&
                Objects.equals(this.falseValue, that.falseValue) &&
                Objects.equals(this.value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, type, required, rootProperty, defaultValue,
                rawReplacementKey, commandLineFlag, commandLineSeparator, trueValue, falseValue, value);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("name", name)
                .add("description", description)
                .add("type", type)
                .add("required", required)
                .add("rootProperty", rootProperty)
                .add("defaultValue", defaultValue)
                .add("rawReplacementKey", rawReplacementKey)
                .add("commandLineFlag", commandLineFlag)
                .add("commandLineSeparator", commandLineSeparator)
                .add("trueValue", trueValue)
                .add("falseValue", falseValue)
                .add("value", value)
                .toString();
    }
}