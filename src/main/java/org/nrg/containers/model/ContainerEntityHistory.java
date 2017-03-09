package org.nrg.containers.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.MoreObjects;
import org.hibernate.envers.Audited;

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
    private Date timeRecorded;
    private long externalTimestamp;

    public ContainerEntityHistory() {}

    public ContainerEntityHistory(final String status, final long externalTimestamp) {
        this.status = status;
        this.timeRecorded = new Date();
        this.externalTimestamp = externalTimestamp;
    }

    public ContainerEntityHistory(final String status) {
        this.status = status;
        this.timeRecorded = new Date();
        this.externalTimestamp = 0L;
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

    public Date getTimeRecorded() {
        return timeRecorded;
    }

    public void setTimeRecorded(final Date timeRecorded) {
        this.timeRecorded = timeRecorded;
    }

    public long getExternalTimestamp() {
        return externalTimestamp;
    }

    public void setExternalTimestamp(final long externalTimestamp) {
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
                .add("timeRecorded", timeRecorded)
                .add("externalTimestamp", externalTimestamp)
                .toString();
    }
}
