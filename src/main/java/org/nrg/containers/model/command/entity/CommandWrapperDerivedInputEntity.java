package org.nrg.containers.model.command.entity;

import com.google.common.base.MoreObjects;
import org.hibernate.envers.Audited;
import org.nrg.containers.model.command.auto.Command;

import javax.annotation.Nonnull;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import java.util.Objects;

@Entity
@Audited
public class CommandWrapperDerivedInputEntity {
    public static final CommandWrapperInputType DEFAULT_TYPE = CommandWrapperInputType.STRING;

    private long id;
    private CommandWrapperEntity commandWrapperEntity;
    private String name;
    private String description;
    private CommandWrapperInputType type;
    private String matcher;
    private String providesValueForCommandInput;
    private String providesFilesForCommandMount;
    private String viaSetupCommand;
    private String defaultValue;
    private Boolean userSettable = true;
    private String rawReplacementKey;
    private boolean required = false;
    private boolean loadChildren = true;

    private String derivedFromWrapperInput;
    private String derivedFromXnatObjectProperty;

    @Nonnull
    public static CommandWrapperDerivedInputEntity fromPojo(final @Nonnull Command.CommandWrapperDerivedInput commandWrapperInput) {
        return new CommandWrapperDerivedInputEntity().update(commandWrapperInput);
    }

    @Nonnull
    public CommandWrapperDerivedInputEntity update(final @Nonnull Command.CommandWrapperDerivedInput commandWrapperInput) {
        if (this.id == 0L || commandWrapperInput.id() != 0L) {
            this.setId(commandWrapperInput.id());
        }
        this.setDerivedFromWrapperInput(commandWrapperInput.derivedFromWrapperInput());
        this.setDerivedFromXnatObjectProperty(commandWrapperInput.derivedFromXnatObjectProperty());
        this.setName(commandWrapperInput.name());
        this.setDescription(commandWrapperInput.description());
        this.setMatcher(commandWrapperInput.matcher());
        this.setProvidesValueForCommandInput(commandWrapperInput.providesValueForCommandInput());
        this.setProvidesFilesForCommandMount(commandWrapperInput.providesFilesForCommandMount());
        this.setViaSetupCommand(commandWrapperInput.viaSetupCommand());
        this.setDefaultValue(commandWrapperInput.defaultValue());
        this.setUserSettable(commandWrapperInput.userSettable());
        this.setRawReplacementKey(commandWrapperInput.rawReplacementKey());
        this.setRequired(commandWrapperInput.required());
        this.setLoadChildren(commandWrapperInput.loadChildren());
        switch (commandWrapperInput.type()) {
            case "string":
                this.setType(CommandWrapperInputType.STRING);
                break;
            case "boolean":
                this.setType(CommandWrapperInputType.BOOLEAN);
                break;
            case "number":
                this.setType(CommandWrapperInputType.NUMBER);
                break;
            case "Directory":
                this.setType(CommandWrapperInputType.DIRECTORY);
                break;
            case "File[]":
                this.setType(CommandWrapperInputType.FILES);
                break;
            case "File":
                this.setType(CommandWrapperInputType.FILE);
                break;
            case "Project":
                this.setType(CommandWrapperInputType.PROJECT);
                break;
            case "Subject":
                this.setType(CommandWrapperInputType.SUBJECT);
                break;
            case "Session":
                this.setType(CommandWrapperInputType.SESSION);
                break;
            case "Scan":
                this.setType(CommandWrapperInputType.SCAN);
                break;
            case "Assessor":
                this.setType(CommandWrapperInputType.ASSESSOR);
                break;
            case "Resource":
                this.setType(CommandWrapperInputType.RESOURCE);
                break;
            case "Config":
                this.setType(CommandWrapperInputType.CONFIG);
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
    public CommandWrapperEntity getCommandWrapperEntity() {
        return commandWrapperEntity;
    }

    public void setCommandWrapperEntity(final CommandWrapperEntity commandWrapperEntity) {
        this.commandWrapperEntity = commandWrapperEntity;
    }

    public String getDerivedFromWrapperInput() {
        return derivedFromWrapperInput;
    }

    public void setDerivedFromWrapperInput(final String derivedFromXnatInput) {
        this.derivedFromWrapperInput = derivedFromXnatInput;
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

    public String getViaSetupCommand() {
        return viaSetupCommand;
    }

    public void setViaSetupCommand(final String viaSetupCommand) {
        this.viaSetupCommand = viaSetupCommand;
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

    public String getRawReplacementKey() {
        return rawReplacementKey;
    }

    public void setRawReplacementKey(final String rawReplacementKey) {
        this.rawReplacementKey = rawReplacementKey;
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

    public boolean getLoadChildren() {
        return loadChildren;
    }

    public void setLoadChildren(final boolean loadChildren) {
        this.loadChildren = loadChildren;
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
                .add("derivedFromWrapperInput", derivedFromWrapperInput)
                .add("derivedFromXnatObjectProperty", derivedFromXnatObjectProperty)
                .add("matcher", matcher)
                .add("providesValueForCommandInput", providesValueForCommandInput)
                .add("providesFilesForCommandMount", providesFilesForCommandMount)
                .add("viaSetupCommand", viaSetupCommand)
                .add("defaultValue", defaultValue)
                .add("userSettable", userSettable)
                .add("rawReplacementKey", rawReplacementKey)
                .add("required", required)
                .add("loadChildren", loadChildren)
                .toString();
    }
}
