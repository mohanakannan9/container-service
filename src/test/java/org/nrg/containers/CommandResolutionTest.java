package org.nrg.containers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
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
import org.nrg.containers.services.CommandService;
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

    @Autowired private ObjectMapper mapper;
    @Autowired private CommandService commandService;
    @Autowired private ConfigService mockConfigService;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder(new File("/tmp"));

    @Before
    public void setup() {
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
    }

    @Test
    public void testSessionScanResource() throws Exception {
        final String scantype = "SCANTYPE";
        final String scantypeCsv = "\"" + scantype + "\", \"OTHER_SCANTYPE\"";
        final String scantypeCsvEscaped = "\\\"" + scantype + "\\\", \\\"OTHER_SCANTYPE\\\"";

        final String sessionCommandInputJson =
                "{\"name\": \"session\", \"type\": \"Session\", \"required\": true}";
        final String scantypeCommandInputJson =
                "{\"name\": \"T1-scantype\", \"description\": \"Scantype of T1 scans\", \"type\": \"string\", " +
                        "\"default-value\": \"" + scantypeCsvEscaped + "\", \"required\": true}";
        final String t1CommandInputJson =
                "{\"name\": \"T1\", \"description\": \"Input T1 scan\"," +
                        "\"type\": \"Scan\"," +
                        "\"parent\": \"session\"," +
                        "\"prerequisites\": \"T1-scantype\"," +
                        "\"matcher\": \"@.scan-type in [^$.inputs[?(@.name == 'T1-scantype')].value^]\"," +
                        "\"required\": true" +
                        "}";
        final String dicomCommandInputJson =
                "{\"name\": \"dicom\", \"description\": \"Input resource: DICOM \", " +
                        "\"type\": \"Resource\", " +
                        "\"parent\": \"T1\", " +
                        "\"matcher\": \"@.label == 'DICOM'\"," +
                        "\"required\": true" +
                        "}";

        final String commandLine = "echo hello world";
        final String commandJson =
                "{\"name\": \"foo\", \"description\": \"Doing some stuff\"," +
                        "\"docker-image\": \"" + BUSYBOX_LATEST + "\"," +
                        "\"run\": {" +
                        "\"command-line\": \"" + commandLine + "\"" +
                        "}," +
                        "\"inputs\": [" +
                            sessionCommandInputJson + "," +
                            scantypeCommandInputJson + "," +
                            t1CommandInputJson + "," +
                            dicomCommandInputJson +
                        "]" +
                        "}";

        final Command command = mapper.readValue(commandJson, Command.class);
        commandService.create(command);

        final String dicomDir = folder.newFolder("DICOM").getAbsolutePath();
        final String scanDicomResourceId = "0";
        final String scanId = "scan1";
        final String sessionId = "session1";
        final String sessionUri = "/experiments/" + sessionId;
        final String scanUri = sessionUri + "/scans/" + scanId;
        final String scanDicomResourceUri = scanUri + "/resources/" + scanDicomResourceId;
        final String scanDicomResource = "{" +
                "\"id\":" + scanDicomResourceId + ", " +
                "\"uri\":\"" + scanDicomResourceUri + "\", " +
                "\"type\": \"Resource\", " +
                "\"label\": \"DICOM\", " +
                "\"directory\": \"" + dicomDir + "\"}";
        final String scanRuntimeJson = "{" +
                "\"id\": \"" + scanId + "\", " +
                "\"uri\": \"" + scanUri + "\", " +
                "\"type\": \"Scan\", " +
                "\"scan-type\": \"" + scantype + "\"," +
                "\"resources\": [" + scanDicomResource + "]" +
                "}";
        final String sessionRuntimeJson = "{" +
                "\"id\": \"" + sessionId + "\", " +
                "\"uri\": \"" + sessionUri + "\", " +
                "\"type\": \"Session\", " +
                "\"label\": \"" + sessionId + "\", " +
                "\"scans\": [" + scanRuntimeJson + "]" +
                "}";
        final Map<String, String> runtimeValues = Maps.newHashMap();
        runtimeValues.put("session", sessionRuntimeJson);

//        final Session session = mapper.readValue(sessionRuntimeJson, Session.class);

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
        assertThat(inputValues, hasEntry("T1-scantype", scantypeCsv));
        assertThat(inputValues, hasEntry("T1", scanUri));
        assertThat(inputValues, hasEntry("session", sessionUri));
        assertThat(inputValues, hasEntry("dicom", scanDicomResourceUri));
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
