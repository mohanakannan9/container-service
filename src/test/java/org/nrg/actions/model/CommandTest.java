package org.nrg.actions.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nrg.actions.config.CommandTestConfig;
import org.nrg.actions.services.CommandService;
import org.nrg.actions.services.ScriptEnvironmentService;
import org.nrg.automation.entities.Script;
import org.nrg.automation.services.ScriptService;
import org.nrg.containers.model.DockerImage;
import org.nrg.containers.model.DockerImageCommand;
import org.nrg.containers.model.DockerImageDto;
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
@ContextConfiguration(classes = CommandTestConfig.class)
public class CommandTest {

    private static final String DOCKER_IMAGE_JSON =
            "{\"name\":\"sweet\", \"image-id\":\"abc123\", " +
                    "\"repo-tags\":[\"abc123:latest\"], \"labels\":{\"foo\":\"bar\"}}";

    private static final String SCRIPT_ENVIRONMENT_JSON_TEMPLATE =
            "{\"name\":\"Mr. Big Stuff\", \"description\":\"Who do you think you are?\", " +
                    "\"docker-image\":{\"id\":%d}}";

    private static final String SCRIPT_JSON =
            "{\"scriptId\":\"abc123\", \"scriptLabel\":\"a-script\", " +
                    "\"description\":\"The script for the test\"," +
                    "\"language\":\"English\"," +
                    "\"content\":\"It was the best of times, it was the *blurst* of times? You stupid monkey!\"}";

    private static final String VARIABLE_0_JSON =
            "{\"name\":\"my_cool_input\", \"description\":\"A boolean value\", " +
                    "\"type\":\"boolean\", \"required\":true," +
                    "\"true-value\":\"-b\", \"false-value\":\"\"}";
    private static final String VARIABLE_1_JSON =
            "{\"name\":\"my_uncool_input\", \"description\":\"No one loves me :(\", " +
                    "\"type\":\"string\", \"required\":false," +
                    "\"arg-template\":\"--uncool=#value#\"}";
    private static final String VARIABLE_LIST_JSON =
            "[" + VARIABLE_0_JSON + ", " + VARIABLE_1_JSON + "]";

    private static final String MOUNT_IN = "{\"name\":\"in\", \"path\":\"/input\"}";
    private static final String MOUNT_OUT = "{\"name\":\"out\", \"path\":\"/output\"}";
    private static final String MOUNTS =
            "{\"inputs\":[" + MOUNT_IN + "], \"outputs\":[" + MOUNT_OUT + "]}";

    private static final String DOCKER_IMAGE_COMMAND_JSON_TEMPLATE =
            "{\"name\":\"docker_image_command\", \"description\":\"Docker Image command for the test\", " +
                    "\"info-url\":\"http://abc.xyz\", \"env\":{\"foo\":\"bar\"}, " +
                    "\"variables\":" + VARIABLE_LIST_JSON + ", " +
                    "\"template\":\"foo\", \"type\":\"docker-image\", " +
                    "\"docker-image\":{\"id\":%d}, " +
                    "\"mounts\": " + MOUNTS + "}";

    private static final String SCRIPT_COMMAND_JSON_TEMPLATE =
            "{\"name\":\"script_command\", \"description\":\"The Script command for the test\", " +
                    "\"info-url\":\"http://abc.xyz\", " +
                    "\"variables\":" + VARIABLE_LIST_JSON + ", " +
                    "\"template\":\"foo\", \"type\":\"script\", " +
                    "\"script\":{\"id\":%d}," +
                    "\"script-environment\":{\"id\":%d}}";

    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private DockerImageService dockerImageService;

    @Autowired
    private ScriptService scriptService;

    @Autowired
    private ScriptEnvironmentService scriptEnvironmentService;

    @Autowired
    private CommandService commandService;

    @Test
    public void testSpringConfiguration() {
        assertThat(commandService, not(nullValue()));
    }


    @Test
    public void testDeserializeCommandInput() throws Exception {
        final CommandVariable commandVariable0 =
                mapper.readValue(VARIABLE_0_JSON, CommandVariable.class);
        final CommandVariable commandVariable1 =
                mapper.readValue(VARIABLE_1_JSON, CommandVariable.class);

        assertEquals("my_cool_input", commandVariable0.getName());
        assertEquals("A boolean value", commandVariable0.getDescription());
        assertEquals("boolean", commandVariable0.getType());
        assertEquals(true, commandVariable0.isRequired());
        assertEquals("-b", commandVariable0.getTrueValue());
        assertEquals("", commandVariable0.getFalseValue());
        assertNull(commandVariable0.getArgTemplate());

        assertEquals("my_uncool_input", commandVariable1.getName());
        assertEquals("No one loves me :(", commandVariable1.getDescription());
        assertEquals("string", commandVariable1.getType());
        assertEquals(false, commandVariable1.isRequired());
        assertNull(commandVariable1.getTrueValue());
        assertNull(commandVariable1.getFalseValue());
        assertEquals("--uncool=#value#", commandVariable1.getArgTemplate());
    }

    @Test
    public void testDeserializeDockerImageCommand() throws Exception {

        final List<CommandVariable> commandVariableList =
                mapper.readValue(VARIABLE_LIST_JSON, new TypeReference<List<CommandVariable>>() {});

        final Mount input = mapper.readValue(MOUNT_IN, Mount.class);
        final Mount output = mapper.readValue(MOUNT_OUT, Mount.class);

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

        assertThat(dockerImageCommand.getVariables(), hasSize(2));
        assertThat(commandVariableList, everyItem(isIn(dockerImageCommand.getVariables())));

        assertThat(dockerImageCommand.getCommandMounts().getInputs(), hasSize(1));
        assertEquals(input, dockerImageCommand.getCommandMounts().getInputs().get(0));
        assertThat(dockerImageCommand.getCommandMounts().getOutputs(), hasSize(1));
        assertEquals(output, dockerImageCommand.getCommandMounts().getOutputs().get(0));

        assertEquals(0L, dockerImageCommand.getDockerImage().getId());
    }

    @Test
    public void testPersistDockerImageCommand() throws Exception {

        final DockerImageDto imageDto = mapper.readValue(DOCKER_IMAGE_JSON, DockerImageDto.class);
        final DockerImage image = imageDto.toDbImage();
        dockerImageService.create(image);

        final String dockerImageCommandJson =
                String.format(DOCKER_IMAGE_COMMAND_JSON_TEMPLATE, image.getId());
        final Command command = mapper.readValue(dockerImageCommandJson, Command.class);

        assertThat(command, instanceOf(DockerImageCommand.class));
        final DockerImageCommand dockerImageCommand = (DockerImageCommand) command;
        commandService.create(dockerImageCommand);
        commandService.flush();
        commandService.refresh(dockerImageCommand);

        final Command retrievedCommand = commandService.retrieve(dockerImageCommand.getId());

        assertEquals(dockerImageCommand, retrievedCommand);
    }

    @Test
    public void testDeserializeScriptCommand() throws Exception {

        final List<CommandVariable> commandVariableList =
                mapper.readValue(VARIABLE_LIST_JSON, new TypeReference<List<CommandVariable>>() {});

        final String scriptCommandJson =
                String.format(SCRIPT_COMMAND_JSON_TEMPLATE, 0, 0);
        final Command command = mapper.readValue(scriptCommandJson, Command.class);

        assertThat(command, instanceOf(ScriptCommand.class));
        final ScriptCommand scriptCommand = (ScriptCommand) command;

        assertEquals("script_command", scriptCommand.getName());
        assertEquals("The Script command for the test", scriptCommand.getDescription());
        assertEquals("http://abc.xyz", scriptCommand.getInfoUrl());
        assertEquals("foo", scriptCommand.getTemplate());

        assertThat(scriptCommand.getVariables(), hasSize(2));
        assertThat(commandVariableList, everyItem(isIn(scriptCommand.getVariables())));

        assertEquals(0L, scriptCommand.getScript().getId());
        assertEquals(0L, scriptCommand.getScriptEnvironment().getId());
    }

    @Test
    public void testPersistScriptCommand() throws Exception {

        final Script script = mapper.readValue(SCRIPT_JSON, Script.class);
        scriptService.create(script);
        final Script retrievedScript = scriptService.retrieve(script.getId());

        final DockerImageDto imageDto = mapper.readValue(DOCKER_IMAGE_JSON, DockerImageDto.class);
        final DockerImage image = imageDto.toDbImage();
        dockerImageService.create(image);

        final String scriptEnvironmentJson =
                String.format(SCRIPT_ENVIRONMENT_JSON_TEMPLATE, image.getId());
        final ScriptEnvironment scriptEnvironment =
                mapper.readValue(scriptEnvironmentJson, ScriptEnvironment.class);
        scriptEnvironmentService.create(scriptEnvironment);
        scriptEnvironmentService.flush();
        scriptEnvironmentService.refresh(scriptEnvironment);

        final ScriptEnvironment retrievedScriptEnvironment =
                scriptEnvironmentService.retrieve(scriptEnvironment.getId());
        assertEquals(image, retrievedScriptEnvironment.getDockerImage());

        final String scriptCommandJson =
                String.format(SCRIPT_COMMAND_JSON_TEMPLATE,
                        script.getId(), scriptEnvironment.getId());
        final Command command = mapper.readValue(scriptCommandJson, Command.class);

        assertThat(command, instanceOf(ScriptCommand.class));
        final ScriptCommand scriptCommand = (ScriptCommand) command;
        commandService.create(scriptCommand);
        commandService.flush();
        commandService.refresh(scriptCommand);

        final ScriptCommand retrievedCommand = (ScriptCommand) commandService.retrieve(scriptCommand.getId());

        assertEquals(scriptCommand, retrievedCommand);
        assertNotNull(retrievedCommand.getScript());
        assertEquals(retrievedScript, retrievedCommand.getScript());
        assertNotNull(retrievedCommand.getScriptEnvironment());
        assertEquals(retrievedScriptEnvironment, retrievedCommand.getScriptEnvironment());
    }
}
