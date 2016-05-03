package org.nrg.containers.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nrg.containers.config.RestApiTestConfig;
import org.nrg.containers.exceptions.DockerServerException;
import org.nrg.containers.exceptions.NoServerPrefException;
import org.nrg.containers.exceptions.NotFoundException;
import org.nrg.containers.model.DockerHub;
import org.nrg.containers.model.DockerServer;
import org.nrg.containers.services.DockerService;
import org.nrg.framework.exceptions.NrgServiceRuntimeException;
import org.nrg.prefs.exceptions.InvalidPreferenceName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = RestApiTestConfig.class)
public class DockerRestApiTest {
    private MockMvc mockMvc;
    private final ObjectMapper mapper = new ObjectMapper();

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
    private DockerService mockDockerService;

    @Before
    public void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    @Test
    public void testGetServer() throws Exception {

        final String path = "/docker/server";

        // REQUEST 0: No "onServer" param (defaults to false)
        final MockHttpServletRequestBuilder request =
                get(path).accept(JSON);

        when(mockDockerService.getServer())
                .thenReturn(MOCK_CONTAINER_SERVER)
                .thenThrow(NOT_FOUND_EXCEPTION);

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
        assertEquals("I should think of a message", exceptionResponse);
    }

    @Test
    public void testSetServer() throws Exception {

        final String containerServerJson =
                mapper.writeValueAsString(MOCK_CONTAINER_SERVER);

        final String path = "/docker/server";

        // REQUEST 0: No "onServer" param (defaults to false)
        final MockHttpServletRequestBuilder request =
                post(path).content(containerServerJson).contentType(JSON);

        doNothing().when(mockDockerService).setServer(MOCK_CONTAINER_SERVER); // Have to use a different mocking syntax when method returns void

        mockMvc.perform(request)
                .andExpect(status().isCreated());

        verify(mockDockerService, times(1)).setServer(MOCK_CONTAINER_SERVER); // Method has been called once

        // Now mock out the exception
        doThrow(INVALID_PREFERENCE_NAME).when(mockDockerService).setServer(MOCK_CONTAINER_SERVER);

        final String exceptionResponse =
                mockMvc.perform(request)
                        .andExpect(status().isInternalServerError())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        assertThat(exceptionResponse, containsString("*invalid name*"));

        verify(mockDockerService, times(2)).setServer(MOCK_CONTAINER_SERVER);
    }

    @Test
    public void testPingServer() throws Exception {
        final String path = "/docker/server/ping";
        final MockHttpServletRequestBuilder request = get(path);

        when(mockDockerService.pingServer())
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
    public void testGetHubs() throws Exception {
        final String path = "/docker/hubs";
        final MockHttpServletRequestBuilder request = get(path).accept(JSON);

        final DockerHub hub1 = DockerHub.builder()
                .email("user@email.com")
                .username("joe_schmoe")
                .password("insecure")
                .url("http://hub.io")
                .name("My cool hub")
                .build();
        final DockerHub hub2 = DockerHub.builder()
                .url("https://index.docker.io/v1/")
                .name("Docker Hub")
                .build();
        final List<DockerHub> hubList = Lists.newArrayList(hub1, hub2);

        when(mockDockerService.getHubs())
                .thenReturn(hubList);

        final String response =
                mockMvc.perform(request)
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(JSON))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        assertEquals(hubList, mapper.readValue(response, new TypeReference<List<DockerHub>>(){}));
    }

    @Test
    public void testSetHub() throws Exception {
        final String path = "/docker/hubs";

        final DockerHub hub = DockerHub.builder()
                .email("user@email.com")
                .username("joe_schmoe")
                .password("insecure")
                .url("http://hub.io")
                .build();

        doNothing().when(mockDockerService).setHub(hub);

        // Make a json representation of the hub
        final String hubJsonString = mapper.writeValueAsString(hub);

//        // Make an html form representation of the hub
//        final String hubFormString =
//            String.format("email=%s&username=%s&password=%s&url=%s",
//                urlEncode(hub.email()),
//                urlEncode(hub.username()),
//                urlEncode(hub.password()),
//                urlEncode(hub.url()));


        // send json in
        final MockHttpServletRequestBuilder requestJson =
                post(path).content(hubJsonString).contentType(JSON);

        mockMvc.perform(requestJson)
                .andExpect(status().isCreated());

        verify(mockDockerService, times(1)).setHub(hub); // Method has been called once

//        // send form in
//        final MockHttpServletRequestBuilder requestForm =
//            post(path).content(hubFormString).contentType(FORM);
//
//        mockMvc.perform(requestForm)
//            .andExpect(status().isOk());
//
//        verify(service, times(2)).setHub(hub); // Method has been called twice

        // Exception
        doThrow(NrgServiceRuntimeException.class).when(mockDockerService).setHub(hub);

        final String exceptionResponse =
                mockMvc.perform(requestJson)
                        .andExpect(status().isBadRequest())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        assertEquals("Body was not a valid Docker Hub.", exceptionResponse);
    }
}
