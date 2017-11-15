package org.nrg.containers.model.command.auto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import javax.annotation.Nullable;
import java.util.List;

@AutoValue
public abstract class ResolvedCommandMount {
    @JsonProperty("name") public abstract String name();
    @JsonProperty("writable") public abstract Boolean writable();
    @JsonProperty("container-path") public abstract String containerPath();
    @JsonProperty("input-files") public abstract ImmutableList<ResolvedCommandMountFiles> inputFiles();
    @JsonProperty("xnat-host-path") public abstract String xnatHostPath();
    @JsonProperty("container-host-path") public abstract String containerHostPath();

    public static Builder builder() {
        return new AutoValue_ResolvedCommandMount.Builder();
    }

    public abstract Builder toBuilder();

    @JsonIgnore
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
        @JsonProperty("from-wrapper-input") public abstract String fromWrapperInput();
        @JsonProperty("from-uri") @Nullable public abstract String fromUri();
        @JsonProperty("root-directory") @Nullable public abstract String rootDirectory();
        @JsonProperty("path") @Nullable public abstract String path();

        public static ResolvedCommandMountFiles create(final String fromWrapperInput,
                                                       final String fromUri,
                                                       final String rootDirectory,
                                                       final String path) {
            return new AutoValue_ResolvedCommandMount_ResolvedCommandMountFiles(fromWrapperInput, fromUri, rootDirectory, path);
        }
    }
}
