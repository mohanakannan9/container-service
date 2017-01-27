package org.nrg.containers.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Sets;
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang3.StringUtils;

import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Transient;
import java.util.Objects;
import java.util.Set;

@Embeddable
public class XnatCommandInput {
    private String name;
    private Type type;
    @JsonProperty("derived-from-xnat-input") private String derivedFromXnatInput;
    @JsonProperty("derived-from-xnat-object-property") private String derivedFromXnatObjectProperty;
    private String matcher;
    @JsonProperty("provides-value-for-command-inputs") private Set<String> providesValueForCommandInputs;
    @JsonProperty("default-value") private String defaultValue;
    @JsonProperty("user-settable") private Boolean userSettable = true;
    @JsonProperty("replacement-key") private String rawReplacementKey;
    private Boolean required;
    private String value;
    private String jsonRepresentation;

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
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

    @ElementCollection
    public Set<String> getProvidesValueForCommandInputs() {
        return providesValueForCommandInputs;
    }

    public void setProvidesValueForCommandInputs(final Set<String> providesValueForCommandInput) {
        this.providesValueForCommandInputs = providesValueForCommandInput == null ?
                Sets.<String>newHashSet() :
                providesValueForCommandInput;
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
        final XnatCommandInput that = (XnatCommandInput) o;
        return Objects.equals(this.name, that.name) &&
                type == that.type &&
                Objects.equals(this.derivedFromXnatInput, that.derivedFromXnatInput) &&
                Objects.equals(this.derivedFromXnatObjectProperty, that.derivedFromXnatObjectProperty) &&
                Objects.equals(this.matcher, that.matcher) &&
                Objects.equals(this.providesValueForCommandInputs, that.providesValueForCommandInputs) &&
                Objects.equals(this.defaultValue, that.defaultValue) &&
                Objects.equals(this.userSettable, that.userSettable) &&
                Objects.equals(this.rawReplacementKey, that.rawReplacementKey) &&
                Objects.equals(this.required, that.required);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type, derivedFromXnatInput, derivedFromXnatObjectProperty, matcher,
                providesValueForCommandInputs, defaultValue, userSettable, rawReplacementKey, required);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("name", name)
                .add("type", type)
                .add("derivedFromXnatInput", derivedFromXnatInput)
                .add("derivedFromXnatObjectProperty", derivedFromXnatObjectProperty)
                .add("matcher", matcher)
                .add("providesValueForCommandInputs", providesValueForCommandInputs)
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
        FILE("file"),
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
    }
}
