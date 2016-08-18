package org.nrg.execution.events;

import com.google.common.base.MoreObjects;

import java.util.Date;
import java.util.Objects;

public class DockerContainerEvent implements DockerEvent {
    private String status;
    private String containerId;
    private Date time;
    private String exitCode;

    public DockerContainerEvent(final String status, final String containerId, final Date time) {
        this(status, containerId, time, null);
    }

    public DockerContainerEvent(final String status, final String containerId, final Date time, final String exitCode) {
        this.status = status;
        this.containerId = containerId;
        this.time = time;
        this.exitCode = exitCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public String getContainerId() {
        return containerId;
    }

    public void setContainerId(final String containerId) {
        this.containerId = containerId;
    }

    public Date getTime() {
        return time == null ? null : new Date(time.getTime());
    }

    public void setTime(final Date time) {
        this.time = time;
    }

    public String getExitCode() {
        return exitCode;
    }

    public void setExitCode(final String exitCode) {
        this.exitCode = exitCode;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final DockerContainerEvent that = (DockerContainerEvent) o;

        return Objects.equals(this.status, that.status) &&
                Objects.equals(this.containerId, that.containerId) &&
                Objects.equals(this.time, that.time) &&
                Objects.equals(this.exitCode, that.exitCode);

    }

    @Override
    public int hashCode() {
        return Objects.hash(status, containerId, time, exitCode);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("status", status)
                .add("id", containerId)
                .add("time", time)
                .add("exitCode", exitCode)
                .toString();
    }
}
