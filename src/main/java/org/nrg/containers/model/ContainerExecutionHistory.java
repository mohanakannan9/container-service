package org.nrg.containers.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.MoreObjects;
import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

@Entity
@Audited
public class ContainerExecutionHistory {
    private long id;
    @JsonIgnore private ContainerExecution containerExecution;
    private String status;
    private String timeNano;

    public ContainerExecutionHistory() {}

    public ContainerExecutionHistory(final String status, final Long timeNano) {
        this.status = status;
        this.timeNano = String.valueOf(timeNano);
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
    public ContainerExecution getContainerExecution() {
        return containerExecution;
    }

    public void setContainerExecution(final ContainerExecution containerExecution) {
        this.containerExecution = containerExecution;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public String getTimeNano() {
        return timeNano;
    }

    public void setTimeNano(final String timeNano) {
        this.timeNano = timeNano;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("status", status)
                .add("timeNano", timeNano)
                .toString();
    }
}
