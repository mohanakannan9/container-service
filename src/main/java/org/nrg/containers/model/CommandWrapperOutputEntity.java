package org.nrg.containers.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import org.hibernate.envers.Audited;
import org.nrg.containers.model.command.auto.Command;

import javax.annotation.Nullable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Entity
@Audited
public class CommandWrapperOutputEntity {
    public static final Type DEFAULT_TYPE = Type.RESOURCE;

    private long id;
    @JsonIgnore private CommandWrapperEntity commandWrapperEntity;
    private String name;
    @JsonProperty("accepts-command-output") private String commandOutputName;
    @JsonProperty("as-a-child-of-xnat-input") private String xnatInputName;
    private Type type;
    private String label;

    public static CommandWrapperOutputEntity fromPojo(final Command.CommandWrapperOutput commandWrapperOutput) {
        final CommandWrapperOutputEntity commandWrapperOutputEntity = new CommandWrapperOutputEntity();
        commandWrapperOutputEntity.id = commandWrapperOutput.id();
        commandWrapperOutputEntity.name = commandWrapperOutput.name();
        commandWrapperOutputEntity.commandOutputName = commandWrapperOutput.commandOutputName();
        commandWrapperOutputEntity.xnatInputName = commandWrapperOutput.xnatInputName();
        commandWrapperOutputEntity.label = commandWrapperOutput.label();

        switch (commandWrapperOutput.type()) {
            case "Resource":
                commandWrapperOutputEntity.type = Type.RESOURCE;
                break;
            case "Assessor":
                commandWrapperOutputEntity.type = Type.ASSESSOR;
                break;
            default:
                commandWrapperOutputEntity.type = DEFAULT_TYPE;
        }

        return commandWrapperOutputEntity;
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

    public String getCommandOutputName() {
        return commandOutputName;
    }

    public void setCommandOutputName(final String commandOutputName) {
        this.commandOutputName = commandOutputName;
    }

    public String getXnatInputName() {
        return xnatInputName;
    }

    public void setXnatInputName(final String xnatInputName) {
        this.xnatInputName = xnatInputName;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(final String label) {
        this.label = label;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final CommandWrapperOutputEntity that = (CommandWrapperOutputEntity) o;
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
                .add("commandOutputName", commandOutputName)
                .add("xnatInputName", xnatInputName)
                .add("type", type)
                .add("label", label)
                .toString();
    }

    public enum Type {
        RESOURCE("Resource"),
        ASSESSOR("Assessor");

        private final String name;

        @JsonCreator
        Type(final String name) {
            this.name = name;
        }

        @JsonValue
        public String getName() {
            return name;
        }

        public static List<String> names() {
            return Lists.transform(Arrays.asList(Type.values()), new Function<Type, String>() {
                @Nullable
                @Override
                public String apply(@Nullable final Type type) {
                    return type != null ? type.getName() : "";
                }
            });
        }
    }
}
