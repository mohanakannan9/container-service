package org.nrg.containers.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntity;

import javax.persistence.Entity;
import java.util.Objects;

@Entity
public class CommandEventMapping extends AbstractHibernateEntity {
    @JsonProperty("event-type") private String eventType;
    @JsonProperty("command-id") private Long commandId;
    @JsonProperty("xnat-command-wrapper") private String xnatCommandWrapperName;
    @JsonProperty("project") private String projectId;

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Long getCommandId() {
        return commandId;
    }

    public void setCommandId(Long commandId) {
        this.commandId = commandId;
    }

    public String getXnatCommandWrapperName() {
        return xnatCommandWrapperName;
    }

    public void setXnatCommandWrapperName(final String xnatCommandWrapperName) {
        this.xnatCommandWrapperName = xnatCommandWrapperName;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(final String projectId) {
        this.projectId = projectId;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        final CommandEventMapping that = (CommandEventMapping) o;
        return Objects.equals(this.eventType, that.eventType) &&
                Objects.equals(this.commandId, that.commandId) &&
                Objects.equals(this.xnatCommandWrapperName, that.xnatCommandWrapperName) &&
                Objects.equals(this.projectId, that.projectId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), eventType, commandId, xnatCommandWrapperName, projectId);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("eventType", eventType)
                .add("commandId", commandId)
                .add("xnatCommandWrapper", xnatCommandWrapperName)
                .add("projectId", projectId)
                .toString();
    }

}
