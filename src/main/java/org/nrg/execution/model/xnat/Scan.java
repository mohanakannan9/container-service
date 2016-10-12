package org.nrg.execution.model.xnat;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import org.nrg.xdat.model.XnatAbstractresourceI;
import org.nrg.xdat.model.XnatImagescandataI;
import org.nrg.xdat.om.XnatResourcecatalog;

import java.util.List;
import java.util.Objects;

public class Scan {
    private String id;
    private String xsiType;
    private String scanType;
    private List<Resource> resources;

    public Scan() {}

    public Scan(final XnatImagescandataI xnatImagescandataI, final String rootArchivePath) {
        this.id = xnatImagescandataI.getId();
        this.xsiType = xnatImagescandataI.getXSIType();
        this.scanType = xnatImagescandataI.getType();

        this.resources = Lists.newArrayList();
        for (final XnatAbstractresourceI xnatAbstractresourceI : xnatImagescandataI.getFile()) {
            if (xnatAbstractresourceI instanceof XnatResourcecatalog) {
                resources.add(new Resource((XnatResourcecatalog) xnatAbstractresourceI, rootArchivePath));
            }
        }
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getXsiType() {
        return xsiType;
    }

    public void setXsiType(final String xsiType) {
        this.xsiType = xsiType;
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
        final Scan that = (Scan) o;
        return Objects.equals(this.id, that.id) &&
                Objects.equals(this.xsiType, that.xsiType) &&
                Objects.equals(this.scanType, that.scanType) &&
                Objects.equals(this.resources, that.resources);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, xsiType, scanType, resources);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("xsiType", xsiType)
                .add("scanType", scanType)
                .add("resources", resources)
                .toString();
    }

}
