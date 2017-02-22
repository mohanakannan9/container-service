package org.nrg.containers.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nrg.containers.config.CommandTestConfig;
import org.nrg.containers.services.CommandService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
@ContextConfiguration(classes = CommandTestConfig.class)
public class CommandTest {
    private static final String COOL_INPUT_JSON = "{" +
            "\"name\":\"my_cool_input\", " +
            "\"description\":\"A boolean value\", " +
            "\"type\":\"boolean\", " +
            "\"required\":true," +
            "\"true-value\":\"-b\", " +
            "\"false-value\":\"\"" +
            "}";
    private static final String FOO_INPUT_JSON = "{" +
            "\"name\":\"foo\", " +
            "\"description\":\"A foo that bars\", " +
            "\"required\":false," +
            "\"default-value\":\"bar\"," +
            "\"command-line-flag\":\"--flag\"," +
            "\"command-line-separator\":\"=\"" +
            "}";
    
    private static final String OUTPUT_JSON = "{" +
            "\"name\":\"the_output\"," +
            "\"description\":\"It's the output\"," +
            "\"mount\":\"out\"," +
            "\"path\":\"relative/path/to/dir\"" +
            "}";
    private static final String INPUT_LIST_JSON = "[" + COOL_INPUT_JSON + ", " + FOO_INPUT_JSON + "]";

    private static final String MOUNT_IN = "{\"name\":\"in\", \"writable\": false, \"path\":\"/input\"}";
    private static final String MOUNT_OUT = "{\"name\":\"out\", \"writable\": true, \"path\":\"/output\"}";

    private static final String DOCKER_IMAGE_COMMAND_JSON = "{" +
            "\"name\":\"docker_image_command\", " +
            "\"description\":\"Docker Image command for the test\", " +
            "\"type\": \"docker\", " +
            "\"info-url\":\"http://abc.xyz\", " +
            "\"environment-variables\":{\"foo\":\"bar\"}, " +
            "\"command-line\":\"cmd #foo# #my_cool_input#\", " +
            "\"mounts\":[" + MOUNT_IN + ", " + MOUNT_OUT + "]," +
            "\"ports\": {\"22\": \"2222\"}, " +
            "\"inputs\":" + INPUT_LIST_JSON + ", " +
            "\"outputs\":[" + OUTPUT_JSON + "], " +
            "\"image\":\"abc123\"" +
            "}";

    @Autowired private ObjectMapper mapper;
    @Autowired private CommandService commandService;

    @Test
    public void testSpringConfiguration() {
        assertThat(commandService, not(nullValue()));
    }


    @Test
    public void testDeserializeCommandInput() throws Exception {
        final CommandInput commandInput0 =
                mapper.readValue(COOL_INPUT_JSON, CommandInput.class);
        final CommandInput fooInput =
                mapper.readValue(FOO_INPUT_JSON, CommandInput.class);

        assertEquals("my_cool_input", commandInput0.getName());
        assertEquals("A boolean value", commandInput0.getDescription());
        assertEquals(CommandInput.Type.BOOLEAN, commandInput0.getType());
        assertEquals(true, commandInput0.isRequired());
        assertEquals("-b", commandInput0.getTrueValue());
        assertEquals("", commandInput0.getFalseValue());
        assertEquals("#my_cool_input#", commandInput0.getReplacementKey());
        assertEquals("", commandInput0.getCommandLineFlag());
        assertEquals(" ", commandInput0.getCommandLineSeparator());
        assertNull(commandInput0.getDefaultValue());

        assertEquals("foo", fooInput.getName());
        assertEquals("A foo that bars", fooInput.getDescription());
        assertEquals(CommandInput.Type.STRING, fooInput.getType());
        assertEquals(false, fooInput.isRequired());
        assertNull(fooInput.getTrueValue());
        assertNull(fooInput.getFalseValue());
        assertEquals("#foo#", fooInput.getReplacementKey());
        assertEquals("--flag", fooInput.getCommandLineFlag());
        assertEquals("=", fooInput.getCommandLineSeparator());
        assertEquals("bar", fooInput.getDefaultValue());
    }

    @Test
    public void testDeserializeDockerImageCommand() throws Exception {

        final List<CommandInput> commandInputList =
                mapper.readValue(INPUT_LIST_JSON, new TypeReference<List<CommandInput>>() {});
        final CommandOutput commandOutput = mapper.readValue(OUTPUT_JSON, CommandOutput.class);

        final CommandMountEntity input = mapper.readValue(MOUNT_IN, CommandMountEntity.class);
        final CommandMountEntity output = mapper.readValue(MOUNT_OUT, CommandMountEntity.class);

        final CommandEntity commandEntity = mapper.readValue(DOCKER_IMAGE_COMMAND_JSON, CommandEntity.class);

        assertEquals("abc123", commandEntity.getImage());

        assertEquals("docker_image_command", commandEntity.getName());
        assertEquals("Docker Image command for the test", commandEntity.getDescription());
        assertEquals("http://abc.xyz", commandEntity.getInfoUrl());
        assertEquals(commandInputList, commandEntity.getInputs());
        assertEquals(Lists.newArrayList(commandOutput), commandEntity.getOutputs());

        // final CommandRun run = command.getRun();
        assertEquals("cmd #foo# #my_cool_input#", commandEntity.getCommandLine());
        assertEquals(ImmutableMap.of("foo", "bar"), commandEntity.getEnvironmentVariables());
        assertEquals(Lists.newArrayList(input, output), commandEntity.getMounts());

        assertThat(commandEntity, instanceOf(DockerCommandEntity.class));
        assertEquals(ImmutableMap.of("22", "2222"), ((DockerCommandEntity) commandEntity).getPorts());
    }

    @Test
    public void testPersistDockerImageCommand() throws Exception {

        final CommandEntity commandEntity = mapper.readValue(DOCKER_IMAGE_COMMAND_JSON, CommandEntity.class);

        commandService.create(commandEntity);
        // commandService.flush();
        // commandService.refresh(command);

        final CommandEntity retrievedCommandEntity = commandService.retrieve(commandEntity.getId());

        assertEquals(commandEntity, retrievedCommandEntity);
    }
}
