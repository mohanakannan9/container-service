package org.nrg.containers.model.xnat;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import org.nrg.xdat.model.XnatAbstractresourceI;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.XnatResourcecatalog;

import java.util.List;
import java.util.Objects;

public class Project extends XnatModelObject {
    @JsonIgnore private XnatProjectdata xnatProjectdata;
    private List<Resource> resources;
    private List<Subject> subjects;

    public Project() {}

    public Project(final XnatProjectdata xnatProjectdata) {
        this.xnatProjectdata = xnatProjectdata;

        this.id = xnatProjectdata.getId();
        this.label = xnatProjectdata.getName();
        this.xsiType = xnatProjectdata.getXSIType();

        this.subjects = Lists.newArrayList();
        // TODO how do I get subjects from an XnatProjecdata?

        this.resources = Lists.newArrayList();
        for (final XnatAbstractresourceI xnatAbstractresourceI: xnatProjectdata.getResources_resource()) {
            if (xnatAbstractresourceI instanceof XnatResourcecatalog) {
                resources.add(new Resource((XnatResourcecatalog) xnatAbstractresourceI, this.id, xnatProjectdata.getRootArchivePath()));
            }
        }
    }

    public XnatProjectdata getXnatProjectdata() {
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
