package org.nrg.containers.model.xnat;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import org.nrg.xdat.model.XnatAbstractresourceI;
import org.nrg.xdat.model.XnatImageassessordataI;
import org.nrg.xdat.model.XnatImagescandataI;
import org.nrg.xdat.model.XnatImagesessiondataI;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.XnatResourcecatalog;
import org.nrg.xft.security.UserI;

import java.util.List;
import java.util.Objects;


public class Session extends XnatModelObject {
    public static Type type = Type.SESSION;
    @JsonIgnore private XnatImagesessiondataI xnatImagesessiondata;
    @JsonProperty(value = "parent-id") private String parentId;
    private List<Scan> scans;
    private List<Assessor> assessors;
    private List<Resource> resources;

    public Session() {}

    public Session(final XnatImagesessiondataI xnatImagesessiondata, final UserI userI) {
        this(xnatImagesessiondata, XnatProjectdata.getXnatProjectdatasById(xnatImagesessiondata.getProject(), userI, false).getRootArchivePath());
    }
    public Session(final XnatImagesessiondataI xnatImagesessiondata, final String rootArchivePath) {
        this.xnatImagesessiondata = xnatImagesessiondata;
        this.id = xnatImagesessiondata.getId();
        this.label = xnatImagesessiondata.getLabel();
        this.xsiType = xnatImagesessiondata.getXSIType();
        this.uri = "/experiments/" + id;

        this.parentId = xnatImagesessiondata.getSubjectId();

        this.scans = Lists.newArrayList();
        for (final XnatImagescandataI xnatImagescandataI : xnatImagesessiondata.getScans_scan()) {
            this.scans.add(new Scan(xnatImagescandataI, this.id, this.uri, rootArchivePath));
        }

        this.resources = Lists.newArrayList();
        for (final XnatAbstractresourceI xnatAbstractresourceI : xnatImagesessiondata.getResources_resource()) {
            if (xnatAbstractresourceI instanceof XnatResourcecatalog) {
                resources.add(new Resource((XnatResourcecatalog) xnatAbstractresourceI, this.id, this.uri, rootArchivePath));
            }
        }

        this.assessors = Lists.newArrayList();
        for (final XnatImageassessordataI xnatImageassessordataI : xnatImagesessiondata.getAssessors_assessor()) {
            assessors.add(new Assessor(xnatImageassessordataI, this.id, this.uri, rootArchivePath));
        }
    }

    public XnatImagesessiondataI loadXnatImagesessiondata(final UserI userI) {
        xnatImagesessiondata = XnatImagesessiondata.getXnatImagesessiondatasById(id, userI, false);
        return xnatImagesessiondata;
    }

    public XnatImagesessiondataI getXnatImagesessiondata() {
        return xnatImagesessiondata;
    }

    public void setXnatImagesessiondata(final XnatImagesessiondataI xnatImagesessiondata) {
        this.xnatImagesessiondata = xnatImagesessiondata;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(final String parentId) {
        this.parentId = parentId;
    }

    public List<Resource> getResources() {
        return resources;
    }

    public void setResources(final List<Resource> resources) {
        this.resources = resources;
    }

    public List<Assessor> getAssessors() {
        return assessors;
    }

    public void setAssessors(final List<Assessor> assessors) {
        this.assessors = assessors;
    }

    public List<Scan> getScans() {
        return scans;
    }

    public void setScans(final List<Scan> scans) {
        this.scans = scans;
    }

    public Type getType() {
        return type;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        final Session that = (Session) o;
        return Objects.equals(this.xnatImagesessiondata, that.xnatImagesessiondata) &&
                Objects.equals(this.parentId, that.parentId) &&
                Objects.equals(this.scans, that.scans) &&
                Objects.equals(this.assessors, that.assessors) &&
                Objects.equals(this.resources, that.resources);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), xnatImagesessiondata, parentId, scans, assessors, resources);
    }

    @Override
    public String toString() {
        return addParentPropertiesToString(MoreObjects.toStringHelper(this))
                .add("parentId", parentId)
                .add("scans", scans)
                .add("assessors", assessors)
                .add("resources", resources)
                .toString();
    }
}
