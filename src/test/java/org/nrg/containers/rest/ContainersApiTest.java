package org.nrg.containers.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nrg.containers.config.ContainersApiTestConfig;
import org.nrg.containers.mocks.MockImages;
import org.nrg.containers.model.Image;
import org.nrg.containers.services.ContainerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.ServletContext;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertNotNull;
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
        // TODO
    }

    @Test
    public void testGetContainerById() throws Exception {
        // TODO
    }

    @Test
    public void testGetContainerStatus() throws Exception {
        // TODO
    }

    @Test
    public void testGetAllImages() throws Exception {
        final List<Image> mockImageList = MockImages.FIRST_AND_SECOND;

        when(service.getAllImages()).thenReturn(mockImageList);

        final String response =
                mockMvc.perform(get(IMAGES).accept(MediaType.APPLICATION_JSON_UTF8))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        List<Image> responseImageList = mapper.readValue(response, new TypeReference<List<Image>>(){});
        assertThat(responseImageList, equalTo(mockImageList));
    }

    @Test
    public void testGetImageByName() throws Exception {
        final String name = MockImages.FOO_NAME;
        final Image mockImage = MockImages.FOO;

        when(service.getImageByName(name))
                .thenReturn(mockImage)
                .thenReturn(null);

        final String responseByName =
                mockMvc.perform(get(IMAGES)
                        .param("name", name)
                        .accept(MediaType.APPLICATION_JSON_UTF8))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        final Image imageByName = mapper.readValue(responseByName, Image.class);
        assertThat(imageByName, equalTo(mockImage));
    }

    @Test
    public void testGetImageById() throws Exception {
        final String id = MockImages.FOO_ID;
        final Image mockImage = MockImages.FOO;

        when(service.getImageById(id))
                .thenReturn(mockImage)
                .thenReturn(null);

        final String responseById =
                mockMvc.perform(get(IMAGES)
                        .param("id", id)
                        .accept(MediaType.APPLICATION_JSON_UTF8))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        final Image imageById = mapper.readValue(responseById, Image.class);
        assertThat(imageById, equalTo(mockImage));

        mockMvc.perform(get(IMAGES)
                .param("id", id)
                .accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testDeleteImageByName() throws Exception {
        final String name = MockImages.FOO_NAME;
        final String id = MockImages.FOO_ID;

        when(service.deleteImageByName(name))
                .thenReturn(id)
                .thenReturn(null);

        final String responseByName =
                mockMvc.perform(delete(IMAGES)
                        .param("name", name)
                        .accept(MediaType.APPLICATION_JSON_UTF8))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        assertThat(responseByName, equalTo(id));
    }

    @Test
    public void testDeleteImageById() throws Exception {
        final String id = MockImages.FOO_ID;

        when(service.deleteImageById(id))
                .thenReturn(id)
                .thenReturn(null);

        final String responseById =
                mockMvc.perform(delete(IMAGES)
                        .param("id", id)
                        .accept(MediaType.APPLICATION_JSON_UTF8))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        assertThat(responseById, equalTo(id));
    }

    @Test
    public void testDeleteImageNoParams() throws Exception {
        final String id = MockImages.FOO_ID;

        mockMvc.perform(delete(IMAGES)
                .param("id", id)
                .accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete(IMAGES)) // Note, no query params
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
