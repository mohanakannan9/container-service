package org.nrg.execution.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.junit.Test;
import org.junit.runner.RunWith;
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

@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
@ContextConfiguration(classes = CommandTestConfig.class)
public class CommandTest {

    private static final String INPUT_0_JSON =
            "{\"name\":\"my_cool_input\", \"description\":\"A boolean value\", " +
                    "\"type\":\"boolean\", \"required\":true," +
                    "\"true-value\":\"-b\", \"false-value\":\"\"}";
    private static final String FOO_INPUT =
            "{\"name\":\"foo\", \"description\":\"A foo that bars\", " +
                    "\"required\":false," +
                    "\"default-value\":\"bar\"," +
                    "\"command-line-flag\":\"--flag\"," +
                    "\"command-line-separator\":\"=\"}";
    private static final String OUTPUT_JSON =
            "{" +
                    "\"name\":\"the_output\"," +
                    "\"description\":\"It's the output\"," +
                    "\"type\":\"Resource\"," +
                    "\"label\":\"DATA\"," +
                    "\"root\":\"$.json.path.expression\"," +
                    "\"mount\":\"$.run.mounts[name='out']\"," +
                    "\"path\":\"relative/path/to/dir\"" +
            "}";
    private static final String INPUT_LIST_JSON =
            "[" + INPUT_0_JSON + ", " + FOO_INPUT + "]";

    private static final String MOUNT_IN = "{\"name\":\"in\", \"type\": \"input\", \"remote-path\":\"/input\"}";
    private static final String MOUNT_OUT = "{\"name\":\"out\", \"type\": \"output\", \"remote-path\":\"/output\"}";
    private static final String RESOLVED_MOUNT_IN = "{\"name\":\"in\", \"type\": \"input\", \"remote-path\":\"/input\"}";
    private static final String RESOLVED_MOUNT_OUT = "{\"name\":\"out\", \"type\":\"output\", \"remote-path\":\"/output\"}";

    private static final String DOCKER_IMAGE_COMMAND_JSON =
            "{\"name\":\"docker_image_command\", \"description\":\"Docker Image command for the test\", " +
                    "\"info-url\":\"http://abc.xyz\", " +
                    "\"run\": {" +
                        "\"environment-variables\":{\"foo\":\"bar\"}, " +
                        "\"command-line\":\"cmd #foo# #my_cool_input#\", " +
                        "\"mounts\":[" + MOUNT_IN + ", " + MOUNT_OUT + "]" +
                    "}," +
                    "\"inputs\":" + INPUT_LIST_JSON + ", " +
                    "\"outputs\":[" + OUTPUT_JSON + "], " +
                    "\"docker-image\":\"abc123\"}";

    private static final String RESOLVED_DOCKER_IMAGE_COMMAND_JSON_TEMPLATE =
            "{\"command-id\":%d, " +
                    "\"docker-image\":\"abc123\", " +
                    "\"env\":{\"foo\":\"bar\"}, " +
                    "\"command-line\":\"cmd --flag=bar \", " +
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
        final CommandInput commandInput0 =
                mapper.readValue(INPUT_0_JSON, CommandInput.class);
        final CommandInput fooInput =
                mapper.readValue(FOO_INPUT, CommandInput.class);

        assertEquals("my_cool_input", commandInput0.getName());
        assertEquals("A boolean value", commandInput0.getDescription());
        assertEquals("boolean", commandInput0.getType());
        assertEquals(true, commandInput0.isRequired());
        assertEquals("-b", commandInput0.getTrueValue());
        assertEquals("", commandInput0.getFalseValue());
        assertEquals("#my_cool_input#", commandInput0.getReplacementKey());
        assertEquals("", commandInput0.getCommandLineFlag());
        assertEquals(" ", commandInput0.getCommandLineSeparator());
        assertNull(commandInput0.getDefaultValue());

        assertEquals("foo", fooInput.getName());
        assertEquals("A foo that bars", fooInput.getDescription());
        assertNull(fooInput.getType());
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

        final CommandMount input = mapper.readValue(MOUNT_IN, CommandMount.class);
        final CommandMount output = mapper.readValue(MOUNT_OUT, CommandMount.class);

        final Command command = mapper.readValue(DOCKER_IMAGE_COMMAND_JSON, Command.class);

        assertEquals("abc123", command.getDockerImage());

        assertEquals("docker_image_command", command.getName());
        assertEquals("Docker Image command for the test", command.getDescription());
        assertEquals("http://abc.xyz", command.getInfoUrl());
        assertThat(command.getInputs(), hasSize(2));
        assertThat(commandInputList, everyItem(isIn(command.getInputs())));
        assertThat(command.getOutputs(), hasSize(1));
        assertEquals(commandOutput, command.getOutputs().get(0));

        final CommandRun run = command.getRun();
        assertEquals("cmd #foo# #my_cool_input#", run.getCommandLine());
        assertEquals(ImmutableMap.of("foo", "bar"), run.getEnvironmentVariables());
        assertEquals(Lists.newArrayList(input, output), run.getMounts());
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

        assertEquals(expected.getCommandLine(), resolvedCommand.getCommandLine());
        assertEquals(expected.getEnvironmentVariables(), resolvedCommand.getEnvironmentVariables());
        assertEquals(expected.getMountsIn(), resolvedCommand.getMountsIn());
        assertEquals(expected.getMountsOut(), resolvedCommand.getMountsOut());
        assertEquals(expected, resolvedCommand);

        variableRuntimeValues.put("my_cool_input", "true");
        final ResolvedCommand resolvedCommand2 = commandService.resolveCommand(command, variableRuntimeValues);

        assertEquals(expected.getEnvironmentVariables(), resolvedCommand2.getEnvironmentVariables());
        assertEquals(expected.getMountsIn(), resolvedCommand2.getMountsIn());
        assertEquals(expected.getMountsOut(), resolvedCommand2.getMountsOut());

        assertEquals("cmd --flag=bar -b", resolvedCommand2.getCommandLine());
    }
}
