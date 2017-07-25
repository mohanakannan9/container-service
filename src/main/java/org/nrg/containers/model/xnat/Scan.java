package org.nrg.containers.model.xnat;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import org.nrg.xdat.model.XnatAbstractresourceI;
import org.nrg.xdat.model.XnatImagescandataI;
import org.nrg.xdat.om.XnatImagescandata;
import org.nrg.xdat.om.XnatResourcecatalog;
import org.nrg.xft.security.UserI;
import org.nrg.xnat.helpers.uri.URIManager;
import org.nrg.xnat.helpers.uri.UriParserUtils;
import org.nrg.xnat.helpers.uri.archive.ScanURII;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

@JsonInclude(Include.NON_NULL)
public class Scan extends XnatModelObject {
    @JsonIgnore private XnatImagescandataI xnatImagescandataI;
    @JsonProperty("integer-id") private Integer integerId;
    @JsonProperty("scan-type") private String scanType;
    private List<Resource> resources;
    private String directory;

    public Scan() {}

    public Scan(final ScanURII scanURII) {
        this.xnatImagescandataI = scanURII.getScan();
        this.uri = ((URIManager.ArchiveItemURI)scanURII).getUri();
        populateProperties(null);
    }

    public Scan(final XnatImagescandataI xnatImagescandataI, final String parentUri, final String rootArchivePath) {
        this.xnatImagescandataI = xnatImagescandataI;
        if (parentUri == null) {
            this.uri = UriParserUtils.getArchiveUri(xnatImagescandataI);
        } else {
            this.uri = parentUri + "/scans/" + xnatImagescandataI.getId();
        }
        populateProperties(rootArchivePath);
    }

    private void populateProperties(final String rootArchivePath) {
        this.integerId = xnatImagescandataI.getXnatImagescandataId();
        this.id = xnatImagescandataI.getId();
        this.xsiType = xnatImagescandataI.getXSIType();
        this.scanType = xnatImagescandataI.getType();
        this.label = String.format("%s – %s", this.id, this.scanType);
        if (this.xnatImagescandataI instanceof XnatImagescandata) {
            this.directory = ((XnatImagescandata) xnatImagescandataI).deriveScanDir();
        }

        this.resources = Lists.newArrayList();
        for (final XnatAbstractresourceI xnatAbstractresourceI : this.xnatImagescandataI.getFile()) {
            if (xnatAbstractresourceI instanceof XnatResourcecatalog) {
                resources.add(new Resource((XnatResourcecatalog) xnatAbstractresourceI, this.uri, rootArchivePath));
            }
        }

    }

    public static Function<URIManager.ArchiveItemURI, Scan> uriToModelObject() {
        return new Function<URIManager.ArchiveItemURI, Scan>() {
            @Nullable
            @Override
            public Scan apply(@Nullable URIManager.ArchiveItemURI uri) {
                if (uri != null &&
                        ScanURII.class.isAssignableFrom(uri.getClass())) {
                    return new Scan((ScanURII) uri);
                }

                return null;
            }
        };
    }

    public static Function<String, Scan> idToModelObject(final UserI userI) {
        return null;
    }

    public Project getProject(final UserI userI) {
        loadXnatImagescandataI(userI);
        return new Project(xnatImagescandataI.getProject(), userI);
    }

    public Session getSession(final UserI userI) {
        loadXnatImagescandataI(userI);
        return new Session(xnatImagescandataI.getImageSessionId(), userI);
    }

    public void loadXnatImagescandataI(final UserI userI) {
        if (xnatImagescandataI == null) {
            xnatImagescandataI = XnatImagescandata.getXnatImagescandatasByXnatImagescandataId(integerId, userI, false);
        }
    }

    public XnatImagescandataI getXnatImagescandataI() {
        return xnatImagescandataI;
    }

    public void setXnatImagescandataI(final XnatImagescandataI xnatImagescandataI) {
        this.xnatImagescandataI = xnatImagescandataI;
    }

    public String getScanType() {
        return scanType;
    }

    public void setScanType(final String scanType) {
        this.scanType = scanType;
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
        final Scan that = (Scan) o;
        return Objects.equals(this.xnatImagescandataI, that.xnatImagescandataI) &&
                Objects.equals(this.integerId, that.integerId) &&
                Objects.equals(this.scanType, that.scanType) &&
                Objects.equals(this.resources, that.resources) &&
                Objects.equals(this.directory, that.directory);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), xnatImagescandataI, integerId, scanType, resources, directory);
    }

    @Override
    public String toString() {
        return addParentPropertiesToString(MoreObjects.toStringHelper(this))
                .add("integerId", integerId)
                .add("scanType", scanType)
                .add("resources", resources)
                .add("directory", directory)
                .toString();
    }
}
