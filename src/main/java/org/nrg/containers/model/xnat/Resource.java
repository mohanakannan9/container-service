package org.nrg.containers.model.xnat;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import org.nrg.xdat.bean.CatCatalogBean;
import org.nrg.xdat.model.XnatResourcecatalogI;
import org.nrg.xdat.om.XnatResourcecatalog;
import org.nrg.xft.security.UserI;
import org.nrg.xnat.utils.CatalogUtils;

import java.io.File;
import java.util.List;
import java.util.Objects;

public class Resource extends XnatModelObject {
    @JsonIgnore private XnatResourcecatalogI xnatResourcecatalog;
    @JsonProperty(value = "parent-id") private String parentId;
    private String directory;
    private List<XnatFile> files;

    public Resource() {}

    public Resource(final XnatResourcecatalog xnatResourcecatalogI, final String parentId, final String rootArchivePath) {
        this.xnatResourcecatalog = xnatResourcecatalogI;

        this.id = xnatResourcecatalogI.getXnatAbstractresourceId() != null ? xnatResourcecatalogI.getXnatAbstractresourceId().toString() : "";
        this.label = xnatResourcecatalogI.getLabel();
        this.xsiType = xnatResourcecatalogI.getXSIType();

        this.parentId = parentId;

        final CatCatalogBean cat = xnatResourcecatalogI.getCleanCatalog(rootArchivePath, true, null, null);
        this.directory = xnatResourcecatalogI.getCatalogFile(rootArchivePath).getParent();

        final List<Object[]> entryDetails = CatalogUtils.getEntryDetails(cat, this.directory, null, xnatResourcecatalogI, true, null, null, "absolutePath");
        this.files = Lists.newArrayList();
        for (final Object[] entry: entryDetails) {
            // See CatalogUtils.getEntryDetails to see where all these "entry" elements come from
            files.add(new XnatFile((String) entry[0], (String) entry[2], (String) entry[4], (String) entry[5], (String) entry[6], (File) entry[8]));
        }
    }

    public XnatResourcecatalogI loadXnatResourcecatalog(UserI userI) {
        xnatResourcecatalog = XnatResourcecatalog.getXnatResourcecatalogsByXnatAbstractresourceId(id, userI, false);
        return xnatResourcecatalog;
    }

    public XnatResourcecatalogI getXnatResourcecatalog() {
        return xnatResourcecatalog;
    }

    public void setXnatResourcecatalog(final XnatResourcecatalog xnatResourcecatalogI) {
        this.xnatResourcecatalog = xnatResourcecatalogI;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(final String parentId) {
        this.parentId = parentId;
    }

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(final String directory) {
        this.directory = directory;
    }

    public List<XnatFile> getFiles() {
        return files;
    }

    public void setFiles(final List<XnatFile> files) {
        this.files = files;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        final Resource that = (Resource) o;
        return Objects.equals(this.xnatResourcecatalog, that.xnatResourcecatalog) &&
                Objects.equals(this.parentId, that.parentId) &&
                Objects.equals(this.directory, that.directory) &&
                Objects.equals(this.files, that.files);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), xnatResourcecatalog, parentId, directory, files);
    }

    @Override
    public String toString() {
        return addParentPropertiesToString(MoreObjects.toStringHelper(this))
                .add("parentId", parentId)
                .add("directory", directory)
                .add("files", files)
                .toString();
    }
}
