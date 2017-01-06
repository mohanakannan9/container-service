package org.nrg.containers.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.nrg.containers.config.IntegrationTestConfig;
import org.nrg.containers.model.Command;
import org.nrg.containers.model.CommandMount;
import org.nrg.containers.model.ContainerExecutionMount;
import org.nrg.containers.model.ResolvedCommand;
import org.nrg.xft.security.UserI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = IntegrationTestConfig.class)
public class CommandResolutionTest {
    private static final Logger log = LoggerFactory.getLogger(CommandResolutionTest.class);
    private final String BUSYBOX_LATEST = "busybox:latest";

    private UserI mockUser;

    @Autowired private ObjectMapper mapper;
    @Autowired private CommandService commandService;

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
    @Transactional
    public void testCommandNoTestNameYet() throws Exception {
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
                "\"parent-id\": \"" + scanId + "\", " +
                "\"directory\": \"" + dicomDir + "\"}";
        final String scanRuntimeJson = "{" +
                "\"id\": \"" + scanId + "\", " +
                "\"uri\": \"" + scanUri + "\", " +
                "\"type\": \"Scan\", " +
                "\"parent-id\": \"" + sessionId + "\", " +
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
        assertEquals(command.getDockerImage(), resolvedCommand.getDockerImage());
        assertEquals(commandLine, resolvedCommand.getCommandLine());
        assertTrue(resolvedCommand.getEnvironmentVariables().isEmpty());
        assertTrue(resolvedCommand.getMountsIn().isEmpty());
        assertTrue(resolvedCommand.getMountsOut().isEmpty());
        assertTrue(resolvedCommand.getPorts().isEmpty());
        assertTrue(resolvedCommand.getOutputs().isEmpty());

        final Map<String, String> inputValues = resolvedCommand.getInputValues();
        assertThat(inputValues, hasEntry("T1-scantype", scantypeCsv));
        assertThat(inputValues, hasEntry("T1", scanUri));
        assertThat(inputValues, hasEntry("session", sessionUri));
        assertThat(inputValues, hasEntry("dicom", scanDicomResourceUri));
    }
}
