package org.nrg.containers.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nrg.containers.config.CommandTestConfig;
import org.nrg.containers.model.auto.Command;
import org.nrg.containers.services.CommandEntityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
@ContextConfiguration(classes = CommandTestConfig.class)
public class CommandTest {
    private static final Logger log = LoggerFactory.getLogger(CommandTest.class);
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
    @Autowired private CommandEntityService commandEntityService;

    @Test
    public void testSpringConfiguration() {
        assertThat(commandEntityService, not(nullValue()));
    }


    @Test
    public void testDeserializeCommandInput() throws Exception {
        final CommandInputEntity commandInputEntity0 =
                mapper.readValue(COOL_INPUT_JSON, CommandInputEntity.class);
        final Command.CommandInput commandInput0 = Command.CommandInput.create(commandInputEntity0);
        final CommandInputEntity fooInputEntity =
                mapper.readValue(FOO_INPUT_JSON, CommandInputEntity.class);
        final Command.CommandInput fooInput = Command.CommandInput.create(fooInputEntity);

        assertEquals("my_cool_input", commandInput0.name());
        assertEquals("A boolean value", commandInput0.description());
        assertEquals(CommandInputEntity.Type.BOOLEAN.getName(), commandInput0.type());
        assertTrue(commandInput0.required());
        assertEquals("-b", commandInput0.trueValue());
        assertEquals("", commandInput0.falseValue());
        assertEquals("#my_cool_input#", commandInput0.replacementKey());
        assertEquals("", commandInput0.commandLineFlag());
        assertEquals(" ", commandInput0.commandLineSeparator());
        assertNull(commandInput0.defaultValue());

        assertEquals("foo", fooInput.name());
        assertEquals("A foo that bars", fooInput.description());
        assertEquals(CommandInputEntity.Type.STRING.getName(), fooInput.type());
        assertFalse(fooInput.required());
        assertNull(fooInput.trueValue());
        assertNull(fooInput.falseValue());
        assertEquals("#foo#", fooInput.replacementKey());
        assertEquals("--flag", fooInput.commandLineFlag());
        assertEquals("=", fooInput.commandLineSeparator());
        assertEquals("bar", fooInput.defaultValue());
    }

    @Test
    public void testDeserializeDockerImageCommand() throws Exception {

        final List<CommandInputEntity> commandInputEntityList =
                mapper.readValue(INPUT_LIST_JSON, new TypeReference<List<CommandInputEntity>>() {});
        final CommandOutputEntity commandOutputEntity = mapper.readValue(OUTPUT_JSON, CommandOutputEntity.class);

        final CommandMountEntity input = mapper.readValue(MOUNT_IN, CommandMountEntity.class);
        final CommandMountEntity output = mapper.readValue(MOUNT_OUT, CommandMountEntity.class);

        final CommandEntity commandEntity = mapper.readValue(DOCKER_IMAGE_COMMAND_JSON, CommandEntity.class);

        assertEquals("abc123", commandEntity.getImage());

        assertEquals("docker_image_command", commandEntity.getName());
        assertEquals("Docker Image command for the test", commandEntity.getDescription());
        assertEquals("http://abc.xyz", commandEntity.getInfoUrl());
        assertEquals(commandInputEntityList, commandEntity.getInputs());
        assertEquals(Lists.newArrayList(commandOutputEntity), commandEntity.getOutputs());

        // final CommandRun run = command.getRun();
        assertEquals("cmd #foo# #my_cool_input#", commandEntity.getCommandLine());
        assertEquals(ImmutableMap.of("foo", "bar"), commandEntity.getEnvironmentVariables());
        assertEquals(Lists.newArrayList(input, output), commandEntity.getMounts());

        assertThat(commandEntity, instanceOf(DockerCommandEntity.class));
        assertEquals(ImmutableMap.of("22", "2222"), ((DockerCommandEntity) commandEntity).getPorts());
    }

    @Test
    @DirtiesContext
    public void testPersistDockerImageCommand() throws Exception {

        final CommandEntity commandEntity = mapper.readValue(DOCKER_IMAGE_COMMAND_JSON, CommandEntity.class);

        commandEntityService.create(commandEntity);

        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        final CommandEntity retrievedCommandEntity = commandEntityService.retrieve(commandEntity.getId());

        assertEquals(commandEntity, retrievedCommandEntity);
    }
}
