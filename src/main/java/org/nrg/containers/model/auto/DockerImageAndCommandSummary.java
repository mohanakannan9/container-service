package org.nrg.containers.model.auto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.nrg.containers.helpers.CommandLabelHelper;
import org.nrg.containers.model.CommandEntity;
import org.nrg.containers.model.command.auto.Command;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

@AutoValue
public abstract class DockerImageAndCommandSummary {
    @Nullable @JsonProperty("image-id") public abstract String imageId();
    @Nullable @JsonProperty("server") public abstract String server();
    @JsonProperty("names") public abstract Set<String> imageNames();
    @JsonProperty("commands") public abstract List<Command> commands();

    @JsonCreator
    public static DockerImageAndCommandSummary create(@JsonProperty("image-id") final String imageId,
                                                      @JsonProperty("server") final String server,
                                                      @JsonProperty("names") final Set<String> imageNames,
                                                      @JsonProperty("commands") final List<Command> commands) {
        return new AutoValue_DockerImageAndCommandSummary(
                imageId == null ? "" : imageId,
                server,
                imageNames == null ? Sets.<String>newHashSet() : imageNames,
                commands == null ? Lists.<Command>newArrayList() : commands);
    }

    public static DockerImageAndCommandSummary create(final DockerImage dockerImage, final String server) {
        final DockerImageAndCommandSummary created = create(
                dockerImage == null ? "" : dockerImage.imageId(),
                server,
                dockerImage == null ? Sets.<String>newHashSet() : Sets.newHashSet(dockerImage.tags()),
                Lists.<Command>newArrayList());

        final List<Command> commandsFromImageLabels = CommandLabelHelper.parseLabels(null, dockerImage);
        if (commandsFromImageLabels != null) {
            for (final Command command : commandsFromImageLabels) {
                created.addCommand(command);
            }
        }

        return created;
    }

    public static DockerImageAndCommandSummary create(final String imageId, final String server, final Command command) {
        final Set<String> imageNames = Sets.newHashSet(command.image());
        final List<Command> commandList = Lists.newArrayList(command);
        return create(imageId, server, imageNames, commandList);
    }

    public static DockerImageAndCommandSummary create(final String imageId, final String server, final CommandEntity commandEntity) {
        return create(imageId, server, Command.create(commandEntity));
    }

    public static DockerImageAndCommandSummary create(final Command command) {
        final Set<String> imageNames = Sets.newHashSet(command.image());
        final List<Command> commandList = Lists.newArrayList(command);
        return create("", null, imageNames, commandList);
    }

    public static DockerImageAndCommandSummary create(final CommandEntity commandEntity) {
        return create(Command.create(commandEntity));
    }

    public void addOrUpdateCommand(final Command commandToAddOrUpdate) {
        addImageName(commandToAddOrUpdate.image());

        // Check to see if the list of commands already has one with this name.
        // If so, we added the existing command from the labels.
        // It will not have an id, and might not have any xnat wrappers.
        // So we should replace it.
        boolean shouldUpdate = false;
        int updateIndex = -1;
        int numCommands = commands().size();
        for (int i = 0; i < numCommands; i++) {
            final Command existingCommand = commands().get(i);
            if (existingCommand.name().equals(commandToAddOrUpdate.name())) {
                shouldUpdate = true;
                updateIndex = i;
                break;
            }
        }
        if (shouldUpdate && updateIndex > -1) {
            commands().remove(updateIndex);
            commands().add(updateIndex, commandToAddOrUpdate);
        } else {
            commands().add(commandToAddOrUpdate);
        }
    }

    private void addCommand(final Command command) {
        addImageName(command.image());
        commands().add(command);
    }

    public void addOrUpdateCommand(final CommandEntity commandEntityToAddOrUpdate) {
        addOrUpdateCommand(Command.create(commandEntityToAddOrUpdate));
    }

    public void addImageName(final String imageName) {
        if (StringUtils.isBlank(imageName)) {
            return;
        }

        imageNames().add(imageName);
    }
}
