package org.nrg.actions.model;

import com.google.common.base.MoreObjects;
import org.apache.commons.lang3.StringUtils;

import javax.persistence.Embeddable;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class ResolvedCommandMount implements Serializable {

    private String name;
    private String localPath;
    private String remotePath;
    private Boolean readOnly;

    public ResolvedCommandMount() {}

    public ResolvedCommandMount(final CommandMount commandMount) {
        this.name = commandMount.getName();
        this.remotePath = commandMount.getPath();
        this.readOnly = true;
    }

    public ResolvedCommandMount(final CommandMount commandMount,
                                final Boolean readOnly) {
        this.name = commandMount.getName();
        this.remotePath = commandMount.getPath();
        this.readOnly = readOnly;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getLocalPath() {
        return localPath;
    }

    public void setLocalPath(final String localPath) {
        this.localPath = localPath;
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

    @Transient
    public String toBindMountString() {
        return StringUtils.join(localPath, remotePath, readOnly?"ro":"", ':');
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final ResolvedCommandMount that = (ResolvedCommandMount) o;
        return Objects.equals(this.name, that.name) &&
                Objects.equals(this.localPath, that.localPath) &&
                Objects.equals(this.remotePath, that.remotePath) &&
                Objects.equals(this.readOnly, that.readOnly);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, localPath, remotePath, readOnly);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("name", name)
                .add("localPath", localPath)
                .add("remotePath", remotePath)
                .add("readOnly", readOnly)
                .toString();
    }
}