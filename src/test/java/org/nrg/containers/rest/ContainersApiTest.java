package org.nrg.containers.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nrg.containers.config.ContainersApiTestConfig;
import org.nrg.containers.model.Container;
import org.nrg.containers.model.ContainerMocks;
import org.nrg.containers.model.ExceptionMocks;
import org.nrg.containers.model.Image;
import org.nrg.containers.model.ImageMocks;
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
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = ContainersApiTestConfig.class)
public class ContainersApiTest {
    private final String IMAGES =
            ContainerService.CONTAINER_SERVICE_REST_PATH_PREFIX + ContainerService.IMAGES_REST_PATH;
    private final String CONTAINERS =
            ContainerService.CONTAINER_SERVICE_REST_PATH_PREFIX + ContainerService.CONTAINERS_REST_PATH;

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

        final String path = ContainerService.CONTAINER_SERVICE_REST_PATH_PREFIX;
        final MockHttpServletRequestBuilder request = get(path).accept(MediaType.APPLICATION_JSON_UTF8);

        when(service.getAllContainers())
                .thenReturn(mockContainerList)                          // Happy path
                .thenThrow(ExceptionMocks.NO_SERVER_PREF_EXCEPTION);    // No server pref defined

        // Happy path
        final String response =
                mockMvc.perform(request)
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
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
    public void testGetContainerById() throws Exception {
        final String id = ContainerMocks.FOO_ID;
        final Container mockContainer = ContainerMocks.FOO;

        final String path = CONTAINERS;
        final MockHttpServletRequestBuilder request = get(path).param("id", id).accept(MediaType.APPLICATION_JSON_UTF8);

        when(service.getContainer(id))
                .thenReturn(mockContainer)                              // Happy path
                .thenThrow(ExceptionMocks.NOT_FOUND_EXCEPTION)          // Not found
                .thenThrow(ExceptionMocks.NO_SERVER_PREF_EXCEPTION);    // No server pref defined

        // Happy path
        final String responseById =
                mockMvc.perform(request)
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
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

        final String path = CONTAINERS+"/status";
        final MockHttpServletRequestBuilder request = get(path).param("id", id).accept(MediaType.APPLICATION_JSON_UTF8);

        when(service.getContainerStatus(id))
                .thenReturn(status)                                     // Happy path
                .thenThrow(ExceptionMocks.NO_SERVER_PREF_EXCEPTION);    // No server pref defined

        // Happy path
        final String response =
                mockMvc.perform(request)
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        assertThat(response, equalTo(status));

        // No server pref defined
        mockMvc.perform(request)
                .andExpect(status().isFailedDependency());
    }

    @Test
    public void testGetAllImages() throws Exception {
        final List<Image> mockImageList = ImageMocks.FIRST_AND_SECOND;

        final String path = IMAGES;
        final MockHttpServletRequestBuilder request = get(path).accept(MediaType.APPLICATION_JSON_UTF8);

        when(service.getAllImages())
                .thenReturn(mockImageList)                              // Happy path
                .thenThrow(ExceptionMocks.NO_SERVER_PREF_EXCEPTION);    // No server pref defined

        // Happy path
        final String response =
                mockMvc.perform(request)
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        List<Image> responseImageList = mapper.readValue(response, new TypeReference<List<Image>>(){});
        assertThat(responseImageList, equalTo(mockImageList));

        // No server pref defined
        mockMvc.perform(request)
                .andExpect(status().isFailedDependency());
    }

    @Test
    public void testGetImageByName() throws Exception {
        final String name = ImageMocks.FOO_NAME;
        final Image mockImage = ImageMocks.FOO;

        final String path = IMAGES;
        final MockHttpServletRequestBuilder request = get(path).param("name", name).accept(MediaType.APPLICATION_JSON_UTF8);

        when(service.getImageByName(name))
                .thenReturn(mockImage)                                  // Happy path
                .thenThrow(ExceptionMocks.NOT_FOUND_EXCEPTION)          // Not found
                .thenThrow(ExceptionMocks.NO_SERVER_PREF_EXCEPTION);    // No server pref defined

        // Happy path
        final String responseByName =
                mockMvc.perform(request)
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        final Image imageByName = mapper.readValue(responseByName, Image.class);
        assertThat(imageByName, equalTo(mockImage));

        // Not found
        mockMvc.perform(request)
                .andExpect(status().isNotFound());

        // No server pref defined
        mockMvc.perform(request)
                .andExpect(status().isFailedDependency());
    }

    @Test
    public void testGetImageById() throws Exception {
        final String id = ImageMocks.FOO_ID;
        final Image mockImage = ImageMocks.FOO;

        final String path = IMAGES;
        final MockHttpServletRequestBuilder request = get(path).param("id", id).accept(MediaType.APPLICATION_JSON_UTF8);

        when(service.getImageById(id))
                .thenReturn(mockImage)                                  // Happy path
                .thenThrow(ExceptionMocks.NOT_FOUND_EXCEPTION)          // Not found
                .thenThrow(ExceptionMocks.NO_SERVER_PREF_EXCEPTION);    // No server pref defined

        // Happy path
        final String responseById =
                mockMvc.perform(request)
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        final Image imageById = mapper.readValue(responseById, Image.class);
        assertThat(imageById, equalTo(mockImage));

        // Not found
        mockMvc.perform(request)
                .andExpect(status().isNotFound());

        // No server pref defined
        mockMvc.perform(request)
                .andExpect(status().isFailedDependency());
    }

    @Test
    public void testDeleteImageByName() throws Exception {
        final String name = ImageMocks.FOO_NAME;
        final String id = ImageMocks.FOO_ID;

        final String path = IMAGES;

        // REQUEST 0: No "onServer" param (defaults to false)
        final MockHttpServletRequestBuilder request0 =
                delete(path).param("name", name).accept(MediaType.APPLICATION_JSON_UTF8);

        // REQUEST 1: "onServer=false"
        final MockHttpServletRequestBuilder request1 = request0.param("server", "false");

        // REQUEST 2: "onServer=true"
        final MockHttpServletRequestBuilder request2 =
                delete(path).param("name", name).accept(MediaType.APPLICATION_JSON_UTF8).param("server", "true");

        // Mock service
        when(service.deleteImageByName(name, false))
                .thenReturn(id)                                         // Happy path Request 0
                .thenReturn(id)                                         // Happy path Request 1
                .thenThrow(ExceptionMocks.NOT_FOUND_EXCEPTION)          // Not found
                .thenThrow(ExceptionMocks.NO_SERVER_PREF_EXCEPTION);    // No server pref defined

        when(service.deleteImageByName(name, true))
                .thenReturn(id)                                         // Happy path Request 2
                .thenThrow(ExceptionMocks.NOT_FOUND_EXCEPTION)          // Not found
                .thenThrow(ExceptionMocks.NO_SERVER_PREF_EXCEPTION);    // No server pref defined

        // Happy path Request 0
        final String response0 =
                mockMvc.perform(request0)
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        assertThat(response0, equalTo(id));

        // Happy path Request 1
        final String response1 =
                mockMvc.perform(request1)
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        assertThat(response1, equalTo(id));

        // Not found
        mockMvc.perform(request0).andExpect(status().isNotFound());

        // No server pref defined
        mockMvc.perform(request0).andExpect(status().isFailedDependency());

        // Happy path Request 2
        final String response2 =
                mockMvc.perform(request2)
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        assertThat(response2, equalTo(id));

        // Not found
        mockMvc.perform(request2).andExpect(status().isNotFound());

        // No server pref defined
        mockMvc.perform(request2).andExpect(status().isFailedDependency());
    }

    @Test
    public void testDeleteImageById() throws Exception {
        final String id = ImageMocks.FOO_ID;

        final String path = IMAGES;

        // REQUEST 0: No "onServer" param (defaults to false)
        final MockHttpServletRequestBuilder request0 =
                delete(path).param("id", id).accept(MediaType.APPLICATION_JSON_UTF8);

        // REQUEST 1: "onServer=false"
        final MockHttpServletRequestBuilder request1 = request0.param("server", "false");

        // REQUEST 2: "onServer=true"
        final MockHttpServletRequestBuilder request2 =
                delete(path).param("id", id).param("server", "true").accept(MediaType.APPLICATION_JSON_UTF8);

        // Mock service
        when(service.deleteImageById(id, false))
                .thenReturn(id)                                         // Happy path Request 0
                .thenReturn(id)                                         // Happy path Request 1
                .thenThrow(ExceptionMocks.NOT_FOUND_EXCEPTION)          // Not found
                .thenThrow(ExceptionMocks.NO_SERVER_PREF_EXCEPTION);    // No server pref defined

        when(service.deleteImageById(id, true))
                .thenReturn(id)                                         // Happy path Request 2
                .thenThrow(ExceptionMocks.NOT_FOUND_EXCEPTION)          // Not found
                .thenThrow(ExceptionMocks.NO_SERVER_PREF_EXCEPTION);    // No server pref defined

        // Happy path Request 0
        final String response0 =
                mockMvc.perform(request0)
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        assertThat(response0, equalTo(id));

        // Happy path Request 1
        final String response1 =
                mockMvc.perform(request1)
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        assertThat(response1, equalTo(id));

        // Not found
        mockMvc.perform(request0).andExpect(status().isNotFound());

        // No server pref defined
        mockMvc.perform(request0).andExpect(status().isFailedDependency());

        // Happy path Request 2
        final String response2 =
                mockMvc.perform(request2)
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        assertThat(response2, equalTo(id));

        // Not found
        mockMvc.perform(request2).andExpect(status().isNotFound());

        // No server pref defined
        mockMvc.perform(request2).andExpect(status().isFailedDependency());
    }

    @Test
    public void testDeleteImageByIdInPath() throws Exception {
        final String id = ImageMocks.FOO_ID;

        final String path = IMAGES + "/" + id;

        // REQUEST 0: No "onServer" param (defaults to false)
        final MockHttpServletRequestBuilder request0 =
                delete(path).accept(MediaType.APPLICATION_JSON_UTF8);

        // REQUEST 1: "onServer=false"
        final MockHttpServletRequestBuilder request1 = request0.param("server", "false");

        // REQUEST 2: "onServer=true"
        final MockHttpServletRequestBuilder request2 =
                delete(path).param("server", "true").accept(MediaType.APPLICATION_JSON_UTF8);

        // Mock service
        when(service.deleteImageById(id, false))
                .thenReturn(id)                                         // Happy path Request 0
                .thenReturn(id)                                         // Happy path Request 1
                .thenThrow(ExceptionMocks.NOT_FOUND_EXCEPTION)          // Not found
                .thenThrow(ExceptionMocks.NO_SERVER_PREF_EXCEPTION);    // No server pref defined

        when(service.deleteImageById(id, true))
                .thenReturn(id)                                         // Happy path Request 2
                .thenThrow(ExceptionMocks.NOT_FOUND_EXCEPTION)          // Not found
                .thenThrow(ExceptionMocks.NO_SERVER_PREF_EXCEPTION);    // No server pref defined

        // Happy path Request 0
        final String response0 =
                mockMvc.perform(request0)
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        assertThat(response0, equalTo(id));

        // Happy path Request 1
        final String response1 =
                mockMvc.perform(request1)
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        assertThat(response1, equalTo(id));

        // Not found
        mockMvc.perform(request0).andExpect(status().isNotFound());

        // No server pref defined
        mockMvc.perform(request0).andExpect(status().isFailedDependency());

        // Happy path Request 2
        final String response2 =
                mockMvc.perform(request2)
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        assertThat(response2, equalTo(id));

        // Not found
        mockMvc.perform(request2).andExpect(status().isNotFound());

        // No server pref defined
        mockMvc.perform(request2).andExpect(status().isFailedDependency());
    }

    @Test
    public void testDeleteImageNoParams() throws Exception {
        final String path = IMAGES;
        final MockHttpServletRequestBuilder request = delete(path).accept(MediaType.APPLICATION_JSON_UTF8); // Note, no query params

        mockMvc.perform(request)
                .andExpect(status().isBadRequest());
        //.andExpect(content().string("Include the name or id of an image to delete in the query parameters."));
        // I wish my exception message got passed to the response body, but it doesn't.
        // May need to move to a different exception handling model
    }

    @Test
    public void testLaunch() throws Exception {
        // TODO
    }

    @Test
    public void testGetServer() throws Exception {
        // TODO
    }

    @Test
    public void testSetServer() throws Exception {
        // TODO
    }
}
