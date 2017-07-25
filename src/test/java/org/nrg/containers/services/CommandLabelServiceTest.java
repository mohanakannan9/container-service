package org.nrg.containers.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nrg.containers.config.CommandLabelServiceTestConfig;
import org.nrg.containers.model.command.auto.Command;
import org.nrg.containers.model.image.docker.DockerImage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = CommandLabelServiceTestConfig.class)
public class CommandLabelServiceTest {

    @Autowired private CommandLabelService commandLabelService;
    @Autowired private ObjectMapper objectMapper;

    @Test
    public void testCommandLabelsCanDeserialize() throws Exception {
        final String commandName = "command";
        final String imageName = "an image";
        final Command command = Command.builder()
                .name(commandName)
                .image(imageName)
                .build();

        final String dockerImageLabel = objectMapper.writeValueAsString(Collections.singletonList(command));
        final DockerImage dockerImage = DockerImage.builder()
                .imageId("image id")
                .addLabel(CommandLabelService.LABEL_KEY, dockerImageLabel)
                .build();

        final List<Command> commandsFromLabels = commandLabelService.parseLabels(imageName, dockerImage);
        assertThat(commandsFromLabels, hasSize(1));
        assertThat(commandsFromLabels.get(0).name(), is(commandName));
    }
}
