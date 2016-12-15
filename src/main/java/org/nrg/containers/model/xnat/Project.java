package org.nrg.containers.model.xnat;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import org.nrg.xdat.model.XnatAbstractresourceI;
import org.nrg.xdat.model.XnatProjectdataI;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.XnatResourcecatalog;
import org.nrg.xft.security.UserI;

import java.util.List;
import java.util.Objects;

@JsonInclude(Include.NON_NULL)
public class Project extends XnatModelObject {
    public static Type type = Type.PROJECT;
    @JsonIgnore private XnatProjectdataI xnatProjectdata;
    private List<Resource> resources;
    private List<Subject> subjects;

    public Project() {}

    public Project(final XnatProjectdata xnatProjectdataI) {
        this.xnatProjectdata = xnatProjectdataI;

        this.id = xnatProjectdataI.getId();
        this.label = xnatProjectdataI.getName();
        this.xsiType = xnatProjectdataI.getXSIType();
        this.uri = "/projects/" + id;

        this.subjects = Lists.newArrayList();
        // TODO how do I get subjects from an XnatProjectdata?

        this.resources = Lists.newArrayList();
        for (final XnatAbstractresourceI xnatAbstractresourceI: xnatProjectdataI.getResources_resource()) {
            if (xnatAbstractresourceI instanceof XnatResourcecatalog) {
                resources.add(new Resource((XnatResourcecatalog) xnatAbstractresourceI, this.id, this.uri, xnatProjectdataI.getRootArchivePath()));
            }
        }
    }

    public XnatProjectdataI loadXnatProjectdata(final UserI userI) {
        xnatProjectdata = XnatProjectdata.getXnatProjectdatasById(id, userI, false);
        return xnatProjectdata;
    }

    public XnatProjectdataI getXnatProjectdata() {
        return xnatProjectdata;
    }

    public void setXnatProjectdata(final XnatProjectdata xnatProjectdata) {
        this.xnatProjectdata = xnatProjectdata;
    }

    public List<Resource> getResources() {
        return resources;
    }

    public void setResources(final List<Resource> resources) {
        this.resources = resources;
    }

    public List<Subject> getSubjects() {
        return subjects;
    }

    public void setSubjects(final List<Subject> subjects) {
        this.subjects = subjects;
    }

    public Type getType() {
        return type;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        final Project that = (Project) o;
        return Objects.equals(this.xnatProjectdata, that.xnatProjectdata) &&
                Objects.equals(this.resources, that.resources) &&
                Objects.equals(this.subjects, that.subjects);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), xnatProjectdata, resources, subjects);
    }


    @Override
    public String toString() {
        return addParentPropertiesToString(MoreObjects.toStringHelper(this))
                .add("resources", resources)
                .add("subjects", subjects)
                .toString();
    }
}
