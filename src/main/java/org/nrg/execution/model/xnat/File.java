package org.nrg.execution.model.xnat;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.Objects;

public class File {
    private String name;
    private String path;
    private List<String> tags;
    private String format;
    private String content;
    private java.io.File file;

    public File() {}

    public File(final String name,
                final String path,
                final String tagsCsv,
                final String format,
                final String content,
                final java.io.File file) {
        this.name = name;
        this.path = path;
        this.tags = Lists.newArrayList(tagsCsv.split(","));
        this.format = format;
        this.content = content;
        this.file = file;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(final String path) {
        this.path = path;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(final List<String> tags) {
        this.tags = tags;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(final String format) {
        this.format = format;
    }

    public String getContent() {
        return content;
    }

    public void setContent(final String content) {
        this.content = content;
    }

    public java.io.File getFile() {
        return file;
    }

    public void setFile(final java.io.File file) {
        this.file = file;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final File that = (File) o;
        return Objects.equals(this.name, that.name) &&
                Objects.equals(this.path, that.path) &&
                Objects.equals(this.tags, that.tags) &&
                Objects.equals(this.format, that.format) &&
                Objects.equals(this.content, that.content) &&
                Objects.equals(this.file, that.file);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, path, tags, format, content, file);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("name", name)
                .add("path", path)
                .add("tags", tags)
                .add("format", format)
                .add("content", content)
                .add("file", file)
                .toString();
    }
}
