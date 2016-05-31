package org.nrg.actions.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nrg.actions.config.AceModelTestConfig;
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

    private static final String ACTION_INPUT_JSON =
            "{\"name\":\"some_identifier\", \"command-variable-name\":\"my_cool_input\", " +
                    "\"root-property\":\"label\", " +
                    "\"required\":true, \"type\":\"property\"," +
                    "\"value\":\"something\"}";

    private static final String ACE_RESOURCE_STAGED_JSON =
            "{\"name\":\"DICOM\", \"mount\":\"in\", \"id\":123}";
    private static final String ACE_RESOURCE_CREATED_JSON =
            "{\"name\":\"NIFTI\", \"mount\":\"out\"}";

    private static final String ACE_JSON =
            "{\"name\":\"an_ace\", \"description\":\"Aces!\", " +
                    "\"action-id\":100, " +
                    "\"command-id\":10, " +
                    "\"root-id\":\"XNAT_123.1\"," +
                    "\"inputs\":[" + ACTION_INPUT_JSON + "]," +
                    "\"resources-created\":[" + ACE_RESOURCE_CREATED_JSON + "]," +
                    "\"resources-staged\":[" + ACE_RESOURCE_STAGED_JSON + "]}";

    private final ObjectMapper mapper = new ObjectMapper();

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
        assertEquals(Integer.valueOf(123), staged.getResourceId());

        assertEquals("NIFTI", created.getResourceName());
        assertEquals("out", created.getMountName());
        assertThat(created.getResourceId(), nullValue());
    }

    @Test
    public void testDeserializeAceDto() throws Exception {

        final ActionInput input = mapper.readValue(ACTION_INPUT_JSON, ActionInput.class);
        final ActionResource created =
                mapper.readValue(ACE_RESOURCE_CREATED_JSON, ActionResource.class);
        final ActionResource staged =
                mapper.readValue(ACE_RESOURCE_STAGED_JSON, ActionResource.class);

        final ActionContextExecutionDto ace =
                mapper.readValue(ACE_JSON, ActionContextExecutionDto.class);

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
}
