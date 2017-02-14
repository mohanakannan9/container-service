package org.nrg.containers.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.swagger.annotations.ApiModelProperty;
import org.nrg.containers.exceptions.CommandValidationException;
import org.nrg.containers.model.auto.CommandPojo;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntity;

import javax.persistence.CascadeType;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.OneToMany;
import javax.persistence.Transient;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Entity
@JsonTypeInfo(use = Id.NAME, include = As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = DockerCommand.class, name = "docker")
})
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type")
public abstract class Command extends AbstractHibernateEntity {
    public static CommandType DEFAULT_TYPE = CommandType.DOCKER;
    private String name;
    private String label;
    private String description;
    private String version;
    @JsonProperty("schema-version") private String schemaVersion;
    @JsonProperty("info-url") private String infoUrl;
    private String image;
    @JsonProperty("working-directory") private String workingDirectory;
    @JsonProperty("command-line") private String commandLine;
    @JsonProperty("mounts") private List<CommandMount> mounts;
    @JsonProperty("environment-variables") private Map<String, String> environmentVariables;
    private List<CommandInput> inputs;
    private List<CommandOutput> outputs;
    @JsonProperty("xnat") private List<XnatCommandWrapper> xnatCommandWrappers;

    public static Command commandPojoToCommand(final CommandPojo commandPojo) throws CommandValidationException {
        final List<String> errors = commandPojo.validate();
        if (!errors.isEmpty()) {
            throw new CommandValidationException(commandPojo.name(), errors);
        }

        final Command command;
        final String type = commandPojo.type();
        switch (type) {
            case "docker":
                command = DockerCommand.fromPojo(commandPojo);
                break;
            default:
                // This should have been caught already, but still...
                throw new CommandValidationException(commandPojo.name(), "Cannot instantiate command with type " + type);
        }

        command.setName(commandPojo.name());
        command.setLabel(commandPojo.label());
        command.setDescription(commandPojo.description());
        command.setVersion(commandPojo.version());
        command.setSchemaVersion(commandPojo.schemaVersion());
        command.setInfoUrl(commandPojo.infoUrl());
        command.setImage(commandPojo.image());
        command.setWorkingDirectory(commandPojo.workingDirectory());
        command.setCommandLine(commandPojo.commandLine());
        command.setEnvironmentVariables(commandPojo.environmentVariables());

        for (final CommandPojo.CommandMountPojo commandMountPojo : commandPojo.mounts()) {
            command.addMount(CommandMount.fromPojo(commandMountPojo));
        }

        for (final CommandPojo.CommandInputPojo commandInputPojo : commandPojo.inputs()) {
            command.addInput(CommandInput.fromPojo(commandInputPojo));
        }

        for (final CommandPojo.CommandOutputPojo commandOutputPojo : commandPojo.outputs()) {
            command.addOutput(CommandOutput.fromPojo(commandOutputPojo));
        }
        for (final CommandPojo.CommandWrapperPojo commandWrapperPojo : commandPojo.xnatCommandWrappers()) {
            command.addXnatCommandWrapper(XnatCommandWrapper.fromPojo(commandWrapperPojo));
        }

        return command;
    }

    @Transient
    public abstract CommandType getType();

    // @javax.persistence.Id
    // @GeneratedValue(strategy = GenerationType.TABLE)
    // // @Override
    // public long getId() {
    //     return super.getId();
    // }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(final String label) {
        this.label = label;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(final String version) {
        this.version = version;
    }

    public String getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(final String schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    @ApiModelProperty("A URL where users can get more information about the Command")
    public String getInfoUrl() {
        return infoUrl;
    }

    public void setInfoUrl(final String infoUrl) {
        this.infoUrl = infoUrl;
    }

    public String getImage() {
        return image;
    }

    public void setImage(final String image) {
        this.image = image;
    }

    public String getWorkingDirectory() {
        return workingDirectory;
    }

    public void setWorkingDirectory(final String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    @ApiModelProperty("The command that will be executed in the container when the Command is launched.")
    public String getCommandLine() {
        return commandLine;
    }

    public void setCommandLine(final String commandLine) {
        this.commandLine = commandLine;
    }

    @ElementCollection
    public List<CommandMount> getMounts() {
        return mounts;
    }

    public void setMounts(final List<CommandMount> mounts) {
        this.mounts = mounts == null ?
                Lists.<CommandMount>newArrayList() :
                mounts;
    }

    public void addMount(final CommandMount mount) {
        if (mount == null) {
            return;
        }

        if (this.mounts == null) {
            this.mounts = Lists.newArrayList();
        }
        this.mounts.add(mount);
    }

    @ElementCollection
    @ApiModelProperty("A Map of environment variables. Each kay is the environment variable's name, and each value is the environment variable's value." +
            "Both the names and values can use template strings, e.g. #variable-name#, which will be resolved into a value when the Command is launched.")
    public Map<String, String> getEnvironmentVariables() {
        return environmentVariables;
    }

    public void setEnvironmentVariables(final Map<String, String> environmentVariables) {
        this.environmentVariables = environmentVariables == null ?
                Maps.<String, String>newHashMap() :
                environmentVariables;
    }

    @ElementCollection
    @ApiModelProperty("A list of inputs. " +
            "When the Command is launched, these inputs receive values; " +
            "those values will be used to fill in any template strings in the Command's run-template, mounts, or environment variables.")
    public List<CommandInput> getInputs() {
        return inputs;
    }

    public void setInputs(final List<CommandInput> inputs) {
        this.inputs = inputs == null ?
                Lists.<CommandInput>newArrayList() :
                inputs;
    }

    public void addInput(final CommandInput input) {
        if (input == null) {
            return;
        }

        if (this.inputs == null) {
            this.inputs = Lists.newArrayList();
        }
        this.inputs.add(input);
    }

    @ElementCollection
    @ApiModelProperty("A list of outputs.")
    public List<CommandOutput> getOutputs() {
        return outputs;
    }

    public void setOutputs(final List<CommandOutput> outputs) {
        this.outputs = outputs == null ?
                Lists.<CommandOutput>newArrayList() :
                outputs;
    }

    public void addOutput(final CommandOutput output) {
        if (output == null) {
            return;
        }

        if (this.outputs == null) {
            this.outputs = Lists.newArrayList();
        }
        this.outputs.add(output);
    }

    @OneToMany(mappedBy = "command", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<XnatCommandWrapper> getXnatCommandWrappers() {
        return xnatCommandWrappers;
    }

    public void setXnatCommandWrappers(final List<XnatCommandWrapper> xnatCommandWrappers) {
        this.xnatCommandWrappers = xnatCommandWrappers == null ?
                Lists.<XnatCommandWrapper>newArrayList() :
                xnatCommandWrappers;
    }

    @Transient
    public void addXnatCommandWrapper(final XnatCommandWrapper xnatCommandWrapper) {
        if (xnatCommandWrapper == null) {
            return;
        }
        xnatCommandWrapper.setCommand(this);

        if (this.xnatCommandWrappers == null) {
            this.xnatCommandWrappers = Lists.newArrayList();
        }
        this.xnatCommandWrappers.add(xnatCommandWrapper);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        final Command that = (Command) o;
        return Objects.equals(this.name, that.name) &&
                Objects.equals(this.label, that.label) &&
                Objects.equals(this.description, that.description) &&
                Objects.equals(this.version, that.version) &&
                Objects.equals(this.schemaVersion, that.schemaVersion) &&
                Objects.equals(this.infoUrl, that.infoUrl) &&
                Objects.equals(this.image, that.image) &&
                Objects.equals(this.workingDirectory, that.workingDirectory) &&
                Objects.equals(this.commandLine, that.commandLine) &&
                Objects.equals(this.mounts, that.mounts) &&
                Objects.equals(this.environmentVariables, that.environmentVariables) &&
                Objects.equals(this.inputs, that.inputs) &&
                Objects.equals(this.outputs, that.outputs) &&
                Objects.equals(this.xnatCommandWrappers, that.xnatCommandWrappers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), name, label, description, version, schemaVersion, infoUrl, image,
                workingDirectory, commandLine, mounts, environmentVariables, inputs, outputs, xnatCommandWrappers);
    }

    @Override
    public ToStringHelper addParentPropertiesToString(final ToStringHelper helper) {
        return super.addParentPropertiesToString(helper)
                .add("name", name)
                .add("label", label)
                .add("description", description)
                .add("version", version)
                .add("schemaVersion", schemaVersion)
                .add("infoUrl", infoUrl)
                .add("image", image)
                .add("workingDirectory", workingDirectory)
                .add("commandLine", commandLine)
                .add("mounts", mounts)
                .add("environmentVariables", environmentVariables)
                .add("inputs", inputs)
                .add("outputs", outputs)
                .add("xnatCommandWrappers", xnatCommandWrappers);
    }

    @Override
    public String toString() {
        return addParentPropertiesToString(MoreObjects.toStringHelper(this))
                .toString();
    }
}
