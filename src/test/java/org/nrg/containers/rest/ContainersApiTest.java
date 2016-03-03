package org.nrg.containers.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nrg.containers.config.RestApiTestConfig;
import org.nrg.containers.model.Container;
import org.nrg.containers.model.ContainerMocks;
import org.nrg.containers.model.ContainerServer;
import org.nrg.containers.model.ExceptionMocks;
import org.nrg.containers.services.ContainerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.ServletContext;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
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
public class ContainersApiTest {

    public static final MediaType JSON = MediaType.APPLICATION_JSON_UTF8;
    public static final MediaType PLAIN_TEXT = MediaType.TEXT_PLAIN;
    private MockMvc mockMvc;
    final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private ContainerService service;

    @Before
    public void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();

        reset(service); // To ensure test mock objects are isolated
    }

    @Test
    public void testWebApplicationContextSetup() {
        assertNotNull(service);
        assertNotNull(wac);

        ServletContext childServletContext = wac.getServletContext();
        assertNotNull(childServletContext);
    }

    @Test
    public void testGetAllContainers() throws Exception {
        final List<Container> mockContainerList = ContainerMocks.FIRST_AND_SECOND;

        final String path = "/containers";
        final MockHttpServletRequestBuilder request = get(path).accept(JSON);

        when(service.getAllContainers())
                .thenReturn(mockContainerList)                          // Happy path
                .thenThrow(ExceptionMocks.NO_SERVER_PREF_EXCEPTION);    // No server pref defined

        // Happy path
        final String response =
                mockMvc.perform(request)
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(JSON))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        List<Container> responseImageList = mapper.readValue(response, new TypeReference<List<Container>>(){});
        assertThat(responseImageList, equalTo(mockContainerList));
        
        // No server pref defined
        mockMvc.perform(request)
                .andExpect(status().isFailedDependency());
    }

    @Test
    public void testGetContainer() throws Exception {
        final String id = ContainerMocks.FOO_ID;
        final Container mockContainer = ContainerMocks.FOO;

        final String path = "/containers/" + id;
        final MockHttpServletRequestBuilder request = get(path).accept(JSON);

        when(service.getContainer(id))
                .thenReturn(mockContainer)                              // Happy path
                .thenThrow(ExceptionMocks.NOT_FOUND_EXCEPTION)          // Not found
                .thenThrow(ExceptionMocks.NO_SERVER_PREF_EXCEPTION);    // No server pref defined

        // Happy path
        final String responseById =
                mockMvc.perform(request)
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(JSON))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        final Container containerById = mapper.readValue(responseById, Container.class);
        assertThat(containerById, equalTo(mockContainer));

        // Not found
        mockMvc.perform(request)
                .andExpect(status().isNotFound());

        // No server pref defined
        mockMvc.perform(request)
                .andExpect(status().isFailedDependency());
    }

    @Test
    public void testGetContainerStatus() throws Exception {
        final String id = ContainerMocks.FOO_ID;
        final String status = ContainerMocks.FOO_STATUS;

        final String path = "/containers/" + id + "/status";
        final MockHttpServletRequestBuilder request = get(path);

        when(service.getContainerStatus(id))
                .thenReturn(status)                                     // Happy path
                .thenThrow(ExceptionMocks.NO_SERVER_PREF_EXCEPTION);    // No server pref defined

        // Happy path
        final String response =
                mockMvc.perform(request)
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(PLAIN_TEXT))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        assertThat(response, equalTo(status));

        // No server pref defined
        mockMvc.perform(request)
                .andExpect(status().isFailedDependency());
    }

    @Test
    public void testVerbContainer() throws Exception {
        // TODO
    }

    @Test
    public void testGetContainerLogs() throws Exception {
        // TODO
    }

    @Test
    public void testLaunch() throws Exception {
        final String path = "/containers/launch";
        // TODO
    }

    @Test
    public void testGetServer() throws Exception {
        final String server = "http://foo.bar:123";
        final ContainerServer containerServer = new ContainerServer(server);

        final String path = "/containers/server";

        // REQUEST 0: No "onServer" param (defaults to false)
        final MockHttpServletRequestBuilder request =
                get(path).accept(JSON);

        when(service.getServer())
                .thenReturn(containerServer)
                .thenThrow(ExceptionMocks.NO_SERVER_PREF_EXCEPTION);

        final String response =
                mockMvc.perform(request)
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(JSON))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        final ContainerServer responseServer = mapper.readValue(response, ContainerServer.class);
        assertThat(responseServer, equalTo(containerServer));

        // Not found
        mockMvc.perform(request).andExpect(status().isNotFound());
    }

    @Test
    public void testSetServer() throws Exception {
        final String server = "http://foo.bar:123";
        final String postBody = "{\"host\":\"" + server + "\"}";

        final String path = "/containers/server";

        // REQUEST 0: No "onServer" param (defaults to false)
        final MockHttpServletRequestBuilder request =
                post(path).content(postBody).contentType(JSON);

        doNothing().when(service).setServer(server); // Have to use a different mocking syntax when method returns void

        mockMvc.perform(request)
                .andExpect(status().isOk());

        verify(service, times(1)).setServer(server); // Method has been called once

        // Now mock out the exception
        doThrow(ExceptionMocks.INVALID_PREFERENCE_NAME).when(service).setServer(server);

        mockMvc.perform(request).andExpect(status().isInternalServerError());
        verify(service, times(2)).setServer(server);
    }

    @Test
    public void testGetHub() throws Exception {
        // TODO
    }

    @Test
    public void testGetHubs() throws Exception {
        // TODO
    }

    @Test
    public void testSetHub() throws Exception {
        // TODO
    }

    @Test
    public void testSearch() throws Exception {
        // TODO
    }

    @Test
    public void testPullByName() throws Exception {
        // TODO
    }

    @Test
    public void testPullFromSource() throws Exception {
        // TODO
    }
}
