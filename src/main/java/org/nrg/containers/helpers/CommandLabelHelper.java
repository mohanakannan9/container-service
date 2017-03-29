package org.nrg.containers.helpers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.nrg.containers.model.command.entity.CommandType;
import org.nrg.containers.model.image.docker.DockerImage;
import org.nrg.containers.model.command.auto.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class CommandLabelHelper {
    private static final Logger log = LoggerFactory.getLogger(CommandLabelHelper.class);

    public static final String LABEL_KEY = "org.nrg.commands";

    @Nonnull
    public static List<Command> parseLabels(final DockerImage dockerImage, final ObjectMapper objectMapper) {
        return parseLabels(null, dockerImage, objectMapper);
    }

    @Nonnull
    public static List<Command> parseLabels(final String imageName, final @Nonnull DockerImage dockerImage, final ObjectMapper objectMapper) {
        final List<Command> commandsToReturn = Lists.newArrayList();
        final Map<String, String> labels = dockerImage.labels();
        if (labels == null || labels.isEmpty() || !labels.containsKey(LABEL_KEY)) {
            return commandsToReturn;
        }

        final String labelValue = labels.get(LABEL_KEY);
        if (StringUtils.isNotBlank(labelValue)) {
            try {
                final List<Command> commandsFromLabels =
                        objectMapper.readValue(labelValue, new TypeReference<List<Command>>() {});

                if (commandsFromLabels != null && !commandsFromLabels.isEmpty()) {
                    for (final Command command : commandsFromLabels) {
                        // The command as read from the image may not contain all the values we want to store
                        // So we add them now.
                        commandsToReturn.add(
                                command.toBuilder()
                                        .type(CommandType.DOCKER.getName())
                                        .image(imageName)
                                        .hash(dockerImage.imageId())
                                        .build()
                        );
                    }
                }
            } catch (IOException e) {
                // TODO throw exception?
                log.error("Could not parse Commands from label: " + labelValue, e);
            }
        }
        return commandsToReturn;
    }
}
