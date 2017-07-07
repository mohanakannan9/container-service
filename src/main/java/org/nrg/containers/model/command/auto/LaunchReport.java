package org.nrg.containers.model.command.auto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

public abstract class LaunchReport {
    @JsonProperty("params") public abstract ImmutableMap<String, String> launchParams();
    @Nullable @JsonProperty("command") public abstract String command();
    @Nullable @JsonProperty("wrapper") public abstract String wrapper();

    @AutoValue
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public static abstract class Success extends LaunchReport {
        @JsonProperty("container-id") public abstract String containerId();

        @JsonCreator
        public static Success create(@JsonProperty("params") final Map<String, String> launchParams,
                                     @JsonProperty("container-id") final @Nonnull String containerId,
                                     @JsonProperty("command") final String command,
                                     @JsonProperty("wrapper") final String wrapper) {
            final ImmutableMap<String, String> launchParamsCopy =
                    launchParams == null ?
                            ImmutableMap.<String, String>of() :
                            ImmutableMap.copyOf(launchParams);
            return new AutoValue_LaunchReport_Success(launchParamsCopy, command, wrapper, containerId);
        }
    }

    @AutoValue
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public static abstract class Failure extends LaunchReport {
        @JsonProperty("message") public abstract String message();

        @JsonCreator
        public static Failure create(@JsonProperty("params") final Map<String, String> launchParams,
                                     @JsonProperty("message") final @Nonnull String message,
                                     @JsonProperty("command") final String command,
                                     @JsonProperty("wrapper") final String wrapper) {
            final ImmutableMap<String, String> launchParamsCopy =
                    launchParams == null ?
                            ImmutableMap.<String, String>of() :
                            ImmutableMap.copyOf(launchParams);
            return new AutoValue_LaunchReport_Failure(launchParamsCopy, command, wrapper, message);
        }
    }

    @AutoValue
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public abstract static class BulkLaunchReport {
        @JsonProperty("successes") public abstract ImmutableList<Success> successes();
        @JsonProperty("failures") public abstract ImmutableList<Failure> failures();

        public static Builder builder() {
            return new AutoValue_LaunchReport_BulkLaunchReport.Builder();
        }

        @JsonCreator
        public static BulkLaunchReport create(@JsonProperty("successes") final List<Success> successes,
                                              @JsonProperty("failures") final List<Failure> failures) {
            return builder()
                    .successes(successes)
                    .failures(failures)
                    .build();
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
    }
}
