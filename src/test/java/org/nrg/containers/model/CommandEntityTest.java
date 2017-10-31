package org.nrg.containers.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.CustomTypeSafeMatcher;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.nrg.containers.config.CommandTestConfig;
import org.nrg.containers.model.command.auto.Command;
import org.nrg.containers.model.command.auto.Command.CommandInput;
import org.nrg.containers.model.command.auto.Command.CommandMount;
import org.nrg.containers.model.command.auto.Command.CommandOutput;
import org.nrg.containers.model.command.auto.Command.CommandWrapper;
import org.nrg.containers.model.command.auto.Command.CommandWrapperDerivedInput;
import org.nrg.containers.model.command.auto.Command.CommandWrapperExternalInput;
import org.nrg.containers.model.command.auto.Command.CommandWrapperOutput;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.nio.file.Paths;
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

    private Command COMMAND;
    private CommandEntity COMMAND_ENTITY;

    private final String OUTPUT_MOUNT_NAME = "out";
    private CommandMount MOUNT_IN;
    private CommandMountEntity MOUNT_ENTITY_IN;
    private CommandMount MOUNT_OUT;
    private CommandMountEntity MOUNT_ENTITY_OUT;

    private final String STRING_INPUT_NAME = "foo";
    private CommandInput STRING_INPUT;
    private CommandInputEntity STRING_INPUT_ENTITY;
    private CommandInput COOL_INPUT;
    private CommandInputEntity COOL_INPUT_ENTITY;

    private final String COMMAND_OUTPUT_NAME = "the_output";
    private CommandOutput COMMAND_OUTPUT;
    private CommandOutputEntity COMMAND_OUTPUT_ENTITY;

    private final String EXTERNAL_INPUT_NAME = "session";
    private CommandWrapperExternalInput EXTERNAL_INPUT;
    private CommandWrapperExternalInputEntity EXTERNAL_INPUT_ENTITY;

    private final String DERIVED_INPUT_NAME = "label";
    private final String XNAT_OBJECT_PROPERTY = "label";
    private CommandWrapperDerivedInput DERIVED_INPUT;
    private CommandWrapperDerivedInputEntity DERIVED_INPUT_ENTITY;

    private final String OUTPUT_HANDLER_LABEL = "a_label";
    private final String OUTPUT_HANDLER_NAME = "output-handler-name";
    private CommandWrapperOutput OUTPUT_HANDLER;
    private CommandWrapperOutputEntity OUTPUT_HANDLER_ENTITY;

    private final String COMMAND_WRAPPER_NAME = "wrappername";
    private final String COMMAND_WRAPPER_DESC = "the wrapper description";
    private CommandWrapper COMMAND_WRAPPER;
    private CommandWrapperEntity COMMAND_WRAPPER_ENTITY;


    @Autowired private ObjectMapper mapper;
    @Autowired private CommandEntityService commandEntityService;

    @Rule public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setup() throws Exception {
        MOUNT_IN = CommandMount.create("in", false, "/input");
        MOUNT_OUT = CommandMount.create(OUTPUT_MOUNT_NAME, true, "/output");
        MOUNT_ENTITY_IN = CommandMountEntity.fromPojo(MOUNT_IN);
        MOUNT_ENTITY_OUT = CommandMountEntity.fromPojo(MOUNT_OUT);

        COOL_INPUT = CommandInput.builder()
                .name("my_cool_input")
                .description("A boolean value")
                .type("boolean")
                .required(true)
                .trueValue("-b")
                .falseValue("")
                .build();
        STRING_INPUT = CommandInput.builder()
                .name(STRING_INPUT_NAME)
                .description("A foo that bars")
                .required(false)
                .defaultValue("bar")
                .commandLineFlag("--flag")
                .commandLineSeparator("=")
                .build();
        COOL_INPUT_ENTITY = CommandInputEntity.fromPojo(COOL_INPUT);
        STRING_INPUT_ENTITY = CommandInputEntity.fromPojo(STRING_INPUT);

        COMMAND_OUTPUT = CommandOutput.builder()
                .name(COMMAND_OUTPUT_NAME)
                .description("It's the output")
                .mount(OUTPUT_MOUNT_NAME)
                .path("relative/path/to/dir")
                .build();
        COMMAND_OUTPUT_ENTITY = CommandOutputEntity.fromPojo(COMMAND_OUTPUT);

        EXTERNAL_INPUT = CommandWrapperExternalInput.builder()
                .name(EXTERNAL_INPUT_NAME)
                .type("Session")
                .build();
        EXTERNAL_INPUT_ENTITY = CommandWrapperExternalInputEntity.fromPojo(EXTERNAL_INPUT);

        DERIVED_INPUT = CommandWrapperDerivedInput.builder()
                .name(DERIVED_INPUT_NAME)
                .type("string")
                .derivedFromWrapperInput(EXTERNAL_INPUT_NAME)
                .derivedFromXnatObjectProperty(XNAT_OBJECT_PROPERTY)
                .providesValueForCommandInput(STRING_INPUT_NAME)
                .build();
        DERIVED_INPUT_ENTITY = CommandWrapperDerivedInputEntity.fromPojo(DERIVED_INPUT);

        OUTPUT_HANDLER = CommandWrapperOutput.create(OUTPUT_HANDLER_NAME,
                COMMAND_OUTPUT_NAME, EXTERNAL_INPUT_NAME, "Resource", OUTPUT_HANDLER_LABEL);
        OUTPUT_HANDLER_ENTITY = CommandWrapperOutputEntity.fromPojo(OUTPUT_HANDLER);

        COMMAND_WRAPPER = CommandWrapper.builder()
                .name(COMMAND_WRAPPER_NAME)
                .description(COMMAND_WRAPPER_DESC)
                .addExternalInput(EXTERNAL_INPUT)
                .addDerivedInput(DERIVED_INPUT)
                .addOutputHandler(OUTPUT_HANDLER)
                .build();
        COMMAND_WRAPPER_ENTITY = CommandWrapperEntity.fromPojo(COMMAND_WRAPPER);

        COMMAND = Command.builder()
                .name("docker_image_command")
                .description("Docker Image command for the test")
                .image("abc123")
                .type("docker")
                .infoUrl("http://abc.xyz")
                .addEnvironmentVariable("foo", "bar")
                .commandLine("cmd #foo# #my_cool_input#")
                .addMount(MOUNT_IN)
                .addMount(MOUNT_OUT)
                .addInput(COOL_INPUT)
                .addInput(STRING_INPUT)
                .addOutput(COMMAND_OUTPUT)
                .addPort("22", "2222")
                .addCommandWrapper(COMMAND_WRAPPER)
                .build();

        COMMAND_ENTITY = CommandEntity.fromPojo(COMMAND);

    }

    @Test
    public void testSpringConfiguration() {
        assertThat(commandEntityService, not(nullValue()));
    }

    @Test
    public void testSerializeDeserializeCommand() throws Exception {
        assertThat(mapper.readValue(mapper.writeValueAsString(COMMAND), Command.class), is(COMMAND));
    }

    @Test
    @DirtiesContext
    public void testPersistCommandWithWrapper() throws Exception {
        final CommandEntity created = commandEntityService.create(COMMAND_ENTITY);

        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        final CommandEntity retrievedCommandEntity = commandEntityService.retrieve(created.getId());

        assertThat(retrievedCommandEntity, is(created));
        assertThat(Command.create(created).validate(), is(Matchers.<String>emptyIterable()));

        final List<CommandWrapperEntity> commandWrappers = retrievedCommandEntity.getCommandWrapperEntities();
        assertThat(commandWrappers, hasSize(1));

        final CommandWrapperEntity commandWrapperEntity = commandWrappers.get(0);
        assertThat(commandWrapperEntity.getId(), not(0L));
        assertThat(commandWrapperEntity.getCommandEntity(), is(created));
    }

    @Test
    @DirtiesContext
    public void testDeleteCommandWithWrapper() throws Exception {

        final CommandEntity created = commandEntityService.create(COMMAND_ENTITY);

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

        final CommandEntity created = commandEntityService.create(COMMAND_ENTITY);

        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        final CommandWrapperEntity createdWrapper = created.getCommandWrapperEntities().get(0);
        final long wrapperId = createdWrapper.getId();
        assertThat(commandEntityService.retrieveWrapper(wrapperId), is(createdWrapper));

        assertThat(Command.create(created).validate(), is(Matchers.<String>emptyIterable()));
    }

    @Test
    @DirtiesContext
    public void testAddCommandWrapper() throws Exception {

        final CommandWrapperEntity toAdd = COMMAND_ENTITY.getCommandWrapperEntities().get(0);
        COMMAND_ENTITY.setCommandWrapperEntities(null);

        final CommandEntity created = commandEntityService.create(COMMAND_ENTITY);

        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        final CommandWrapperEntity added = commandEntityService.addWrapper(created, toAdd);

        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        final CommandEntity retrieved = commandEntityService.get(COMMAND_ENTITY.getId());
        assertThat(retrieved.getCommandWrapperEntities().get(0), is(added));

        assertThat(Command.create(retrieved).validate(), is(Matchers.<String>emptyIterable()));
    }

    @Test
    @DirtiesContext
    public void testUpdateCommandWrapperDescription() throws Exception {

        final CommandEntity created = commandEntityService.create(COMMAND_ENTITY);

        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        final CommandWrapperEntity createdWrapper = created.getCommandWrapperEntities().get(0);

        final String newDescription = "This is probably a new description, right?";
        createdWrapper.setDescription(newDescription);

        commandEntityService.update(created);
        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        final CommandEntity retrieved = commandEntityService.get(created.getId());
        final CommandWrapperEntity retrievedWrapper = retrieved.getCommandWrapperEntities().get(0);

        assertThat(retrievedWrapper.getDescription(), is(newDescription));
        assertThat(Command.create(retrieved).validate(), is(Matchers.<String>emptyIterable()));
    }

    @Test
    @DirtiesContext
    public void testUpdateAddInput() throws Exception {

        final CommandEntity created = commandEntityService.create(COMMAND_ENTITY);

        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        final CommandInput inputToAdd = CommandInput.builder()
                .name("this is new")
                .description("A new input that didn't exist before")
                .commandLineFlag("--flag")
                .commandLineSeparator("=")
                .defaultValue("yes")
                .build();
        created.addInput(CommandInputEntity.fromPojo(inputToAdd));

        commandEntityService.update(created);
        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        final CommandEntity retrieved = commandEntityService.get(created.getId());

        final Command retrievedPojo = Command.create(retrieved);
        assertThat(inputToAdd, isInIgnoreId(retrievedPojo.inputs()));
        assertThat(retrievedPojo.validate(), is(Matchers.<String>emptyIterable()));
    }

    private Matcher<CommandInput> isInIgnoreId(final List<CommandInput> expected) {
        final String description = "a CommandInput equal to (other than the ID) one of " + expected;
        return new CustomTypeSafeMatcher<CommandInput>(description) {
            @Override
            protected boolean matchesSafely(final CommandInput actual) {
                for (final CommandInput input : expected) {
                    final CommandInput actualWithSameId =
                            actual.toBuilder().id(input.id()).build();
                    if (input.equals(actualWithSameId)) {
                        return true;
                    }
                }
                return false;
            }
        };
    }

    @Test
    @DirtiesContext
    public void testDeleteCommandWrapper() throws Exception {

        final CommandEntity created = commandEntityService.create(COMMAND_ENTITY);

        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        final long wrapperId = created.getCommandWrapperEntities().get(0).getId();
        commandEntityService.deleteWrapper(wrapperId);

        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        assertThat(commandEntityService.retrieveWrapper(wrapperId), is(nullValue()));
    }

    @Test
    public void testCreateEcatHeaderDump() throws Exception {
        // A User was attempting to create the command in this resource.
        // Spring didn't tell us why. See CS-70.
        final String dir = Paths.get(ClassLoader.getSystemResource("ecatHeaderDump").toURI()).toString().replace("%20", " ");
        final String commandJsonFile = dir + "/command.json";
        final CommandEntity ecatHeaderDump = mapper.readValue(new File(commandJsonFile), CommandEntity.class);
        commandEntityService.create(ecatHeaderDump);
    }
}
