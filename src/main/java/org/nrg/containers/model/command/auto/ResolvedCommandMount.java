package org.nrg.containers.model.command.auto;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import javax.annotation.Nullable;
import java.util.List;

@AutoValue
public abstract class ResolvedCommandMount {
    public abstract String name();
    public abstract Boolean writable();
    public abstract String containerPath();
    public abstract ImmutableList<ResolvedCommandMountFiles> inputFiles();
    public abstract String xnatHostPath();
    public abstract String containerHostPath();

    public static Builder builder() {
        return new AutoValue_ResolvedCommandMount.Builder();
    }

    public abstract Builder toBuilder();

    public String toBindMountString() {
        return containerHostPath() + ":" + containerPath() + (writable() ? "" : ":ro");
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder name(String name);
        public abstract Builder writable(Boolean writable);
        public abstract Builder xnatHostPath(String xnatHostPath);
        public abstract Builder containerHostPath(String containerHostPath);
        public abstract Builder containerPath(String containerPath);
        public abstract Builder inputFiles(List<ResolvedCommandMountFiles> inputFiles);
        public abstract ImmutableList.Builder<ResolvedCommandMountFiles> inputFilesBuilder();
        public Builder addInputFiles(final ResolvedCommandMountFiles inputFiles) {
            inputFilesBuilder().add(inputFiles);
            return this;
        }

        public abstract ResolvedCommandMount build();
    }

    @AutoValue
    public abstract static class ResolvedCommandMountFiles {
        public abstract String fromWrapperInput();
        @Nullable
        public abstract String fromUri();
        @Nullable public abstract String rootDirectory();
        @Nullable public abstract String path();

        public static ResolvedCommandMountFiles create(final String fromWrapperInput,
                                                       final String fromUri,
                                                       final String rootDirectory,
                                                       final String path) {
            return new AutoValue_ResolvedCommandMount_ResolvedCommandMountFiles(fromWrapperInput, fromUri, rootDirectory, path);
        }
    }
}
