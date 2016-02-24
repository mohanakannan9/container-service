package org.nrg.containers.test.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.nrg.containers.model.Image;
import org.nrg.containers.services.ContainerService;
import org.nrg.containers.services.impl.DefaultContainerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import javax.servlet.ServletContext;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = ImagesApiTest.ContainersTestConfig.class)
public class ImagesApiTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    @Qualifier("mockContainerService")
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
        final String firstName = "first";
        final String firstId = "0";
        final Long firstSize = 0L;
        final List<String> firstTags = Lists.newArrayList("tag1", "tag2");
        final Map<String, String> firstLabels = ImmutableMap.of("label0", "value0");
        final Image first = new Image(firstName, firstId, firstSize, firstTags, firstLabels);

        final String secondName = "second";
        final String secondId = "0";
        final Long secondSize = 0L;
        final List<String> secondTags = Lists.newArrayList("tagx", "tagY");
        final Map<String, String> secondLabels = ImmutableMap.of("label1", "value1");
        final Image second = new Image(secondName, secondId, secondSize, secondTags, secondLabels);

        final List<Image> mockImageList = Lists.newArrayList(first, second);

        when(service.getAllImages()).thenReturn(mockImageList);

        final String response = mockMvc.perform(get("/images").accept(MediaType.parseMediaType("application/json;charset=UTF-8")))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        ObjectMapper mapper = new ObjectMapper();
        List<Image> responseImageList = mapper.readValue(response, new TypeReference<List<Image>>(){});
        assertThat(responseImageList, equalTo(mockImageList));
    }

    @Test
    public void testGetImageByName() throws Exception {
        final String name = "foo";
        final String id = "0";
        final Long size = 0L;
        final List<String> tags = Lists.newArrayList("tag1", "tag2");
        final Map<String, String> labels = ImmutableMap.of("label0", "value0");
        final Image mockImage = new Image(name, id, size, tags, labels);

        when(service.getImageByName(name)).thenReturn(mockImage);

        final String response =
                mockMvc.perform(
                                get("/images")
                                .param("name", "foo")
                                .accept(MediaType.parseMediaType("application/json;charset=UTF-8")))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        ObjectMapper mapper = new ObjectMapper();
        Image responseImage = mapper.readValue(response, Image.class);
        assertThat(responseImage, equalTo(mockImage));
    }

    @Configuration
    @EnableWebMvc
    @ComponentScan(value = "org.nrg.containers.services, org.nrg.containers.rest", resourcePattern = "*.class")
    static class ContainersTestConfig {
        @Bean
        public ContainerService mockContainerService() {
            return Mockito.mock(DefaultContainerService.class);
        }
    }

}