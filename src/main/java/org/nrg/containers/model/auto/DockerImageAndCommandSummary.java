package org.nrg.containers.model.auto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.nrg.containers.model.Command;
import org.nrg.containers.model.XnatCommandWrapper;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

@AutoValue
public abstract class DockerImageAndCommandSummary {
    @JsonProperty("names") public abstract Set<String> imageNames();
    @Nullable @JsonProperty("server") public abstract String server();
    @JsonProperty("commands") public abstract List<CommandSummary> commandSummaries();

    @JsonCreator
    public static DockerImageAndCommandSummary create(@JsonProperty("names") final Set<String> imageNames,
                                                      @JsonProperty("server") final String server,
                                                      @JsonProperty("commands") final List<CommandSummary> commandSummaries) {
        return new AutoValue_DockerImageAndCommandSummary(
                imageNames == null ? Sets.<String>newHashSet() : imageNames,
                server,
                commandSummaries == null ? Lists.<CommandSummary>newArrayList() : commandSummaries);
    }

    public static DockerImageAndCommandSummary create(final Command command, final String server) {
        final DockerImageAndCommandSummary created = new AutoValue_DockerImageAndCommandSummary(Sets.<String>newHashSet(), server, Lists.<CommandSummary>newArrayList());

        created.addCommandSummary(command);

        return created;
    }

    public void addCommandSummary(final Command command) {
        imageNames().add(command.getImage());
        commandSummaries().add(CommandSummary.create(command));
    }

    @AutoValue
    static abstract class CommandSummary {
        @JsonProperty("id") public abstract long id();
        @Nullable @JsonProperty("name") public abstract String name();
        @Nullable @JsonProperty("version") public abstract String version();
        @JsonProperty("wrappers") public abstract List<XnatCommandWrapperSummary> xnatCommandWrapperSummaries();

        @JsonCreator
        static CommandSummary create(@JsonProperty("id") final long id,
                                     @JsonProperty("name") final String name,
                                     @JsonProperty("version") final String version,
                                     @JsonProperty("wrappers") final List<XnatCommandWrapperSummary> wrappers) {
            return new AutoValue_DockerImageAndCommandSummary_CommandSummary(id, name, version,
                    wrappers == null ? Lists.<XnatCommandWrapperSummary>newArrayList() : wrappers);
        }

        static CommandSummary create(final Command command) {
            final CommandSummary created =
                    new AutoValue_DockerImageAndCommandSummary_CommandSummary(
                            command.getId(),
                            command.getName(),
                            command.getVersion(),
                            Lists.<XnatCommandWrapperSummary>newArrayList());
            if (command.getXnatCommandWrappers() != null) {
                for (final XnatCommandWrapper xnatCommandWrapper : command.getXnatCommandWrappers()) {
                    created.xnatCommandWrapperSummaries().add(XnatCommandWrapperSummary.create(xnatCommandWrapper));
                }
            }
            return created;
        }
    }

    @AutoValue
    static abstract class XnatCommandWrapperSummary {
        @JsonProperty("id") public abstract long id();
        @Nullable @JsonProperty("name") public abstract String name();
        @Nullable @JsonProperty("contexts") public abstract Set<String> contexts();

        @JsonCreator
        static XnatCommandWrapperSummary create(@JsonProperty("id") final long id,
                                                @JsonProperty("name") final String name,
                                                @JsonProperty("contexts") final Set<String> contexts) {
            return new AutoValue_DockerImageAndCommandSummary_XnatCommandWrapperSummary(id, name, contexts);
        }

        static XnatCommandWrapperSummary create(final XnatCommandWrapper xnatCommandWrapper) {
            return new AutoValue_DockerImageAndCommandSummary_XnatCommandWrapperSummary(
                    xnatCommandWrapper.getId(),
                    xnatCommandWrapper.getName(),
                    //xnatCommandWrapper.getContexts()); TODO
                    Sets.newHashSet("I am not storing contexts for the wrappers yet"));
        }
    }
}
