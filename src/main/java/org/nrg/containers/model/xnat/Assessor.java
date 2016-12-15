package org.nrg.containers.model.xnat;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import org.nrg.xdat.model.XnatAbstractresourceI;
import org.nrg.xdat.model.XnatImageassessordataI;
import org.nrg.xdat.model.XnatImagescandataI;
import org.nrg.xdat.om.XnatImageassessordata;
import org.nrg.xdat.om.XnatResourcecatalog;
import org.nrg.xft.security.UserI;

import java.util.List;
import java.util.Objects;

@JsonInclude(Include.NON_NULL)
public class Assessor extends XnatModelObject {
    public static Type type = Type.ASSESSOR;
    @JsonIgnore private XnatImageassessordataI xnatImageassessordata;
    @JsonProperty(value = "parent-id") private String parentId;
    private List<Resource> resources;

    public Assessor() {}

    public Assessor(final XnatImageassessordataI xnatImageassessordata, final XnatModelObject parent) {
        this(xnatImageassessordata, parent.getId(), parent.getUri());
    }

    public Assessor(final XnatImageassessordataI xnatImageassessordata, final String parentId, final String parentUri) {
        this(xnatImageassessordata, parentId, parentUri, null);
    }

    public Assessor(final XnatImageassessordataI xnatImageassessordata, final String parentId, final String parentUri, final String rootArchivePath) {
        this.id = xnatImageassessordata.getId();
        this.label = xnatImageassessordata.getLabel();
        this.xsiType = xnatImageassessordata.getXSIType();
        this.uri = parentUri + "/assessors/" + id;

        this.parentId = parentId;

        this.resources = Lists.newArrayList();
        for (final XnatAbstractresourceI xnatAbstractresourceI : xnatImageassessordata.getResources_resource()) {
            if (xnatAbstractresourceI instanceof XnatResourcecatalog) {
                resources.add(new Resource((XnatResourcecatalog) xnatAbstractresourceI, this.id, this.uri, rootArchivePath));
            }
        }
    }

    public XnatImageassessordataI getXnatImageassessordata() {
        return xnatImageassessordata;
    }

    public void setXnatImageassessordata(final XnatImageassessordataI xnatImageassessordata) {
        this.xnatImageassessordata = xnatImageassessordata;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public List<Resource> getResources() {
        return resources;
    }

    public void setResources(final List<Resource> resources) {
        this.resources = resources;
    }

    public XnatImageassessordataI loadXnatImageassessordataI(final UserI userI) {
        xnatImageassessordata = XnatImageassessordata.getXnatImageassessordatasById(id, userI, false);
        return xnatImageassessordata;
    }

    @Override
    public String getUri() {
        return null;
    }

    public Type getType() {
        return type;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        final Assessor that = (Assessor) o;
        return Objects.equals(this.parentId, that.parentId) &&
                Objects.equals(this.resources, that.resources);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), parentId, resources);
    }

    @Override
    public String toString() {
        return addParentPropertiesToString(MoreObjects.toStringHelper(this))
                .add("parentId", parentId)
                .add("resources", resources)
                .toString();
    }
}
