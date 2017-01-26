package org.nrg.containers.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Sets;

import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import java.util.Objects;
import java.util.Set;

@Embeddable
public class XnatCommandInput {
    private String name;
    private Type type;
    @JsonProperty("derived-from-xnat-input") private String derivedFromXnatInput;
    private String matcher;
    @JsonProperty("provides-value-for-command-inputs") private Set<String> providesValueForCommandInputs;
    @JsonProperty("handles-command-outputs") private Set<XnatCommandOutput> commandOutputHandlers;
    @JsonProperty("default-value") private String defaultValue;
    @JsonProperty("user-settable") private Boolean userSettable = true;

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

    @ElementCollection
    public Set<XnatCommandOutput> getCommandOutputHandlers() {
        return commandOutputHandlers;
    }

    public void setCommandOutputHandlers(final Set<XnatCommandOutput> commandOutputHandlers) {
        this.commandOutputHandlers = commandOutputHandlers == null ?
                Sets.<XnatCommandOutput>newHashSet() :
                commandOutputHandlers;
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

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final XnatCommandInput that = (XnatCommandInput) o;
        return Objects.equals(this.name, that.name) &&
                type == that.type &&
                Objects.equals(this.derivedFromXnatInput, that.derivedFromXnatInput) &&
                Objects.equals(this.matcher, that.matcher) &&
                Objects.equals(this.providesValueForCommandInputs, that.providesValueForCommandInputs) &&
                Objects.equals(this.commandOutputHandlers, that.commandOutputHandlers) &&
                Objects.equals(this.defaultValue, that.defaultValue) &&
                Objects.equals(this.userSettable, that.userSettable);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type, derivedFromXnatInput, matcher,
                providesValueForCommandInputs, commandOutputHandlers, defaultValue, userSettable);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("name", name)
                .add("type", type)
                .add("derivedFromXnatInput", derivedFromXnatInput)
                .add("matcher", matcher)
                .add("providesValueForCommandInputs", providesValueForCommandInputs)
                .add("commandOutputHandlers", commandOutputHandlers)
                .add("defaultValue", defaultValue)
                .add("userSettable", userSettable)
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
