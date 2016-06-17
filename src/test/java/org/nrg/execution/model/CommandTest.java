package org.nrg.execution.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nrg.execution.config.CommandTestConfig;
import org.nrg.execution.services.CommandService;
import org.nrg.automation.entities.Script;
import org.nrg.automation.services.ScriptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
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

//    private static final String SCRIPT_ENVIRONMENT_JSON =
//            "{\"name\":\"Mr. Big Stuff\", \"description\":\"Who do you think you are?\", " +
//                    "\"docker-image\":\"abc123\"," +
//                    "\"run-prefix\":[\"/bin/bash\"]}";

    private static final String SCRIPT_JSON =
            "{\"scriptId\":\"123\", \"scriptLabel\":\"a-script\", " +
                    "\"description\":\"The script for the test\"," +
                    "\"language\":\"English\"," +
                    "\"content\":\"It was the best of times, it was the *blurst* of times? You stupid monkey!\"}";

    private static final String VARIABLE_0_JSON =
            "{\"name\":\"my_cool_input\", \"description\":\"A boolean value\", " +
                    "\"type\":\"boolean\", \"required\":true," +
                    "\"true-value\":\"-b\", \"false-value\":\"\"}";
    private static final String FOO_VARIABLE =
            "{\"name\":\"foo\", \"description\":\"A foo that bars\", " +
                    "\"required\":false," +
                    "\"value\":\"bar\"," +
                    "\"arg-template\":\"--flag=#value#\"}";
    private static final String VARIABLE_LIST_JSON =
            "[" + VARIABLE_0_JSON + ", " + FOO_VARIABLE + "]";

    private static final String MOUNT_IN = "{\"in\":\"/input\"}";
    private static final String MOUNT_OUT = "{\"out\":\"/output\"}";
    private static final String RESOLVED_MOUNT_IN = "{\"name\":\"in\", \"remote-path\":\"/input\"}";
    private static final String RESOLVED_MOUNT_OUT = "{\"name\":\"out\", \"remote-path\":\"/output\", \"read-only\":false}";

    private static final String DOCKER_IMAGE_COMMAND_JSON =
            "{\"name\":\"docker_image_command\", \"description\":\"Docker Image command for the test\", " +
                    "\"info-url\":\"http://abc.xyz\", " +
                    "\"env\":{\"foo\":\"bar\"}, " +
                    "\"variables\":" + VARIABLE_LIST_JSON + ", " +
                    "\"run-template\":[\"cmd\",\"#foo#\"], " +
                    "\"docker-image\":\"abc123\", " +
                    "\"mounts-in\":" + MOUNT_IN + "," +
                    "\"mounts-out\":" + MOUNT_OUT + "}";

    private static final String SCRIPT_COMMAND_JSON_TEMPLATE =
            "{\"name\":\"script_command\", \"description\":\"The Script command for the test\", " +
                    "\"info-url\":\"http://abc.xyz\", " +
                    "\"variables\":" + VARIABLE_LIST_JSON + ", " +
                    "\"run-template\":[\"foo\"], " +
                    "\"docker-image\":\"abc123\", " +
                    "\"script-id\":%d}me";

//    private static final String RESOLVED_DOCKER_IMAGE_COMMAND_JSON_TEMPLATE =
//            "{\"command-id\":%d, " +
//                    "\"docker-image-id\":%s, " +
//                    "\"env\":{\"foo\":\"bar\"}, " +
//                    "\"run\":[\"cmd\", \"--flag=bar\"], " +
//                    "\"mounts-in\":[" + RESOLVED_MOUNT_IN + "]," +
//                    "\"mounts-out\":[" + RESOLVED_MOUNT_OUT + "]}";

    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private ScriptService scriptService;

//    @Autowired
//    private ScriptEnvironmentService scriptEnvironmentService;

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
        final CommandVariable fooVariable =
                mapper.readValue(FOO_VARIABLE, CommandVariable.class);

        assertEquals("my_cool_input", commandVariable0.getName());
        assertEquals("A boolean value", commandVariable0.getDescription());
        assertEquals("boolean", commandVariable0.getType());
        assertEquals(true, commandVariable0.isRequired());
        assertEquals("-b", commandVariable0.getTrueValue());
        assertEquals("", commandVariable0.getFalseValue());
        assertNull(commandVariable0.getArgTemplate());
        assertNull(commandVariable0.getValue());

        assertEquals("foo", fooVariable.getName());
        assertEquals("A foo that bars", fooVariable.getDescription());
        assertNull(fooVariable.getType());
        assertEquals(false, fooVariable.isRequired());
        assertNull(fooVariable.getTrueValue());
        assertNull(fooVariable.getFalseValue());
        assertEquals("--flag=#value#", fooVariable.getArgTemplate());
        assertEquals("bar", fooVariable.getValue());
    }

    @Test
    public void testDeserializeDockerImageCommand() throws Exception {

        final List<CommandVariable> commandVariableList =
                mapper.readValue(VARIABLE_LIST_JSON, new TypeReference<List<CommandVariable>>() {});

        final Map<String, String> input = mapper.readValue(MOUNT_IN, new TypeReference<Map<String, String>>(){});
        final Map<String, String> output = mapper.readValue(MOUNT_OUT, new TypeReference<Map<String, String>>(){});

        final Command command = mapper.readValue(DOCKER_IMAGE_COMMAND_JSON, Command.class);

        assertEquals("abc123", command.getDockerImage());

        assertEquals("docker_image_command", command.getName());
        assertEquals("Docker Image command for the test", command.getDescription());
        assertEquals("http://abc.xyz", command.getInfoUrl());
        assertEquals(Lists.newArrayList("cmd", "#foo#"), command.getRunTemplate());
        assertEquals(ImmutableMap.of("foo", "bar"), command.getEnvironmentVariables());

        assertThat(command.getVariables(), hasSize(2));
        assertThat(commandVariableList, everyItem(isIn(command.getVariables())));

        assertNotNull(command.getMountsIn());
        assertEquals(input, command.getMountsIn());
        assertNotNull(command.getMountsOut());
        assertEquals(output, command.getMountsOut());
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
    public void testDeserializeScriptCommand() throws Exception {

        final List<CommandVariable> commandVariableList =
                mapper.readValue(VARIABLE_LIST_JSON, new TypeReference<List<CommandVariable>>() {});

        final String scriptCommandJson =
                String.format(SCRIPT_COMMAND_JSON_TEMPLATE, 0);
        final Command command = mapper.readValue(scriptCommandJson, Command.class);

        assertEquals("script_command", command.getName());
        assertEquals("The Script command for the test", command.getDescription());
        assertEquals("http://abc.xyz", command.getInfoUrl());
        assertEquals(Lists.newArrayList("foo"), command.getRunTemplate());

        assertThat(command.getVariables(), hasSize(2));
        assertThat(commandVariableList, everyItem(isIn(command.getVariables())));

        assertEquals((Long)0L, command.getScriptId());
        assertEquals("abc123", command.getDockerImage());
    }

    @Test
    public void testPersistScriptCommand() throws Exception {

        final Script script = mapper.readValue(SCRIPT_JSON, Script.class);
        scriptService.create(script);
        final Script retrievedScript = scriptService.retrieve(script.getId());

//        final ScriptEnvironment scriptEnvironment =
//                mapper.readValue(SCRIPT_ENVIRONMENT_JSON, ScriptEnvironment.class);
//        scriptEnvironmentService.create(scriptEnvironment);
//        scriptEnvironmentService.flush();
//        scriptEnvironmentService.refresh(scriptEnvironment);

//        final ScriptEnvironment retrievedScriptEnvironment =
//                scriptEnvironmentService.retrieve(scriptEnvironment.getId());
//        assertEquals(scriptEnvironment, retrievedScriptEnvironment);

        final String scriptCommandJson =
                String.format(SCRIPT_COMMAND_JSON_TEMPLATE, script.getId());
        final Command command = mapper.readValue(scriptCommandJson, Command.class);

        commandService.create(command);
        commandService.flush();
        commandService.refresh(command);

        final Command retrievedCommand = commandService.retrieve(command.getId());

        assertEquals(command, retrievedCommand);
        assertNotNull(retrievedCommand.getScript());
        assertEquals(retrievedScript, retrievedCommand.getScript());
//        assertNotNull(retrievedCommand.getScriptEnvironment());
//        assertEquals(retrievedScriptEnvironment, retrievedCommand.getScriptEnvironment());
    }

//    @Test
//    public void testResolveCommand() throws Exception {
//
//        final Command command = mapper.readValue(DOCKER_IMAGE_COMMAND_JSON, Command.class);
//
//        final String resolvedCommandJson =
//                String.format(RESOLVED_DOCKER_IMAGE_COMMAND_JSON_TEMPLATE, command.getId(), dockerImageId);
//        final ResolvedCommand expected = mapper.readValue(resolvedCommandJson, ResolvedCommand.class);
//
//        final ResolvedCommand resolvedCommand = commandService.resolveCommand(command);
//
//        assertEquals(expected.getRun(), resolvedCommand.getRun());
//        assertEquals(expected.getEnvironmentVariables(), resolvedCommand.getEnvironmentVariables());
//        assertEquals(expected.getMountsIn(), resolvedCommand.getMountsIn());
//        assertEquals(expected.getMountsOut(), resolvedCommand.getMountsOut());
//    }
}
