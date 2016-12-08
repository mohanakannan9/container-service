package org.nrg.containers.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nrg.containers.config.CommandTestConfig;
import org.nrg.containers.model.xnat.Session;
import org.nrg.containers.services.CommandService;
import org.nrg.framework.exceptions.NrgServiceRuntimeException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

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
    private static final String RESOURCE_JSON = "{\"id\":1, \"type\":\"Resource\", \"label\":\"a_resource\", \"directory\":\"/path/to/files\"}";
    private static final String SESSION_JSON = "{\"id\":\"1\", \"type\":\"Session\", \"label\":\"a_session\", " +
            "\"xsiType\":\"xnat:fakesessiondata\", \"resources\":[" + RESOURCE_JSON + "]}";

    private static final String COOL_INPUT_JSON =
            "{\"name\":\"my_cool_input\", \"description\":\"A boolean value\", " +
                    "\"type\":\"boolean\", \"required\":true," +
                    "\"true-value\":\"-b\", \"false-value\":\"\"}";
    private static final String FOO_INPUT_JSON =
            "{\"name\":\"foo\", \"description\":\"A foo that bars\", " +
                    "\"required\":false," +
                    "\"default-value\":\"bar\"," +
                    "\"command-line-flag\":\"--flag\"," +
                    "\"command-line-separator\":\"=\"}";
    private static final String SESSION_INPUT_JSON =
            "{\"name\":\"session\", \"description\":\"A session\", " +
                    "\"type\":\"Session\", " +
                    "\"required\":true}";
    
    private static final String OUTPUT_JSON =
            "{" +
                    "\"name\":\"the_output\"," +
                    "\"description\":\"It's the output\"," +
                    "\"type\":\"Resource\"," +
                    "\"label\":\"DATA\"," +
                    "\"parent\":\"session\"," +
                    "\"files\": {" +
                        "\"mount\":\"out\"," +
                        "\"path\":\"relative/path/to/dir\"" +
                    "}" +
            "}";
    private static final String INPUT_LIST_JSON =
            "[" + COOL_INPUT_JSON + ", " + FOO_INPUT_JSON + "," + SESSION_INPUT_JSON + "]";

    private static final String MOUNT_IN = "{\"name\":\"in\", \"type\": \"input\", \"path\":\"/input\", \"file-input\":\"session\", \"resource\":\"a_resource\"}";
    private static final String MOUNT_OUT = "{\"name\":\"out\", \"type\": \"output\", \"path\":\"/output\", \"file-input\":\"session\", \"resource\":\"out\"}";
    private static final String RESOLVED_MOUNT_IN = "{\"name\":\"in\", \"is-input\": true, " +
            "\"path\":\"/input\", \"host-path\":\"/path/to/files\", \"file-input\":\"session\", \"resource\":\"a_resource\"}";
    private static final String RESOLVED_MOUNT_OUT = "{\"name\":\"out\", \"is-input\":false, \"path\":\"/output\", \"file-input\":\"session\", \"resource\":\"out\"}";
    private static final String RESOLVED_OUTPUT_JSON =
            "{" +
                    "\"name\":\"the_output\"," +
                    "\"type\":\"Resource\"," +
                    "\"label\":\"DATA\"," +
                    "\"parent\":\"session\"," +
                    "\"mount\":\"out\"," +
                    "\"path\":\"relative/path/to/dir\"" +
                    "}";

    private static final String DOCKER_IMAGE_COMMAND_JSON =
            "{\"name\":\"docker_image_command\", \"description\":\"Docker Image command for the test\", " +
                    "\"info-url\":\"http://abc.xyz\", " +
                    "\"run\": {" +
                        "\"environment-variables\":{\"foo\":\"bar\"}, " +
                        "\"command-line\":\"cmd #foo# #my_cool_input#\", " +
                        "\"mounts\":[" + MOUNT_IN + ", " + MOUNT_OUT + "]," +
                        "\"ports\": {\"22\": \"2222\"}" +
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
                    "\"mounts-out\":[" + RESOLVED_MOUNT_OUT + "]," +
                    "\"input-values\": {" +
                        "\"my_cool_input\": \"%s\"," +
                        "\"foo\": \"%s\"," +
                        "\"session\": \"%s\"" +
                    "}," +
                    "\"outputs\":[ " + RESOLVED_OUTPUT_JSON + "]}";

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

        final CommandMount input = mapper.readValue(MOUNT_IN, CommandMount.class);
        final CommandMount output = mapper.readValue(MOUNT_OUT, CommandMount.class);

        final Command command = mapper.readValue(DOCKER_IMAGE_COMMAND_JSON, Command.class);

        assertEquals("abc123", command.getDockerImage());

        assertEquals("docker_image_command", command.getName());
        assertEquals("Docker Image command for the test", command.getDescription());
        assertEquals("http://abc.xyz", command.getInfoUrl());
        assertEquals(commandInputList, command.getInputs());
        assertThat(command.getOutputs(), hasSize(1));
        assertEquals(commandOutput, command.getOutputs().get(0));

        final CommandRun run = command.getRun();
        assertEquals("cmd #foo# #my_cool_input#", run.getCommandLine());
        assertEquals(ImmutableMap.of("foo", "bar"), run.getEnvironmentVariables());
        assertEquals(Lists.newArrayList(input, output), run.getMounts());
        assertEquals(ImmutableMap.of("22", "2222"), run.getPorts());
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

        final Map<String, String> runtimeValues = Maps.newHashMap();
        runtimeValues.put("my_cool_input", "false");
        runtimeValues.put("session", SESSION_JSON);
        final ResolvedCommand resolvedCommand = commandService.resolveCommand(command, runtimeValues, null);

        final String filledOutSessionJson = mapper.writeValueAsString(mapper.readValue(SESSION_JSON, Session.class)).replaceAll("\\\"", "\\\\\\\"");
        final String resolvedCommandJson1 =
                String.format(RESOLVED_DOCKER_IMAGE_COMMAND_JSON_TEMPLATE,
                        command.getId(), "", "bar", filledOutSessionJson);
        final ResolvedCommand expected1 = mapper.readValue(resolvedCommandJson1, ResolvedCommand.class);
        assertEquals(expected1, resolvedCommand);

        runtimeValues.put("my_cool_input", "true");
        final ResolvedCommand resolvedCommand2 = commandService.resolveCommand(command, runtimeValues, null);

        final String resolvedCommandJson2 =
                String.format(RESOLVED_DOCKER_IMAGE_COMMAND_JSON_TEMPLATE,
                        command.getId(), "true", "bar", filledOutSessionJson);
        final ResolvedCommand expected2 = mapper.readValue(resolvedCommandJson2, ResolvedCommand.class);
        assertEquals(expected2.getEnvironmentVariables(), resolvedCommand2.getEnvironmentVariables());
        assertEquals(expected2.getMountsIn(), resolvedCommand2.getMountsIn());
        assertEquals(expected2.getMountsOut(), resolvedCommand2.getMountsOut());

        assertEquals("cmd --flag=bar -b", resolvedCommand2.getCommandLine());
    }
}
