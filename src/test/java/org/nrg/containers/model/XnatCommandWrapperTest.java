package org.nrg.containers.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nrg.containers.config.CommandTestConfig;
import org.nrg.containers.services.CommandService;
import org.nrg.containers.services.XnatCommandWrapperService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
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
public class XnatCommandWrapperTest {
    private static final String COOL_INPUT_JSON = "{" +
            "\"name\":\"my_cool_input\", " +
            "\"description\":\"A boolean value\", " +
            "\"type\":\"boolean\", " +
            "\"required\":true," +
            "\"true-value\":\"-b\", " +
            "\"false-value\":\"\"" +
            "}";
    private static final String STRING_INPUT_NAME = "foo";
    private static final String STRING_INPUT_JSON = "{" +
            "\"name\":\"" + STRING_INPUT_NAME + "\", " +
            "\"description\":\"A foo that bars\", " +
            "\"required\":false," +
            "\"default-value\":\"bar\"," +
            "\"command-line-flag\":\"--flag\"," +
            "\"command-line-separator\":\"=\"" +
            "}";

    private static final String COMMAND_OUTPUT_NAME = "the_output";
    private static final String COMMAND_OUTPUT = "{" +
            "\"name\":\"" + COMMAND_OUTPUT_NAME + "\"," +
            "\"description\":\"It's the output\"," +
            "\"mount\":\"out\"," +
            "\"path\":\"relative/path/to/dir\"" +
            "}";
    private static final String INPUT_LIST_JSON = "[" + COOL_INPUT_JSON + ", " + STRING_INPUT_JSON + "]";

    private static final String MOUNT_IN = "{\"name\":\"in\", \"writable\": false, \"path\":\"/input\"}";
    private static final String MOUNT_OUT = "{\"name\":\"out\", \"writable\": true, \"path\":\"/output\"}";

    private static final String EXTERNAL_INPUT_NAME = "session";
    private static final String XNAT_COMMAND_WRAPPER_EXTERNAL_INPUT = "{" +
            "\"name\": \"" + EXTERNAL_INPUT_NAME + "\"" +
            ", \"type\": \"Session\"" +
            "}";
    private static final String DERIVED_INPUT_NAME = "label";
    private static final String XNAT_OBJECT_PROPERTY = "label";
    private static final String XNAT_COMMAND_WRAPPER_DERIVED_INPUT = "{" +
            "\"name\": \"" + DERIVED_INPUT_NAME + "\"" +
            ", \"type\": \"string\"" +
            ", \"derived-from-xnat-input\": \"" + EXTERNAL_INPUT_NAME + "\"" +
            ", \"derived-from-xnat-object-property\": \"" + XNAT_OBJECT_PROPERTY + "\"" +
            ", \"provides-value-for-command-input\": \"" + STRING_INPUT_NAME + "\"" +
            "}";

    private static final String OUTPUT_HANDLER_LABEL = "a_label";
    private static final String XNAT_COMMAND_WRAPPER_OUTPUT_HANDLER = "{" +
            "\"type\": \"Resource\"" +
            ", \"accepts-command-output\": \"" + COMMAND_OUTPUT_NAME + "\"" +
            ", \"as-child-of-xnat-input\": \"" + EXTERNAL_INPUT_NAME + "\"" +
            ", \"label\": \"" + OUTPUT_HANDLER_LABEL + "\"" +
            "}";

    private static final String XNAT_COMMAND_WRAPPER_NAME = "";
    private static final String XNAT_COMMAND_WRAPPER_DESC = "the wrapper description";
    private static final String XNAT_COMMAND_WRAPPER = "{" +
            "\"name\": \"" + XNAT_COMMAND_WRAPPER_NAME + "\", " +
            "\"description\": \"" + XNAT_COMMAND_WRAPPER_DESC + "\"," +
            "\"external-inputs\": [" + XNAT_COMMAND_WRAPPER_EXTERNAL_INPUT + "], " +
            "\"derived-inputs\": [" + XNAT_COMMAND_WRAPPER_DERIVED_INPUT + "], " +
            "\"output-handlers\": [" + XNAT_COMMAND_WRAPPER_OUTPUT_HANDLER + "]" +
            "}";

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
            "\"outputs\":[" + COMMAND_OUTPUT + "], " +
            "\"image\":\"abc123\"" +
            ", \"xnat\": [" + XNAT_COMMAND_WRAPPER + "]" +
            "}";


    @Autowired private ObjectMapper mapper;
    @Autowired private CommandService commandService;
    @Autowired private XnatCommandWrapperService commandWrapperService;

    @Test
    public void testSpringConfiguration() {
        assertThat(commandWrapperService, not(nullValue()));
    }

    @Test
    public void testDeserializeXnatCommandInputsAndOutputs() throws Exception {
        final XnatCommandInput externalInput = mapper.readValue(XNAT_COMMAND_WRAPPER_EXTERNAL_INPUT, XnatCommandInput.class);
        assertEquals(EXTERNAL_INPUT_NAME, externalInput.getName());
        assertEquals(XnatCommandInput.Type.SESSION, externalInput.getType());
        assertNull(externalInput.getDerivedFromXnatInput());
        assertNull(externalInput.getDerivedFromXnatObjectProperty());
        assertNull(externalInput.getProvidesValueForCommandInput());
        assertNull(externalInput.getDefaultValue());
        assertNull(externalInput.getMatcher());
        assertFalse(externalInput.getRequired());

        final XnatCommandInput derivedInput = mapper.readValue(XNAT_COMMAND_WRAPPER_DERIVED_INPUT, XnatCommandInput.class);
        assertEquals(DERIVED_INPUT_NAME, derivedInput.getName());
        assertEquals(XnatCommandInput.Type.STRING, derivedInput.getType());
        assertEquals(EXTERNAL_INPUT_NAME, derivedInput.getDerivedFromXnatInput());
        assertEquals(XNAT_OBJECT_PROPERTY, derivedInput.getDerivedFromXnatObjectProperty());
        assertEquals(STRING_INPUT_NAME, derivedInput.getProvidesValueForCommandInput());
        assertNull(derivedInput.getDefaultValue());
        assertNull(derivedInput.getMatcher());
        assertFalse(derivedInput.getRequired());

        final XnatCommandOutput output = mapper.readValue(XNAT_COMMAND_WRAPPER_OUTPUT_HANDLER, XnatCommandOutput.class);
        assertEquals(XnatCommandOutput.Type.RESOURCE, output.getType());
        assertEquals(EXTERNAL_INPUT_NAME, output.getXnatInputName());
        assertEquals(COMMAND_OUTPUT_NAME, output.getCommandOutputName());
        assertEquals(OUTPUT_HANDLER_LABEL, output.getLabel());
    }

    @Test
    public void testDeserializeCommandWithCommandWrapper() throws Exception {

        final XnatCommandWrapper xnatCommandWrapper = mapper.readValue(XNAT_COMMAND_WRAPPER, XnatCommandWrapper.class);

        final Command command = mapper.readValue(DOCKER_IMAGE_COMMAND_JSON, Command.class);

        assertThat(command.getXnatCommandWrappers(), hasSize(1));
        assertTrue(command.getXnatCommandWrappers().contains(xnatCommandWrapper));
    }

    @Test
    public void testPersistCommandWithWrapper() throws Exception {

        final Command command = mapper.readValue(DOCKER_IMAGE_COMMAND_JSON, Command.class);

        commandService.create(command);
        commandService.flush();

        final Command retrievedCommand = commandService.retrieve(command.getId());

        assertEquals(command, retrievedCommand);

        final List<XnatCommandWrapper> commandWrappers = retrievedCommand.getXnatCommandWrappers();
        assertThat(commandWrappers, hasSize(1));

        final XnatCommandWrapper xnatCommandWrapper = commandWrappers.get(0);
        assertThat(xnatCommandWrapper.getId(), not(0L));
    }

    @Test
    public void testCreateEcatHeaderDump() throws Exception {
        // A User was attempting to create the command in this resource.
        // Spring didn't tell us why. See CS-70.
        final String dir = Resources.getResource("ecatHeaderDump").getPath().replace("%20", " ");
        final String commandJsonFile = dir + "/command.json";
        final Command ecatHeaderDump = mapper.readValue(new File(commandJsonFile), Command.class);
        commandService.create(ecatHeaderDump);
    }
}
