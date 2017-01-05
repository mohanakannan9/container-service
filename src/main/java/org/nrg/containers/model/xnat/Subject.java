package org.nrg.containers.model.xnat;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import org.nrg.xdat.model.XnatAbstractresourceI;
import org.nrg.xdat.model.XnatExperimentdataI;
import org.nrg.xdat.model.XnatImagesessiondataI;
import org.nrg.xdat.model.XnatSubjectdataI;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.XnatResourcecatalog;
import org.nrg.xdat.om.XnatSubjectdata;
import org.nrg.xft.security.UserI;

import java.util.List;
import java.util.Objects;

@JsonInclude(Include.NON_NULL)
public class Subject extends XnatModelObject {
    public static Type type = Type.SUBJECT;

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), xnatSubjectdata, parentId, sessions, resources);
    }

    @JsonIgnore XnatSubjectdataI xnatSubjectdata;
    @JsonProperty(value = "parent-id") private String parentId;
    private List<Session> sessions;
    private List<Resource> resources;

    public Subject() {}

    public Subject(final XnatSubjectdataI xnatSubjectdataI) {
        this(xnatSubjectdataI, null);
    }

    public Subject(final XnatSubjectdataI xnatSubjectdataI, final String rootArchivePath) {
        this.xnatSubjectdata = xnatSubjectdataI;

        this.id = xnatSubjectdataI.getId();
        this.label = xnatSubjectdataI.getLabel();
        this.xsiType = xnatSubjectdataI.getXSIType();
        this.parentId = xnatSubjectdataI.getProject();
        this.uri = "/subjects/" + id;

        this.sessions = Lists.newArrayList();
        for (final XnatExperimentdataI xnatExperimentdataI : xnatSubjectdataI.getExperiments_experiment()) {
            if (xnatExperimentdataI instanceof XnatImagesessiondataI) {
                sessions.add(new Session((XnatImagesessiondataI) xnatExperimentdataI, rootArchivePath));
            }
        }

        this.resources = Lists.newArrayList();
        for (final XnatAbstractresourceI xnatAbstractresourceI : xnatSubjectdataI.getResources_resource()) {
            if (xnatAbstractresourceI instanceof XnatResourcecatalog) {
                resources.add(new Resource((XnatResourcecatalog) xnatAbstractresourceI, this.id, this.uri, rootArchivePath));
            }
        }
    }

    public XnatSubjectdataI loadXnatSubjectdataI(UserI userI) {
        xnatSubjectdata = XnatSubjectdata.getXnatSubjectdatasById(id, userI, false);
        return xnatSubjectdata;
    }

    public XnatSubjectdataI getXnatSubjectdata() {
        return xnatSubjectdata;
    }

    public void setXnatSubjectdata(final XnatSubjectdataI xnatSubjectdata) {
        this.xnatSubjectdata = xnatSubjectdata;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(final String parentId) {
        this.parentId = parentId;
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

    public Type getType() {
        return type;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        final Subject that = (Subject) o;
        return Objects.equals(this.xnatSubjectdata, that.xnatSubjectdata) &&
                Objects.equals(this.parentId, that.parentId) &&
                Objects.equals(this.sessions, that.sessions) &&
                Objects.equals(this.resources, that.resources);
    }

    @Override
    public String toString() {
        return addParentPropertiesToString(MoreObjects.toStringHelper(this))
//                .add("parentId", parentId)
                .add("sessions", sessions)
                .add("resources", resources)
                .toString();
    }
}
