package org.nrg.execution.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nrg.execution.api.ContainerControlApi;
import org.nrg.execution.config.CommandTestConfig;
import org.nrg.execution.services.CommandService;
import org.nrg.framework.exceptions.NrgServiceRuntimeException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.isIn;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;
import static org.nrg.execution.api.ContainerControlApi.LABEL_KEY;

@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
@ContextConfiguration(classes = CommandTestConfig.class)
public class CommandTest {

    private static final String VARIABLE_0_JSON =
            "{\"name\":\"my_cool_input\", \"description\":\"A boolean value\", " +
                    "\"type\":\"boolean\", \"required\":true," +
                    "\"true-value\":\"-b\", \"false-value\":\"\"}";
    private static final String FOO_VARIABLE =
            "{\"name\":\"foo\", \"description\":\"A foo that bars\", " +
                    "\"required\":false," +
                    "\"default-value\":\"bar\"," +
                    "\"arg-template\":\"--flag=#value#\"}";
    private static final String VARIABLE_LIST_JSON =
            "[" + VARIABLE_0_JSON + ", " + FOO_VARIABLE + "]";

    private static final String MOUNT_IN = "{\"name\":\"in\", \"remote-path\":\"/input\"}";
    private static final String MOUNT_OUT = "{\"name\":\"out\", \"remote-path\":\"/output\", \"read-only\":false}";
    private static final String RESOLVED_MOUNT_IN = "{\"name\":\"in\", \"remote-path\":\"/input\"}";
    private static final String RESOLVED_MOUNT_OUT = "{\"name\":\"out\", \"remote-path\":\"/output\", \"read-only\":false}";

    private static final String DOCKER_IMAGE_COMMAND_JSON =
            "{\"name\":\"docker_image_command\", \"description\":\"Docker Image command for the test\", " +
                    "\"info-url\":\"http://abc.xyz\", " +
                    "\"env\":{\"foo\":\"bar\"}, " +
                    "\"variables\":" + VARIABLE_LIST_JSON + ", " +
                    "\"run-template\":[\"cmd\",\"#foo# #my_cool_input#\"], " +
                    "\"docker-image\":\"abc123\", " +
                    "\"mounts-in\":[" + MOUNT_IN + "]," +
                    "\"mounts-out\":[" + MOUNT_OUT + "]}";

    private static final String RESOLVED_DOCKER_IMAGE_COMMAND_JSON_TEMPLATE =
            "{\"command-id\":%d, " +
                    "\"docker-image\":\"abc123\", " +
                    "\"env\":{\"foo\":\"bar\"}, " +
                    "\"run\":[\"cmd\", \"--flag=bar \"], " +
                    "\"mounts-in\":[" + RESOLVED_MOUNT_IN + "]," +
                    "\"mounts-out\":[" + RESOLVED_MOUNT_OUT + "]}";

    @Autowired
    private ObjectMapper mapper;

//    @Autowired
//    private ScriptService scriptService;

//    @Autowired
//    private ScriptEnvironmentService scriptEnvironmentService;

    @Autowired
    private CommandService commandService;

//    @Autowired
//    private ContainerControlApi mockContainerControlApi;

    @Test
    public void testSpringConfiguration() {
        assertThat(commandService, not(nullValue()));
    }


    @Test
    public void testDeserializeCommandInput() throws Exception {
        final CommandVariable commandVariable0 =
                mapper.readValue(VARIABLE_0_JSON, CommandVariable.class);
        final CommandVariable fooVariable =
                mapper.readValue(FOO_VARIABLE, CommandVariable.class);

        assertEquals("my_cool_input", commandVariable0.getName());
        assertEquals("A boolean value", commandVariable0.getDescription());
        assertEquals("boolean", commandVariable0.getType());
        assertEquals(true, commandVariable0.isRequired());
        assertEquals("-b", commandVariable0.getTrueValue());
        assertEquals("", commandVariable0.getFalseValue());
        assertNull(commandVariable0.getArgTemplate());
        assertNull(commandVariable0.getDefaultValue());

        assertEquals("foo", fooVariable.getName());
        assertEquals("A foo that bars", fooVariable.getDescription());
        assertNull(fooVariable.getType());
        assertEquals(false, fooVariable.isRequired());
        assertNull(fooVariable.getTrueValue());
        assertNull(fooVariable.getFalseValue());
        assertEquals("--flag=#value#", fooVariable.getArgTemplate());
        assertEquals("bar", fooVariable.getDefaultValue());
    }

    @Test
    public void testDeserializeDockerImageCommand() throws Exception {

        final List<CommandVariable> commandVariableList =
                mapper.readValue(VARIABLE_LIST_JSON, new TypeReference<List<CommandVariable>>() {});

        final CommandMount input = mapper.readValue(MOUNT_IN, CommandMount.class);
        final CommandMount output = mapper.readValue(MOUNT_OUT, CommandMount.class);

        final Command command = mapper.readValue(DOCKER_IMAGE_COMMAND_JSON, Command.class);

        assertEquals("abc123", command.getDockerImage());

        assertEquals("docker_image_command", command.getName());
        assertEquals("Docker Image command for the test", command.getDescription());
        assertEquals("http://abc.xyz", command.getInfoUrl());
        assertEquals(Lists.newArrayList("cmd", "#foo# #my_cool_input#"), command.getRunTemplate());
        assertEquals(ImmutableMap.of("foo", "bar"), command.getEnvironmentVariables());

        assertThat(command.getVariables(), hasSize(2));
        assertThat(commandVariableList, everyItem(isIn(command.getVariables())));

        assertNotNull(command.getMountsIn());
        assertEquals(Lists.newArrayList(input), command.getMountsIn());
        assertNotNull(command.getMountsOut());
        assertEquals(Lists.newArrayList(output), command.getMountsOut());
    }

    @Test
    public void testPersistDockerImageCommand() throws Exception {

        final Command command = mapper.readValue(DOCKER_IMAGE_COMMAND_JSON, Command.class);

        commandService.create(command);
        commandService.flush();
        commandService.refresh(command);

        final Command retrievedCommand = commandService.retrieve(command.getId());

        assertEquals(command, retrievedCommand);
    }

    @Test
    public void testCommandConstraint() throws Exception {
        // We cannot create two commands with the same name & docker image id

        final Command command = new Command();
        command.setName("name");
        command.setDockerImage("abc123");
        final Command commandSameDockerImageId = new Command();
        commandSameDockerImageId.setName("different_name");
        commandSameDockerImageId.setDockerImage("abc123");
        final Command commandSameName = new Command();
        commandSameName.setName("name");
        commandSameDockerImageId.setDockerImage("ABC456");
        final Command commandSameNameAndDockerImageId = new Command();
        commandSameNameAndDockerImageId.setName("name");
        commandSameNameAndDockerImageId.setDockerImage("abc123");

        commandService.create(command);                  // Initial create
        commandService.create(commandSameDockerImageId); // Should be ok
        commandService.create(commandSameName);          // Should be ok
        try {
            commandService.create(commandSameNameAndDockerImageId);
            fail("Should not be able to create a command with same name and docker image id as one that already exists.");
        } catch (NrgServiceRuntimeException ignored) {
            //
        }
    }

    @Test
    public void testResolveCommand() throws Exception {

        final Command command = mapper.readValue(DOCKER_IMAGE_COMMAND_JSON, Command.class);

        final String resolvedCommandJson =
                String.format(RESOLVED_DOCKER_IMAGE_COMMAND_JSON_TEMPLATE, command.getId());
        final ResolvedCommand expected = mapper.readValue(resolvedCommandJson, ResolvedCommand.class);

        final Map<String, String> variableRuntimeValues = Maps.newHashMap();
        variableRuntimeValues.put("my_cool_input", "false");
        final ResolvedCommand resolvedCommand = commandService.resolveCommand(command, variableRuntimeValues);

        assertEquals(expected.getRun(), resolvedCommand.getRun());
        assertEquals(expected.getEnvironmentVariables(), resolvedCommand.getEnvironmentVariables());
        assertEquals(expected.getMountsIn(), resolvedCommand.getMountsIn());
        assertEquals(expected.getMountsOut(), resolvedCommand.getMountsOut());
        assertEquals(expected, resolvedCommand);

        variableRuntimeValues.put("my_cool_input", "true");
        final ResolvedCommand resolvedCommand2 = commandService.resolveCommand(command, variableRuntimeValues);

        assertEquals(expected.getEnvironmentVariables(), resolvedCommand2.getEnvironmentVariables());
        assertEquals(expected.getMountsIn(), resolvedCommand2.getMountsIn());
        assertEquals(expected.getMountsOut(), resolvedCommand2.getMountsOut());

        assertEquals(Lists.newArrayList("cmd", "--flag=bar -b"), resolvedCommand2.getRun());

    }
}
