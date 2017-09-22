package org.nrg.containers.model.container.auto;

import com.google.auto.value.AutoValue;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@AutoValue
public abstract class ServiceTask {
    private static final Pattern exitStatusPattern = Pattern.compile("complete|shutdown|failed|rejected");

    public abstract String serviceId();
    public abstract String taskId();
    public abstract String nodeId();
    public abstract String status();
    @Nullable public abstract Date statusTime();
    @Nullable public abstract String containerId();
    @Nullable public abstract String message();
    @Nullable public abstract Integer exitCode();

    public boolean isExitStatus() {
        final Matcher exitStatusMatcher = exitStatusPattern.matcher(status());
        return exitStatusMatcher.matches();
    }

    public static Builder builder() {
        return new AutoValue_ServiceTask.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder serviceId(final String serviceId);
        public abstract Builder taskId(final String taskId);
        public abstract Builder containerId(final String containerId);
        public abstract Builder nodeId(final String nodeId);
        public abstract Builder status(final String status);
        public abstract Builder statusTime(final Date statusTime);
        public abstract Builder message(final String message);
        public abstract Builder exitCode(final Integer exitCode);

        public abstract ServiceTask build();
    }
}
