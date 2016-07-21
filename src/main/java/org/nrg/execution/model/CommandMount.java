package org.nrg.execution.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import io.swagger.annotations.ApiModelProperty;

import javax.persistence.Embeddable;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class CommandMount implements Serializable {

    private String name;
    @JsonProperty("host-path") private String hostPath;
    @JsonProperty("remote-path") private String remotePath;
    @JsonProperty("read-only") private Boolean readOnly = true;
    private Boolean overwrite = false;

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getHostPath() {
        return hostPath;
    }

    public void setHostPath(final String localPath) {
        this.hostPath = localPath;
    }

    public String getRemotePath() {
        return remotePath;
    }

    public void setRemotePath(final String remotePath) {
        this.remotePath = remotePath;
    }

    public Boolean getReadOnly() {
        return readOnly;
    }

    public void setReadOnly(final Boolean readOnly) {
        this.readOnly = readOnly;
    }

    public Boolean getOverwrite() {
        return overwrite;
    }

    public void setOverwrite(final Boolean overwrite) {
        this.overwrite = overwrite;
    }

    @Transient
    @ApiModelProperty(hidden = true)
    public String toBindMountString() {
        return hostPath + ":" + remotePath + (readOnly?":ro":"");
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final CommandMount that = (CommandMount) o;
        return Objects.equals(this.name, that.name) &&
                Objects.equals(this.hostPath, that.hostPath) &&
                Objects.equals(this.remotePath, that.remotePath) &&
                Objects.equals(this.readOnly, that.readOnly) &&
                Objects.equals(this.overwrite, that.overwrite);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, hostPath, remotePath, readOnly, overwrite);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("name", name)
                .add("hostPath", hostPath)
                .add("remotePath", remotePath)
                .add("readOnly", readOnly)
                .add("overwrite", overwrite)
                .toString();
    }
}