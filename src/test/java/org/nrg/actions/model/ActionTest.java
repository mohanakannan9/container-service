package org.nrg.actions.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nrg.actions.config.ActionTestConfig;
import org.nrg.actions.model.matcher.Matcher;
import org.nrg.actions.model.tree.MatchTreeNode;
import org.nrg.actions.services.ActionService;
import org.nrg.containers.model.DockerImage;
import org.nrg.actions.services.CommandService;
import org.nrg.containers.services.DockerImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.emptyCollectionOf;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
@ContextConfiguration(classes = ActionTestConfig.class)
public class ActionTest {
    private static final String DOCKER_IMAGE_JSON =
            "{\"name\":\"name\", \"repo-tags\":[\"a\", \"b\"], \"image-id\":\"abc123\"," +
                    "\"labels\":{\"foo\":\"bar\"}, \"size\":0}";

    private static final String DICOM_RESOURCE_MATCHER_JSON =
            "{\"value\":\"RESOURCE_NAME\", \"type\":\"string\", \"operator\":\"equals\", \"property\":\"name\"}";
    private static final String RESOURCE_MATCH_TREE_NODE_JSON =
            "{\"provides-input-value\":true, \"type\":\"resource\", " +
                    "\"input-property\":\"files\", \"matchers\":[" + DICOM_RESOURCE_MATCHER_JSON + "]}";
    private static final String SCAN_MATCH_TREE_NODE_JSON =
            "{\"provides-input-value\":false, \"type\":\"scan\", " +
                    "\"matchers\":[], \"children\":[" + RESOURCE_MATCH_TREE_NODE_JSON + "]}";

    private static final String INPUT_NAME = "my_cool_input";
    private static final String COMMAND_INPUT_JSON =
            "{\"name\":\"" + INPUT_NAME + "\", " +
                    "\"description\":\"A directory containing some files\", \"type\":\"directory\", \"required\":true}";
    private static final String COMMAND_JSON_TEMPLATE =
            "{\"name\":\"test_command\", \"description\":\"The command for the test\", " +
                    "\"info-url\":\"http://abc.xyz\", \"env\":{\"foo\":\"bar\"}, " +
                    "\"inputs\":[" + COMMAND_INPUT_JSON + "], " +
                    "\"outputs\":[], " +
                    "\"template\":\"foo\", \"type\":\"docker-image\", " +
                    "\"image\":{\"id\":%d}}";

    private static final String ACTION_INPUT_JSON =
            "{\"root-context-property-name\":\"some_identifier\", " +
                    "\"command-input-name\":\"" + INPUT_NAME + "\", " +
                    "\"root\":" + SCAN_MATCH_TREE_NODE_JSON + ", " +
                    "\"required\":true}";
    private static final String ACTION_JSON_TEMPLATE =
            "{\"name\":\"an_action\", \"description\":\"Yep, it's an action all right\", " +
                    "\"command-id\":%d, " +
                    "\"inputs\":[" + ACTION_INPUT_JSON + "]}";

    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private ActionService actionService;

    @Autowired
    private CommandService commandService;

    @Autowired
    private DockerImageService dockerImageService;

    @Test
    public void testDeserializeMatcher() throws Exception {
        final Matcher matcher = mapper.readValue(DICOM_RESOURCE_MATCHER_JSON, Matcher.class);

        //assertTrue(StringMatcher.class.isAssignableFrom(matcher.getClass()));
        assertEquals("RESOURCE_NAME", matcher.getValue());
        assertEquals("equals", matcher.getOperator());
        assertEquals("name", matcher.getProperty());
    }

    @Test
    public void testDeserializeMatchTreeNodeNoParentNoChild() throws Exception {
        final Matcher resourceMatcher = mapper.readValue(DICOM_RESOURCE_MATCHER_JSON, Matcher.class);
        final MatchTreeNode resourceMtNode = mapper.readValue(RESOURCE_MATCH_TREE_NODE_JSON, MatchTreeNode.class);

        assertEquals(true, resourceMtNode.getProvidesInputValue());
        assertEquals("resource", resourceMtNode.getType());
        assertEquals("files", resourceMtNode.getInputProperty());
        assertEquals(Lists.newArrayList(resourceMatcher), resourceMtNode.getMatchers());
        assertEquals(null, resourceMtNode.getChildren());
    }

    @Test
    public void testDeserializeMatchTreeNodeWithChild() throws Exception {
        final MatchTreeNode resourceMtNode = mapper.readValue(RESOURCE_MATCH_TREE_NODE_JSON, MatchTreeNode.class);
        final MatchTreeNode scanMtNode = mapper.readValue(SCAN_MATCH_TREE_NODE_JSON, MatchTreeNode.class);

        assertEquals(false, scanMtNode.getProvidesInputValue());
        assertEquals("scan", scanMtNode.getType());
        assertThat(scanMtNode.getInputProperty(), isEmptyOrNullString());
        assertThat(scanMtNode.getMatchers(), emptyCollectionOf(Matcher.class));
        assertEquals(Lists.newArrayList(resourceMtNode), scanMtNode.getChildren());
    }

    @Test
    public void testDeserializeActionInput() throws Exception {
        final MatchTreeNode scanMtNode = mapper.readValue(SCAN_MATCH_TREE_NODE_JSON, MatchTreeNode.class);

        final String commandJson = String.format(COMMAND_JSON_TEMPLATE, 0);
        final Command command = mapper.readValue(commandJson, Command.class);
        final CommandInput commandInput = command.getInputs().get(0);

        final ActionInput actionInput = mapper.readValue(ACTION_INPUT_JSON, ActionInput.class);

        assertEquals("some_identifier", actionInput.getRootContextPropertyName());
        assertEquals(scanMtNode, actionInput.getRoot());
        assertEquals(commandInput.getName(), actionInput.getCommandInputName());
    }

    @Test
    public void testDeserializeAction() throws Exception {

        final ActionInput actionInput = mapper.readValue(ACTION_INPUT_JSON, ActionInput.class);

        final String actionJson = String.format(ACTION_JSON_TEMPLATE, 0);
        final ActionDto actionDto = mapper.readValue(actionJson, ActionDto.class);

        assertEquals("an_action", actionDto.getName());
        assertEquals("Yep, it's an action all right", actionDto.getDescription());
        assertEquals(Long.valueOf(0), actionDto.getCommandId());
        assertEquals(actionInput, actionDto.getInputs().get(0));
    }

    @Test
    public void testPersistAction() throws Exception {
        final DockerImage image = mapper.readValue(DOCKER_IMAGE_JSON, DockerImage.class);
        dockerImageService.create(image);

        final String commandJson = String.format(COMMAND_JSON_TEMPLATE, image.getId());
        final Command command = mapper.readValue(commandJson, Command.class);
        commandService.create(command);

        final String actionJson = String.format(ACTION_JSON_TEMPLATE, command.getId());
        final ActionDto actionDto = mapper.readValue(actionJson, ActionDto.class);
        final Action action = actionService.createFromDto(actionDto);

        final Action retrievedAction = actionService.retrieve(action.getId());

        assertEquals(action, retrievedAction);
    }
}
