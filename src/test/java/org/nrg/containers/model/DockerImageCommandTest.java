package org.nrg.containers.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nrg.actions.model.Command;
import org.nrg.actions.model.CommandInput;
import org.nrg.actions.model.Output;
import org.nrg.containers.config.DockerImageCommandTestConfig;
import org.nrg.containers.services.DockerImageCommandService;
import org.nrg.containers.services.DockerImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
@ContextConfiguration(classes = DockerImageCommandTestConfig.class)
public class DockerImageCommandTest {

    private static final String DOCKER_IMAGE_JSON =
            "{\"name\":\"sweet\", \"image-id\":\"abc123\", \"repo-tags\":[\"abc123:latest\"], \"size\":0, \"labels\":{\"foo\":\"bar\"}}";
    private static final String COMMAND_INPUT_0_JSON =
            "{\"name\":\"my_cool_input\", \"description\":\"A directory containing some files\", \"type\":\"directory\", \"required\":true}";
    private static final String COMMAND_INPUT_1_JSON =
            "{\"name\":\"my_uncool_input\", \"description\":\"No one loves me :(\", \"type\":\"string\", \"required\":false}";
    private static final String COMMAND_OUTPUT_JSON =
            "{\"name\":\"an_output\", \"description\":\"It will be put out\", \"required\":true, \"type\":\"files\"}";

    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private DockerImageService dockerImageService;

    @Autowired
    private DockerImageCommandService dockerImageCommandService;

    @Test
    public void testSpringConfiguration() {
        assertThat(dockerImageCommandService, not(nullValue()));
    }


    @Test
    public void testDeserializeCommandInput() throws Exception {
        final CommandInput commandInput = mapper.readValue(COMMAND_INPUT_0_JSON, CommandInput.class);

        assertEquals("my_cool_input", commandInput.getName());
        assertEquals("A directory containing some files", commandInput.getDescription());
        assertEquals("directory", commandInput.getType());
        assertEquals(true, commandInput.isRequired());
        assertEquals(null, commandInput.getValue());
    }

    @Test
    public void testDeserializeCommand() throws Exception {
        final String commandInputListJson = "[" + COMMAND_INPUT_0_JSON + ", " + COMMAND_INPUT_1_JSON + "]";
        final List<CommandInput> commandInputList = mapper.readValue(commandInputListJson, new TypeReference<List<CommandInput>>() {});

        final Output output = mapper.readValue(COMMAND_OUTPUT_JSON, Output.class);

        final DockerImage dockerImage = mapper.readValue(DOCKER_IMAGE_JSON, DockerImage.class);
        dockerImageService.create(dockerImage);

        final String commandJson =
                "{\"name\":\"test_command\", \"description\":\"The command for the test\", " +
                        "\"info-url\":\"http://abc.xyz\", \"env\":{\"foo\":\"bar\"}, " +
                        "\"inputs\":" + commandInputListJson + ", " +
                        "\"outputs\":[" + COMMAND_OUTPUT_JSON + "], " +
                        "\"template\":\"foo\", \"type\":\"docker-image\", " +
                        "\"docker-image\":{\"id\":" + dockerImage.getId() + "}}";
        final Command command = mapper.readValue(commandJson, Command.class);

        assertThat(command, instanceOf(DockerImageCommand.class));
        final DockerImageCommand dockerImageCommand = (DockerImageCommand) command;
        dockerImageCommandService.create(dockerImageCommand);

        assertThat(dockerImageCommand.getId(), notNullValue());
        assertEquals("test_command", dockerImageCommand.getName());
        assertEquals("The command for the test", dockerImageCommand.getDescription());
        assertEquals("http://abc.xyz", dockerImageCommand.getInfoUrl());
        assertEquals("foo", dockerImageCommand.getTemplate());
        assertEquals(ImmutableMap.of("foo", "bar"), dockerImageCommand.getEnvironmentVariables());

        assertThat(dockerImageCommand.getInputs(), hasSize(2));
        assertEquals(commandInputList, dockerImageCommand.getInputs());

        assertEquals(output, dockerImageCommand.getOutputs().get(0));

        // We need to flush before a refresh. This ensures all the "cascade" operations, i.e. saving
        // all our element collections, will occur. If we don't flush first, then our inputs, outputs,
        // and environment variables will not be saved.
        dockerImageCommandService.flush();
        dockerImageCommandService.refresh(dockerImageCommand);
        assertEquals(dockerImage, dockerImageCommand.getDockerImage());


        assertEquals(commandInputList, dockerImageCommand.getInputs());
        assertEquals(output, dockerImageCommand.getOutputs().get(0));
        assertEquals(ImmutableMap.of("foo", "bar"), dockerImageCommand.getEnvironmentVariables());
    }
}
