package org.nrg.containers.model.xnat;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import org.nrg.xdat.model.XnatAbstractresourceI;
import org.nrg.xdat.model.XnatImagescandataI;
import org.nrg.xdat.om.XnatImagescandata;
import org.nrg.xdat.om.XnatResourcecatalog;
import org.nrg.xft.security.UserI;

import java.util.List;
import java.util.Objects;

public class Scan extends XnatModelObject {
    @JsonIgnore private XnatImagescandataI xnatImagescandata;
    @JsonProperty(value = "parent-id") private String parentId;
    @JsonProperty("scan-type") private String scanType;
    private List<Resource> resources;

    public Scan() {}

    public Scan(final XnatImagescandataI xnatImagescandata, final String parentId, final String rootArchivePath) {
        this.xnatImagescandata = xnatImagescandata;
        this.id = xnatImagescandata.getId();
        this.xsiType = xnatImagescandata.getXSIType();
        this.scanType = xnatImagescandata.getType();

        this.parentId = parentId;

        this.resources = Lists.newArrayList();
        for (final XnatAbstractresourceI xnatAbstractresourceI : xnatImagescandata.getFile()) {
            if (xnatAbstractresourceI instanceof XnatResourcecatalog) {
                resources.add(new Resource((XnatResourcecatalog) xnatAbstractresourceI, this.id, rootArchivePath));
            }
        }
    }

    public XnatImagescandataI loatXnatImagescandataI(UserI userI) {
        xnatImagescandata = XnatImagescandata.getXnatImagescandatasByXnatImagescandataId(id, userI, false);
        return xnatImagescandata;
    }

    public XnatImagescandataI getXnatImagescandata() {
        return xnatImagescandata;
    }

    public void setXnatImagescandata(final XnatImagescandataI xnatImagescandata) {
        this.xnatImagescandata = xnatImagescandata;
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
        return Objects.equals(this.xnatImagescandata, that.xnatImagescandata) &&
                Objects.equals(this.parentId, that.parentId) &&
                Objects.equals(this.scanType, that.scanType) &&
                Objects.equals(this.resources, that.resources);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), xnatImagescandata, parentId, scanType, resources);
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
