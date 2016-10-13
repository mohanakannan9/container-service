package org.nrg.execution.model.xnat;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import org.nrg.xdat.bean.CatCatalogBean;
import org.nrg.xdat.om.XnatResourcecatalog;
import org.nrg.xnat.utils.CatalogUtils;

import java.util.List;
import java.util.Objects;

public class Resource {
    private Integer id;
    private String label;
    private String directory;
    private List<File> files;

    public Resource() {}

    public Resource(final XnatResourcecatalog xnatResourcecatalog, final String rootArchivePath) {
        this.id = xnatResourcecatalog.getXnatAbstractresourceId();
        this.label = xnatResourcecatalog.getLabel();

        final CatCatalogBean cat = xnatResourcecatalog.getCleanCatalog(rootArchivePath, true, null, null);
        this.directory = xnatResourcecatalog.getCatalogFile(rootArchivePath).getParent();

        final List<Object[]> entryDetails = CatalogUtils.getEntryDetails(cat, this.directory, null, xnatResourcecatalog, true, null, null, "absolutePath");
        this.files = Lists.newArrayList();
        for (final Object[] entry: entryDetails) {
            // See CatalogUtils.getEntryDetails to see where all these "entry" elements come from
            files.add(new File((String) entry[0], (String) entry[2], (String) entry[4], (String) entry[5], (String) entry[6], (java.io.File) entry[8]));
        }
    }

    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(final String label) {
        this.label = label;
    }

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(final String directory) {
        this.directory = directory;
    }

    public List<File> getFiles() {
        return files;
    }

    public void setFiles(final List<File> files) {
        this.files = files;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final Resource that = (Resource) o;
        return Objects.equals(this.id, that.id) &&
                Objects.equals(this.label, that.label) &&
                Objects.equals(this.directory, that.directory) &&
                Objects.equals(this.files, that.files);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, label, directory, files);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("label", label)
                .add("directory", directory)
                .add("files", files)
                .toString();
    }
}
