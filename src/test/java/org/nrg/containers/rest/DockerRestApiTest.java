package org.nrg.containers.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.spotify.docker.client.exceptions.VolumeNotFoundException;
import org.apache.ecs.wml.Do;
import org.hamcrest.CustomTypeSafeMatcher;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nrg.containers.api.ContainerControlApi;
import org.nrg.containers.config.DockerRestApiTestConfig;
import org.nrg.containers.exceptions.DockerServerException;
import org.nrg.containers.exceptions.NoServerPrefException;
import org.nrg.containers.exceptions.NotFoundException;
import org.nrg.containers.model.Command;
import org.nrg.containers.model.DockerCommand;
import org.nrg.containers.model.DockerImage;
import org.nrg.containers.model.DockerServer;
import org.nrg.containers.model.DockerServerPrefsBean;
import org.nrg.containers.model.XnatCommandWrapper;
import org.nrg.containers.rest.models.DockerImageAndCommandSummary;
import org.nrg.containers.services.CommandService;
import org.nrg.containers.services.DockerService;
import org.nrg.prefs.exceptions.InvalidPreferenceName;
import org.nrg.xdat.security.services.RoleServiceI;
import org.nrg.xft.security.UserI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
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
import static org.hamcrest.Matchers.arrayContainingInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isIn;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.nrg.containers.api.ContainerControlApi.LABEL_KEY;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.testSecurityContext;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
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

    private final static String MOCK_CONTAINER_SERVER_NAME = "test server";
    private final static String MOCK_CONTAINER_HOST = "fake://host.url";

    private final static String MOCK_CONTAINER_CERT_PATH = "/path/to/file";
    private final static DockerServer MOCK_CONTAINER_SERVER =
            new DockerServer(MOCK_CONTAINER_SERVER_NAME, MOCK_CONTAINER_HOST, MOCK_CONTAINER_CERT_PATH);
    private final NoServerPrefException NO_SERVER_PREF_EXCEPTION =
            new NoServerPrefException("message");

    private final InvalidPreferenceName INVALID_PREFERENCE_NAME =
            new InvalidPreferenceName("*invalid name*");
    private final DockerServerException DOCKER_SERVER_EXCEPTION =
            new DockerServerException("Your server dun goofed.");
    private final NotFoundException NOT_FOUND_EXCEPTION =
            new NotFoundException("I should think of a message");

    @Autowired private WebApplicationContext wac;
    @Autowired private ObjectMapper mapper;
    @Autowired private ContainerControlApi mockContainerControlApi;
    @Autowired private RoleServiceI mockRoleService;
    @Autowired private CommandService mockCommandService;
    @Autowired private DockerServerPrefsBean mockDockerServerPrefsBean;

    @Before
    public void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).apply(springSecurity()).build();
    }

    @Test
    public void testGetServer() throws Exception {

        final String path = "/docker/server";

        final Authentication authentication = new TestingAuthenticationToken("nonAdmin", "nonAdmin");
        final MockHttpServletRequestBuilder request =
                get(path).accept(JSON)
                        .with(authentication(authentication))
                        .with(csrf())
                        .with(testSecurityContext());

        doReturn(MOCK_CONTAINER_SERVER)
                .when(mockContainerControlApi).getServer();

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

        doThrow(NO_SERVER_PREF_EXCEPTION)
                .when(mockContainerControlApi).getServer();

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

        final UserI admin = mock(UserI.class);
        when(admin.getLogin()).thenReturn("admin");
        when(admin.getPassword()).thenReturn("admin");
        when(mockRoleService.isSiteAdmin(admin)).thenReturn(true);

        final Authentication authentication = new TestingAuthenticationToken(admin, "admin");

        final MockHttpServletRequestBuilder request =
                post(path)
                        .content(containerServerJson)
                        .contentType(JSON)
                        .with(authentication(authentication))
                        .with(csrf())
                        .with(testSecurityContext());

        doReturn(MOCK_CONTAINER_SERVER)
                .when(mockContainerControlApi).setServer(MOCK_CONTAINER_SERVER);

        mockMvc.perform(request)
                .andExpect(status().isAccepted());

        verify(mockContainerControlApi, times(1)).setServer(MOCK_CONTAINER_SERVER); // Method has been called once

        // Now verify the exception
        doThrow(INVALID_PREFERENCE_NAME)
                .when(mockContainerControlApi).setServer(MOCK_CONTAINER_SERVER);

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
    public void testSetServerNonAdmin() throws Exception {

        final String containerServerJson =
                mapper.writeValueAsString(MOCK_CONTAINER_SERVER);

        final String path = "/docker/server";

        final UserI nonAdmin = mock(UserI.class);
        when(nonAdmin.getLogin()).thenReturn("nonAdmin");
        when(nonAdmin.getPassword()).thenReturn("nonAdmin");
        when(mockRoleService.isSiteAdmin(nonAdmin)).thenReturn(false);
        final Authentication authentication = new TestingAuthenticationToken(nonAdmin, "nonAdmin");

        final MockHttpServletRequestBuilder request =
                post(path)
                        .content(containerServerJson)
                        .contentType(JSON)
                        .with(authentication(authentication))
                        .with(csrf())
                        .with(testSecurityContext());

        final String exceptionResponse =
                mockMvc.perform(request)
                        .andExpect(status().isUnauthorized())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        assertThat(exceptionResponse, containsString("nonAdmin"));
    }

    @Test
    public void testPingServer() throws Exception {
        final String path = "/docker/server/ping";

        final Authentication authentication = new TestingAuthenticationToken("nonAdmin", "nonAdmin");
        final MockHttpServletRequestBuilder request =
                get(path).with(authentication(authentication))
                        .with(csrf()).with(testSecurityContext());

        doReturn("OK")
                .when(mockContainerControlApi).pingServer();

        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(content().string(equalTo("OK")));

        doThrow(DOCKER_SERVER_EXCEPTION)
                .when(mockContainerControlApi).pingServer();

        final String ISEResponse =
                mockMvc.perform(request)
                        .andExpect(status().isInternalServerError())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        assertEquals("The Docker server returned an error:\nYour server dun goofed.", ISEResponse);

        doThrow(NO_SERVER_PREF_EXCEPTION)
                .when(mockContainerControlApi).pingServer();

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
        final String fakeImageId = "xnat/thisisfake";
        final String labelTestCommandJson =
                "{\"name\": \"label-test\"," +
                        "\"description\": \"Command to test label-parsing and command-importing code\"," +
                        "\"type\": \"docker\", " +
                        "\"command-line\": \"#CMD#\"," +
                        "\"inputs\": [{\"name\": \"CMD\", \"description\": \"Command to run\", \"required\": true}]}";
        final Command expected = mapper.readValue(labelTestCommandJson, Command.class);
        final Command toReturn = mapper.readValue(labelTestCommandJson, Command.class);
        toReturn.setImage(fakeImageId);
        final List<Command> toReturnList = Lists.newArrayList(toReturn);

        final Map<String, String> imageLabels = Maps.newHashMap();
        imageLabels.put(LABEL_KEY, "[" + labelTestCommandJson + "]");

        doReturn(toReturnList).when(mockContainerControlApi).parseLabels(fakeImageId);
        when(mockCommandService.save(toReturnList)).thenReturn(toReturnList);

        final String path = "/docker/images/save";
        final Authentication authentication = new TestingAuthenticationToken("nonAdmin", "nonAdmin");
        final MockHttpServletRequestBuilder request =
                post(path).param("image", fakeImageId)
                        .with(authentication(authentication))
                        .with(csrf()).with(testSecurityContext());

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
        assertEquals(expected.getCommandLine(), response.getCommandLine());
        assertEquals(fakeImageId, response.getImage()); // Did not set image ID on "expected"
        assertEquals(expected.getInputs(), response.getInputs());
        assertEquals(fakeImageId, response.getImage());
    }

    @Test
    @Transactional
    public void testSaveFromLabels2() throws Exception {
        final String fakeImageId = "xnat/thisisfake";
        final String labelTestCommandListJson =
                "[{\"name\":\"dcm2niix-scan\", \"description\":\"Run dcm2niix on a scan's DICOMs\", " +
                        "\"type\": \"docker\", " +
                        "\"command-line\": \"/run/dcm2niix-scan.sh #scanId# #sessionId#\", " +
                        "\"mounts\": [" +
                            "{\"name\":\"DICOM\", \"path\":\"/input\"}," +
                            "{\"name\":\"NIFTI\", \"path\":\"/output\"}" +
                        "]," +
                        "\"inputs\":[" +
                            "{\"name\":\"scanId\", \"required\":true}, " +
                            "{\"name\":\"sessionId\", \"required\":true}" +
                        "] " +
                    "}]";
        final List<Command> expectedList = mapper.readValue(labelTestCommandListJson, new TypeReference<List<Command>>(){});
        final Command expected = expectedList.get(0);

        final List<Command> toReturnList = mapper.readValue(labelTestCommandListJson, new TypeReference<List<Command>>(){});
        for (final Command toReturn : toReturnList) {
            toReturn.setImage(fakeImageId);
        }

        final Map<String, String> imageLabels = Maps.newHashMap();
        imageLabels.put(LABEL_KEY, labelTestCommandListJson);

        doReturn(toReturnList).when(mockContainerControlApi).parseLabels(fakeImageId);
        when(mockCommandService.save(toReturnList)).thenReturn(toReturnList);

        final String path = "/docker/images/save";
        final Authentication authentication = new TestingAuthenticationToken("nonAdmin", "nonAdmin");
        final MockHttpServletRequestBuilder request =
                post(path).param("image", fakeImageId)
                        .with(authentication(authentication))
                        .with(csrf()).with(testSecurityContext());

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
        assertEquals(expected.getCommandLine(), response.getCommandLine());
        assertEquals(fakeImageId, response.getImage()); // Did not set image ID on "expected"
        assertEquals(expected.getInputs(), response.getInputs());
        assertEquals(fakeImageId, response.getImage());
    }

    @Test
    public void getImages() throws Exception {
        final String fakeImageId = "sha256:some godawful hash";
        final String fakeImageName = "xnat/thisisfake";
        final DockerImage fakeDockerImage = new DockerImage();
        fakeDockerImage.setImageId(fakeImageId);
        fakeDockerImage.addTag(fakeImageName);

        doReturn(Lists.newArrayList(fakeDockerImage)).when(mockContainerControlApi).getAllImages();

        final String path = "/docker/images";
        final Authentication authentication = new TestingAuthenticationToken("nonAdmin", "nonAdmin");
        final MockHttpServletRequestBuilder request = get(path)
                .with(authentication(authentication))
                .with(csrf()).with(testSecurityContext());

        final String responseStr =
                mockMvc.perform(request)
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        final List<DockerImage> responseList = mapper.readValue(responseStr, new TypeReference<List<DockerImage>>(){});
        assertThat(responseList, hasSize(1));
        assertEquals(fakeDockerImage, responseList.get(0));
    }

    @Test
    public void getImageSummaries() throws Exception {
        final String fakeImageId = "sha256:some godawful hash";
        final String fakeImageName = "xnat/thisisfake";
        final DockerImage fakeDockerImage = new DockerImage();
        fakeDockerImage.setImageId(fakeImageId);
        fakeDockerImage.addTag(fakeImageName);

        final String fakeCommandName = "fake";
        final String fakeCommandWrapperName = "fake-on-thing";
        final XnatCommandWrapper fakeWrapper = new XnatCommandWrapper();
        fakeWrapper.setName(fakeCommandWrapperName);
        final DockerCommand fakeCommand = new DockerCommand();
        fakeCommand.setHash(fakeImageId);
        fakeCommand.setName(fakeCommandName);
        fakeCommand.setImage(fakeImageName);
        fakeCommand.addXnatCommandWrapper(fakeWrapper);

        final String unknownImageName = "unknown";
        final String unknownCommandName = "image-unknown";
        final DockerCommand unknownCommand = new DockerCommand();
        unknownCommand.setName(unknownCommandName);
        unknownCommand.setImage(unknownImageName);

        doReturn(Lists.newArrayList(fakeDockerImage)).when(mockContainerControlApi).getAllImages();
        when(mockCommandService.getAll()).thenReturn(Lists.<Command>newArrayList(fakeCommand, unknownCommand));
        when(mockDockerServerPrefsBean.getName()).thenReturn(MOCK_CONTAINER_SERVER_NAME);

        final List<DockerImageAndCommandSummary> expected = Lists.newArrayList(
                DockerImageAndCommandSummary.create(fakeCommand, MOCK_CONTAINER_SERVER_NAME),
                DockerImageAndCommandSummary.create(unknownCommand, null)
        );

        final String path = "/docker/image-summaries";
        final Authentication authentication = new TestingAuthenticationToken("nonAdmin", "nonAdmin");
        final MockHttpServletRequestBuilder request = get(path)
                .with(authentication(authentication))
                .with(csrf()).with(testSecurityContext());

        final String responseStr =
                mockMvc.perform(request)
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        final List<DockerImageAndCommandSummary> responseList = mapper.readValue(responseStr, new TypeReference<List<DockerImageAndCommandSummary>>(){});
        assertThat(expected, everyItem(isIn(responseList)));
        assertThat(responseList, everyItem(isIn(expected)));
    }
}
