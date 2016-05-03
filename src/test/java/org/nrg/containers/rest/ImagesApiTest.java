package org.nrg.containers.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nrg.containers.config.RestApiTestConfig;
import org.nrg.containers.exceptions.NoServerPrefException;
import org.nrg.containers.exceptions.NotFoundException;
import org.nrg.containers.model.DockerImage;
import org.nrg.containers.services.ContainerService;
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

import javax.servlet.ServletContext;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = RestApiTestConfig.class)
@SuppressWarnings("ThrowableInstanceNeverThrown")
public class ImagesApiTest {

    private MockMvc mockMvc;
    final ObjectMapper mapper = new ObjectMapper();

    final MediaType JSON = MediaType.APPLICATION_JSON_UTF8;
    final MediaType PLAIN_TEXT = MediaType.TEXT_PLAIN;

    final String FOO_REPO = "foo_repo";
    final String FOO = "foo";
    final String FOO_ID = "0";
    final DockerImage FOO_IMAGE = new DockerImage(FOO, FOO_ID, 0L,
            Lists.newArrayList("tag1", "tag2"), ImmutableMap.of("label0", "value0"));

    final NotFoundException NOT_FOUND_EXCEPTION = new NotFoundException("Some cool message");
    final NoServerPrefException NO_SERVER_PREF_EXCEPTION = new NoServerPrefException("message");
    final InvalidPreferenceName INVALID_PREFERENCE_NAME = new InvalidPreferenceName("*invalid name*");

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
    public void testGetAllImages() throws Exception {
        final DockerImage first = new DockerImage(
                "first",
                "0",
                0L,
                Lists.newArrayList("tag1", "tag2"),
                ImmutableMap.of("label0", "value0"));
        final DockerImage second = new DockerImage(
                "second",
                "1",
                1L,
                Lists.newArrayList("tagX", "tagY"),
                ImmutableMap.of("label1", "value1"));
        final List<DockerImage> mockImageList = Lists.newArrayList(first, second);

        final String path = "/containers/images";
        final MockHttpServletRequestBuilder request = get(path).accept(JSON);

        when(service.getAllImages())
                .thenReturn(mockImageList)                              // Happy path
                .thenThrow(NO_SERVER_PREF_EXCEPTION);    // No server pref defined

        // Happy path
        final String response =
                mockMvc.perform(request)
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(JSON))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        List<DockerImage> responseImageList = mapper.readValue(response, new TypeReference<List<DockerImage>>(){});
        assertThat(responseImageList, equalTo(mockImageList));

        // No server pref defined
        mockMvc.perform(request)
                .andExpect(status().isFailedDependency());
    }

    @Test
    public void testGetImageByIdWithNameFallback() throws Exception {
        final String id = "abc123"; // at least 6 hex digits
        final String path = "/containers/images/" + id;
        final MockHttpServletRequestBuilder request = get(path).accept(JSON);

        when(service.getImageById(id))
                .thenReturn(FOO_IMAGE)                                  // Happy path
                .thenThrow(NO_SERVER_PREF_EXCEPTION)    // No server pref defined
                .thenThrow(NOT_FOUND_EXCEPTION)          // Not found
                .thenThrow(NOT_FOUND_EXCEPTION);          // Not found part 2


        when(service.getImageByName(id))
                .thenReturn(FOO_IMAGE)                 // Not found (continued)
                .thenThrow(NOT_FOUND_EXCEPTION);          // Not found part 2 (continued)

        // Happy path
        final String responseHappyPath =
                mockMvc.perform(request)
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(JSON))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        final DockerImage imageHappyPath = mapper.readValue(responseHappyPath, DockerImage.class);
        assertEquals(imageHappyPath, FOO_IMAGE);

        // No server pref defined
        mockMvc.perform(request)
                .andExpect(status().isFailedDependency());

        // Not found
        final String responseNotFound =
                mockMvc.perform(request)
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(JSON))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        final DockerImage imageNotFound = mapper.readValue(responseNotFound, DockerImage.class);
        assertEquals(imageNotFound, FOO_IMAGE);

        // Not found part 2
        mockMvc.perform(request)
                .andExpect(status().isNotFound());

    }

    @Test
    public void testGetImageById() throws Exception {
        final String path = "/containers/images/id/" + FOO_ID;
        final MockHttpServletRequestBuilder request = get(path).accept(JSON);

        when(service.getImageById(FOO_ID))
                .thenReturn(FOO_IMAGE)                                  // Happy path
                .thenThrow(NOT_FOUND_EXCEPTION)          // Not found
                .thenThrow(NO_SERVER_PREF_EXCEPTION);    // No server pref defined

        // Happy path
        final String responseById =
                mockMvc.perform(request)
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(JSON))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        final DockerImage imageById = mapper.readValue(responseById, DockerImage.class);
        assertThat(imageById, equalTo(FOO_IMAGE));

        // Not found
        mockMvc.perform(request)
                .andExpect(status().isNotFound());

        // No server pref defined
        mockMvc.perform(request)
                .andExpect(status().isFailedDependency());
    }

    @Test
    public void testGetImageByName() throws Exception {

        final String path = "/containers/images/" + FOO;
        final MockHttpServletRequestBuilder request = get(path).accept(JSON);

        when(service.getImageByName(FOO))
                .thenReturn(FOO_IMAGE)                                  // Happy path
                .thenThrow(NOT_FOUND_EXCEPTION)          // Not found
                .thenThrow(NO_SERVER_PREF_EXCEPTION);    // No server pref defined

        // Happy path
        final String responseByName =
                mockMvc.perform(request)
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(JSON))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        final DockerImage imageByName = mapper.readValue(responseByName, DockerImage.class);
        assertThat(imageByName, equalTo(FOO_IMAGE));

        // Not found
        mockMvc.perform(request)
                .andExpect(status().isNotFound());

        // No server pref defined
        mockMvc.perform(request)
                .andExpect(status().isFailedDependency());
    }

    @Test
    public void testGetImageByNameWithRepo() throws Exception {
        final String name = FOO_REPO + "/" + FOO;
        final String path = "/containers/images/" + name;
        final MockHttpServletRequestBuilder request = get(path).accept(JSON);

        when(service.getImageByName(name))
                .thenReturn(FOO_IMAGE)                                  // Happy path
                .thenThrow(NOT_FOUND_EXCEPTION)          // Not found
                .thenThrow(NO_SERVER_PREF_EXCEPTION);    // No server pref defined

        // Happy path
        final String responseByName =
                mockMvc.perform(request)
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(JSON))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        final DockerImage imageByName = mapper.readValue(responseByName, DockerImage.class);
        assertThat(imageByName, equalTo(FOO_IMAGE));

        // Not found
        mockMvc.perform(request)
                .andExpect(status().isNotFound());

        // No server pref defined
        mockMvc.perform(request)
                .andExpect(status().isFailedDependency());
    }

    @Test
    public void testDeleteImageByName() throws Exception {
        final String path = "/containers/images/name/" + FOO;

        // REQUEST 0: No "onServer" param (defaults to false)
        final MockHttpServletRequestBuilder request0 = delete(path);

        // REQUEST 1: "server=false"
        final MockHttpServletRequestBuilder request1 = delete(path).param("server", "false");

        // REQUEST 2: "server=true"
        final MockHttpServletRequestBuilder request2 = delete(path).param("server", "true");

        // Mock service
        when(service.deleteImageByName(FOO, Boolean.FALSE))
                .thenReturn(FOO_ID)                      // Happy path Request 0
                .thenReturn(FOO_ID)                      // Happy path Request 1
                .thenThrow(NOT_FOUND_EXCEPTION)          // Not found
                .thenThrow(NO_SERVER_PREF_EXCEPTION);    // No server pref defined

        when(service.deleteImageByName(FOO, Boolean.TRUE))
                .thenReturn(FOO_ID)                      // Happy path Request 2
                .thenThrow(NOT_FOUND_EXCEPTION)          // Not found
                .thenThrow(NO_SERVER_PREF_EXCEPTION);    // No server pref defined

        // Happy path Request 0
        final String response0 =
                mockMvc.perform(request0)
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(PLAIN_TEXT))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        assertThat(response0, equalTo(FOO_ID));

        // Happy path Request 1
        final String response1 =
                mockMvc.perform(request1)
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(PLAIN_TEXT))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        assertThat(response1, equalTo(FOO_ID));

        // Not found
        mockMvc.perform(request0).andExpect(status().isNotFound());

        // No server pref defined
        mockMvc.perform(request0).andExpect(status().isFailedDependency());

        // Happy path Request 2
        final String response2 =
                mockMvc.perform(request2)
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(PLAIN_TEXT))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        assertThat(response2, equalTo(FOO_ID));

        // Not found
        mockMvc.perform(request2).andExpect(status().isNotFound());

        // No server pref defined
        mockMvc.perform(request2).andExpect(status().isFailedDependency());
    }

    @Test
    public void testDeleteImageByNameWithRepo() throws Exception {
        final String name = FOO_REPO + "/" + FOO;
        final String path = "/containers/images/name/" + name;

        // REQUEST 0: No "onServer" param (defaults to false)
        final MockHttpServletRequestBuilder request0 = delete(path);

        // REQUEST 1: "server=false"
        final MockHttpServletRequestBuilder request1 = delete(path).param("server", "false");

        // REQUEST 2: "server=true"
        final MockHttpServletRequestBuilder request2 = delete(path).param("server", "true");

        // Mock service
        when(service.deleteImageByName(name, Boolean.FALSE))
                .thenReturn(FOO_ID)                      // Happy path Request 0
                .thenReturn(FOO_ID)                      // Happy path Request 1
                .thenThrow(NOT_FOUND_EXCEPTION)          // Not found
                .thenThrow(NO_SERVER_PREF_EXCEPTION);    // No server pref defined

        when(service.deleteImageByName(name, Boolean.TRUE))
                .thenReturn(FOO_ID)                      // Happy path Request 2
                .thenThrow(NOT_FOUND_EXCEPTION)          // Not found
                .thenThrow(NO_SERVER_PREF_EXCEPTION);    // No server pref defined

        // Happy path Request 0
        final String response0 =
                mockMvc.perform(request0)
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(PLAIN_TEXT))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        assertThat(response0, equalTo(FOO_ID));

        // Happy path Request 1
        final String response1 =
                mockMvc.perform(request1)
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(PLAIN_TEXT))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        assertThat(response1, equalTo(FOO_ID));

        // Not found
        mockMvc.perform(request0).andExpect(status().isNotFound());

        // No server pref defined
        mockMvc.perform(request0).andExpect(status().isFailedDependency());

        // Happy path Request 2
        final String response2 =
                mockMvc.perform(request2)
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(PLAIN_TEXT))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        assertThat(response2, equalTo(FOO_ID));

        // Not found
        mockMvc.perform(request2).andExpect(status().isNotFound());

        // No server pref defined
        mockMvc.perform(request2).andExpect(status().isFailedDependency());
    }

    @Test
    public void testDeleteImageById() throws Exception {
        final String path = "/containers/images/id/" + FOO_ID;

        // REQUEST 0: No "onServer" param (defaults to false)
        final MockHttpServletRequestBuilder request0 = delete(path);

        // REQUEST 1: "server=false"
        final MockHttpServletRequestBuilder request1 = delete(path).param("server", "false");

        // REQUEST 2: "server=true"
        final MockHttpServletRequestBuilder request2 = delete(path).param("server", "true");

        // Mock service
        when(service.deleteImageById(FOO_ID, Boolean.FALSE))
                .thenReturn(FOO_ID)                                         // Happy path Request 0
                .thenReturn(FOO_ID)                                         // Happy path Request 1
                .thenThrow(NOT_FOUND_EXCEPTION)          // Not found
                .thenThrow(NO_SERVER_PREF_EXCEPTION);    // No server pref defined

        when(service.deleteImageById(FOO_ID, Boolean.TRUE))
                .thenReturn(FOO_ID)                                         // Happy path Request 2
                .thenThrow(NOT_FOUND_EXCEPTION)          // Not found
                .thenThrow(NO_SERVER_PREF_EXCEPTION);    // No server pref defined

        // Happy path Request 0
        final String response0 =
                mockMvc.perform(request0)
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(PLAIN_TEXT))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        assertThat(response0, equalTo(FOO_ID));

        // Happy path Request 1
        final String response1 =
                mockMvc.perform(request1)
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(PLAIN_TEXT))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        assertThat(response1, equalTo(FOO_ID));

        // Not found
        mockMvc.perform(request0).andExpect(status().isNotFound());

        // No server pref defined
        mockMvc.perform(request0).andExpect(status().isFailedDependency());

        // Happy path Request 2
        final String response2 =
                mockMvc.perform(request2)
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(PLAIN_TEXT))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        assertThat(response2, equalTo(FOO_ID));

        // Not found
        mockMvc.perform(request2).andExpect(status().isNotFound());

        // No server pref defined
        mockMvc.perform(request2).andExpect(status().isFailedDependency());
    }

    @Test
    public void testSetMetadataByName() throws Exception {
        // TODO
    }

    @Test
    public void testSetMetadataById() throws Exception {
        // TODO
    }

    @Test
    public void testSetMetadataByIdWithNameFallback() throws Exception {

    }
}
