package org.nrg.containers.model.xnat;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import org.nrg.xdat.model.XnatAbstractresourceI;
import org.nrg.xdat.model.XnatImageassessordataI;
import org.nrg.xdat.model.XnatImagescandataI;
import org.nrg.xdat.model.XnatImagesessiondataI;
import org.nrg.xdat.om.XnatExperimentdata;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.om.XnatResourcecatalog;
import org.nrg.xft.security.UserI;
import org.nrg.xnat.helpers.uri.URIManager;
import org.nrg.xnat.helpers.uri.UriParserUtils;
import org.nrg.xnat.helpers.uri.archive.ExperimentURII;

import java.util.List;
import java.util.Objects;

@JsonInclude(Include.NON_NULL)
public class Session extends XnatModelObject {
    public static Type type = Type.SESSION;
    @JsonIgnore private XnatImagesessiondataI xnatImagesessiondataI;
    private List<Scan> scans;
    private List<Assessor> assessors;
    private List<Resource> resources;

    public Session() {}

    public Session(final ExperimentURII experimentURII) {
        final XnatExperimentdata experiment = experimentURII.getExperiment();
        if (experiment != null && XnatImagesessiondata.class.isAssignableFrom(experiment.getClass())) {
            this.xnatImagesessiondataI = (XnatImagesessiondata) experiment;
            this.uri = ((URIManager.DataURIA) experimentURII).getUri();
            populateProperties(null);
        }
    }

    public Session(final XnatImagesessiondataI xnatImagesessiondataI) {
        this(xnatImagesessiondataI, null, null);
    }

    public Session(final XnatImagesessiondataI xnatImagesessiondataI, final String parentUri, final String rootArchivePath) {
        this.xnatImagesessiondataI = xnatImagesessiondataI;
        if (parentUri == null) {
            this.uri = UriParserUtils.getArchiveUri(xnatImagesessiondataI);
        } else {
            this.uri = parentUri + "/experiments/" + id;
        }
        populateProperties(rootArchivePath);
    }

    private void populateProperties(final String rootArchivePath) {
        this.id = xnatImagesessiondataI.getId();
        this.label = xnatImagesessiondataI.getLabel();
        this.xsiType = xnatImagesessiondataI.getXSIType();

        this.scans = Lists.newArrayList();
        for (final XnatImagescandataI xnatImagescandataI : xnatImagesessiondataI.getScans_scan()) {
            this.scans.add(new Scan(xnatImagescandataI, this.uri, rootArchivePath));
        }

        this.resources = Lists.newArrayList();
        for (final XnatAbstractresourceI xnatAbstractresourceI : xnatImagesessiondataI.getResources_resource()) {
            if (xnatAbstractresourceI instanceof XnatResourcecatalog) {
                resources.add(new Resource((XnatResourcecatalog) xnatAbstractresourceI, this.uri, rootArchivePath));
            }
        }

        this.assessors = Lists.newArrayList();
        for (final XnatImageassessordataI xnatImageassessordataI : xnatImagesessiondataI.getAssessors_assessor()) {
            assessors.add(new Assessor(xnatImageassessordataI, this.uri, rootArchivePath));
        }
    }

    public XnatImagesessiondataI loadXnatImagesessiondata(final UserI userI) {
        xnatImagesessiondataI = XnatImagesessiondata.getXnatImagesessiondatasById(id, userI, false);
        return xnatImagesessiondataI;
    }

    public XnatImagesessiondataI getXnatImagesessiondataI() {
        return xnatImagesessiondataI;
    }

    public void setXnatImagesessiondataI(final XnatImagesessiondataI xnatImagesessiondataI) {
        this.xnatImagesessiondataI = xnatImagesessiondataI;
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
        return Objects.equals(this.xnatImagesessiondataI, that.xnatImagesessiondataI) &&
                Objects.equals(this.scans, that.scans) &&
                Objects.equals(this.assessors, that.assessors) &&
                Objects.equals(this.resources, that.resources);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), xnatImagesessiondataI, scans, assessors, resources);
    }

    @Override
    public String toString() {
        return addParentPropertiesToString(MoreObjects.toStringHelper(this))
                .add("scans", scans)
                .add("assessors", assessors)
                .add("resources", resources)
                .toString();
    }
}
