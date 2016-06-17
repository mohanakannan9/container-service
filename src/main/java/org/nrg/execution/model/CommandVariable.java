package org.nrg.execution.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
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
    private String value;
    @JsonProperty("arg-template") private String argTemplate;
    @JsonProperty("true-value") private String trueValue;
    @JsonProperty("false-value") private String falseValue;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
    }

    public String getArgTemplate() {
        return argTemplate;
    }

    public void setArgTemplate(final String argTemplate) {
        this.argTemplate = argTemplate;
    }

    public String getTrueValue() {
        return trueValue;
    }

    public void setTrueValue(final String trueValue) {
        this.trueValue = trueValue;
    }

    public String getFalseValue() {
        return falseValue;
    }

    public void setFalseValue(final String falseValue) {
        this.falseValue = falseValue;
    }

    @Transient
    public String getValueWithTrueOrFalseValue() {
        if (value == null) {
            return null;
        }

        if (type != null && type.equalsIgnoreCase("boolean")) {
            return Boolean.valueOf(value) ? trueValue : falseValue;
        } else {
            return value;
        }
    }

    @Transient
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
                Objects.equals(this.value, that.value) &&
                Objects.equals(this.argTemplate, that.argTemplate) &&
                Objects.equals(this.trueValue, that.trueValue) &&
                Objects.equals(this.falseValue, that.falseValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, type, required, value, argTemplate, trueValue, falseValue);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("name", name)
                .add("description", description)
                .add("type", type)
                .add("required", required)
                .add("value", value)
                .add("argTemplate", argTemplate)
                .add("trueValue", trueValue)
                .add("falseValue", falseValue)
                .toString();
    }
}