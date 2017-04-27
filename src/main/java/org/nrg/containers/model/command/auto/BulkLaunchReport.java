package org.nrg.containers.model.command.auto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;

@AutoValue
public abstract class BulkLaunchReport {
    @JsonProperty("successes") public abstract ImmutableList<Success> successes();
    @JsonProperty("failures") public abstract ImmutableList<Failure> failures();

    public static Builder builder() {
        return new AutoValue_BulkLaunchReport.Builder();
    }

    @AutoValue.Builder
    public static abstract class Builder {
        public abstract Builder successes(List<Success> successes);
        abstract ImmutableList.Builder<Success> successesBuilder();
        public Builder addSuccess(final @Nonnull Success success) {
            successesBuilder().add(success);
            return this;
        }
        public abstract Builder failures(List<Failure> failures);
        abstract ImmutableList.Builder<Failure> failuresBuilder();
        public Builder addFailure(final @Nonnull Failure failure) {
            failuresBuilder().add(failure);
            return this;
        }

        public abstract BulkLaunchReport build();
    }

    @AutoValue
    public static abstract class Success {
        @JsonProperty("params") public abstract ImmutableMap<String, String> launchParams();
        @JsonProperty("container-id") public abstract String containerId();

        public static Success create(final Map<String, String> launchParams,
                                     final @Nonnull String containerId) {
            final ImmutableMap<String, String> launchParamsCopy =
                    launchParams == null ?
                            ImmutableMap.<String, String>of() :
                            ImmutableMap.copyOf(launchParams);
            return new AutoValue_BulkLaunchReport_Success(launchParamsCopy, containerId);
        }
    }

    @AutoValue
    public static abstract class Failure {
        @JsonProperty("params") public abstract ImmutableMap<String, String> launchParams();
        @JsonProperty("container-id") public abstract String containerId();

        public static Failure create(final Map<String, String> launchParams,
                                     final @Nonnull String message) {
            final ImmutableMap<String, String> launchParamsCopy =
                    launchParams == null ?
                            ImmutableMap.<String, String>of() :
                            ImmutableMap.copyOf(launchParams);
            return new AutoValue_BulkLaunchReport_Failure(launchParamsCopy, message);
        }

    }
}
