package org.nrg.containers.helpers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.nrg.containers.model.CommandType;
import org.nrg.containers.model.DockerImage;
import org.nrg.containers.model.auto.CommandPojo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class CommandLabelHelper {
    private static final Logger log = LoggerFactory.getLogger(CommandLabelHelper.class);

    public static final String LABEL_KEY = "org.nrg.commands";


    public static List<CommandPojo> parseLabels(final String imageName, final DockerImage dockerImage) {
        return parseLabels(imageName, dockerImage, new ObjectMapper());
    }

    public static List<CommandPojo> parseLabels(final String imageName, final DockerImage dockerImage, final ObjectMapper objectMapper) {
        final Map<String, String> labels = dockerImage.getLabels();
        if (labels != null && !labels.isEmpty() && labels.containsKey(LABEL_KEY)) {
            final String labelValue = labels.get(LABEL_KEY);
            if (StringUtils.isNotBlank(labelValue)) {
                try {
                    final List<CommandPojo> commandsFromLabels =
                            objectMapper.readValue(labelValue, new TypeReference<List<CommandPojo>>() {});
                    final List<CommandPojo> commandsToReturn = Lists.newArrayList();
                    if (commandsFromLabels != null && !commandsFromLabels.isEmpty()) {
                        for (final CommandPojo commandPojo : commandsFromLabels) {
                            // The command as read from the image may not contain all the values we want to store
                            // So we add them now.
                            commandsToReturn.add(
                                    commandPojo.toBuilder()
                                            .type(CommandType.DOCKER.getName())
                                            .image(imageName)
                                            .hash(dockerImage.getImageId())
                                            .build()
                            );
                        }
                    }
                    return commandsToReturn;
                } catch (IOException e) {
                    // TODO throw exception?
                    log.error("Could not parse Commands from label: " + labelValue, e);
                }
            }
        }
        return null;
    }
}
