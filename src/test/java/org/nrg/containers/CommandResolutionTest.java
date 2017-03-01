package org.nrg.containers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.nrg.config.services.ConfigService;
import org.nrg.containers.config.IntegrationTestConfig;
import org.nrg.containers.model.ResolvedCommand;
import org.nrg.containers.model.ResolvedDockerCommand;
import org.nrg.containers.model.auto.Command;
import org.nrg.containers.model.auto.Command.CommandWrapper;
import org.nrg.containers.model.xnat.Project;
import org.nrg.containers.model.xnat.Resource;
import org.nrg.containers.model.xnat.Scan;
import org.nrg.containers.model.xnat.Session;
import org.nrg.containers.services.CommandService;
import org.nrg.containers.services.ContainerLaunchService;
import org.nrg.xft.security.UserI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = IntegrationTestConfig.class)
@Transactional
public class CommandResolutionTest {
    private static final Logger log = LoggerFactory.getLogger(CommandResolutionTest.class);
    private final String BUSYBOX_LATEST = "busybox:latest";

    private UserI mockUser;
    private Command dummyCommand;
    private String resourceDir;
    private Map<String, CommandWrapper> xnatCommandWrappers;

    @Autowired private ObjectMapper mapper;
    @Autowired private CommandService commandService;
    @Autowired private ContainerLaunchService containerLaunchService;
    @Autowired private ConfigService mockConfigService;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder(new File("/tmp"));

    @Before
    public void setup() throws Exception {
        Configuration.setDefaults(new Configuration.Defaults() {

            private final JsonProvider jsonProvider = new JacksonJsonProvider();
            private final MappingProvider mappingProvider = new JacksonMappingProvider();

            @Override
            public JsonProvider jsonProvider() {
                return jsonProvider;
            }

            @Override
            public MappingProvider mappingProvider() {
                return mappingProvider;
            }

            @Override
            public Set<Option> options() {
                return Sets.newHashSet(Option.DEFAULT_PATH_LEAF_TO_NULL);
            }
        });

        mockUser = Mockito.mock(UserI.class);
        when(mockUser.getLogin()).thenReturn("mockUser");

        resourceDir = Resources.getResource("commandResolutionTest").getPath().replace("%20", " ");
        final String commandJsonFile = resourceDir + "/command.json";
        dummyCommand = mapper.readValue(new File(commandJsonFile), Command.class);
        commandService.create(dummyCommand);

        xnatCommandWrappers = Maps.newHashMap();
        for (final CommandWrapper commandWrapperEntity : dummyCommand.xnatCommandWrappers()) {
            xnatCommandWrappers.put(commandWrapperEntity.name(), commandWrapperEntity);
        }
    }

    @Test
    public void testSessionScanResource() throws Exception {
        final String commandWrapperName = "session-scan-resource";
        final String inputPath = resourceDir + "/testSessionScanResource/session.json";

        // I want to set a resource directory at runtime, so pardon me while I do some unchecked stuff with the values I just read
        final String dicomDir = folder.newFolder("DICOM").getAbsolutePath();
        final Session session = mapper.readValue(new File(inputPath), Session.class);
        session.getScans().get(0).getResources().get(0).setDirectory(dicomDir);
        final String sessionRuntimeJson = mapper.writeValueAsString(session);

        final Map<String, String> runtimeValues = Maps.newHashMap();
        runtimeValues.put("session", sessionRuntimeJson);

        final CommandWrapper commandWrapper = xnatCommandWrappers.get(commandWrapperName);
        assertNotNull(commandWrapper);

        final ResolvedCommand resolvedCommand = containerLaunchService.resolveCommand(commandWrapper, dummyCommand, runtimeValues, mockUser);
        assertEquals((Long) dummyCommand.id(), resolvedCommand.getCommandId());
        assertEquals((Long) commandWrapper.id(), resolvedCommand.getXnatCommandWrapperId());
        assertEquals(dummyCommand.image(), resolvedCommand.getImage());
        assertEquals(dummyCommand.commandLine(), resolvedCommand.getCommandLine());
        assertTrue(resolvedCommand.getEnvironmentVariables().isEmpty());
        assertTrue(resolvedCommand.getMounts().isEmpty());
        assertThat(resolvedCommand, instanceOf(ResolvedDockerCommand.class));
        assertTrue(((ResolvedDockerCommand) resolvedCommand).getPorts().isEmpty());

        // Raw inputs
        assertEquals(runtimeValues, resolvedCommand.getRawInputValues());

        // xnat wrapper inputs
        final Map<String, String> expectedXnatInputValues = Maps.newHashMap();
        expectedXnatInputValues.put("T1-scantype", "\"SCANTYPE\", \"OTHER_SCANTYPE\"");
        expectedXnatInputValues.put("session", session.getUri());
        expectedXnatInputValues.put("scan", session.getScans().get(0).getUri());
        expectedXnatInputValues.put("dicom", session.getScans().get(0).getResources().get(0).getUri());
        expectedXnatInputValues.put("scan-id", session.getScans().get(0).getId());
        assertEquals(expectedXnatInputValues, resolvedCommand.getXnatInputValues());

        // command inputs
        final Map<String, String> expectedCommandInputValues = Maps.newHashMap();
        expectedCommandInputValues.put("file-path", null);
        expectedCommandInputValues.put("whatever", session.getScans().get(0).getId());
        assertEquals(expectedCommandInputValues, resolvedCommand.getCommandInputValues());

        // Outputs
        assertTrue(resolvedCommand.getOutputs().isEmpty());
    }

    @Test
    public void testResourceFile() throws Exception {
        final String commandWrapperName = "scan-resource-file";
        final String inputPath = resourceDir + "/testResourceFile/scan.json";

        // I want to set a resource directory at runtime, so pardon me while I do some unchecked stuff with the values I just read
        final String resourceDir = folder.newFolder("resource").getAbsolutePath();
        final Scan scan = mapper.readValue(new File(inputPath), Scan.class);
        final Resource resource = scan.getResources().get(0);
        resource.setDirectory(resourceDir);
        resource.getFiles().get(0).setPath(resourceDir + "/" + resource.getFiles().get(0).getName());
        final String scanRuntimeJson = mapper.writeValueAsString(scan);

        final Map<String, String> runtimeValues = Maps.newHashMap();
        runtimeValues.put("a scan", scanRuntimeJson);

        final CommandWrapper commandWrapper = xnatCommandWrappers.get(commandWrapperName);
        assertNotNull(commandWrapper);

        final ResolvedCommand resolvedCommand = containerLaunchService.resolveCommand(commandWrapper, dummyCommand, runtimeValues, mockUser);
        assertEquals((Long) dummyCommand.id(), resolvedCommand.getCommandId());
        assertEquals((Long) commandWrapper.id(), resolvedCommand.getXnatCommandWrapperId());
        assertEquals(dummyCommand.image(), resolvedCommand.getImage());
        assertEquals(dummyCommand.commandLine(), resolvedCommand.getCommandLine());
        assertTrue(resolvedCommand.getEnvironmentVariables().isEmpty());
        assertTrue(resolvedCommand.getMounts().isEmpty());
        assertThat(resolvedCommand, instanceOf(ResolvedDockerCommand.class));
        assertTrue(((ResolvedDockerCommand) resolvedCommand).getPorts().isEmpty());

        // Raw inputs
        assertEquals(runtimeValues, resolvedCommand.getRawInputValues());

        // xnat wrapper inputs
        final Map<String, String> expectedXnatInputValues = Maps.newHashMap();
        expectedXnatInputValues.put("a scan", scan.getUri());
        expectedXnatInputValues.put("a resource", resource.getUri());
        expectedXnatInputValues.put("a file", resource.getFiles().get(0).getUri());
        expectedXnatInputValues.put("a file path", resource.getFiles().get(0).getPath());
        expectedXnatInputValues.put("scan-id", scan.getId());
        assertEquals(expectedXnatInputValues, resolvedCommand.getXnatInputValues());

        // command inputs
        final Map<String, String> expectedCommandInputValues = Maps.newHashMap();
        expectedCommandInputValues.put("file-path", resource.getFiles().get(0).getPath());
        expectedCommandInputValues.put("whatever", scan.getId());
        assertEquals(expectedCommandInputValues, resolvedCommand.getCommandInputValues());

        // Outputs
        assertTrue(resolvedCommand.getOutputs().isEmpty());
    }

    @Test
    public void testProject() throws Exception {
        final String commandWrapperName = "project";
        final String inputPath = resourceDir + "/testProject/project.json";

        // I want to set a resource directory at runtime, so pardon me while I do some unchecked stuff with the values I just read
        final Project project = mapper.readValue(new File(inputPath), Project.class);
        final String projectRuntimeJson = mapper.writeValueAsString(project);

        final Map<String, String> runtimeValues = Maps.newHashMap();
        runtimeValues.put("project", projectRuntimeJson);

        final CommandWrapper commandWrapper = xnatCommandWrappers.get(commandWrapperName);
        assertNotNull(commandWrapper);

        final ResolvedCommand resolvedCommand = containerLaunchService.resolveCommand(commandWrapper, dummyCommand, runtimeValues, mockUser);
        assertEquals((Long) dummyCommand.id(), resolvedCommand.getCommandId());
        assertEquals((Long) commandWrapper.id(), resolvedCommand.getXnatCommandWrapperId());
        assertEquals(dummyCommand.image(), resolvedCommand.getImage());
        assertEquals(dummyCommand.commandLine(), resolvedCommand.getCommandLine());
        assertTrue(resolvedCommand.getEnvironmentVariables().isEmpty());
        assertTrue(resolvedCommand.getMounts().isEmpty());
        assertThat(resolvedCommand, instanceOf(ResolvedDockerCommand.class));
        assertTrue(((ResolvedDockerCommand) resolvedCommand).getPorts().isEmpty());

        // Raw inputs
        assertEquals(runtimeValues, resolvedCommand.getRawInputValues());

        // xnat wrapper inputs
        final Map<String, String> expectedXnatInputValues = Maps.newHashMap();
        expectedXnatInputValues.put("project", project.getUri());
        expectedXnatInputValues.put("project-label", project.getLabel());
        assertEquals(expectedXnatInputValues, resolvedCommand.getXnatInputValues());

        // command inputs
        final Map<String, String> expectedCommandInputValues = Maps.newHashMap();
        expectedCommandInputValues.put("file-path", null);
        expectedCommandInputValues.put("whatever", project.getLabel());
        assertEquals(expectedCommandInputValues, resolvedCommand.getCommandInputValues());

        // Outputs
        assertTrue(resolvedCommand.getOutputs().isEmpty());
    }

    @Test
    public void testProjectSubject() throws Exception {
        final String commandWrapperName = "project-subject";
        final String inputPath = resourceDir + "/testProjectSubject/project.json";

        // I want to set a resource directory at runtime, so pardon me while I do some unchecked stuff with the values I just read
        final Project project = mapper.readValue(new File(inputPath), Project.class);
        final String projectRuntimeJson = mapper.writeValueAsString(project);

        final Map<String, String> runtimeValues = Maps.newHashMap();
        runtimeValues.put("project", projectRuntimeJson);

        final CommandWrapper commandWrapper = xnatCommandWrappers.get(commandWrapperName);
        assertNotNull(commandWrapper);

        final ResolvedCommand resolvedCommand = containerLaunchService.resolveCommand(commandWrapper, dummyCommand, runtimeValues, mockUser);
        assertEquals((Long) dummyCommand.id(), resolvedCommand.getCommandId());
        assertEquals((Long) commandWrapper.id(), resolvedCommand.getXnatCommandWrapperId());
        assertEquals(dummyCommand.image(), resolvedCommand.getImage());
        assertEquals(dummyCommand.commandLine(), resolvedCommand.getCommandLine());
        assertTrue(resolvedCommand.getEnvironmentVariables().isEmpty());
        assertTrue(resolvedCommand.getMounts().isEmpty());
        assertThat(resolvedCommand, instanceOf(ResolvedDockerCommand.class));
        assertTrue(((ResolvedDockerCommand) resolvedCommand).getPorts().isEmpty());

        // Raw inputs
        assertEquals(runtimeValues, resolvedCommand.getRawInputValues());

        // xnat wrapper inputs
        final Map<String, String> expectedXnatInputValues = Maps.newHashMap();
        expectedXnatInputValues.put("project", project.getUri());
        expectedXnatInputValues.put("subject", project.getSubjects().get(0).getUri());
        expectedXnatInputValues.put("project-label", project.getLabel());
        assertEquals(expectedXnatInputValues, resolvedCommand.getXnatInputValues());

        // command inputs
        final Map<String, String> expectedCommandInputValues = Maps.newHashMap();
        expectedCommandInputValues.put("file-path", null);
        expectedCommandInputValues.put("whatever", project.getLabel());
        assertEquals(expectedCommandInputValues, resolvedCommand.getCommandInputValues());

        // Outputs
        assertTrue(resolvedCommand.getOutputs().isEmpty());
    }

    @Test
    public void testSessionAssessor() throws Exception {
        final String commandWrapperName = "session-assessor";
        final String inputPath = resourceDir + "/testSessionAssessor/session.json";

        // I want to set a resource directory at runtime, so pardon me while I do some unchecked stuff with the values I just read
        final Session session = mapper.readValue(new File(inputPath), Session.class);
        final String sessionRuntimeJson = mapper.writeValueAsString(session);

        final Map<String, String> runtimeValues = Maps.newHashMap();
        runtimeValues.put("session", sessionRuntimeJson);

        final CommandWrapper commandWrapper = xnatCommandWrappers.get(commandWrapperName);
        assertNotNull(commandWrapper);

        final ResolvedCommand resolvedCommand = containerLaunchService.resolveCommand(commandWrapper, dummyCommand, runtimeValues, mockUser);
        assertEquals((Long) dummyCommand.id(), resolvedCommand.getCommandId());
        assertEquals((Long) commandWrapper.id(), resolvedCommand.getXnatCommandWrapperId());
        assertEquals(dummyCommand.image(), resolvedCommand.getImage());
        assertEquals(dummyCommand.commandLine(), resolvedCommand.getCommandLine());
        assertTrue(resolvedCommand.getEnvironmentVariables().isEmpty());
        assertTrue(resolvedCommand.getMounts().isEmpty());
        assertThat(resolvedCommand, instanceOf(ResolvedDockerCommand.class));
        assertTrue(((ResolvedDockerCommand) resolvedCommand).getPorts().isEmpty());

        // Raw inputs
        assertEquals(runtimeValues, resolvedCommand.getRawInputValues());

        // xnat wrapper inputs
        final Map<String, String> expectedXnatInputValues = Maps.newHashMap();
        expectedXnatInputValues.put("session", session.getUri());
        expectedXnatInputValues.put("assessor", session.getAssessors().get(0).getUri());
        expectedXnatInputValues.put("assessor-label", session.getAssessors().get(0).getLabel());
        assertEquals(expectedXnatInputValues, resolvedCommand.getXnatInputValues());

        // command inputs
        final Map<String, String> expectedCommandInputValues = Maps.newHashMap();
        expectedCommandInputValues.put("file-path", null);
        expectedCommandInputValues.put("whatever", session.getAssessors().get(0).getLabel());
        assertEquals(expectedCommandInputValues, resolvedCommand.getCommandInputValues());

        // Outputs
        assertTrue(resolvedCommand.getOutputs().isEmpty());
    }

    // TODO Re-do this test when I figure out how config inputs should work & should be resolved
    // @Test
    // public void testConfig() throws Exception {
    //     final String siteConfigName = "site-config";
    //     final String siteConfigInput = "{" +
    //             "\"name\": \"" + siteConfigName + "\", " +
    //             "\"type\": \"Config\", " +
    //             "\"required\": true" +
    //             "}";
    //     final String projectInputName = "project";
    //     final String projectInput = "{" +
    //             "\"name\": \"" + projectInputName + "\", " +
    //             "\"description\": \"This input accepts a project\", " +
    //             "\"type\": \"Project\", " +
    //             "\"required\": true" +
    //             "}";
    //     final String projectConfigName = "project-config";
    //     final String projectConfigInput = "{" +
    //             "\"name\": \"" + projectConfigName + "\", " +
    //             "\"type\": \"Config\", " +
    //             "\"required\": true," +
    //             "\"parent\": \"" + projectInputName + "\"" +
    //             "}";
    //
    //     final String commandLine = "echo hello world";
    //     final String commandJson = "{" +
    //             "\"name\": \"command\", " +
    //             "\"description\": \"Testing config inputs\"," +
    //             "\"docker-image\": \"" + BUSYBOX_LATEST + "\"," +
    //             "\"run\": {" +
    //                 "\"command-line\": \"" + commandLine + "\"" +
    //             "}," +
    //             "\"inputs\": [" +
    //                 projectInput + ", " +
    //                 siteConfigInput + ", " +
    //                 projectConfigInput + //", " +
    //             "]" +
    //             "}";
    //     final Command command = mapper.readValue(commandJson, Command.class);
    //     commandService.create(command);
    //
    //     final String toolname = "toolname";
    //     final String siteConfigFilename = "site-config-filename";
    //     final String siteConfigContents = "Hey, I am stored in a site config!";
    //     when(mockConfigService.getConfigContents(toolname, siteConfigFilename, Scope.Site, null))
    //             .thenReturn(siteConfigContents);
    //
    //     final String projectId = "theProject";
    //     final String projectConfigFilename = "project-config-filename";
    //     final String projectConfigContents = "Hey, I am stored in a project config!";
    //     when(mockConfigService.getConfigContents(toolname, projectConfigFilename, Scope.Project, projectId))
    //             .thenReturn(projectConfigContents);
    //
    //     final String projectUri = "/projects/" + projectId;
    //     final String projectRuntimeJson = "{" +
    //             "\"id\": \"" + projectId + "\", " +
    //             "\"label\": \"" + projectId + "\", " +
    //             "\"uri\": \"" + projectUri + "\", " +
    //             "\"type\": \"Project\"" +
    //             "}";
    //
    //     final Map<String, String> runtimeValues = Maps.newHashMap();
    //     runtimeValues.put(siteConfigName, toolname + "/" + siteConfigFilename);
    //     runtimeValues.put(projectConfigName, toolname + "/" + projectConfigFilename);
    //     runtimeValues.put(projectInputName, projectRuntimeJson);
    //
    //     final ResolvedCommand resolvedCommand = commandService.resolveCommand(command, runtimeValues, mockUser);
    //     assertEquals((Long) command.getId(), resolvedCommand.getCommandId());
    //     assertEquals(command.getImage(), resolvedCommand.getImage());
    //     assertEquals(commandLine, resolvedCommand.getCommandLine());
    //     assertTrue(resolvedCommand.getEnvironmentVariables().isEmpty());
    //     assertTrue(resolvedCommand.getMountsIn().isEmpty());
    //     assertTrue(resolvedCommand.getMountsOut().isEmpty());
    //     assertTrue(resolvedCommand.getOutputs().isEmpty());
    //     assertThat(resolvedCommand, instanceOf(ResolvedDockerCommand.class));
    //     assertTrue(((ResolvedDockerCommand) resolvedCommand).getPorts().isEmpty());
    //
    //     final Map<String, String> inputValues = resolvedCommand.getCommandInputValues();
    //     assertThat(inputValues, hasEntry(siteConfigName, siteConfigContents));
    //     assertThat(inputValues, hasEntry(projectConfigName, projectConfigContents));
    //     assertThat(inputValues, hasEntry(projectInputName, projectUri));
    // }
}
