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
import org.nrg.xdat.om.XnatResourcecatalog;
import org.nrg.xdat.om.XnatSubjectdata;
import org.nrg.xft.security.UserI;
import org.nrg.xnat.helpers.uri.URIManager;
import org.nrg.xnat.helpers.uri.UriParserUtils;
import org.nrg.xnat.helpers.uri.archive.SubjectURII;

import java.util.List;
import java.util.Objects;

@JsonInclude(Include.NON_NULL)
public class Subject extends XnatModelObject {
    public static Type type = Type.SUBJECT;

    @JsonIgnore private XnatSubjectdataI xnatSubjectdataI;
    private List<Session> sessions;
    private List<Resource> resources;

    public Subject() {}

    public Subject(final SubjectURII subjectURII) {
        this.xnatSubjectdataI = subjectURII.getSubject();
        this.uri = ((URIManager.DataURIA) subjectURII).getUri();
        populateProperties(null);
    }

    public Subject(final XnatSubjectdataI xnatSubjectdataI) {
        this(xnatSubjectdataI, null, null);
    }

    public Subject(final XnatSubjectdataI xnatSubjectdataI, final String parentUri, final String rootArchivePath) {
        this.xnatSubjectdataI = xnatSubjectdataI;
        if (parentUri == null) {
            this.uri = UriParserUtils.getArchiveUri(xnatSubjectdataI);
        } else {
            this.uri = parentUri + "/subjects/" + id;
        }
        populateProperties(rootArchivePath);
    }

    private void populateProperties(final String rootArchivePath) {
        this.id = xnatSubjectdataI.getId();
        this.label = xnatSubjectdataI.getLabel();
        this.xsiType = xnatSubjectdataI.getXSIType();

        this.sessions = Lists.newArrayList();
        for (final XnatExperimentdataI xnatExperimentdataI : xnatSubjectdataI.getExperiments_experiment()) {
            if (xnatExperimentdataI instanceof XnatImagesessiondataI) {
                sessions.add(new Session((XnatImagesessiondataI) xnatExperimentdataI, this.uri, rootArchivePath));
            }
        }

        this.resources = Lists.newArrayList();
        for (final XnatAbstractresourceI xnatAbstractresourceI : xnatSubjectdataI.getResources_resource()) {
            if (xnatAbstractresourceI instanceof XnatResourcecatalog) {
                resources.add(new Resource((XnatResourcecatalog) xnatAbstractresourceI, this.uri, rootArchivePath));
            }
        }
    }

    public XnatSubjectdataI loadXnatSubjectdataI(final UserI userI) {
        xnatSubjectdataI = XnatSubjectdata.getXnatSubjectdatasById(id, userI, false);
        return xnatSubjectdataI;
    }

    public XnatSubjectdataI getXnatSubjectdataI() {
        return xnatSubjectdataI;
    }

    public void setXnatSubjectdataI(final XnatSubjectdataI xnatSubjectdataI) {
        this.xnatSubjectdataI = xnatSubjectdataI;
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
        return Objects.equals(this.xnatSubjectdataI, that.xnatSubjectdataI) &&
                Objects.equals(this.sessions, that.sessions) &&
                Objects.equals(this.resources, that.resources);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), xnatSubjectdataI, sessions, resources);
    }

    @Override
    public String toString() {
        return addParentPropertiesToString(MoreObjects.toStringHelper(this))
                .add("sessions", sessions)
                .add("resources", resources)
                .toString();
    }
}
