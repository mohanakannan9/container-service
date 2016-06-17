package org.nrg.execution.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nrg.execution.config.AceModelTestConfig;
import org.nrg.execution.services.AceService;
import org.nrg.execution.services.CommandService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
@ContextConfiguration(classes = AceModelTestConfig.class)
public class AceTest {

    private static final String SCAN_MATCHER_JSON =
            "{\"property\":\"type\", \"operator\":\"equals\", \"value\":\"T1|MPRAGE\"}";

    private static final String VARIABLE_0_JSON =
            "{\"name\":\"my_cool_input\", \"description\":\"A boolean value\", " +
                    "\"type\":\"boolean\", \"required\":true," +
                    "\"true-value\":\"-b\", \"false-value\":\"\"," +
                    "\"default-value\":\"true\"}";
    private static final String VARIABLE_1_JSON =
            "{\"name\":\"foo\", \"description\":\"No one loves me :(\", " +
                    "\"type\":\"string\", \"required\":false," +
                    "\"arg-template\":\"--uncool=#value#\"}";
    private static final String VARIABLE_LIST_JSON =
            "[" + VARIABLE_0_JSON + ", " + VARIABLE_1_JSON + "]";

    private static final String COMMAND_MOUNT_IN_JSON = "{\"in\":\"/input\"}";
    private static final String COMMAND_MOUNT_OUT_JSON = "{\"out\":\"/output\"}";

    private static final String DOCKER_IMAGE_COMMAND_JSON =
            "{\"name\":\"docker_image_command\", \"description\":\"Docker Image command for the test\", " +
                    "\"info-url\":\"http://abc.xyz\", " +
                    "\"env\":{\"foo\":\"bar\"}, " +
                    "\"variables\":" + VARIABLE_LIST_JSON + ", " +
                    "\"run-template\":[\"cmd\", \"#foo#\"], " +
                    "\"docker-image\":\"abc123\", " +
                    "\"mounts-in\":" + COMMAND_MOUNT_IN_JSON + "," +
                    "\"mounts-out\":" + COMMAND_MOUNT_OUT_JSON + "}";

    private static final String ACTION_INPUT_JSON =
            "{\"name\":\"some_identifier\", \"command-variable-name\":\"my_cool_input\", " +
                    "\"root-property\":\"label\", " +
                    "\"required\":true, \"type\":\"property\"," +
                    "\"value\":\"something\"}";

    private static final String ACE_RESOURCE_STAGED_JSON =
            "{\"name\":\"DICOM\", \"mount\":\"in\", \"path\":\"/path\"}";
    private static final String ACE_RESOURCE_CREATED_JSON =
            "{\"name\":\"NIFTI\", \"mount\":\"out\"}";

    private static final String ACE_JSON_TEMPLATE =
            "{\"name\":\"an_ace\", \"description\":\"Aces!\", " +
                    "\"action-id\":%d, " +
                    "\"command-id\":%d, " +
                    "\"root-id\":\"XNAT_123.1\"," +
                    "\"inputs\":[" + ACTION_INPUT_JSON + "]," +
                    "\"resources-created\":[" + ACE_RESOURCE_CREATED_JSON + "]," +
                    "\"resources-staged\":[" + ACE_RESOURCE_STAGED_JSON + "]}";

    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private CommandService commandService;

    @Autowired
    private AceService aceService;

    @Test
    public void testDeserializeActionInput() throws Exception {
        final ActionInput actionInput = mapper.readValue(ACTION_INPUT_JSON, ActionInput.class);

        assertEquals("some_identifier", actionInput.getInputName());
        assertEquals("my_cool_input", actionInput.getCommandVariableName());
        assertTrue(actionInput.getRequired());
        assertEquals("property", actionInput.getType());
        assertEquals("something", actionInput.getValue());
        assertEquals("label", actionInput.getRootProperty());
    }

    @Test
    public void testDeserializeResources() throws Exception {
        final ActionResource staged =
                mapper.readValue(ACE_RESOURCE_STAGED_JSON, ActionResource.class);
        final ActionResource created =
                mapper.readValue(ACE_RESOURCE_CREATED_JSON, ActionResource.class);

        assertEquals("DICOM", staged.getResourceName());
        assertEquals("in", staged.getMountName());
        assertEquals("/path", staged.getPath());

        assertEquals("NIFTI", created.getResourceName());
        assertEquals("out", created.getMountName());
        assertThat(created.getPath(), nullValue());
    }

    @Test
    public void testDeserializeAceDto() throws Exception {

        final ActionInput input = mapper.readValue(ACTION_INPUT_JSON, ActionInput.class);
        final ActionResource created =
                mapper.readValue(ACE_RESOURCE_CREATED_JSON, ActionResource.class);
        final ActionResource staged =
                mapper.readValue(ACE_RESOURCE_STAGED_JSON, ActionResource.class);

        final String aceJson =
                String.format(ACE_JSON_TEMPLATE, 100, 10);
        final ActionContextExecutionDto ace =
                mapper.readValue(aceJson, ActionContextExecutionDto.class);

        assertEquals("an_ace", ace.getName());
        assertEquals("Aces!", ace.getDescription());
        assertEquals(Long.valueOf(100), ace.getActionId());
        assertEquals(Long.valueOf(10), ace.getCommandId());
        assertEquals("XNAT_123.1", ace.getRootId());
        assertThat(ace.getInputs(), hasSize(1));
        assertEquals(input, ace.getInputs().get(0));
        assertEquals(created, ace.getResourcesCreated().get(0));
        assertEquals(staged, ace.getResourcesStaged().get(0));
    }

    @Test
    public void testPersistAce() throws Exception {
        final Command command = mapper.readValue(DOCKER_IMAGE_COMMAND_JSON, Command.class);

        final ResolvedCommand resolvedCommand = commandService.resolveCommand(command);

        final String aceDtoJson =
                String.format(ACE_JSON_TEMPLATE, 100, 10);
        final ActionContextExecutionDto aceDto =
                mapper.readValue(aceDtoJson, ActionContextExecutionDto.class);

        final ActionContextExecution ace = new ActionContextExecution(aceDto, resolvedCommand);
        aceService.create(ace);
    }
}
