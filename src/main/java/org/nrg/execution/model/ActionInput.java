package org.nrg.execution.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

import javax.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class ActionInput implements Serializable {

    @JsonProperty("name") private String name;
    @JsonProperty("command-variable-name") private String commandVariableName;
    private String type;
    private Boolean required = false;
    private String value;
    @JsonProperty("root-property") private String rootProperty;

    public ActionInput() {}

    public ActionInput(final CommandVariable commandVariable) {
        this.name = commandVariable.getName();
        this.commandVariableName = commandVariable.getName();
        //this.rootProperty = commandVariable.getName();

        setRequired(commandVariable.getRequired());
        this.type = commandVariable.getType();
        this.value = commandVariable.getDefaultValue();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCommandVariableName() {
        return commandVariableName;
    }

    public void setCommandVariableName(final String commandVariableName) {
        if (this.name == null) {
            this.name = commandVariableName;
        }
        this.commandVariableName = commandVariableName;
    }

    public Boolean getRequired() {
        return required;
    }

    public Boolean isRequired() {
        return required;
    }

    public void setRequired(final Boolean required) {
        this.required = required == null ? false : required;
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
    }

    public String getRootProperty() {
        return rootProperty;
    }

    public void setRootProperty(final String itemProperty) {
        this.rootProperty = itemProperty;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final ActionInput that = (ActionInput) o;
        return Objects.equals(this.name, that.name) &&
                Objects.equals(this.commandVariableName, that.commandVariableName) &&
                Objects.equals(this.type, that.type) &&
                Objects.equals(this.required, that.required) &&
                Objects.equals(this.value, that.value) &&
                Objects.equals(this.rootProperty, that.rootProperty);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, commandVariableName, type, required, value, rootProperty);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("name", name)
                .add("commandVariableName", commandVariableName)
                .add("type", type)
                .add("required", required)
                .add("value", value)
                .add("rootProperty", rootProperty)
                .toString();
    }
}
