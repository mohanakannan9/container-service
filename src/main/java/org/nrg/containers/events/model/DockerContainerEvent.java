package org.nrg.containers.events.model;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@AutoValue
public abstract class DockerContainerEvent implements ContainerEvent {
    private static final Pattern exitStatusPattern = Pattern.compile("kill|die|oom");

    public abstract String status();
    public abstract String containerId();
    public abstract Date time();
    @Nullable public abstract Long timeNano();
    public abstract ImmutableMap<String, String> attributes();

    public boolean isExitStatus() {
        final Matcher exitStatusMatcher = exitStatusPattern.matcher(status());
        return exitStatusMatcher.matches();
    }

    public String exitCode() {
        return isExitStatus() ?
                (attributes().containsKey("exitCode") ?
                        attributes().get("exitCode") :
                        "") :
                null;
    }

    public static DockerContainerEvent create(final String status,
                                              final String containerId,
                                              final Date time,
                                              final Long timeNano,
                                              final Map<String, String> attributes) {
        final ImmutableMap<String, String> attributesCopy = attributes == null ?
                ImmutableMap.<String, String>of() :
                ImmutableMap.copyOf(attributes);
        return new AutoValue_DockerContainerEvent(status, containerId, time, timeNano, attributesCopy);
    }
}
