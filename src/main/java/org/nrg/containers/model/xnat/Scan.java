package org.nrg.containers.model.xnat;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import org.nrg.xdat.model.XnatAbstractresourceI;
import org.nrg.xdat.model.XnatImagescandataI;
import org.nrg.xdat.om.XnatResourcecatalog;

import java.util.List;
import java.util.Objects;

public class Scan extends XnatModelObject {
    @JsonIgnore private XnatImagescandataI xnatImagescandataI;
    @JsonProperty(value = "parent-id") private String parentId;
    @JsonProperty("scan-type") private String scanType;
    private List<Resource> resources;

    public Scan() {}

    public Scan(final XnatImagescandataI xnatImagescandataI, final String parentId, final String rootArchivePath) {
        this.xnatImagescandataI = xnatImagescandataI;
        this.id = xnatImagescandataI.getId();
        this.xsiType = xnatImagescandataI.getXSIType();
        this.scanType = xnatImagescandataI.getType();

        this.parentId = parentId;

        this.resources = Lists.newArrayList();
        for (final XnatAbstractresourceI xnatAbstractresourceI : xnatImagescandataI.getFile()) {
            if (xnatAbstractresourceI instanceof XnatResourcecatalog) {
                resources.add(new Resource((XnatResourcecatalog) xnatAbstractresourceI, this.id, rootArchivePath));
            }
        }
    }

    public XnatImagescandataI getXnatImagescandataI() {
        return xnatImagescandataI;
    }

    public void setXnatImagescandataI(final XnatImagescandataI xnatImagescandataI) {
        this.xnatImagescandataI = xnatImagescandataI;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(final String parentId) {
        this.parentId = parentId;
    }

    public String getScanType() {
        return scanType;
    }

    public void setScanType(final String scanType) {
        this.scanType = scanType;
    }

    public List<Resource> getResources() {
        return resources;
    }

    public void setResources(final List<Resource> resources) {
        this.resources = resources;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        final Scan that = (Scan) o;
        return Objects.equals(this.xnatImagescandataI, that.xnatImagescandataI) &&
                Objects.equals(this.parentId, that.parentId) &&
                Objects.equals(this.scanType, that.scanType) &&
                Objects.equals(this.resources, that.resources);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), xnatImagescandataI, parentId, scanType, resources);
    }

    @Override
    public String toString() {
        return addParentPropertiesToString(MoreObjects.toStringHelper(this))
                .add("parentId", parentId)
                .add("scanType", scanType)
                .add("resources", resources)
                .toString();
    }
}
