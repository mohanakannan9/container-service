package org.nrg.execution.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

import javax.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class ActionResource implements Serializable {

    private String path;
    @JsonProperty("name") private String resourceName;
    @JsonProperty("mount") private String mountName;
    private Boolean overwrite = false;

    public ActionResource() {}

    public ActionResource(final String name) {
        // In lieu of any explicit user-provided information,
        // assume the command mount was named the same as the resource.
        // This provides an easy default for users.
        this.resourceName = name;
        this.mountName = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(final String path) {
        this.path = path;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(final String resourceName) {
        this.resourceName = resourceName;
    }

    public String getMountName() {
        return mountName;
    }

    public void setMountName(final String mountName) {
        this.mountName = mountName;
    }

    public Boolean getOverwrite() {
        return overwrite;
    }

    public void setOverwrite(final Boolean overwrite) {
        this.overwrite = overwrite;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final ActionResource that = (ActionResource) o;
        return Objects.equals(this.path, that.path) &&
                Objects.equals(this.resourceName, that.resourceName) &&
                Objects.equals(this.mountName, that.mountName) &&
                Objects.equals(this.overwrite, that.overwrite);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, resourceName, mountName, overwrite);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("path", path)
                .add("resourceName", resourceName)
                .add("mountName", mountName)
                .add("overwrite", overwrite)
                .toString();
    }
}
