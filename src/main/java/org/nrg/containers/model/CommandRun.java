package org.nrg.containers.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.swagger.annotations.ApiModelProperty;

import javax.annotation.Nullable;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Embeddable
public class CommandRun implements Serializable {
    @JsonProperty("command-line") private String commandLine;
    @JsonProperty("mounts") private List<CommandMount> mounts = Lists.newArrayList();
    @JsonProperty("environment-variables") private Map<String, String> environmentVariables = Maps.newHashMap();
    private Map<String, String> ports = Maps.newHashMap();

    @Nullable
    @ApiModelProperty("The command that will be executed in the container when the Command is launched.")
    public String getCommandLine() {
        return commandLine;
    }

    public void setCommandLine(final String commandLine) {
        this.commandLine = commandLine;
    }

    @Nullable
    @ElementCollection
    public List<CommandMount> getMounts() {
        return mounts;
    }

    public void setMounts(final List<CommandMount> mounts) {
        this.mounts = mounts == null ?
                Lists.<CommandMount>newArrayList() :
                mounts;
    }

    @Nullable
    @ElementCollection
    @ApiModelProperty("A Map of environment variables. Each kay is the environment variable's name, and each value is the environment variable's value." +
            "Both the names and values can use template strings, e.g. #variable-name#, which will be resolved into a value when the Command is launched.")
    public Map<String, String> getEnvironmentVariables() {
        return environmentVariables;
    }

    public void setEnvironmentVariables(final Map<String, String> environmentVariables) {
        this.environmentVariables = environmentVariables == null ?
                Maps.<String, String>newHashMap() :
                environmentVariables;
    }

    @ElementCollection
    public Map<String, String> getPorts() {
        return ports;
    }

    public void setPorts(final Map<String, String> ports) {
        this.ports = ports == null ?
                Maps.<String, String>newHashMap() :
                ports;

    }

    @Transient
    void update(final CommandRun other, final Boolean ignoreNull) {
        if (other == null) {
            return;
        }

        if (!(other.commandLine == null && ignoreNull)) {
            this.commandLine = other.commandLine;
        }

        if (this.mounts == null || this.mounts.isEmpty()) {
            // If we have no mounts, just take what we are given.
            // No need to go digging through lists when we're comparing to nothing.
            // Even if the "update" is empty too, no big deal.
            this.mounts = other.mounts;
        } else {
            final Map<String, CommandMount> mountsToUpdateMap = Maps.newHashMap();
            if (other.mounts != null) {
                for (final CommandMount otherMount : other.mounts) {
                    mountsToUpdateMap.put(otherMount.getName(), otherMount);
                }
            }

            // Update any mounts that already exist
            final Iterator<CommandMount> iterator = this.mounts.iterator();
            while (iterator.hasNext()) {
                final CommandMount thisMount = iterator.next();
                if (mountsToUpdateMap.containsKey(thisMount.getName())) {
                    // The mount we are looking at is in the list of mounts to update
                    // So update it and keep it in the list.
                    thisMount.update(mountsToUpdateMap.get(thisMount.getName()), ignoreNull);

                    // Now that we've updated that mount, remove it from the map of ones to update.
                    mountsToUpdateMap.remove(thisMount.getName());
                } else {
                    // The mount we are looking at is not in the list of mounts to update.
                    // If we ignore nulls, than that's fine, the mount can stay. Do nothing.
                    // If ignoreNull==false, then not seeing the mount means "remove it".
                    if (!ignoreNull) {
                        iterator.remove();
                    }
                }
            }

            // We have now updated/removed all mounts we already knew about.
            // Any mounts still in the toUpdateMap are ones we didn't know about before,
            // so we can just add them.
            this.mounts.addAll(mountsToUpdateMap.values());
        }

        this.ports = updateMap(this.ports, other.ports, ignoreNull);
        this.environmentVariables = updateMap(this.environmentVariables, other.environmentVariables, ignoreNull);
    }


    private Map<String, String> updateMap(final Map<String, String> existingMap, final Map<String, String> toUpdateMap, final Boolean ignoreNull) {
        if (existingMap == null || existingMap.isEmpty()) {
            // If we have nothing now, just accept all updates.
            return toUpdateMap;
        }

        if (toUpdateMap == null || toUpdateMap.isEmpty()) {
            if (ignoreNull) {
                // The updates are empty, and we were told to ignore that. So keep everything the same.
                return existingMap;
            } else {
                // The updates are empty, but we were instructed to treat "null" as meaningful.
                // Just drop all the existing stuff. Returning this empty map is a good way to do that, I guess?
                return toUpdateMap;
            }
        }

        // We have some existing stuff, and some updates. So we must check each element.
        for (final String existingKey : existingMap.keySet()) {
            if (toUpdateMap.containsKey(existingKey)) {
                // We were told to update this key.
                existingMap.put(existingKey, toUpdateMap.get(existingKey));
            } else if (!ignoreNull) {
                // This key isn't in the update, and we were told to not ignore that.
                existingMap.remove(existingKey);
            }
        }

        return existingMap;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final CommandRun that = (CommandRun) o;
        return Objects.equals(this.commandLine, that.commandLine) &&
                Objects.equals(this.mounts, that.mounts) &&
                Objects.equals(this.environmentVariables, that.environmentVariables) &&
                Objects.equals(this.ports, that.ports);
    }

    @Override
    public int hashCode() {
        return Objects.hash(commandLine, mounts, environmentVariables, ports);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("commandLine", commandLine)
                .add("mounts", mounts)
                .add("environmentVariables", environmentVariables)
                .add("ports", ports)
                .toString();
    }
}
