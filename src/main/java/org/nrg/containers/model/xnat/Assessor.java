package org.nrg.containers.model.xnat;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.nrg.xdat.model.XnatAbstractresourceI;
import org.nrg.xdat.model.XnatImageassessordataI;
import org.nrg.xdat.om.XnatExperimentdata;
import org.nrg.xdat.om.XnatImageassessordata;
import org.nrg.xdat.om.XnatResourcecatalog;
import org.nrg.xdat.om.base.BaseXnatExperimentdata.UnknownPrimaryProjectException;
import org.nrg.xft.security.UserI;
import org.nrg.xnat.exceptions.InvalidArchiveStructure;
import org.nrg.xnat.helpers.uri.URIManager;
import org.nrg.xnat.helpers.uri.UriParserUtils;
import org.nrg.xnat.helpers.uri.archive.AssessorURII;
import org.nrg.xnat.helpers.uri.archive.SubjectURII;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

@JsonInclude(Include.NON_NULL)
public class Assessor extends XnatModelObject {
    @JsonIgnore private XnatImageassessordataI xnatImageassessordataI;
    private List<Resource> resources;
    private String directory;

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
            this.uri = parentUri + "/assessors/" + xnatImageassessordataI.getId();
        }
        populateProperties(rootArchivePath);
    }

    private void populateProperties(final String rootArchivePath) {
        this.id = xnatImageassessordataI.getId();
        this.label = xnatImageassessordataI.getLabel();
        this.xsiType = xnatImageassessordataI.getXSIType();
        this.directory = null;
        try {
            // TODO
            // I don't know if this will return the correct directory for the assessor,
            // or if it will give us the directory of the parent session
            this.directory = ((XnatImageassessordata) xnatImageassessordataI).getCurrentArchiveFolder();
        } catch (UnknownPrimaryProjectException | InvalidArchiveStructure e) {
            // ignored, I guess?
        }

        this.resources = Lists.newArrayList();
        for (final XnatAbstractresourceI xnatAbstractresourceI : xnatImageassessordataI.getResources_resource()) {
            if (xnatAbstractresourceI instanceof XnatResourcecatalog) {
                resources.add(new Resource((XnatResourcecatalog) xnatAbstractresourceI, this.uri, rootArchivePath));
            }
        }
    }

    public static Function<URIManager.ArchiveItemURI, Assessor> uriToModelObject() {
        return new Function<URIManager.ArchiveItemURI, Assessor>() {
            @Nullable
            @Override
            public Assessor apply(@Nullable URIManager.ArchiveItemURI uri) {
                if (uri != null &&
                        AssessorURII.class.isAssignableFrom(uri.getClass())) {
                    final XnatImageassessordata assessor = ((AssessorURII) uri).getAssessor();
                    if (assessor != null &&
                            XnatImageassessordata.class.isAssignableFrom(assessor.getClass())) {
                        return new Assessor((AssessorURII) uri);
                    }
                } else if (uri != null &&
                        ExptURI.class.isAssignableFrom(uri.getClass())) {
                    final XnatExperimentdata expt = ((ExptURI) uri).getExperiment();
                    if (expt != null &&
                            XnatImageassessordata.class.isAssignableFrom(expt.getClass())) {
                        return new Assessor((XnatImageassessordata) expt);
                    }
                }

                return null;
            }
        };
    }

    public static Function<String, Assessor> idToModelObject(final UserI userI) {
        return new Function<String, Assessor>() {
            @Nullable
            @Override
            public Assessor apply(@Nullable String s) {
                if (StringUtils.isBlank(s)) {
                    return null;
                }
                final XnatImageassessordata xnatImageassessordata =
                        XnatImageassessordata.getXnatImageassessordatasById(s, userI, true);
                if (xnatImageassessordata != null) {
                    return new Assessor(xnatImageassessordata);
                }
                return null;
            }
        };
    }

    public Project getProject(final UserI userI) {
        loadXnatImageassessordataI(userI);
        return new Project(xnatImageassessordataI.getProject(), userI);
    }

    public void loadXnatImageassessordataI(final UserI userI) {
        if (xnatImageassessordataI == null) {
            xnatImageassessordataI = XnatImageassessordata.getXnatImageassessordatasById(id, userI, false);
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

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(final String directory) {
        this.directory = directory;
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
