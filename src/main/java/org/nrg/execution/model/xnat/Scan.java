package org.nrg.execution.model.xnat;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import org.nrg.xdat.model.XnatAbstractresourceI;
import org.nrg.xdat.model.XnatImagescandataI;
import org.nrg.xdat.om.XnatResourcecatalog;

import java.util.List;
import java.util.Objects;

public class Scan {
    @JsonIgnore private XnatImagescandataI xnatImagescandataI;
    private String id;
    private String sessionId;
    private String xsiType;
    private String scanType;
    private List<Resource> resources;

    public Scan() {}

    public Scan(final XnatImagescandataI xnatImagescandataI, final String sessionId, final String rootArchivePath) {
        this.xnatImagescandataI = xnatImagescandataI;
        this.id = xnatImagescandataI.getId();
        this.xsiType = xnatImagescandataI.getXSIType();
        this.scanType = xnatImagescandataI.getType();

        this.sessionId = sessionId;

        this.resources = Lists.newArrayList();
        for (final XnatAbstractresourceI xnatAbstractresourceI : xnatImagescandataI.getFile()) {
            if (xnatAbstractresourceI instanceof XnatResourcecatalog) {
                resources.add(new Resource((XnatResourcecatalog) xnatAbstractresourceI, rootArchivePath));
            }
        }
    }

    public XnatImagescandataI getXnatImagescandataI() {
        return xnatImagescandataI;
    }

    public void setXnatImagescandataI(final XnatImagescandataI xnatImagescandataI) {
        this.xnatImagescandataI = xnatImagescandataI;
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(final String sessionId) {
        this.sessionId = sessionId;
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
        return Objects.equals(this.xnatImagescandataI, that.xnatImagescandataI) &&
                Objects.equals(this.id, that.id) &&
                Objects.equals(this.sessionId, that.sessionId) &&
                Objects.equals(this.xsiType, that.xsiType) &&
                Objects.equals(this.scanType, that.scanType) &&
                Objects.equals(this.resources, that.resources);
    }

    @Override
    public int hashCode() {
        return Objects.hash(xnatImagescandataI, id, sessionId, xsiType, scanType, resources);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("xnatImagescandataI", xnatImagescandataI)
                .add("id", id)
                .add("sessionId", sessionId)
                .add("xsiType", xsiType)
                .add("scanType", scanType)
                .add("resources", resources)
                .toString();
    }
}
