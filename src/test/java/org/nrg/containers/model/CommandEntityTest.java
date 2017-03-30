package org.nrg.containers.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.nrg.containers.config.CommandTestConfig;
import org.nrg.containers.model.command.auto.Command;
import org.nrg.containers.model.command.entity.CommandEntity;
import org.nrg.containers.model.command.entity.CommandInputEntity;
import org.nrg.containers.model.command.entity.CommandMountEntity;
import org.nrg.containers.model.command.entity.CommandOutputEntity;
import org.nrg.containers.model.command.entity.CommandWrapperDerivedInputEntity;
import org.nrg.containers.model.command.entity.CommandWrapperEntity;
import org.nrg.containers.model.command.entity.CommandWrapperExternalInputEntity;
import org.nrg.containers.model.command.entity.CommandWrapperInputType;
import org.nrg.containers.model.command.entity.CommandWrapperOutputEntity;
import org.nrg.containers.model.command.entity.DockerCommandEntity;
import org.nrg.containers.services.CommandEntityService;
import org.nrg.framework.exceptions.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.List;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
@ContextConfiguration(classes = CommandTestConfig.class)
public class CommandEntityTest {
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
    private static final String OUTPUT_HANDLER_NAME = "output-handler-name";
    private static final String XNAT_COMMAND_WRAPPER_OUTPUT_HANDLER = "{" +
            "\"type\": \"Resource\"" +
            ", \"accepts-command-output\": \"" + COMMAND_OUTPUT_NAME + "\"" +
            ", \"as-a-child-of-xnat-input\": \"" + EXTERNAL_INPUT_NAME + "\"" +
            ", \"label\": \"" + OUTPUT_HANDLER_LABEL + "\"" +
            ", \"name\": \"" + OUTPUT_HANDLER_NAME + "\"" +
            "}";

    private static final String XNAT_COMMAND_WRAPPER_NAME = "wrappername";
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
    @Autowired private CommandEntityService commandEntityService;

    @Rule public ExpectedException expectedException = ExpectedException.none();

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
                mapper.readValue(STRING_INPUT_JSON, CommandInputEntity.class);
        final Command.CommandInput fooInput = Command.CommandInput.create(fooInputEntity);

        assertThat(commandInput0.name(), is("my_cool_input"));
        assertThat(commandInput0.description(), is("A boolean value"));
        assertThat(commandInput0.type(), is(CommandInputEntity.Type.BOOLEAN.getName()));
        assertTrue(commandInput0.required());
        assertThat(commandInput0.trueValue(), is("-b"));
        assertThat(commandInput0.falseValue(), is(""));
        assertThat(commandInput0.replacementKey(), is("#my_cool_input#"));
        assertThat(commandInput0.commandLineFlag(), is(""));
        assertThat(commandInput0.commandLineSeparator(), is(" "));
        assertThat(commandInput0.defaultValue(), is(nullValue()));

        assertThat(fooInput.name(), is("foo"));
        assertThat(fooInput.description(), is("A foo that bars"));
        assertThat(fooInput.type(), is(CommandInputEntity.Type.STRING.getName()));
        assertThat(fooInput.required(), is(false));
        assertThat(fooInput.trueValue(), is(nullValue()));
        assertThat(fooInput.falseValue(), is(nullValue()));
        assertThat(fooInput.replacementKey(), is("#foo#"));
        assertThat(fooInput.commandLineFlag(), is("--flag"));
        assertThat(fooInput.commandLineSeparator(), is("="));
        assertThat(fooInput.defaultValue(), is("bar"));
    }

    @Test
    public void testDeserializeDockerImageCommand() throws Exception {

        final List<CommandInputEntity> commandInputEntityList =
                mapper.readValue(INPUT_LIST_JSON, new TypeReference<List<CommandInputEntity>>() {});
        final CommandOutputEntity commandOutputEntity = mapper.readValue(COMMAND_OUTPUT, CommandOutputEntity.class);

        final CommandMountEntity input = mapper.readValue(MOUNT_IN, CommandMountEntity.class);
        final CommandMountEntity output = mapper.readValue(MOUNT_OUT, CommandMountEntity.class);

        final CommandEntity commandEntity = mapper.readValue(DOCKER_IMAGE_COMMAND_JSON, CommandEntity.class);
        for (final CommandInputEntity commandInputEntity : commandInputEntityList) {
            commandInputEntity.setCommandEntity(commandEntity);
        }
        commandOutputEntity.setCommandEntity(commandEntity);
        input.setCommandEntity(commandEntity);
        output.setCommandEntity(commandEntity);

        assertThat(commandEntity.getImage(), is("abc123"));

        assertThat(commandEntity.getName(), is("docker_image_command"));
        assertThat(commandEntity.getDescription(), is("Docker Image command for the test"));
        assertThat(commandEntity.getInfoUrl(), is("http://abc.xyz"));
        assertThat(commandEntity.getInputs(), is(commandInputEntityList));
        assertThat(commandEntity.getOutputs(), contains(commandOutputEntity));

        // final CommandRun run = command.getRun();
        assertThat(commandEntity.getCommandLine(), is("cmd #foo# #my_cool_input#"));
        assertThat(commandEntity.getEnvironmentVariables(), hasEntry("foo", "bar"));
        assertThat(commandEntity.getMounts(), contains(input, output));

        assertThat(commandEntity, instanceOf(DockerCommandEntity.class));
        assertThat(((DockerCommandEntity) commandEntity).getPorts(), hasEntry("22", "2222"));
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

        assertThat(retrievedCommandEntity, is(commandEntity));

        assertThat(Command.create(commandEntity).validate(), is(Matchers.<String>emptyIterable()));
    }

    @Test
    public void testDeserializeXnatCommandInputsAndOutputs() throws Exception {
        final CommandWrapperExternalInputEntity externalInput = mapper.readValue(XNAT_COMMAND_WRAPPER_EXTERNAL_INPUT, CommandWrapperExternalInputEntity.class);
        assertThat(externalInput.getName(), is(EXTERNAL_INPUT_NAME));
        assertThat(externalInput.getType(), is(CommandWrapperInputType.SESSION));
        assertThat(externalInput.getProvidesValueForCommandInput(), is(nullValue()));
        assertThat(externalInput.getDefaultValue(), is(nullValue()));
        assertThat(externalInput.getMatcher(), is(nullValue()));
        assertThat(externalInput.getRequired(), is(false));

        final CommandWrapperDerivedInputEntity derivedInput = mapper.readValue(XNAT_COMMAND_WRAPPER_DERIVED_INPUT, CommandWrapperDerivedInputEntity.class);
        assertThat(derivedInput.getName(), is(DERIVED_INPUT_NAME));
        assertThat(derivedInput.getType(), is(CommandWrapperInputType.STRING));
        assertThat(derivedInput.getDerivedFromXnatInput(), is(EXTERNAL_INPUT_NAME));
        assertThat(derivedInput.getDerivedFromXnatObjectProperty(), is(XNAT_OBJECT_PROPERTY));
        assertThat(derivedInput.getProvidesValueForCommandInput(), is(STRING_INPUT_NAME));
        assertThat(derivedInput.getDefaultValue(), is(nullValue()));
        assertThat(derivedInput.getMatcher(), is(nullValue()));
        assertThat(derivedInput.getRequired(), is(false));

        final CommandWrapperOutputEntity output = mapper.readValue(XNAT_COMMAND_WRAPPER_OUTPUT_HANDLER, CommandWrapperOutputEntity.class);
        assertThat(output.getType(), is(CommandWrapperOutputEntity.Type.RESOURCE));
        assertThat(output.getXnatInputName(), is(EXTERNAL_INPUT_NAME));
        assertThat(output.getCommandOutputName(), is(COMMAND_OUTPUT_NAME));
        assertThat(output.getLabel(), is(OUTPUT_HANDLER_LABEL));
        assertThat(output.getName(), is(OUTPUT_HANDLER_NAME));
    }

    @Test
    public void testDeserializeCommandWithCommandWrapper() throws Exception {

        final CommandWrapperEntity commandWrapperEntity = mapper.readValue(XNAT_COMMAND_WRAPPER, CommandWrapperEntity.class);

        final CommandEntity commandEntity = mapper.readValue(DOCKER_IMAGE_COMMAND_JSON, CommandEntity.class);

        assertThat(commandEntity.getCommandWrapperEntities(), hasSize(1));
        assertTrue(commandEntity.getCommandWrapperEntities().contains(commandWrapperEntity));
    }

    @Test
    @DirtiesContext
    public void testPersistCommandWithWrapper() throws Exception {

        final CommandEntity commandEntity = mapper.readValue(DOCKER_IMAGE_COMMAND_JSON, CommandEntity.class);

        commandEntityService.create(commandEntity);

        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        final CommandEntity retrievedCommandEntity = commandEntityService.retrieve(commandEntity.getId());

        assertThat(retrievedCommandEntity, is(commandEntity));

        final List<CommandWrapperEntity> commandWrappers = retrievedCommandEntity.getCommandWrapperEntities();
        assertThat(commandWrappers, hasSize(1));

        final CommandWrapperEntity commandWrapperEntity = commandWrappers.get(0);
        assertThat(commandWrapperEntity.getId(), not(0L));
        assertThat(commandWrapperEntity.getCommandEntity(), is(commandEntity));

        assertThat(Command.create(commandEntity).validate(), is(Matchers.<String>emptyIterable()));
    }

    @Test
    @DirtiesContext
    public void testDeleteCommandWithWrapper() throws Exception {

        final CommandEntity commandEntity = mapper.readValue(DOCKER_IMAGE_COMMAND_JSON, CommandEntity.class);

        final CommandEntity created = commandEntityService.create(commandEntity);

        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        commandEntityService.delete(created);

        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        assertThat(commandEntityService.retrieve(created.getId()), is(nullValue()));
    }

    @Test
    @DirtiesContext
    public void testRetrieveCommandWrapper() throws Exception {

        final CommandEntity commandEntity = mapper.readValue(DOCKER_IMAGE_COMMAND_JSON, CommandEntity.class);

        final CommandEntity created = commandEntityService.create(commandEntity);

        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        final CommandWrapperEntity createdWrapper = created.getCommandWrapperEntities().get(0);
        final long wrapperId = createdWrapper.getId();
        assertThat(commandEntityService.retrieve(created, wrapperId), is(createdWrapper));

        assertThat(Command.create(created).validate(), is(Matchers.<String>emptyIterable()));
    }

    @Test
    @DirtiesContext
    public void testAddCommandWrapper() throws Exception {

        final CommandEntity commandEntity = mapper.readValue(DOCKER_IMAGE_COMMAND_JSON, CommandEntity.class);
        final CommandWrapperEntity toAdd = commandEntity.getCommandWrapperEntities().get(0);
        commandEntity.setCommandWrapperEntities(null);

        final CommandEntity created = commandEntityService.create(commandEntity);

        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        final CommandWrapperEntity added = commandEntityService.addWrapper(created, toAdd);

        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        final CommandEntity retrieved = commandEntityService.get(commandEntity.getId());
        assertThat(retrieved.getCommandWrapperEntities().get(0), is(added));

        assertThat(Command.create(retrieved).validate(), is(Matchers.<String>emptyIterable()));
    }

    @Test
    @DirtiesContext
    public void testUpdateCommandWrapper() throws Exception {

        final CommandEntity commandEntity = mapper.readValue(DOCKER_IMAGE_COMMAND_JSON, CommandEntity.class);

        final CommandEntity created = commandEntityService.create(commandEntity);

        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        final CommandWrapperEntity createdWrapper = created.getCommandWrapperEntities().get(0);

        final String newDescription = "This is probably a new description, right?";
        createdWrapper.setDescription(newDescription);
        final CommandWrapperEntity updated = commandEntityService.update(createdWrapper);
        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        assertThat(updated.getDescription(), is(newDescription));

        assertThat(Command.create(created).validate(), is(Matchers.<String>emptyIterable()));
    }

    @Test
    @DirtiesContext
    public void testDeleteCommandWrapper() throws Exception {

        final CommandEntity commandEntity = mapper.readValue(DOCKER_IMAGE_COMMAND_JSON, CommandEntity.class);

        final CommandEntity created = commandEntityService.create(commandEntity);

        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        final long commandId = created.getId();
        final long wrapperId = created.getCommandWrapperEntities().get(0).getId();
        commandEntityService.delete(commandId, wrapperId);

        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        assertThat(commandEntityService.retrieve(commandId, wrapperId), is(nullValue()));
    }

    @Test
    public void testCreateEcatHeaderDump() throws Exception {
        // A User was attempting to create the command in this resource.
        // Spring didn't tell us why. See CS-70.
        final String dir = Resources.getResource("ecatHeaderDump").getPath().replace("%20", " ");
        final String commandJsonFile = dir + "/command.json";
        final CommandEntity ecatHeaderDump = mapper.readValue(new File(commandJsonFile), CommandEntity.class);
        commandEntityService.create(ecatHeaderDump);
    }

    @Test
    @DirtiesContext
    public void testAssertPairExists() throws Exception {
        final CommandEntity commandEntity = mapper.readValue(DOCKER_IMAGE_COMMAND_JSON, CommandEntity.class);
        commandEntityService.create(commandEntity);

        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        commandEntityService.assertPairExists(commandEntity.getId(), XNAT_COMMAND_WRAPPER_NAME);

        final String fakeWrapperName = "blargle";
        expectedException.expect(NotFoundException.class);
        expectedException.expectMessage("Command " + String.valueOf(commandEntity.getId()) +
                " has no wrapper " + fakeWrapperName);
        commandEntityService.assertPairExists(commandEntity.getId(), fakeWrapperName);
    }
}
