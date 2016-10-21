package org.nrg.containers.model.xnat;

import com.google.common.base.MoreObjects;

import java.util.Objects;

public abstract class XnatModelObject {
    protected String id;
    protected String label;
    protected String xsiType;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getXsiType() {
        return xsiType;
    }

    public void setXsiType(String xsiType) {
        this.xsiType = xsiType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        XnatModelObject that = (XnatModelObject) o;
        return Objects.equals(this.id, that.id) &&
                Objects.equals(this.label, that.label) &&
                Objects.equals(this.xsiType, that.xsiType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, label, xsiType);
    }

    public MoreObjects.ToStringHelper addParentPropertiesToString(final MoreObjects.ToStringHelper helper) {
        return helper
                .add("id", id)
                .add("label", label)
                .add("xsiType", xsiType);
    }
}
