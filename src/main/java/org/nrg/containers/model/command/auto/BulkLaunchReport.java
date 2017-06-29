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

@AutoValue
@JsonInclude(JsonInclude.Include.ALWAYS)
public abstract class BulkLaunchReport {
    @JsonProperty("successes") public abstract ImmutableList<Success> successes();
    @JsonProperty("failures") public abstract ImmutableList<Failure> failures();

    public static Builder builder() {
        return new AutoValue_BulkLaunchReport.Builder();
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

    @AutoValue
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public static abstract class Success extends Launch {
        @JsonProperty("params") public abstract ImmutableMap<String, String> launchParams();
        @Nullable @JsonProperty("command") public abstract String command();
        @Nullable @JsonProperty("wrapper") public abstract String wrapper();
        @JsonProperty("container-id") public abstract String containerId();
//        @JsonProperty("command") public abstract String command();
//        @JsonProperty("wrapper") public abstract String wrapper();
        
//        @JsonCreator
//        public static Success create(@JsonProperty("params") final Map<String, String> launchParams,
//                                     @JsonProperty("container-id") final @Nonnull String containerId) {
//            final ImmutableMap<String, String> launchParamsCopy =
//                    launchParams == null ?
//                            ImmutableMap.<String, String>of() :
//                            ImmutableMap.copyOf(launchParams);
//            return new AutoValue_BulkLaunchReport_Success(launchParamsCopy,"", "", containerId);
//        }

        @JsonCreator
        public static Success create(@JsonProperty("params") final Map<String, String> launchParams,
                                     @JsonProperty("container-id") final @Nonnull String containerId,
                                    @JsonProperty("command") final String command,
                                     @JsonProperty("wrapper") final String wrapper) {
            final ImmutableMap<String, String> launchParamsCopy =
                    launchParams == null ?
                            ImmutableMap.<String, String>of() :
                            ImmutableMap.copyOf(launchParams);
            return new AutoValue_BulkLaunchReport_Success(launchParamsCopy, command, wrapper, containerId);
        }
    }

    @AutoValue
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public static abstract class Failure extends Launch{
        @JsonProperty("params") public abstract ImmutableMap<String, String> launchParams();
        @Nullable @JsonProperty("command") public abstract String command();
        @Nullable @JsonProperty("wrapper") public abstract String wrapper();
        @JsonProperty("message") public abstract String message();

//        @JsonProperty("command") public abstract String command();
//        @JsonProperty("wrapper") public abstract String wrapper();

        @JsonCreator
        public static Failure create(@JsonProperty("params") final Map<String, String> launchParams,
                                     @JsonProperty("message") final @Nonnull String message,
                                     @JsonProperty("command") final String command,
                                     @JsonProperty("wrapper") final String wrapper) {
            final ImmutableMap<String, String> launchParamsCopy =
                    launchParams == null ?
                            ImmutableMap.<String, String>of() :
                            ImmutableMap.copyOf(launchParams);
            return new AutoValue_BulkLaunchReport_Failure(launchParamsCopy, command, wrapper, message);
        }

//        @JsonCreator
//        public static Failure create(@JsonProperty("params") final Map<String, String> launchParams,
//                                     @JsonProperty("message") final @Nonnull String message) {
//            final ImmutableMap<String, String> launchParamsCopy =
//                    launchParams == null ?
//                            ImmutableMap.<String, String>of() :
//                            ImmutableMap.copyOf(launchParams);
//            return new AutoValue_BulkLaunchReport_Failure(launchParamsCopy, "", "", message);
//        }

    }
    
    public static abstract class Launch {

    }
}
