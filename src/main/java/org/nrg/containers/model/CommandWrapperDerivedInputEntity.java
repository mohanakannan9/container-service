package org.nrg.containers.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.envers.Audited;
import org.nrg.containers.model.command.auto.Command;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;
import java.util.Objects;

@Entity
@Audited
public class CommandWrapperDerivedInputEntity {
    public static final CommandWrapperInputType DEFAULT_TYPE = CommandWrapperInputType.STRING;

    private long id;
    @JsonIgnore
    private CommandWrapperEntity commandWrapperEntity;
    private String name;
    private String description;
    private CommandWrapperInputType type;
    private String matcher;
    @JsonProperty("provides-value-for-command-input") private String providesValueForCommandInput;
    @JsonProperty("provides-files-for-command-mount") private String providesFilesForCommandMount;
    @JsonProperty("default-value") private String defaultValue;
    @JsonProperty("user-settable") private Boolean userSettable = true;
    @JsonProperty("replacement-key") private String rawReplacementKey;
    private boolean required = false;
    private String value;

    @JsonProperty("derived-from-xnat-input") private String derivedFromXnatInput;
    @JsonProperty("derived-from-xnat-object-property") private String derivedFromXnatObjectProperty;

    public static CommandWrapperDerivedInputEntity fromPojo(final Command.CommandWrapperDerivedInput commandWrapperInput) {
        final CommandWrapperDerivedInputEntity commandWrapperInputEntity = new CommandWrapperDerivedInputEntity();
        commandWrapperInputEntity.derivedFromXnatInput = commandWrapperInput.derivedFromXnatInput();
        commandWrapperInputEntity.derivedFromXnatObjectProperty = commandWrapperInput.derivedFromXnatObjectProperty();
        commandWrapperInputEntity.id = commandWrapperInput.id();
        commandWrapperInputEntity.name = commandWrapperInput.name();
        commandWrapperInputEntity.description = commandWrapperInput.description();

        commandWrapperInputEntity.matcher = commandWrapperInput.matcher();
        commandWrapperInputEntity.providesValueForCommandInput = commandWrapperInput.providesValueForCommandInput();
        commandWrapperInputEntity.providesFilesForCommandMount = commandWrapperInput.providesFilesForCommandMount();
        commandWrapperInputEntity.defaultValue = commandWrapperInput.defaultValue();
        commandWrapperInputEntity.userSettable = commandWrapperInput.userSettable();
        commandWrapperInputEntity.rawReplacementKey = commandWrapperInput.rawReplacementKey();
        commandWrapperInputEntity.required = commandWrapperInput.required();
        switch (commandWrapperInput.type()) {
            case "string":
                commandWrapperInputEntity.type = CommandWrapperInputType.STRING;
                break;
            case "boolean":
                commandWrapperInputEntity.type = CommandWrapperInputType.BOOLEAN;
                break;
            case "number":
                commandWrapperInputEntity.type = CommandWrapperInputType.NUMBER;
                break;
            case "Directory":
                commandWrapperInputEntity.type = CommandWrapperInputType.DIRECTORY;
                break;
            case "File[]":
                commandWrapperInputEntity.type = CommandWrapperInputType.FILES;
                break;
            case "File":
                commandWrapperInputEntity.type = CommandWrapperInputType.FILE;
                break;
            case "Project":
                commandWrapperInputEntity.type = CommandWrapperInputType.PROJECT;
                break;
            case "Subject":
                commandWrapperInputEntity.type = CommandWrapperInputType.SUBJECT;
                break;
            case "Session":
                commandWrapperInputEntity.type = CommandWrapperInputType.SESSION;
                break;
            case "Scan":
                commandWrapperInputEntity.type = CommandWrapperInputType.SCAN;
                break;
            case "Assessor":
                commandWrapperInputEntity.type = CommandWrapperInputType.ASSESSOR;
                break;
            case "Resource":
                commandWrapperInputEntity.type = CommandWrapperInputType.RESOURCE;
                break;
            case "Config":
                commandWrapperInputEntity.type = CommandWrapperInputType.CONFIG;
                break;
            default:
                commandWrapperInputEntity.type = DEFAULT_TYPE;
        }
        return commandWrapperInputEntity;
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
    public CommandWrapperEntity getCommandWrapperEntity() {
        return commandWrapperEntity;
    }

    public void setCommandWrapperEntity(final CommandWrapperEntity commandWrapperEntity) {
        this.commandWrapperEntity = commandWrapperEntity;
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

    public CommandWrapperInputType getType() {
        return type;
    }

    public void setType(final CommandWrapperInputType type) {
        this.type = type;
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


    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final CommandWrapperDerivedInputEntity that = (CommandWrapperDerivedInputEntity) o;
        return Objects.equals(this.commandWrapperEntity, that.commandWrapperEntity) &&
                Objects.equals(this.name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(commandWrapperEntity, name);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
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
                .toString();
    }
}
