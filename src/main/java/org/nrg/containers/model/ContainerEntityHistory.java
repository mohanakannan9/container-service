package org.nrg.containers.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.MoreObjects;
import org.hibernate.envers.Audited;
import org.nrg.containers.events.ContainerEvent;
import org.nrg.containers.events.DockerContainerEvent;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import java.util.Date;
import java.util.Objects;

@Entity
@Audited
public class ContainerEntityHistory {
    private long id;
    @JsonIgnore private ContainerEntity containerEntity;
    private String status;
    private String entityType;
    private String entityId;
    private Date timeRecorded;
    private String externalTimestamp;

    public ContainerEntityHistory() {}

    public static ContainerEntityHistory fromContainerEvent(final ContainerEvent containerEvent) {
        final ContainerEntityHistory history = new ContainerEntityHistory();
        history.status = containerEvent.getStatus();
        history.entityType = "event";
        history.entityId = null;
        history.timeRecorded = new Date();
        if (containerEvent instanceof DockerContainerEvent) {
            history.externalTimestamp = String.valueOf(((DockerContainerEvent)containerEvent).getTimeNano());
        }
        return history;
    }

    public static ContainerEntityHistory fromSystem(final String status) {
        final ContainerEntityHistory history = new ContainerEntityHistory();
        history.status = status;
        history.externalTimestamp = null;
        history.entityType = "system";
        history.entityId = null;
        history.timeRecorded = new Date();
        return history;
    }

    public static ContainerEntityHistory fromUserAction(final String status, final String username) {
        final ContainerEntityHistory history = new ContainerEntityHistory();
        history.status = status;
        history.externalTimestamp = null;
        history.entityType = "user";
        history.entityId = username;
        history.timeRecorded = new Date();
        return history;
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
    public ContainerEntity getContainerEntity() {
        return containerEntity;
    }

    public void setContainerEntity(final ContainerEntity containerEntity) {
        this.containerEntity = containerEntity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(final String entityType) {
        this.entityType = entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(final String entityId) {
        this.entityId = entityId;
    }

    public Date getTimeRecorded() {
        return timeRecorded;
    }

    public void setTimeRecorded(final Date timeRecorded) {
        this.timeRecorded = timeRecorded;
    }

    public String getExternalTimestamp() {
        return externalTimestamp;
    }

    public void setExternalTimestamp(final String externalTimestamp) {
        this.externalTimestamp = externalTimestamp;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final ContainerEntityHistory that = (ContainerEntityHistory) o;
        return Objects.equals(this.containerEntity, that.containerEntity) &&
                Objects.equals(this.status, that.status) &&
                Objects.equals(this.externalTimestamp, that.externalTimestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(containerEntity, status, externalTimestamp);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("status", status)
                .add("entityType", entityType)
                .add("entityId", entityId)
                .add("timeRecorded", timeRecorded)
                .add("externalTimestamp", externalTimestamp)
                .toString();
    }
}
