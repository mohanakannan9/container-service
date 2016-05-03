package org.nrg.actions.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nrg.actions.config.CommandTestConfig;
import org.nrg.actions.services.CommandService;
import org.nrg.automation.entities.Script;
import org.nrg.automation.model.ScriptCommand;
import org.nrg.automation.services.ScriptService;
import org.nrg.containers.model.DockerImage;
import org.nrg.containers.model.DockerImageCommand;
import org.nrg.containers.services.DockerImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.isIn;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
@ContextConfiguration(classes = CommandTestConfig.class)
public class CommandTest {

    private static final String DOCKER_IMAGE_JSON =
            "{\"name\":\"sweet\", \"image-id\":\"abc123\", \"repo-tags\":[\"abc123:latest\"], \"size\":0, \"labels\":{\"foo\":\"bar\"}}";

    private static final String SCRIPT_JSON =
            "{\"scriptId\":\"abc123\", \"scriptLabel\":\"a-script\", " +
                    "\"description\":\"The script for the test\"," +
                    "\"language\":\"English\"," +
                    "\"content\":\"It was the best of times, it was the *blurst* of times? You stupid monkey!\"}";

    private static final String COMMAND_INPUT_0_JSON =
            "{\"name\":\"my_cool_input\", \"description\":\"A directory containing some files\", \"type\":\"directory\", \"required\":true}";
    private static final String COMMAND_INPUT_1_JSON =
            "{\"name\":\"my_uncool_input\", \"description\":\"No one loves me :(\", \"type\":\"string\", \"required\":false}";
    private static final String COMMAND_INPUT_LIST_JSON =
            "[" + COMMAND_INPUT_0_JSON + ", " + COMMAND_INPUT_1_JSON + "]";
    private static final String COMMAND_OUTPUT_JSON =
            "{\"name\":\"an_output\", \"description\":\"It will be put out\", \"required\":true, \"type\":\"files\"}";

    private static final String DOCKER_IMAGE_COMMAND_JSON_TEMPLATE =
            "{\"name\":\"docker_image_command\", \"description\":\"Docker Image command for the test\", " +
                    "\"info-url\":\"http://abc.xyz\", \"env\":{\"foo\":\"bar\"}, " +
                    "\"inputs\":" + COMMAND_INPUT_LIST_JSON + ", " +
                    "\"outputs\":[" + COMMAND_OUTPUT_JSON + "], " +
                    "\"template\":\"foo\", \"type\":\"docker-image\", " +
                    "\"docker-image\":{\"id\":%d}}";

    private static final String SCRIPT_COMMAND_JSON_TEMPLATE =
            "{\"name\":\"script_command\", \"description\":\"The Script command for the test\", " +
                    "\"info-url\":\"http://abc.xyz\", " +
                    "\"inputs\":" + COMMAND_INPUT_LIST_JSON + ", " +
                    "\"outputs\":[" + COMMAND_OUTPUT_JSON + "], " +
                    "\"template\":\"foo\", \"type\":\"script\", " +
                    "\"script\":{\"id\":%d}}";

    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private DockerImageService dockerImageService;

    @Autowired
    private ScriptService scriptService;

    @Autowired
    private CommandService commandService;

    @Test
    public void testSpringConfiguration() {
        assertThat(commandService, not(nullValue()));
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
    public void testDeserializeDockerImageCommand() throws Exception {

        final List<CommandInput> commandInputList = mapper.readValue(COMMAND_INPUT_LIST_JSON, new TypeReference<List<CommandInput>>() {});

        final Output output = mapper.readValue(COMMAND_OUTPUT_JSON, Output.class);

        final String dockerImageCommandJson =
                String.format(DOCKER_IMAGE_COMMAND_JSON_TEMPLATE, 0);
        final Command command = mapper.readValue(dockerImageCommandJson, Command.class);

        assertThat(command, instanceOf(DockerImageCommand.class));
        final DockerImageCommand dockerImageCommand = (DockerImageCommand) command;

        assertEquals("docker_image_command", dockerImageCommand.getName());
        assertEquals("Docker Image command for the test", dockerImageCommand.getDescription());
        assertEquals("http://abc.xyz", dockerImageCommand.getInfoUrl());
        assertEquals("foo", dockerImageCommand.getTemplate());
        assertEquals(ImmutableMap.of("foo", "bar"), dockerImageCommand.getEnvironmentVariables());

        assertThat(dockerImageCommand.getInputs(), hasSize(2));
        assertThat(commandInputList, everyItem(isIn(dockerImageCommand.getInputs())));

        assertEquals(output, dockerImageCommand.getOutputs().get(0));

        assertEquals(0L, dockerImageCommand.getDockerImage().getId());
    }

    @Test
    public void testPersistDockerImageCommand() throws Exception {

        final DockerImage image = mapper.readValue(DOCKER_IMAGE_JSON, DockerImage.class);
        dockerImageService.create(image);

        final String dockerImageCommandJson =
                String.format(DOCKER_IMAGE_COMMAND_JSON_TEMPLATE, image.getId());
        final Command command = mapper.readValue(dockerImageCommandJson, Command.class);

        assertThat(command, instanceOf(DockerImageCommand.class));
        final DockerImageCommand dockerImageCommand = (DockerImageCommand) command;
        commandService.create(dockerImageCommand);

        final Command retrievedCommand = commandService.retrieve(dockerImageCommand.getId());

        assertEquals(dockerImageCommand, retrievedCommand);
    }

    @Test
    public void testDeserializeScriptCommand() throws Exception {

        final List<CommandInput> commandInputList = mapper.readValue(COMMAND_INPUT_LIST_JSON, new TypeReference<List<CommandInput>>() {});

        final Output output = mapper.readValue(COMMAND_OUTPUT_JSON, Output.class);

        final String scriptCommandJson =
                String.format(SCRIPT_COMMAND_JSON_TEMPLATE, 0);
        final Command command = mapper.readValue(scriptCommandJson, Command.class);

        assertThat(command, instanceOf(ScriptCommand.class));
        final ScriptCommand scriptCommand = (ScriptCommand) command;

        assertEquals("script_command", scriptCommand.getName());
        assertEquals("The Script command for the test", scriptCommand.getDescription());
        assertEquals("http://abc.xyz", scriptCommand.getInfoUrl());
        assertEquals("foo", scriptCommand.getTemplate());

        assertThat(scriptCommand.getInputs(), hasSize(2));
        assertThat(commandInputList, everyItem(isIn(scriptCommand.getInputs())));

        assertEquals(output, scriptCommand.getOutputs().get(0));

        assertEquals(0L, scriptCommand.getScript().getId());
    }

    @Test
    public void testPersistScriptCommand() throws Exception {

            final Script script = mapper.readValue(SCRIPT_JSON, Script.class);
            scriptService.create(script);

            final String scriptCommandJson =
                    String.format(SCRIPT_COMMAND_JSON_TEMPLATE, script.getId());
            final Command command = mapper.readValue(scriptCommandJson, Command.class);

            assertThat(command, instanceOf(ScriptCommand.class));
            final ScriptCommand scriptCommand = (ScriptCommand) command;
            commandService.create(scriptCommand);

            final Command retrievedCommand = commandService.retrieve(scriptCommand.getId());

            assertEquals(scriptCommand, retrievedCommand);
    }
}
