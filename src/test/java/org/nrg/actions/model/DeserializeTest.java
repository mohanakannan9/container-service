package org.nrg.actions.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.junit.Test;
import org.nrg.containers.model.DockerImage;


import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isIn;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class DeserializeTest {
    private static final String DICOM_RESOURCE_MATCHER_JSON =
            "{\"value\":\"RESOURCE_NAME\", \"type\":\"string\", \"operator\":\"equals\", \"property\":\"name\"}";
    private static final String RESOURCE_MATCH_TREE_NODE_JSON =
            "{\"input\":true, \"type\":\"resource\", \"input-property\":\"files\", \"matchers\":[" + DICOM_RESOURCE_MATCHER_JSON + "]}";
    private static final String SCAN_MATCH_TREE_NODE_JSON =
            "{\"input\":false, \"type\":\"scan\", \"matchers\":[], \"children\":[" + RESOURCE_MATCH_TREE_NODE_JSON + "]}";
    private static final String IMAGE_JSON =
            "{\"name\":\"sweet\", \"image-id\":\"abc123\", \"repo-tags\":[\"abc123:latest\"], \"size\":0, \"labels\":{\"foo\":\"bar\"}}";
    private static final String COMMAND_INPUT_0_JSON =
            "{\"name\":\"my_cool_input\", \"description\":\"A directory containing some files\", \"type\":\"directory\", \"required\":true}";
    private static final String COMMAND_INPUT_1_JSON =
            "{\"name\":\"my_uncool_input\", \"description\":\"No one loves me :(\", \"type\":\"string\", \"required\":false}";
    private static final String COMMAND_JSON =
            "{\"name\":\"test_command\", \"description\":\"The command for the test\", " +
                    "\"info-url\":\"http://abc.xyz\", " +
                    "\"inputs\":[" + COMMAND_INPUT_0_JSON + ", " + COMMAND_INPUT_1_JSON + "], " +
                    "\"outputs\":[], \"template\":\"foo\", \"type\":\"docker-image\", " +
                    "\"image-id\":" + IMAGE_JSON + "}";
    private static final String MATCH_TREE_JSON =
            "{\"rootContextProperty\":\"some_identifier\", \"input\":" + COMMAND_INPUT_0_JSON + ", \"root\":" + SCAN_MATCH_TREE_NODE_JSON + "}";
    private static final String ACTION_JSON =
            "{\"name\":\"test\", \"description\":\"test description\", " +
                    "\"command\":" + COMMAND_JSON + ", \"inputs\":[" + MATCH_TREE_JSON + "]}";

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testDeserializeImage() throws Exception {
        final DockerImage image = mapper.readValue(IMAGE_JSON, DockerImage.class);

        assertEquals("sweet", image.getName());
        assertEquals("abc123", image.getImageId());
        assertEquals(Long.valueOf(0L), image.getSize());
        assertEquals(Lists.newArrayList("abc123:latest"), image.getRepoTags());
        assertEquals(ImmutableMap.of("foo", "bar"), image.getLabels());
    }


    @Test
    public void testDeserializeAction() throws Exception {
        final Action action = mapper.readValue(ACTION_JSON, Action.class);
        final Command command = mapper.readValue(COMMAND_JSON, Command.class);
        final ActionInput actionInput = mapper.readValue(MATCH_TREE_JSON, ActionInput.class);

        assertEquals("test", action.getName());
        assertEquals("test description", action.getDescription());
        assertEquals(command, action.getCommand());
        assertEquals(Lists.newArrayList(actionInput), action.getInputs());
    }
}
