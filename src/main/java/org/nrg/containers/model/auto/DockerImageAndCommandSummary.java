package org.nrg.containers.model.auto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.nrg.containers.helpers.CommandLabelHelper;
import org.nrg.containers.model.Command;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

@AutoValue
public abstract class DockerImageAndCommandSummary {
    @Nullable @JsonProperty("image-id") public abstract String imageId();
    @Nullable @JsonProperty("server") public abstract String server();
    @JsonProperty("names") public abstract Set<String> imageNames();
    @JsonProperty("commands") public abstract List<CommandPojo> commands();

    @JsonCreator
    public static DockerImageAndCommandSummary create(@JsonProperty("image-id") final String imageId,
                                                      @JsonProperty("server") final String server,
                                                      @JsonProperty("names") final Set<String> imageNames,
                                                      @JsonProperty("commands") final List<CommandPojo> commands) {
        return new AutoValue_DockerImageAndCommandSummary(
                imageId == null ? "" : imageId,
                server,
                imageNames == null ? Sets.<String>newHashSet() : imageNames,
                commands == null ? Lists.<CommandPojo>newArrayList() : commands);
    }

    public static DockerImageAndCommandSummary create(final DockerImage dockerImage, final String server) {
        final DockerImageAndCommandSummary created = create(
                dockerImage.imageId(),
                server,
                Sets.newHashSet(dockerImage.tags()),
                Lists.<CommandPojo>newArrayList());

        CommandLabelHelper.parseLabels(null, dockerImage);

        return created;
    }

    public static DockerImageAndCommandSummary create(final Command command, final String server) {
        final DockerImageAndCommandSummary created = create("", server, Sets.<String>newHashSet(), Lists.<CommandPojo>newArrayList());

        created.addCommand(CommandPojo.create(command));

        return created;
    }

    public static DockerImageAndCommandSummary create(final CommandPojo command, final String server) {
        final DockerImageAndCommandSummary created = create("", server, Sets.<String>newHashSet(), Lists.<CommandPojo>newArrayList());

        created.addCommand(command);

        return created;
    }

    public void addCommands(final List<CommandPojo> commands) {
        for (final CommandPojo command : commands) {
            addCommand(command);
        }
    }

    public void addCommand(final CommandPojo command) {
        imageNames().add(command.image());
        commands().add(command);
    }

    public void addCommand(final Command command) {
        addCommand(CommandPojo.create(command));
    }
}
