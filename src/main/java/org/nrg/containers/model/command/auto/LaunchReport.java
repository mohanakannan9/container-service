package org.nrg.containers.model.command.auto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
    @JsonProperty("status") public abstract String status();
    @JsonProperty("params") public abstract ImmutableMap<String, String> launchParams();
    @Nullable @JsonProperty("command-id") public abstract Long commandId();
    @Nullable @JsonProperty("wrapper-id") public abstract Long wrapperId();
    @Nullable @JsonProperty("wrapper-name") public abstract String wrapperName();

    @AutoValue
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public static abstract class Success extends LaunchReport {
        private final static String STATUS = "success";
        @JsonProperty("container-id") public abstract String containerId();

        @JsonCreator
        @SuppressWarnings("unused")
        static Success create(@JsonProperty("status") final String ignoredStatus,
                              @JsonProperty("params") final Map<String, String> launchParams,
                              @JsonProperty("container-id") final @Nonnull String containerId,
                              @JsonProperty("command-id") final Long commandId,
                              @JsonProperty("wrapper-id") final Long wrapperId,
                              @JsonProperty("wrapper-name") final String wrapperName) {
            return create(launchParams, containerId, commandId, wrapperId, wrapperName);
        }

        public static Success create(final Map<String, String> launchParams,
                                     final @Nonnull String containerId,
                                     final Long commandId,
                                     final Long wrapperId,
                                     final String wrapperName) {
            final ImmutableMap<String, String> launchParamsCopy =
                    launchParams == null ?
                            ImmutableMap.<String, String>of() :
                            ImmutableMap.copyOf(launchParams);
            return new AutoValue_LaunchReport_Success(STATUS, launchParamsCopy, commandId, wrapperId, wrapperName, containerId);
        }
    }

    @AutoValue
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public static abstract class Failure extends LaunchReport {
        private final static String STATUS = "failure";
        @JsonProperty("message") public abstract String message();

        @JsonCreator
        @SuppressWarnings("unused")
        static Failure create(@JsonProperty("status") final String ignoredStatus,
                              @JsonProperty("params") final Map<String, String> launchParams,
                              @JsonProperty("message") final @Nonnull String message,
                              @JsonProperty("command-id") final Long commandId,
                              @JsonProperty("wrapper-id") final Long wrapperId,
                              @JsonProperty("wrapper-name") final String wrapperName) {
            return create(launchParams, message, commandId, wrapperId, wrapperName);
        }

        public static Failure create(final Map<String, String> launchParams,
                                     final @Nonnull String message,
                                     final Long commandId,
                                     final Long wrapperId,
                                     final String wrapperName) {
            final ImmutableMap<String, String> launchParamsCopy =
                    launchParams == null ?
                            ImmutableMap.<String, String>of() :
                            ImmutableMap.copyOf(launchParams);
            return new AutoValue_LaunchReport_Failure(STATUS, launchParamsCopy, commandId, wrapperId, wrapperName, message);
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

            public Builder addReport(final @Nonnull LaunchReport report) {
                if (Success.class.isAssignableFrom(report.getClass())) {
                    return addSuccess((Success)report);
                } else {
                    return addFailure((Failure)report);
                }
            }

            public abstract BulkLaunchReport build();
        }
    }
}
