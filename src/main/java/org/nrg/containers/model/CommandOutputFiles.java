package org.nrg.containers.model;

import com.google.common.base.MoreObjects;

import javax.persistence.Embeddable;
import javax.persistence.Transient;
import java.util.Objects;

@Embeddable
public class CommandOutputFiles {
    private String mount;
    private String path;

    public String getMount() {
        return mount;
    }

    public void setMount(final String mount) {
        this.mount = mount;
    }

    public String getPath() {
        return path;
    }

    public void setPath(final String path) {
        this.path = path;
    }

    @Transient
    void update(final CommandOutputFiles other, final Boolean ignoreNull) {
        if (other == null) {
            return;
        }

        if (other.mount != null || !ignoreNull) {
            this.mount = other.mount;
        }
        if (other.path != null || !ignoreNull) {
            this.path = other.path;
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final CommandOutputFiles that = (CommandOutputFiles) o;
        return Objects.equals(this.mount, that.mount) &&
                Objects.equals(this.path, that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mount, path);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("mount", mount)
                .add("path", path)
                .toString();
    }
}
