package org.nrg.containers.model.xnat;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import org.nrg.xdat.model.XnatAbstractresourceI;
import org.nrg.xdat.model.XnatExperimentdataI;
import org.nrg.xdat.model.XnatImagesessiondataI;
import org.nrg.xdat.model.XnatSubjectdataI;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.XnatResourcecatalog;
import org.nrg.xft.security.UserI;

import java.util.List;
import java.util.Objects;

public class Subject {
    @JsonIgnore XnatSubjectdataI xnatSubjectdataI;
    @JsonProperty(required = true) private String id;
    @JsonProperty(value = "parent-id") private String parentId;
    private String label;
    private String xsiType;
    private List<Session> sessions;
    private List<Resource> resources;

    public Subject() {}

    public Subject(final XnatSubjectdataI xnatSubjectdataI, final String parentId, final UserI userI) {
        this(xnatSubjectdataI, XnatProjectdata.getXnatProjectdatasById(xnatSubjectdataI.getProject(), userI, false).getRootArchivePath());
    }

    public Subject(final XnatSubjectdataI xnatSubjectdataI, final String rootArchivePath) {
        this.xnatSubjectdataI = xnatSubjectdataI;

        this.id = xnatSubjectdataI.getId();
        this.label = xnatSubjectdataI.getLabel();
        this.xsiType = xnatSubjectdataI.getXSIType();
        this.parentId = xnatSubjectdataI.getProject();

        this.sessions = Lists.newArrayList();
        for (final XnatExperimentdataI xnatExperimentdataI : xnatSubjectdataI.getExperiments_experiment()) {
            if (xnatExperimentdataI instanceof XnatImagesessiondataI) {
                sessions.add(new Session((XnatImagesessiondataI) xnatExperimentdataI, rootArchivePath));
            }
        }

        this.resources = Lists.newArrayList();
        for (final XnatAbstractresourceI xnatAbstractresourceI : xnatSubjectdataI.getResources_resource()) {
            if (xnatAbstractresourceI instanceof XnatResourcecatalog) {
                resources.add(new Resource((XnatResourcecatalog) xnatAbstractresourceI, this.id, rootArchivePath));
            }
        }
    }

    public XnatSubjectdataI getXnatSubjectdataI() {
        return xnatSubjectdataI;
    }

    public void setXnatSubjectdataI(XnatSubjectdataI xnatSubjectdataI) {
        this.xnatSubjectdataI = xnatSubjectdataI;
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(final String label) {
        this.label = label;
    }

    public String getXsiType() {
        return xsiType;
    }

    public void setXsiType(final String xsiType) {
        this.xsiType = xsiType;
    }

    public List<Session> getSessions() {
        return sessions;
    }

    public void setSessions(final List<Session> sessions) {
        this.sessions = sessions;
    }

    public List<Resource> getResources() {
        return resources;
    }

    public void setResources(final List<Resource> resources) {
        this.resources = resources;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Subject that = (Subject) o;
        return Objects.equals(this.id, that.id) &&
                Objects.equals(this.parentId, that.parentId) &&
                Objects.equals(this.label, that.label) &&
                Objects.equals(this.xsiType, that.xsiType) &&
                Objects.equals(this.sessions, that.sessions) &&
                Objects.equals(this.resources, that.resources);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, parentId, label, xsiType, sessions, resources);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("parentId", parentId)
                .add("label", label)
                .add("xsiType", xsiType)
                .add("sessions", sessions)
                .add("resources", resources)
                .toString();
    }
}
