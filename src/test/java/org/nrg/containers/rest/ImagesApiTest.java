package org.nrg.containers.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nrg.containers.config.ImagesApiTestConfig;
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
@ContextConfiguration(classes = ImagesApiTestConfig.class)
public class ImagesApiTest {

    private MockMvc mockMvc;

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
    public void testGetAllImages() throws Exception {
        final List<Image> mockImageList = MockImages.FIRST_AND_SECOND;

        when(service.getAllImages()).thenReturn(mockImageList);

        final String response =
                mockMvc.perform(get("/images").accept(MediaType.APPLICATION_JSON_UTF8))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        ObjectMapper mapper = new ObjectMapper();
        List<Image> responseImageList = mapper.readValue(response, new TypeReference<List<Image>>(){});
        assertThat(responseImageList, equalTo(mockImageList));
    }

    @Test
    public void testGetImage() throws Exception {
        final String name = MockImages.FOO_NAME;
        final String id = MockImages.FOO_ID;
        final Image mockImage = MockImages.FOO;

        when(service.getImageByName(name))
                .thenReturn(mockImage)
                .thenReturn(null);

        final String responseByName =
                mockMvc.perform(get("/images")
                                .param("name", name)
                                .accept(MediaType.APPLICATION_JSON_UTF8))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        final ObjectMapper mapper = new ObjectMapper();
        final Image imageByName = mapper.readValue(responseByName, Image.class);
        assertThat(imageByName, equalTo(mockImage));

        mockMvc.perform(get("/images")
                        .param("name", name)
                        .accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isNotFound());


        when(service.getImageById(id))
                .thenReturn(mockImage)
                .thenReturn(null);

        final String responseById =
                mockMvc.perform(get("/images")
                                .param("id", id)
                                .accept(MediaType.APPLICATION_JSON_UTF8))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        final Image imageById = mapper.readValue(responseById, Image.class);
        assertThat(imageById, equalTo(mockImage));

        mockMvc.perform(get("/images")
                        .param("id", id)
                        .accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testDeleteImage() throws Exception {
        final String name = MockImages.FOO_NAME;
        final String id = MockImages.FOO_ID;

        when(service.deleteImageByName(name))
                .thenReturn(id)
                .thenReturn(null);

        final String responseByName =
                mockMvc.perform(delete("/images")
                        .param("name", name)
                        .accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(responseByName, equalTo(id));

        mockMvc.perform(delete("/images")
                        .param("name", name)
                        .accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isNotFound());


        when(service.deleteImageById(id))
                .thenReturn(id)
                .thenReturn(null);

        final String responseById =
                mockMvc.perform(delete("/images")
                                .param("id", id)
                                .accept(MediaType.APPLICATION_JSON_UTF8))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        assertThat(responseById, equalTo(id));

        mockMvc.perform(delete("/images")
                        .param("id", id)
                        .accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/images")) // Note, no query params
                .andExpect(status().isBadRequest());
                //.andExpect(content().string("Include the name or id of an image to delete in the query parameters."));
                // I wish my exception message got passed to the response body, but it doesn't.
                // May need to move to a different exception handling model
    }
}