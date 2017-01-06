package org.nrg.containers.model.xnat;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import org.nrg.xdat.model.XnatAbstractresourceI;
import org.nrg.xdat.model.XnatImageassessordataI;
import org.nrg.xdat.om.XnatImageassessordata;
import org.nrg.xdat.om.XnatResourcecatalog;
import org.nrg.xft.security.UserI;
import org.nrg.xnat.helpers.uri.URIManager;
import org.nrg.xnat.helpers.uri.UriParserUtils;
import org.nrg.xnat.helpers.uri.archive.AssessorURII;
import org.nrg.xnat.helpers.uri.archive.SubjectURII;

import java.util.List;
import java.util.Objects;

@JsonInclude(Include.NON_NULL)
public class Assessor extends XnatModelObject {
    public static Type type = Type.ASSESSOR;
    @JsonIgnore private XnatImageassessordataI xnatImageassessordataI;
    private List<Resource> resources;

    public Assessor() {}

    public Assessor(final AssessorURII assessorURII) {
        this.xnatImageassessordataI = assessorURII.getAssessor();
        this.uri = ((URIManager.DataURIA) assessorURII).getUri();
        populateProperties(null);
    }

    public Assessor(final XnatImageassessordataI xnatImageassessordataI) {
        this(xnatImageassessordataI, null, null);
    }

    public Assessor(final XnatImageassessordataI xnatImageassessordataI, final String parentUri, final String rootArchivePath) {
        this.xnatImageassessordataI = xnatImageassessordataI;
        if (parentUri == null) {
            this.uri = UriParserUtils.getArchiveUri(xnatImageassessordataI);
        } else {
            this.uri = parentUri + "/assessors/" + id;
        }
        populateProperties(rootArchivePath);
    }

    private void populateProperties(final String rootArchivePath) {
        this.id = xnatImageassessordataI.getId();
        this.label = xnatImageassessordataI.getLabel();
        this.xsiType = xnatImageassessordataI.getXSIType();

        this.resources = Lists.newArrayList();
        for (final XnatAbstractresourceI xnatAbstractresourceI : xnatImageassessordataI.getResources_resource()) {
            if (xnatAbstractresourceI instanceof XnatResourcecatalog) {
                resources.add(new Resource((XnatResourcecatalog) xnatAbstractresourceI, this.uri, rootArchivePath));
            }
        }
    }

    public XnatImageassessordataI getXnatImageassessordataI() {
        return xnatImageassessordataI;
    }

    public void setXnatImageassessordataI(final XnatImageassessordataI xnatImageassessordataI) {
        this.xnatImageassessordataI = xnatImageassessordataI;
    }

    public List<Resource> getResources() {
        return resources;
    }

    public void setResources(final List<Resource> resources) {
        this.resources = resources;
    }

    public XnatImageassessordataI loadXnatImageassessordataI(final UserI userI) {
        xnatImageassessordataI = XnatImageassessordata.getXnatImageassessordatasById(id, userI, false);
        return xnatImageassessordataI;
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
        return Objects.equals(this.resources, that.resources);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), resources);
    }

    @Override
    public String toString() {
        return addParentPropertiesToString(MoreObjects.toStringHelper(this))
                .add("resources", resources)
                .toString();
    }
}
