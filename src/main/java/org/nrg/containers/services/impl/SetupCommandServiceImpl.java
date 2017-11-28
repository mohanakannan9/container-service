package org.nrg.containers.services.impl;

import org.apache.commons.lang3.StringUtils;
import org.nrg.containers.model.command.auto.Command;
import org.nrg.containers.services.CommandService;
import org.nrg.containers.services.DockerService;
import org.nrg.containers.services.SetupCommandService;
import org.nrg.framework.exceptions.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import java.util.List;

@Service
public class SetupCommandServiceImpl implements SetupCommandService {
    private static final Logger log = LoggerFactory.getLogger(SetupCommandServiceImpl.class);

    private final CommandService commandService;
    private final DockerService dockerService;

    @Autowired
    public SetupCommandServiceImpl(final CommandService commandService,
                                   final DockerService dockerService) {
        this.commandService = commandService;
        this.dockerService = dockerService;
    }

    @Override
    @Nonnull
    public Command getSetupCommand(final String imageWithCommandName) throws NotFoundException {
        final String[] imageSplitOnColon = imageWithCommandName.split(":");
        final String imageWithoutCommandName = imageSplitOnColon.length == 1 ? imageSplitOnColon[0] : imageSplitOnColon[0] + ":" + imageSplitOnColon[1];
        final String commandName;
        if (imageSplitOnColon.length > 3) {
            final StringBuilder sb = new StringBuilder(imageSplitOnColon[2]);
            for (int i = 3; i < imageSplitOnColon.length; i++) {
                sb.append(":");
                sb.append(imageSplitOnColon[i]);
            }
            commandName = sb.toString();
        } else if (imageSplitOnColon.length == 3) {
            commandName = imageSplitOnColon[2];
        } else {
            commandName = null;
        }

        try {
            dockerService.pullFromHub(imageWithoutCommandName, true);
        } catch (Exception ignored) {
            // This could be a problem or it could not. We ignore it for now.
            // We may have the command saved already anyway. If we don't, we will soon find out.
            // And if we can't connect to docker, we will find that out soon enough too.
        }

        final List<Command> commandsByImage = commandService.getByImage(imageWithoutCommandName);

        if (commandsByImage != null && commandsByImage.size() > 0) {
            if (StringUtils.isNotBlank(commandName)) {
                for (final Command commandByImage : commandsByImage) {
                    if (commandName.equals(commandByImage.name())) {
                        return commandByImage;
                    }
                }
            } else {
                if (commandsByImage.size() == 1) {
                    return commandsByImage.get(0);
                }
            }
            final String message = String.format("Could not find a command with name %s for image %s. (Input: %s)", commandName, imageWithoutCommandName, imageWithCommandName);
            log.error(message);
            throw new NotFoundException(message);
        }
        final String message = String.format("Could not find any commands for image %s. (Input: %s)", imageWithoutCommandName, imageWithCommandName);
        log.error(message);
        throw new NotFoundException(message);
    }
}
