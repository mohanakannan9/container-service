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
public class CommandVariable implements Serializable {
    private String name;
    private String description;
    private String type;
    private Boolean required;
    @JsonProperty("default-value") private String defaultValue;
    @JsonProperty("arg-template") private String argTemplate;
    @JsonProperty("true-value") private String trueValue;
    @JsonProperty("false-value") private String falseValue;

    @ApiModelProperty(value = "Name of the command variable", required = true)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ApiModelProperty("Description of the command variable")
    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    @ApiModelProperty(value = "Type of the command variable", allowableValues = "string, boolean, number")
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

    public Boolean isRequired() {
        return required;
    }

    public void setRequired(final Boolean required) {
        this.required = required;
    }

    @ApiModelProperty("Default value of the variable")
    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(final String value) {
        this.defaultValue = value;
    }

    @ApiModelProperty(value = "When the variable is used in the run-template, its raw value can be modified to the appropriate form it should take on the command line. " +
            "You can reference the raw variable value as #value#.", example = "--command-line-flag=#value#")
    public String getArgTemplate() {
        return argTemplate;
    }

    public void setArgTemplate(final String argTemplate) {
        this.argTemplate = argTemplate;
    }

    @ApiModelProperty(value = "If the variable is a boolean, this string will be used when the value is \"true\".", example = "1")
    public String getTrueValue() {
        return trueValue;
    }

    public void setTrueValue(final String trueValue) {
        this.trueValue = trueValue;
    }

    @ApiModelProperty(value = "If the variable is a boolean, this string will be used when the value is \"false\".", example = "0")
    public String getFalseValue() {
        return falseValue;
    }

    public void setFalseValue(final String falseValue) {
        this.falseValue = falseValue;
    }

    @Transient
    @JsonIgnore
    @ApiModelProperty(hidden = true)
    public String getValueWithTrueOrFalseValue() {
        if (defaultValue == null) {
            return null;
        }

        if (type != null && type.equalsIgnoreCase("boolean")) {
            return Boolean.valueOf(defaultValue) ? trueValue : falseValue;
        } else {
            return defaultValue;
        }
    }

    @Transient
    @JsonIgnore
    @ApiModelProperty(hidden = true)
    public String getArgTemplateValue() {
        final String valString = getValueWithTrueOrFalseValue();
        if (valString == null) {
            return null;
        }

        if (StringUtils.isBlank(argTemplate)) {
            return valString;
        } else {
            return argTemplate.replaceAll("#value#", valString);
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final CommandVariable that = (CommandVariable) o;
        return Objects.equals(this.name, that.name) &&
                Objects.equals(this.description, that.description) &&
                Objects.equals(this.type, that.type) &&
                Objects.equals(this.required, that.required) &&
                Objects.equals(this.defaultValue, that.defaultValue) &&
                Objects.equals(this.argTemplate, that.argTemplate) &&
                Objects.equals(this.trueValue, that.trueValue) &&
                Objects.equals(this.falseValue, that.falseValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, type, required, defaultValue, argTemplate, trueValue, falseValue);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("name", name)
                .add("description", description)
                .add("type", type)
                .add("required", required)
                .add("defaultValue", defaultValue)
                .add("argTemplate", argTemplate)
                .add("trueValue", trueValue)
                .add("falseValue", falseValue)
                .toString();
    }
}