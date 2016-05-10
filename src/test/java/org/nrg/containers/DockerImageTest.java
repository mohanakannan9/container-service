package org.nrg.containers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.hibernate.annotations.Immutable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nrg.containers.api.ContainerControlApi;
import org.nrg.containers.config.DockerImageTestConfig;
import org.nrg.containers.model.DockerImage;
import org.nrg.containers.model.DockerImageDto;
import org.nrg.containers.services.DockerImageService;
import org.nrg.containers.services.DockerService;
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

import java.util.Date;
import java.util.List;

import static org.hamcrest.Matchers.isIn;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
@WebAppConfiguration
@ContextConfiguration(classes = DockerImageTestConfig.class)
public class DockerImageTest {
    private MockMvc mockMvc;
    final ObjectMapper mapper = new ObjectMapper();

    private final MediaType FORM = MediaType.APPLICATION_FORM_URLENCODED;
    private final MediaType JSON = MediaType.APPLICATION_JSON_UTF8;
    private final MediaType TEXT = MediaType.TEXT_PLAIN;

    @Autowired
    private DockerImageService dockerImageService;

    @Autowired
    private DockerService dockerService;

    @Autowired
    private ContainerControlApi mockDockerControlApi;

    @Autowired
    private WebApplicationContext wac;

    private static final String IMAGE_JSON =
            "{\"name\":\"foo/foo\", \"image-id\":\"abc123efg\", " +
                    "\"repo-tags\":[\"foo/foo:v1.0\", \"foo/foo:latest\"]," +
                    "\"labels\":{\"label0\":\"value0\", \"label1\":\"value1\"}}";

    private static final String IMAGE_TO_POST_JSON =
            "{\"name\":\"Arbitrary name\", \"image-id\":\"0123456789abcdef\"}";
    private static final String DOCKER_SERVER_IMAGE_JSON =
            "{\"image-id\":\"0123456789abcdef\", " +
                    "\"repo-tags\":[\"bar/bar:v1.0\", \"bar/bar:latest\"]," +
                    "\"labels\":{\"baz\":\"qux\"}," +
                    "\"on-docker-server\":true}";

    @Before
    public void setup() throws InvalidPreferenceName {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    @Test
    public void testDeserializeImage() throws Exception {
        final DockerImageDto dockerImageDto =
                mapper.readValue(IMAGE_JSON, DockerImageDto.class);

        assertEquals("foo/foo", dockerImageDto.getName());
        assertEquals("abc123efg", dockerImageDto.getImageId());
        assertEquals(Lists.newArrayList("foo/foo:v1.0", "foo/foo:latest"),
                dockerImageDto.getRepoTags());
        assertEquals(ImmutableMap.of("label0", "value0", "label1", "value1"),
                dockerImageDto.getLabels());
        assertNull(dockerImageDto.getId());
        assertNull(dockerImageDto.getCreated());
        assertNull(dockerImageDto.getEnabled());
        assertNull(dockerImageDto.getUpdated());
        assertNull(dockerImageDto.getInDatabase());
        assertNull(dockerImageDto.getOnDockerServer());
    }

    @Test
    public void testGetAllImages() throws Exception {
        final String path = "/docker/images";

        final DockerImageDto toSaveInDb =
                mapper.readValue(IMAGE_JSON, DockerImageDto.class);
        final DockerImageDto notSavedInDbDto =
                mapper.readValue(DOCKER_SERVER_IMAGE_JSON, DockerImageDto.class);

        final DockerImageDto createdDto = dockerImageService.create(toSaveInDb);
        final DockerImageDto retrievedFromDbDto =
                DockerImageDto.fromDbImage(dockerImageService.retrieve(createdDto.getId()));
        final DockerImageDto dockerServerVersionOfSavedImage =
                toSaveInDb.toBuilder()
                        .setName(null)
                        .setInDatabase(null)
                        .setOnDockerServer(true)
                        .build();
        final DockerImageDto retrievedFromDatabaseAndWithKnowledgeOfDockerServerStatus =
                retrievedFromDbDto.toBuilder().setOnDockerServer(true).build();

        when(mockDockerControlApi.getAllImages())
                .thenReturn(Lists.newArrayList(notSavedInDbDto, dockerServerVersionOfSavedImage));

        // Set up the requests we will make, and what the service will return from each one
        final MockHttpServletRequestBuilder bothRequest =
                get(path).accept(JSON).param("from-db", "true").param("from-docker-server", "true");

        final MockHttpServletRequestBuilder inDbRequest =
                get(path).accept(JSON).param("from-db", "true").param("from-docker-server", "false");

        final MockHttpServletRequestBuilder notInDbRequest =
                get(path).accept(JSON).param("from-db", "false").param("from-docker-server", "true");

        final MockHttpServletRequestBuilder badRequest =
                get(path).accept(JSON).param("from-db", "false").param("from-docker-server", "false");


        // Perform requests, check responses
        final String bothResponse =
                mockMvc.perform(bothRequest)
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(JSON))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        final List<DockerImageDto> bothList =
                mapper.readValue(bothResponse, new TypeReference<List<DockerImageDto>>(){});
        assertThat(notSavedInDbDto, isIn(bothList));
        assertThat(retrievedFromDatabaseAndWithKnowledgeOfDockerServerStatus, isIn(bothList));

        // Both: No server pref defined
//        final String failedDepResponse =
//                mockMvc.perform(bothRequest)
//                        .andExpect(status().isFailedDependency())
//                        .andReturn()
//                        .getResponse()
//                        .getContentAsString();
//        assertEquals("Set up Docker server before using this REST endpoint.",
//                failedDepResponse);

        // Just in db
        final String inDbResponse =
                mockMvc.perform(inDbRequest)
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(JSON))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        assertEquals(Lists.newArrayList(retrievedFromDbDto),
                mapper.readValue(inDbResponse, new TypeReference<List<DockerImageDto>>(){}));

        // Just on server
        final String notInDbResponse =
                mockMvc.perform(notInDbRequest)
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(JSON))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        final List<DockerImageDto> notInDbResponseList =
                mapper.readValue(notInDbResponse, new TypeReference<List<DockerImageDto>>(){});
        assertThat(notSavedInDbDto, isIn(notInDbResponseList));
        assertThat(dockerServerVersionOfSavedImage, isIn(notInDbResponseList));

        // Neither in db nor on server
        final String badReqResponse =
                mockMvc.perform(badRequest)
                        .andExpect(status().isBadRequest())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        assertEquals("At least one of the query params \"from-db\" or \"from-docker-server\" must be \"true\".",
                badReqResponse);
    }

    @Test
    public void testCreateImageFromPost() throws Exception {
        final String path = "/docker/images";
        final MockHttpServletRequestBuilder request =
                post(path)
                        .content(IMAGE_TO_POST_JSON)
                        .contentType(JSON);

        final DockerImageDto toPost =
                mapper.readValue(IMAGE_TO_POST_JSON, DockerImageDto.class);
        final DockerImageDto toRetrieveFromMockDockerServer =
                mapper.readValue(DOCKER_SERVER_IMAGE_JSON, DockerImageDto.class);
        final String imageId = toRetrieveFromMockDockerServer.getImageId();
        assertEquals(imageId, toPost.getImageId());

        when(mockDockerControlApi.getImageById(imageId))
                .thenReturn(toRetrieveFromMockDockerServer);

        mockMvc.perform(request)
                .andExpect(status().isCreated());

        final DockerImage retrievedFromDb = dockerImageService.getByImageId(imageId).get(0);
        assertNotNull(retrievedFromDb.getId());
        assertEquals(toPost.getImageId(), retrievedFromDb.getImageId());
        assertEquals(toPost.getName(), retrievedFromDb.getName());
        assertEquals(toRetrieveFromMockDockerServer.getLabels(), retrievedFromDb.getLabels());
        assertEquals(toRetrieveFromMockDockerServer.getRepoTags(), retrievedFromDb.getRepoTags());

        assertNotNull(retrievedFromDb.getCreated());
        assertNotNull(retrievedFromDb.getTimestamp());
        assertTrue(retrievedFromDb.isEnabled());
    }
}
