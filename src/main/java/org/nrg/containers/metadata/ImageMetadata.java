package org.nrg.containers.metadata;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.envers.Audited;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntity;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import java.util.List;
import java.util.Set;

@Audited
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "nrg")
public class ImageMetadata extends AbstractHibernateEntity {
    @JsonIgnore public final static String VERSION = "alpha";

    @JsonIgnore private String imageId;
    @JsonProperty("xnat.execution") private String execution;
    @JsonProperty("xnat.mounts.in") private Set<String> mountsIn;
    @JsonProperty("xnat.mounts.out") private Set<String> mountsOut;
    @JsonProperty("xnat.args") private List<ImageMetadataArg> args;

    @JsonProperty("xnat.metadata-version")
    public String version() {
        return VERSION;
    }

    public ImageMetadata() {}

    private ImageMetadata(final Builder builder) {
        this.imageId = builder.imageId;
        this.execution = builder.execution;
        this.mountsIn = builder.mountsIn;
        this.mountsOut = builder.mountsOut;
        this.args = builder.args;
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static Builder builder() {
        return new Builder();
    }

    @JsonGetter("image-id")
    public String getImageId() {
        return imageId;
    }

    @JsonIgnore
    public void setImageId(final String imageId) {
        this.imageId = imageId;
    }

    public String getExecution() {
        return execution;
    }

    public void setExecution(final String execution) {
        this.execution = execution;
    }

    @ElementCollection
    public Set<String> getMountsIn() {
        return mountsIn;
    }

    public void setMountsIn(final Set<String> mountsIn) {
        this.mountsIn = mountsIn;
    }

    @ElementCollection
    public Set<String> getMountsOut() {
        return mountsOut;
    }

    public void setMountsOut(final Set<String> mountsOut) {
        this.mountsOut = mountsOut;
    }

    @OneToMany
    public List<ImageMetadataArg> getArgs() {
        return args;
    }

    public void setArgs(final List<ImageMetadataArg> args) {
        this.args = args;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("version", VERSION)
            .add("id", getId())
            .add("imageId", imageId)
            .add("execution", execution)
            .add("args", args)
            .add("mounts.in", mountsIn)
            .add("mounts.out", mountsOut)
            .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ImageMetadata that = (ImageMetadata) o;

        return Objects.equal(this.getId(), that.getId()) &&
            Objects.equal(this.imageId, that.imageId) &&
            Objects.equal(this.execution, that.execution) &&
            Objects.equal(this.args, that.args) &&
            Objects.equal(this.mountsIn, that.mountsIn) &&
            Objects.equal(this.mountsOut, that.mountsOut);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(VERSION, getId(), imageId, execution, args, mountsIn, mountsOut);
    }

    public static class Builder {
        private String imageId;
        private String execution;
        private Set<String> mountsIn = Sets.newHashSet();
        private Set<String> mountsOut = Sets.newHashSet();
        private List<ImageMetadataArg> args = Lists.newArrayList();

        private Builder() {}

        public Builder(final ImageMetadata imageMetadata) {
            this.imageId = imageMetadata.imageId;
            this.execution = imageMetadata.execution;
            this.mountsIn = imageMetadata.mountsIn;
            this.mountsOut = imageMetadata.mountsOut;
            this.args = imageMetadata.args;
        }

        public ImageMetadata build() {
            return new ImageMetadata(this);
        }

        public Builder imageId(final String imageId) {
            this.imageId = imageId;return this;
        }

        public String imageId() {
            return imageId;
        }

        public Builder execution(final String execution) {
            this.execution = execution;
            return this;
        }

        public String execution() {
            return execution;
        }

        public Builder mountsIn(final Set<String> mountsIn) {
            this.mountsIn.addAll(mountsIn);
            return this;
        }

        public Builder mountsIn(final String... mountsIn) {
            final Set<String> inputSet = Sets.newHashSet(mountsIn);
            return mountsIn(inputSet);
        }

        public Builder mountIn(final String mountIn) {
            return mountsIn(mountIn);
        }

        public Set<String> mountsIn() {
            return mountsIn;
        }

        public Builder mountsOut(final Set<String> mountsOut) {
            this.mountsOut.addAll(mountsOut);
            return this;
        }

        public Builder mountsOut(final String... mountsOut) {
            final Set<String> inputSet = Sets.newHashSet(mountsOut);
            return mountsOut(inputSet);
        }

        public Builder mountOut(final String mountOut) {
            return mountsOut(mountOut);
        }

        public Set<String> mountsOut() {
            return mountsOut;
        }

        public Builder args(final List<ImageMetadataArg> args) {
            this.args.addAll(args);
            return this;
        }

        public Builder args(final ImageMetadataArg... args) {
            final List<ImageMetadataArg> inputArgList = Lists.newArrayList(args);
            return args(inputArgList);
        }

        public Builder arg(final ImageMetadataArg arg) {
            return args(arg);
        }

        public List<ImageMetadataArg> args() {
            return args;
        }
    }
}