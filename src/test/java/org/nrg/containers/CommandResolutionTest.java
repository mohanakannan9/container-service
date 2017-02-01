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
import org.nrg.containers.model.Command;
import org.nrg.containers.model.ResolvedCommand;
import org.nrg.containers.model.ResolvedDockerCommand;
import org.nrg.containers.model.XnatCommandWrapper;
import org.nrg.containers.model.xnat.Session;
import org.nrg.containers.services.CommandService;
import org.nrg.containers.services.XnatCommandWrapperService;
import org.nrg.framework.constants.Scope;
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

import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
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
    private Map<String, XnatCommandWrapper> xnatCommandWrappers;

    @Autowired private ObjectMapper mapper;
    @Autowired private CommandService commandService;
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
        for (final XnatCommandWrapper xnatCommandWrapper : dummyCommand.getXnatCommandWrappers()) {
            xnatCommandWrappers.put(xnatCommandWrapper.getName(), xnatCommandWrapper);
        }
    }

    @Test
    public void testSessionScanResource() throws Exception {
        // Read the input value from a file
        final String sessionScanInputFilePath = resourceDir + "/testSessionScanResource/session.json";

        // I want to set a resource directory at runtime, so pardon me while I do some unchecked stuff with the values I just read
        final String dicomDir = folder.newFolder("DICOM").getAbsolutePath();
        final Session session = mapper.readValue(new File(sessionScanInputFilePath), Session.class);
        session.getScans().get(0).getResources().get(0).setDirectory(dicomDir);
        final String sessionRuntimeJson = mapper.writeValueAsString(session);

        final Map<String, String> runtimeValues = Maps.newHashMap();
        runtimeValues.put("session", sessionRuntimeJson);

        final XnatCommandWrapper sessionScanResourceWrapper = xnatCommandWrappers.get("session-scan-resource");
        assertNotNull(sessionScanResourceWrapper);

        final ResolvedCommand resolvedCommand = commandService.resolveCommand(sessionScanResourceWrapper, dummyCommand, runtimeValues, mockUser);
        assertEquals((Long) dummyCommand.getId(), resolvedCommand.getCommandId());
        assertEquals((Long) sessionScanResourceWrapper.getId(), resolvedCommand.getXnatCommandWrapperId());
        assertEquals(dummyCommand.getImage(), resolvedCommand.getImage());
        assertEquals(dummyCommand.getCommandLine(), resolvedCommand.getCommandLine());
        assertTrue(resolvedCommand.getEnvironmentVariables().isEmpty());
        assertTrue(resolvedCommand.getMountsIn().isEmpty());
        assertTrue(resolvedCommand.getMountsOut().isEmpty());
        assertThat(resolvedCommand, instanceOf(ResolvedDockerCommand.class));
        assertTrue(((ResolvedDockerCommand) resolvedCommand).getPorts().isEmpty());

        // Raw inputs
        assertEquals(runtimeValues, resolvedCommand.getRawInputValues());

        // xnat wrapper inputs
        final Map<String, String> xnatInputValues = resolvedCommand.getXnatInputValues();
        assertThat(xnatInputValues, hasEntry("T1-scantype", "\"SCANTYPE\", \"OTHER_SCANTYPE\""));
        assertThat(xnatInputValues, hasEntry("session", session.getUri()));
        assertThat(xnatInputValues, hasEntry("scan", session.getScans().get(0).getUri()));
        assertThat(xnatInputValues, hasEntry("dicom", session.getScans().get(0).getResources().get(0).getUri()));

        // command inputs
        final Map<String, String> commandInputValues = resolvedCommand.getCommandInputValues();
        assertThat(commandInputValues, hasEntry("file-input", null));
        // TODO This assertion ^ is what passes now. When I change the resolution code, figure out what value should be there and make the test fail until code works.

        // Outputs
        assertTrue(resolvedCommand.getOutputs().isEmpty());
    }

    @Test
    public void testResourceFile() throws Exception {
        final String scanInputName = "a scan";
        final String scanInputJson = "{" +
                "\"name\": \"" + scanInputName + "\", " +
                "\"description\": \"An input that takes a scan\"," +
                "\"type\": \"Scan\"," +
                "\"required\": true" +
                "}";
        final String resourceInputName = "a resource";
        final String resourceInputJson = "{" +
                "\"name\": \"" + resourceInputName + "\", " +
                "\"description\": \"An input that takes a resource\", " +
                "\"type\": \"Resource\", " +
                "\"parent\": \"" + scanInputName + "\", " +
                "\"required\": true" +
                "}";
        final String fileInputName = "a file";
        final String fileInputJson = "{" +
                "\"name\": \"" + fileInputName + "\", " +
                "\"description\": \"An input that takes a file\", " +
                "\"type\": \"File\"," +
                "\"parent\": \"" + resourceInputName + "\", " +
                "\"required\": true" +
                "}";

        final String commandLine = "echo hello world";
        final String commandJson = "{" +
                "\"name\": \"foo\", " +
                "\"description\": \"Doing some stuff\"," +
                "\"docker-image\": \"" + BUSYBOX_LATEST + "\"," +
                "\"run\": {" +
                    "\"command-line\": \"" + commandLine + "\"" +
                "}," +
                "\"inputs\": [" +
                    scanInputJson + ", " +
                    resourceInputJson + ", " +
                    fileInputJson +
                "]" +
                "}";

        final Command command = mapper.readValue(commandJson, Command.class);
        commandService.create(command);

        final String resourceDir = folder.newFolder("resource").getAbsolutePath();
        final String scanId = "0";
        final String scanUri = "/scans/" + scanId;
        final String resourceId = "0";
        final String resourceUri = scanUri + "/resources/" + resourceId;
        final String fileName = "file.file";
        final String fileUri = resourceUri + "/" + fileName;
        final String filePath = resourceDir + "/" + fileName;
        final String fileRuntimeJson = "{" +
                "\"type\": \"File\"," +
                "\"name\": \"" + fileName + "\", " +
                "\"path\": \"" + filePath + "\", " +
                "\"uri\": \"" + fileUri + "\"" +
                "}";
        final String resourceRuntimeJson = "{" +
                "\"id\": \"" + resourceId + "\", " +
                "\"uri\":\"" + resourceUri + "\", " +
                "\"type\": \"Resource\", " +
                "\"label\": \"this is the resource label\", " +
                "\"directory\": \"" + resourceDir + "\"," +
                "\"files\": [" +
                    fileRuntimeJson +
                "]" +
                "}";
        final String scanRuntimeJson = "{" +
                "\"id\": \"" + scanId + "\", " +
                "\"type\": \"Scan\", " +
                "\"uri\": \"" + scanUri + "\", " +
                "\"resources\": [" +
                    resourceRuntimeJson +
                "]" +
                "}";
        final Map<String, String> runtimeValues = Maps.newHashMap();
        runtimeValues.put(scanInputName, scanRuntimeJson);

        final ResolvedCommand resolvedCommand = commandService.resolveCommand(command, runtimeValues, mockUser);
        assertEquals((Long) command.getId(), resolvedCommand.getCommandId());
        assertEquals(command.getImage(), resolvedCommand.getImage());
        assertEquals(commandLine, resolvedCommand.getCommandLine());
        assertTrue(resolvedCommand.getEnvironmentVariables().isEmpty());
        assertTrue(resolvedCommand.getMountsIn().isEmpty());
        assertTrue(resolvedCommand.getMountsOut().isEmpty());
        assertTrue(resolvedCommand.getOutputs().isEmpty());
        assertThat(resolvedCommand, instanceOf(ResolvedDockerCommand.class));
        assertTrue(((ResolvedDockerCommand) resolvedCommand).getPorts().isEmpty());

        final Map<String, String> inputValues = resolvedCommand.getCommandInputValues();
        assertThat(inputValues, hasEntry(fileInputName, fileUri));
        assertThat(inputValues, hasEntry(resourceInputName, resourceUri));
        assertThat(inputValues, hasEntry(scanInputName, scanUri));
    }

    @Test
    public void testProject() throws Exception {
        final String projectInput = "{" +
                "\"name\": \"project\"," +
                "\"description\": \"This input accepts a project\"," +
                "\"type\": \"Project\"," +
                "\"required\": true" +
                "}";

        final String commandLine = "echo hello world";
        final String commandJson =
                "{\"name\": \"command\", \"description\": \"Testing project inputs\"," +
                        "\"docker-image\": \"" + BUSYBOX_LATEST + "\"," +
                        "\"run\": {" +
                        "\"command-line\": \"" + commandLine + "\"" +
                        "}," +
                        "\"inputs\": [" +
                            projectInput +
                        "]" +
                        "}";
        final Command command = mapper.readValue(commandJson, Command.class);
        commandService.create(command);

        final String projectId = "aProject";
        final String projectUri = "/projects/" + projectId;
        final String projectRuntimeJson = "{" +
                "\"id\": \"" + projectId + "\", " +
                "\"label\": \"" + projectId + "\", " +
                "\"uri\": \"" + projectUri + "\", " +
                "\"type\": \"Project\"" +
                "}";
        final Map<String, String> runtimeValues = Maps.newHashMap();
        runtimeValues.put("project", projectRuntimeJson);

        final ResolvedCommand resolvedCommand = commandService.resolveCommand(command, runtimeValues, mockUser);
        assertEquals((Long) command.getId(), resolvedCommand.getCommandId());
        assertEquals(command.getImage(), resolvedCommand.getImage());
        assertEquals(commandLine, resolvedCommand.getCommandLine());
        assertTrue(resolvedCommand.getEnvironmentVariables().isEmpty());
        assertTrue(resolvedCommand.getMountsIn().isEmpty());
        assertTrue(resolvedCommand.getMountsOut().isEmpty());
        assertTrue(resolvedCommand.getOutputs().isEmpty());
        assertThat(resolvedCommand, instanceOf(ResolvedDockerCommand.class));
        assertTrue(((ResolvedDockerCommand) resolvedCommand).getPorts().isEmpty());

        final Map<String, String> inputValues = resolvedCommand.getCommandInputValues();
        assertThat(inputValues, hasEntry("project", projectUri));
    }

    @Test
    public void testProjectSubject() throws Exception {
        final String projectInput = "{" +
                "\"name\": \"project\", " +
                "\"description\": \"This input accepts a project\", " +
                "\"type\": \"Project\", " +
                "\"required\": true" +
                "}";
        final String subjectInput = "{" +
                "\"name\": \"subject\", " +
                "\"description\": \"This input accepts a subject\", " +
                "\"type\": \"Subject\", " +
                "\"parent\": \"project\", " +
                "\"required\": true" +
                "}";

        final String commandLine = "echo hello world";
        final String commandJson =
                "{\"name\": \"command\", " +
                        "\"description\": \"Testing project and subject inputs\"," +
                        "\"docker-image\": \"" + BUSYBOX_LATEST + "\"," +
                        "\"run\": {" +
                        "\"command-line\": \"" + commandLine + "\"" +
                        "}," +
                        "\"inputs\": [" +
                            projectInput + ", " +
                            subjectInput +
                        "]" +
                        "}";
        final Command command = mapper.readValue(commandJson, Command.class);
        commandService.create(command);

        final String subjectId = "aSubject";
        final String projectId = "aProject";
        final String projectUri = "/projects/" + projectId;
        final String subjectUri = projectUri + "/subjects/" + subjectId;
        final String subjectRuntimeJson = "{" +
                "\"id\": \"" + subjectId + "\", " +
                "\"label\": \"" + subjectId + "\", " +
                "\"uri\": \"" + subjectUri + "\", " +
                "\"type\": \"Subject\"" +
                "}";
        final String projectRuntimeJson = "{" +
                "\"id\": \"" + projectId + "\", " +
                "\"label\": \"" + projectId + "\", " +
                "\"uri\": \"" + projectUri + "\", " +
                "\"type\": \"Project\", " +
                "\"subjects\" : [" +
                    subjectRuntimeJson +
                "]" +
                "}";
        final Map<String, String> runtimeValues = Maps.newHashMap();
        runtimeValues.put("project", projectRuntimeJson);

        final ResolvedCommand resolvedCommand = commandService.resolveCommand(command, runtimeValues, mockUser);
        assertEquals((Long) command.getId(), resolvedCommand.getCommandId());
        assertEquals(command.getImage(), resolvedCommand.getImage());
        assertEquals(commandLine, resolvedCommand.getCommandLine());
        assertTrue(resolvedCommand.getEnvironmentVariables().isEmpty());
        assertTrue(resolvedCommand.getMountsIn().isEmpty());
        assertTrue(resolvedCommand.getMountsOut().isEmpty());
        assertTrue(resolvedCommand.getOutputs().isEmpty());
        assertThat(resolvedCommand, instanceOf(ResolvedDockerCommand.class));
        assertTrue(((ResolvedDockerCommand) resolvedCommand).getPorts().isEmpty());

        final Map<String, String> inputValues = resolvedCommand.getCommandInputValues();
        assertThat(inputValues, hasEntry("project", projectUri));
        assertThat(inputValues, hasEntry("subject", subjectUri));
    }

    @Test
    public void testSessionAssessor() throws Exception {
        final String sessionInput = "{" +
                "\"name\": \"session\", " +
                "\"type\": \"Session\", " +
                "\"required\": true}";
        final String assessorInput = "{" +
                "\"name\": \"assessor\", " +
                "\"type\": \"Assessor\", " +
                "\"parent\": \"session\", " +
                "\"required\": true" +
                "}";

        final String commandLine = "echo hello world";
        final String commandJson =
                "{\"name\": \"command\", " +
                        "\"description\": \"Testing project and subject inputs\"," +
                        "\"docker-image\": \"" + BUSYBOX_LATEST + "\"," +
                        "\"run\": {" +
                        "\"command-line\": \"" + commandLine + "\"" +
                        "}," +
                        "\"inputs\": [" +
                            sessionInput + ", " +
                            assessorInput +
                        "]" +
                        "}";
        final Command command = mapper.readValue(commandJson, Command.class);
        commandService.create(command);

        final String sessionId = "aSession";
        final String assessorId = "anAssessor";
        final String sessionUri = "/experiments/" + sessionId;
        final String assessorUri = sessionUri + "/assessors/" + assessorId;
        final String assessorRuntimeJson = "{" +
                "\"id\": \"" + assessorId + "\", " +
                "\"label\": \"" + assessorId + "\", " +
                "\"uri\": \"" + assessorUri + "\", " +
                "\"type\": \"Assessor\"" +
                "}";
        final String sessionRuntimeJson = "{" +
                "\"id\": \"" + sessionId + "\", " +
                "\"label\": \"" + sessionId + "\", " +
                "\"uri\": \"" + sessionUri + "\", " +
                "\"type\": \"Session\", " +
                "\"assessors\" : [" +
                    assessorRuntimeJson +
                "]" +
                "}";
        final Map<String, String> runtimeValues = Maps.newHashMap();
        runtimeValues.put("session", sessionRuntimeJson);

        final ResolvedCommand resolvedCommand = commandService.resolveCommand(command, runtimeValues, mockUser);
        assertEquals((Long) command.getId(), resolvedCommand.getCommandId());
        assertEquals(command.getImage(), resolvedCommand.getImage());
        assertEquals(commandLine, resolvedCommand.getCommandLine());
        assertTrue(resolvedCommand.getEnvironmentVariables().isEmpty());
        assertTrue(resolvedCommand.getMountsIn().isEmpty());
        assertTrue(resolvedCommand.getMountsOut().isEmpty());
        assertTrue(resolvedCommand.getOutputs().isEmpty());
        assertThat(resolvedCommand, instanceOf(ResolvedDockerCommand.class));
        assertTrue(((ResolvedDockerCommand) resolvedCommand).getPorts().isEmpty());

        final Map<String, String> inputValues = resolvedCommand.getCommandInputValues();
        assertThat(inputValues, hasEntry("session", sessionUri));
        assertThat(inputValues, hasEntry("assessor", assessorUri));
    }

    @Test
    public void testConfig() throws Exception {
        final String siteConfigName = "site-config";
        final String siteConfigInput = "{" +
                "\"name\": \"" + siteConfigName + "\", " +
                "\"type\": \"Config\", " +
                "\"required\": true" +
                "}";
        final String projectInputName = "project";
        final String projectInput = "{" +
                "\"name\": \"" + projectInputName + "\", " +
                "\"description\": \"This input accepts a project\", " +
                "\"type\": \"Project\", " +
                "\"required\": true" +
                "}";
        final String projectConfigName = "project-config";
        final String projectConfigInput = "{" +
                "\"name\": \"" + projectConfigName + "\", " +
                "\"type\": \"Config\", " +
                "\"required\": true," +
                "\"parent\": \"" + projectInputName + "\"" +
                "}";

        final String commandLine = "echo hello world";
        final String commandJson = "{" +
                "\"name\": \"command\", " +
                "\"description\": \"Testing config inputs\"," +
                "\"docker-image\": \"" + BUSYBOX_LATEST + "\"," +
                "\"run\": {" +
                    "\"command-line\": \"" + commandLine + "\"" +
                "}," +
                "\"inputs\": [" +
                    projectInput + ", " +
                    siteConfigInput + ", " +
                    projectConfigInput + //", " +
                "]" +
                "}";
        final Command command = mapper.readValue(commandJson, Command.class);
        commandService.create(command);

        final String toolname = "toolname";
        final String siteConfigFilename = "site-config-filename";
        final String siteConfigContents = "Hey, I am stored in a site config!";
        when(mockConfigService.getConfigContents(toolname, siteConfigFilename, Scope.Site, null))
                .thenReturn(siteConfigContents);

        final String projectId = "theProject";
        final String projectConfigFilename = "project-config-filename";
        final String projectConfigContents = "Hey, I am stored in a project config!";
        when(mockConfigService.getConfigContents(toolname, projectConfigFilename, Scope.Project, projectId))
                .thenReturn(projectConfigContents);

        final String projectUri = "/projects/" + projectId;
        final String projectRuntimeJson = "{" +
                "\"id\": \"" + projectId + "\", " +
                "\"label\": \"" + projectId + "\", " +
                "\"uri\": \"" + projectUri + "\", " +
                "\"type\": \"Project\"" +
                "}";

        final Map<String, String> runtimeValues = Maps.newHashMap();
        runtimeValues.put(siteConfigName, toolname + "/" + siteConfigFilename);
        runtimeValues.put(projectConfigName, toolname + "/" + projectConfigFilename);
        runtimeValues.put(projectInputName, projectRuntimeJson);

        final ResolvedCommand resolvedCommand = commandService.resolveCommand(command, runtimeValues, mockUser);
        assertEquals((Long) command.getId(), resolvedCommand.getCommandId());
        assertEquals(command.getImage(), resolvedCommand.getImage());
        assertEquals(commandLine, resolvedCommand.getCommandLine());
        assertTrue(resolvedCommand.getEnvironmentVariables().isEmpty());
        assertTrue(resolvedCommand.getMountsIn().isEmpty());
        assertTrue(resolvedCommand.getMountsOut().isEmpty());
        assertTrue(resolvedCommand.getOutputs().isEmpty());
        assertThat(resolvedCommand, instanceOf(ResolvedDockerCommand.class));
        assertTrue(((ResolvedDockerCommand) resolvedCommand).getPorts().isEmpty());

        final Map<String, String> inputValues = resolvedCommand.getCommandInputValues();
        assertThat(inputValues, hasEntry(siteConfigName, siteConfigContents));
        assertThat(inputValues, hasEntry(projectConfigName, projectConfigContents));
        assertThat(inputValues, hasEntry(projectInputName, projectUri));
    }
}
