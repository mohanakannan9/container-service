package org.nrg.containers.rest.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.nrg.containers.model.Command;
import org.nrg.containers.model.XnatCommandWrapper;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public class DockerImageAndCommandSummary {
    @JsonProperty("names") private Set<String> imageNames;
    private String server;
    @JsonProperty("commands") private List<CommandSummary> commandSummaries;

    @JsonCreator
    public DockerImageAndCommandSummary(@JsonProperty("names") final Set<String> imageNames,
                                        @JsonProperty("server") final String server,
                                        @JsonProperty("commands") final List<CommandSummary> commandSummaries) {
        this.imageNames = imageNames;
        this.server = server;
        this.commandSummaries = commandSummaries;
    }



    public DockerImageAndCommandSummary(final Command command, final String server) {
        this.server = server;
        this.imageNames = Sets.newHashSet();
        this.commandSummaries = Lists.newArrayList();
        addCommandSummary(command);
    }

    public Set<String> getImageNames() {
        return imageNames;
    }
    public String getServer() {
        return server;
    }

    public List<CommandSummary> getCommandSummaries() {
        return commandSummaries;
    }

    public void addCommandSummary(final Command command) {
        imageNames.add(command.getImage());
        commandSummaries.add(new CommandSummary(command));
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final DockerImageAndCommandSummary that = (DockerImageAndCommandSummary) o;
        return Objects.equals(this.imageNames, that.imageNames) &&
                Objects.equals(this.server, that.server) &&
                Objects.equals(this.commandSummaries, that.commandSummaries);
    }

    @Override
    public int hashCode() {
        return Objects.hash(imageNames, server, commandSummaries);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("imageNames", imageNames)
                .add("server", server)
                .add("commandSummaries", commandSummaries)
                .toString();
    }

    static class CommandSummary {
        private Long id;
        private String name;
        private String version;
        @JsonProperty("wrappers") private List<XnatCommandWrapperSummary> xnatCommandWrapperSummaries;

        @JsonCreator
        CommandSummary(@JsonProperty("id") final Long id,
                       @JsonProperty("name") final String name,
                       @JsonProperty("version") final String version,
                       @JsonProperty("wrappers") final List<XnatCommandWrapperSummary> wrappers) {
            this.id = id;
            this.name = name;
            this.version = version;
            this.xnatCommandWrapperSummaries = wrappers;
        }

        CommandSummary(final Command command) {
            this.id = command.getId();
            this.name = command.getName();
            this.version = command.getVersion();

            this.xnatCommandWrapperSummaries = Lists.newArrayList();
            if (command.getXnatCommandWrappers() != null) {
                for (final XnatCommandWrapper xnatCommandWrapper : command.getXnatCommandWrappers()) {
                    this.xnatCommandWrapperSummaries.add(new XnatCommandWrapperSummary(xnatCommandWrapper));
                }
            }
        }

        public Long getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getVersion() {
            return version;
        }

        public List<XnatCommandWrapperSummary> getXnatCommandWrapperSummaries() {
            return xnatCommandWrapperSummaries;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            final CommandSummary that = (CommandSummary) o;
            return Objects.equals(this.id, that.id) &&
                    Objects.equals(this.name, that.name) &&
                    Objects.equals(this.version, that.version) &&
                    Objects.equals(this.xnatCommandWrapperSummaries, that.xnatCommandWrapperSummaries);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, name, version, xnatCommandWrapperSummaries);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("id", id)
                    .add("name", name)
                    .add("version", version)
                    .add("xnatCommandWrapperSummaries", xnatCommandWrapperSummaries)
                    .toString();
        }
    }

    static class XnatCommandWrapperSummary {
        private Long id;
        private String name;
        private Set<String> contexts;

        @JsonCreator
        XnatCommandWrapperSummary(@JsonProperty("id") final Long id,
                                  @JsonProperty("name") final String name,
                                  @JsonProperty("contexts") final Set<String> contexts) {
            this.id = id;
            this.name = name;
            this.contexts = contexts;
        }

        XnatCommandWrapperSummary(final XnatCommandWrapper xnatCommandWrapper) {
            this.id = xnatCommandWrapper.getId();
            this.name = xnatCommandWrapper.getName();
            // contexts = xnatCommandWrapper.getContexts(); TODO
            this.contexts = Sets.newHashSet("I am not storing contexts for the wrappers yet");
        }

        public Long getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public Set<String> getContexts() {
            return contexts;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            final XnatCommandWrapperSummary that = (XnatCommandWrapperSummary) o;
            return Objects.equals(this.id, that.id) &&
                    Objects.equals(this.name, that.name) &&
                    Objects.equals(this.contexts, that.contexts);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, name, contexts);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("id", id)
                    .add("name", name)
                    .add("contexts", contexts)
                    .toString();
        }
    }
}
