package org.nrg.actions.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

import javax.persistence.Embeddable;
import java.util.Objects;

@Embeddable
public class ActionResource {

    @JsonProperty("name") private String resourceName;
    @JsonProperty("mount") private String mountName;

    public ActionResource() {};

    public ActionResource(final CommandMount commandMount) {
        this.resourceName = commandMount.getName();
        this.mountName = commandMount.getName();
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

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final ActionResource that = (ActionResource) o;
        return Objects.equals(this.resourceName, that.resourceName) &&
                Objects.equals(this.mountName, that.mountName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resourceName, mountName);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("resourceName", resourceName)
                .add("mountName", mountName)
                .toString();
    }
}
