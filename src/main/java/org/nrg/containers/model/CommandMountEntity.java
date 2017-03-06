package org.nrg.containers.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import io.swagger.annotations.ApiModelProperty;
import org.hibernate.envers.Audited;
import org.nrg.containers.model.auto.Command;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Audited
public class CommandMountEntity implements Serializable {

    private long id;
    @JsonIgnore private CommandEntity commandEntity;
    @JsonProperty(required = true) private String name;
    private Boolean writable;
    @JsonProperty("path") private String containerPath;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public long getId() {
        return id;
    }

    public void setId(final long id) {
        this.id = id;
    }

    @ManyToOne
    public CommandEntity getCommandEntity() {
        return commandEntity;
    }

    public void setCommandEntity(final CommandEntity commandEntity) {
        this.commandEntity = commandEntity;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public Boolean isWritable() {
        return writable;
    }

    public Boolean getWritable() {
        return writable;
    }

    public void setWritable(final Boolean writable) {
        this.writable = writable;
    }

    public String getContainerPath() {
        return containerPath;
    }

    public void setContainerPath(final String remotePath) {
        this.containerPath = remotePath;
    }

    static CommandMountEntity fromPojo(final Command.CommandMount commandMount) {
        final CommandMountEntity commandMountEntity = new CommandMountEntity();
        commandMountEntity.id = commandMount.id();
        commandMountEntity.name = commandMount.name();
        commandMountEntity.writable = commandMount.writable();
        commandMountEntity.containerPath = commandMount.path();
        return commandMountEntity;
    }

    @Transient
    @ApiModelProperty(hidden = true)
    public String toBindMountString(final String hostPath) {
        return hostPath + ":" + containerPath + (writable?"":":ro");
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final CommandMountEntity that = (CommandMountEntity) o;
        return Objects.equals(this.id, that.id) &&
                Objects.equals(this.name, that.name) &&
                Objects.equals(this.writable, that.writable) &&
                Objects.equals(this.containerPath, that.containerPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, writable, containerPath);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("name", name)
                .add("writable", writable)
                .add("containerPath", containerPath)
                .toString();
    }
}