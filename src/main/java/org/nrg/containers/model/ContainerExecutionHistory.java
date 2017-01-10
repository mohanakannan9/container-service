package org.nrg.containers.model;

import com.google.common.base.MoreObjects;

import javax.persistence.Embeddable;
import java.util.Date;

@Embeddable
public class ContainerExecutionHistory {
    private String status;
    private String timeNano;

    public ContainerExecutionHistory() {}

    public ContainerExecutionHistory(final String status, final Long timeNano) {
        this.status = status;
        this.timeNano = String.valueOf(timeNano);
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
                .add("status", status)
                .add("timeNano", timeNano)
                .toString();
    }
}
