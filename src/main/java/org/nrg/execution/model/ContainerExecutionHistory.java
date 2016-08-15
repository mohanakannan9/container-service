package org.nrg.execution.model;

import com.google.common.base.MoreObjects;

import javax.persistence.Embeddable;
import java.util.Date;
import java.util.Objects;

@Embeddable
public class ContainerExecutionHistory {
    private String status;
    private Date time;

    public ContainerExecutionHistory() {}

    public ContainerExecutionHistory(final String status, final Date time) {
        this.status = status;
        this.time = time;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public Date getTime() {
        return time;
    }

    public void setTime(final Date time) {
        this.time = time;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("status", status)
                .add("time", time)
                .toString();
    }
}
