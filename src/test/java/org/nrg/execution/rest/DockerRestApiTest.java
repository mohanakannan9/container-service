package org.nrg.execution.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nrg.execution.api.ContainerControlApi;
import org.nrg.execution.config.DockerRestApiTestConfig;
import org.nrg.execution.exceptions.DockerServerException;
import org.nrg.execution.exceptions.NoServerPrefException;
import org.nrg.execution.exceptions.NotFoundException;
import org.nrg.execution.model.Command;
import org.nrg.execution.model.DockerImage;
import org.nrg.execution.model.DockerServer;
import org.nrg.execution.services.DockerService;
import org.nrg.prefs.exceptions.InvalidPreferenceName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.nrg.execution.api.ContainerControlApi.LABEL_KEY;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = DockerRestApiTestConfig.class)
public class DockerRestApiTest {
    private MockMvc mockMvc;

    private final MediaType JSON = MediaType.APPLICATION_JSON_UTF8;

    private final static String MOCK_CONTAINER_HOST = "fake://host.url";

    private final static String MOCK_CONTAINER_CERT_PATH = "/path/to/file";
    private final static DockerServer MOCK_CONTAINER_SERVER =
            new DockerServer(MOCK_CONTAINER_HOST, MOCK_CONTAINER_CERT_PATH);
    private final NoServerPrefException NO_SERVER_PREF_EXCEPTION =
            new NoServerPrefException("message");

    private final InvalidPreferenceName INVALID_PREFERENCE_NAME =
            new InvalidPreferenceName("*invalid name*");
    private final DockerServerException DOCKER_SERVER_EXCEPTION =
            new DockerServerException("Your server dun goofed.");
    private final NotFoundException NOT_FOUND_EXCEPTION =
            new NotFoundException("I should think of a message");
    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private DockerService dockerService;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private ContainerControlApi mockContainerControlApi;

    @Before
    public void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    @Test
    public void testGetServer() throws Exception {

        final String path = "/docker/server";

        final MockHttpServletRequestBuilder request =
                get(path).accept(JSON);

        when(mockContainerControlApi.getServer())
                .thenReturn(MOCK_CONTAINER_SERVER)
                .thenThrow(NO_SERVER_PREF_EXCEPTION);

        final String response =
                mockMvc.perform(request)
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(JSON))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        final DockerServer responseServer =
                mapper.readValue(response, DockerServer.class);
        assertThat(responseServer, equalTo(MOCK_CONTAINER_SERVER));

        // Not found
        final String exceptionResponse =
                mockMvc.perform(request)
                        .andExpect(status().isNotFound())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        assertThat(exceptionResponse, containsString("message"));
    }

    @Test
    public void testSetServer() throws Exception {

        final String containerServerJson =
                mapper.writeValueAsString(MOCK_CONTAINER_SERVER);

        final String path = "/docker/server";

        final MockHttpServletRequestBuilder request =
                post(path).content(containerServerJson).contentType(JSON);

        when(mockContainerControlApi.setServer(MOCK_CONTAINER_SERVER))
                .thenReturn(MOCK_CONTAINER_SERVER);

        mockMvc.perform(request)
                .andExpect(status().isAccepted());

        verify(mockContainerControlApi, times(1)).setServer(MOCK_CONTAINER_SERVER); // Method has been called once

        // Now mock out the exception
        doThrow(INVALID_PREFERENCE_NAME).when(mockContainerControlApi).setServer(MOCK_CONTAINER_SERVER);

        final String exceptionResponse =
                mockMvc.perform(request)
                        .andExpect(status().isInternalServerError())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        assertThat(exceptionResponse, containsString("*invalid name*"));

        verify(mockContainerControlApi, times(2)).setServer(MOCK_CONTAINER_SERVER);
    }

    @Test
    public void testPingServer() throws Exception {
        final String path = "/docker/server/ping";
        final MockHttpServletRequestBuilder request = get(path);

        when(mockContainerControlApi.pingServer())
                .thenReturn("OK")
                .thenThrow(DOCKER_SERVER_EXCEPTION)
                .thenThrow(NO_SERVER_PREF_EXCEPTION);

        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(content().string(equalTo("OK")));

        final String ISEResponse =
                mockMvc.perform(request)
                        .andExpect(status().isInternalServerError())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        assertEquals("The Docker server returned an error:\nYour server dun goofed.", ISEResponse);

        final String failedDepResponse =
                mockMvc.perform(request)
                        .andExpect(status().isFailedDependency())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        assertEquals("Set up Docker server before using this REST endpoint.",
                failedDepResponse);
    }

    @Test
    @Transactional
    public void testSaveFromLabels() throws Exception {
        final String labelTestCommandListJson =
                "[{\"name\": \"label-test\"," +
                        "\"description\": \"Command to test label-parsing and command-importing code\"," +
                        "\"run\": {\"command-line\": \"#CMD#\"}," +
                        "\"inputs\": [{\"name\": \"CMD\", \"description\": \"Command to run\", \"required\": true}]}]";
        final List<Command> expectedList = mapper.readValue(labelTestCommandListJson, new TypeReference<List<Command>>(){});
        final Command expected = expectedList.get(0);

        final Map<String, String> imageLabels = Maps.newHashMap();
        imageLabels.put(LABEL_KEY, labelTestCommandListJson);

        final String fakeImageId = "xnat/thisisfake";
        final DockerImage fakeDockerImage = new DockerImage();
        fakeDockerImage.setImageId(fakeImageId);
        fakeDockerImage.setLabels(imageLabels);

        when(mockContainerControlApi.getImageById(fakeImageId))
                .thenReturn(fakeDockerImage);
        when(mockContainerControlApi.parseLabels(fakeImageId))
                .thenCallRealMethod();
        when(mockContainerControlApi.parseLabels(fakeDockerImage))
                .thenCallRealMethod();

        final String path = "/docker/images/save";
        final MockHttpServletRequestBuilder request =
                post(path).param("image", fakeImageId);

        final String responseStr =
                mockMvc.perform(request)
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        final List<Command> responseList = mapper.readValue(responseStr, new TypeReference<List<Command>>(){});
        assertThat(responseList, hasSize(1));
        final Command response = responseList.get(0);

        // "response" will have been saved, so it will not be exactly equal to "expected"
        // Must compare attribute-by-attribute
        assertEquals(expected.getName(), response.getName());
        assertEquals(expected.getDescription(), response.getDescription());
        assertEquals(expected.getRun(), response.getRun());
        assertEquals(expected.getInputs(), response.getInputs());
        assertEquals(fakeImageId, response.getDockerImage());
    }

    @Test
    @Transactional
    public void testSaveFromLabels2() throws Exception {
        final String labelTestCommandListJson =
                "[{\"name\":\"dcm2niix-scan\", \"description\":\"Run dcm2niix on a scan's DICOMs\", " +
                        "\"run\": {" +
                            "\"command-line\": \"/run/dcm2niix-scan.sh #scanId# #sessionId#\", " +
                            "\"mounts\": [" +
                                "{\"name\":\"DICOM\", \"type\":\"input\", \"remote-path\":\"/input\"}," +
                                "{\"name\":\"NIFTI\", \"type\":\"output\", \"remote-path\":\"/output\"}" +
                            "]" +
                        "}," +
                        "\"inputs\":[" +
                            "{\"name\":\"scanId\", \"required\":true, \"root-property\":\"ID\"}, " +
                            "{\"name\":\"sessionId\", \"required\":true, \"root-property\":\"ID\"}" +
                        "] " +
                    "}]";
        final List<Command> expectedList = mapper.readValue(labelTestCommandListJson, new TypeReference<List<Command>>(){});
        final Command expected = expectedList.get(0);

        final Map<String, String> imageLabels = Maps.newHashMap();
        imageLabels.put(LABEL_KEY, labelTestCommandListJson);

        final String fakeImageId = "xnat/thisisfake";
        final DockerImage fakeDockerImage = new DockerImage();
        fakeDockerImage.setImageId(fakeImageId);
        fakeDockerImage.setLabels(imageLabels);

        when(mockContainerControlApi.getImageById(fakeImageId))
                .thenReturn(fakeDockerImage);
        when(mockContainerControlApi.parseLabels(fakeImageId))
                .thenCallRealMethod();
        when(mockContainerControlApi.parseLabels(fakeDockerImage))
                .thenCallRealMethod();

        final String path = "/docker/images/save";
        final MockHttpServletRequestBuilder request =
                post(path).param("image", fakeImageId);

        final String responseStr =
                mockMvc.perform(request)
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        final List<Command> responseList = mapper.readValue(responseStr, new TypeReference<List<Command>>(){});
        assertThat(responseList, hasSize(1));
        final Command response = responseList.get(0);

        // "response" will have been saved, so it will not be exactly equal to "expected"
        // Must compare attribute-by-attribute
        assertEquals(expected.getName(), response.getName());
        assertEquals(expected.getDescription(), response.getDescription());
        assertEquals(expected.getRun(), response.getRun());
        assertEquals(expected.getInputs(), response.getInputs());
        assertEquals(fakeImageId, response.getDockerImage());
    }
}
