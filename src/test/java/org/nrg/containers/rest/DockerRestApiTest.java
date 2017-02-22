package org.nrg.containers.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nrg.containers.api.ContainerControlApi;
import org.nrg.containers.config.DockerRestApiTestConfig;
import org.nrg.containers.exceptions.CommandValidationException;
import org.nrg.containers.exceptions.DockerServerException;
import org.nrg.containers.exceptions.NoServerPrefException;
import org.nrg.containers.exceptions.NotFoundException;
import org.nrg.containers.model.CommandEntity;
import org.nrg.containers.model.CommandInput;
import org.nrg.containers.model.DockerCommandEntity;
import org.nrg.containers.model.auto.CommandPojo.CommandWrapperPojo;
import org.nrg.containers.model.auto.DockerImage;
import org.nrg.containers.model.DockerServer;
import org.nrg.containers.model.DockerServerPrefsBean;
import org.nrg.containers.model.XnatCommandWrapper;
import org.nrg.containers.model.auto.CommandPojo;
import org.nrg.containers.model.auto.DockerHub;
import org.nrg.containers.model.auto.DockerImageAndCommandSummary;
import org.nrg.containers.services.CommandService;
import org.nrg.containers.services.DockerHubService;
import org.nrg.prefs.exceptions.InvalidPreferenceName;
import org.nrg.xdat.security.services.RoleServiceI;
import org.nrg.xdat.security.services.UserManagementServiceI;
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

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.isIn;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.nrg.containers.helpers.CommandLabelHelper.LABEL_KEY;
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

    private final static String ADMIN_USERNAME = "admin";
    private final static String NON_ADMIN_USERNAME = "non-admin";
    private Authentication ADMIN_AUTH;
    private Authentication NONADMIN_AUTH;

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
    @Autowired private UserManagementServiceI mockUserManagementServiceI;
    @Autowired private DockerHubService mockDockerHubService;

    @Before
    public void setup() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).apply(springSecurity()).build();

        final String adminPassword = "admin";
        final UserI admin = mock(UserI.class);
        when(admin.getLogin()).thenReturn(ADMIN_USERNAME);
        when(admin.getPassword()).thenReturn(adminPassword);
        when(mockRoleService.isSiteAdmin(admin)).thenReturn(true);
        when(mockUserManagementServiceI.getUser(ADMIN_USERNAME)).thenReturn(admin);
        ADMIN_AUTH = new TestingAuthenticationToken(admin, adminPassword);

        final String nonAdminPassword = "non-admin-pass";
        final UserI nonAdmin = mock(UserI.class);
        when(nonAdmin.getLogin()).thenReturn(NON_ADMIN_USERNAME);
        when(nonAdmin.getPassword()).thenReturn(nonAdminPassword);
        when(mockRoleService.isSiteAdmin(nonAdmin)).thenReturn(false);
        when(mockUserManagementServiceI.getUser(NON_ADMIN_USERNAME)).thenReturn(nonAdmin);
        NONADMIN_AUTH = new TestingAuthenticationToken(NON_ADMIN_USERNAME, nonAdminPassword);
    }

    @Test
    public void testGetServer() throws Exception {

        final String path = "/docker/server";

        final MockHttpServletRequestBuilder request =
                get(path).accept(JSON)
                        .with(authentication(NONADMIN_AUTH))
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

        final String path = "/docker/server";

        final String containerServerJson =
                mapper.writeValueAsString(MOCK_CONTAINER_SERVER);

        final MockHttpServletRequestBuilder request =
                post(path)
                        .content(containerServerJson)
                        .contentType(JSON)
                        .with(authentication(ADMIN_AUTH))
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
        final String path = "/docker/server";

        final String containerServerJson =
                mapper.writeValueAsString(MOCK_CONTAINER_SERVER);

        final MockHttpServletRequestBuilder request =
                post(path)
                        .content(containerServerJson)
                        .contentType(JSON)
                        .with(authentication(NONADMIN_AUTH))
                        .with(csrf())
                        .with(testSecurityContext());

        final String exceptionResponse =
                mockMvc.perform(request)
                        .andExpect(status().isUnauthorized())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        assertThat(exceptionResponse, containsString(NON_ADMIN_USERNAME));
    }

    @Test
    public void testPingServer() throws Exception {
        final String path = "/docker/server/ping";

        final MockHttpServletRequestBuilder request =
                get(path).with(authentication(NONADMIN_AUTH))
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
    public void testGetHubs() throws Exception {
        final String path = "/docker/hubs";

        final MockHttpServletRequestBuilder request =
                get(path)
                        .with(authentication(NONADMIN_AUTH))
                        .with(csrf())
                        .with(testSecurityContext());

        final DockerHub dockerHub = DockerHub.DEFAULT;
        final DockerHub privateHub = DockerHub.create(10L, "my hub", "http://localhost", "me", "still me", "me@me.me");
        final List<DockerHub> hubs = Lists.newArrayList(dockerHub, privateHub);
        final String obscuredHubJson = mapper.writeValueAsString(hubs);

        when(mockDockerHubService.getHubs()).thenReturn(hubs);


        final String response =
                mockMvc.perform(request)
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        assertEquals(obscuredHubJson, response);
    }

    @Test
    public void testGetHubById() throws Exception {
        final String pathTemplate = "/docker/hubs/%d";

        final long privateHubId = 10L;
        final DockerHub privateHubExpected = DockerHub.create(privateHubId, "my hub", "http://localhost", "me", "still me", "me@me.me");
        final String privateHubObscuredJson = mapper.writeValueAsString(privateHubExpected);
        final DockerHub defaultHubExpected = DockerHub.DEFAULT;
        final String defaultHubObscuredJson = mapper.writeValueAsString(defaultHubExpected);
        final long defaultHubId = defaultHubExpected.id();

        when(mockDockerHubService.getHub(defaultHubId)).thenReturn(defaultHubExpected);
        when(mockDockerHubService.getHub(privateHubId)).thenReturn(privateHubExpected);

        // Get default hub
        final MockHttpServletRequestBuilder defaultHubRequest =
                get(String.format(pathTemplate, defaultHubId))
                        .with(authentication(NONADMIN_AUTH))
                        .with(csrf())
                        .with(testSecurityContext());

        final String defaultHubResponse =
                mockMvc.perform(defaultHubRequest)
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        assertEquals(defaultHubObscuredJson, defaultHubResponse);

        // Get private hub
        final MockHttpServletRequestBuilder privateHubRequest =
                get(String.format(pathTemplate, privateHubId))
                        .with(authentication(NONADMIN_AUTH))
                        .with(csrf())
                        .with(testSecurityContext());

        final String privateHubResponse =
                mockMvc.perform(privateHubRequest)
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        assertEquals(privateHubObscuredJson, privateHubResponse);
    }

    @Test
    public void testGetHubByName() throws Exception {
        final String pathTemplate = "/docker/hubs/%s";

        final String privateHubName = "my hub";
        final DockerHub privateHubExpected = DockerHub.create(10L, privateHubName, "http://localhost", "me", "still me", "me@me.me");
        final String privateHubObscuredJson = mapper.writeValueAsString(privateHubExpected);
        final DockerHub defaultHubExpected = DockerHub.DEFAULT;
        final String defaultHubObscuredJson = mapper.writeValueAsString(defaultHubExpected);
        final String defaultHubName = defaultHubExpected.name();

        when(mockDockerHubService.getHub(defaultHubName)).thenReturn(defaultHubExpected);
        when(mockDockerHubService.getHub(privateHubName)).thenReturn(privateHubExpected);

        // Get default hub
        final MockHttpServletRequestBuilder defaultHubRequest =
                get(String.format(pathTemplate, defaultHubName))
                        .with(authentication(NONADMIN_AUTH))
                        .with(csrf())
                        .with(testSecurityContext());

        final String defaultHubResponse =
                mockMvc.perform(defaultHubRequest)
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        assertEquals(defaultHubObscuredJson, defaultHubResponse);

        // Get private hub
        final MockHttpServletRequestBuilder privateHubRequest =
                get(String.format(pathTemplate, privateHubName))
                        .with(authentication(NONADMIN_AUTH))
                        .with(csrf())
                        .with(testSecurityContext());

        final String privateHubResponse =
                mockMvc.perform(privateHubRequest)
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        assertEquals(privateHubObscuredJson, privateHubResponse);
    }

    @Test
    public void testCreateHub() throws Exception {
        final String path = "/docker/hubs";

        // Because we obscure the username and password in the json representation in docker hubs,
        // we have to write the json directly, not create an object an serialized json from it.
        final String hubToCreateJson = "{" +
                "\"id\": 0" +
                ", \"name\": \"a hub name\"" +
                ", \"url\": \"http://localhost\"" +
                ", \"username\": \"me\"" +
                ", \"password\": \"Still me\"" +
                ", \"email\": \"me@me.me\"" +
                "}";
        final DockerHub hubToCreate = mapper.readValue(hubToCreateJson, DockerHub.class);

        final DockerHub created = DockerHub.create(10L, "a hub name", "http://localhost", "me", "still me", "me@me.me");
        final String createdObscuredJson = mapper.writeValueAsString(created);

        when(mockDockerHubService.create(hubToCreate)).thenReturn(created);

        final MockHttpServletRequestBuilder request =
                post(path)
                        .contentType(JSON)
                        .content(hubToCreateJson)
                        .with(authentication(ADMIN_AUTH))
                        .with(csrf())
                        .with(testSecurityContext());

        final String response =
                mockMvc.perform(request)
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        assertEquals(createdObscuredJson, response);

        final MockHttpServletRequestBuilder nonAdminRequest =
                post(path)
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(hubToCreate))
                        .with(authentication(NONADMIN_AUTH))
                        .with(csrf())
                        .with(testSecurityContext());
        mockMvc.perform(nonAdminRequest)
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testPingHub() throws Exception {
        final String pathTemplate = "/docker/hubs/%s/ping";

        final DockerHub defaultHub = DockerHub.DEFAULT;
        final long defaultHubId = defaultHub.id();
        final String defaultHubName = defaultHub.name();

        final String pathById = String.format(pathTemplate, String.valueOf(defaultHubId));
        final String pathByName = String.format(pathTemplate, defaultHubName);

        when(mockDockerHubService.getHub(defaultHubName)).thenReturn(defaultHub);
        when(mockDockerHubService.getHub(defaultHubId)).thenReturn(defaultHub);
        doReturn("OK").when(mockContainerControlApi).pingHub(defaultHub);

        mockMvc.perform(get(pathById)
                        .with(authentication(NONADMIN_AUTH))
                        .with(csrf())
                        .with(testSecurityContext()))
                .andExpect(status().isOk())
                .andExpect(content().string(equalTo("OK")));

        mockMvc.perform(get(pathByName)
                        .with(authentication(NONADMIN_AUTH))
                        .with(csrf())
                        .with(testSecurityContext()))
                .andExpect(status().isOk())
                .andExpect(content().string(equalTo("OK")));
    }

    @Test
    @Transactional
    public void testSaveFromLabels() throws Exception {
        final String path = "/docker/images/save";

        final String fakeImageId = "xnat/thisisfake";
        final String labelTestCommandJson =
                "{\"name\": \"label-test\"," +
                        "\"image\": \"" + fakeImageId + "\"," +
                        "\"description\": \"Command to test label-parsing and command-importing code\"," +
                        "\"type\": \"docker\", " +
                        "\"command-line\": \"#CMD#\"," +
                        "\"inputs\": [{\"name\": \"CMD\", \"description\": \"Command to run\", \"required\": true}]}";
        final CommandPojo expected = mapper.readValue(labelTestCommandJson, CommandPojo.class);
        final List<CommandEntity> toReturnList = Lists.newArrayList(CommandEntity.commandPojoToCommand(expected));

        final Map<String, String> imageLabels = Maps.newHashMap();
        imageLabels.put(LABEL_KEY, "[" + labelTestCommandJson + "]");

        final DockerImage dockerImage = DockerImage.create(fakeImageId, null, imageLabels);
        doReturn(dockerImage).when(mockContainerControlApi).getImageById(fakeImageId);
        when(mockCommandService.save(anyListOf(CommandPojo.class))).thenReturn(toReturnList);

        final MockHttpServletRequestBuilder request =
                post(path).param("image", fakeImageId)
                        .with(authentication(ADMIN_AUTH))
                        .with(csrf()).with(testSecurityContext());

        final String responseStr =
                mockMvc.perform(request)
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        final List<CommandEntity> responseList = mapper.readValue(responseStr, new TypeReference<List<CommandEntity>>(){});
        assertThat(responseList, hasSize(1));
        final CommandEntity response = responseList.get(0);

        // "response" will have been saved, so it will not be exactly equal to "expected"
        // Must compare attribute-by-attribute
        assertEquals(expected.name(), response.getName());
        assertEquals(expected.description(), response.getDescription());
        assertEquals(expected.commandLine(), response.getCommandLine());
        assertEquals(expected.image(), response.getImage()); // Did not set image ID on "expected"

        for (final CommandPojo.CommandInputPojo commandInputPojo : expected.inputs()) {
            assertThat(CommandInput.fromPojo(commandInputPojo), isIn(response.getInputs()));
        }
    }

    @Test
    @Transactional
    public void testSaveFromLabels2() throws Exception {
        final String path = "/docker/images/save";

        final String fakeImageId = "xnat/thisisfake";
        final String labelTestCommandListJson =
                "[{\"name\":\"dcm2niix-scan\", \"description\":\"Run dcm2niix on a scan's DICOMs\", " +
                        "\"image\": \"" + fakeImageId + "\"," +
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
        final List<CommandPojo> expectedList = mapper.readValue(labelTestCommandListJson, new TypeReference<List<CommandPojo>>(){});
        final CommandPojo expected = expectedList.get(0);

        final List<CommandEntity> toReturnList = Lists.transform(expectedList, new Function<CommandPojo, CommandEntity>() {
            @Nullable
            @Override
            public CommandEntity apply(@Nullable final CommandPojo commandPojo) {
                try {
                    return CommandEntity.commandPojoToCommand(commandPojo);
                } catch (CommandValidationException e) {
                    String message = "";
                    for (final String error : e.getErrors()) {
                        message += error + "\n";
                    }
                    message += e.getMessage();
                    fail(message);
                }
                return null;
            }
        });

        final Map<String, String> imageLabels = Maps.newHashMap();
        imageLabels.put(LABEL_KEY, labelTestCommandListJson);

        final DockerImage dockerImage = DockerImage.create(fakeImageId, null, imageLabels);
        doReturn(dockerImage).when(mockContainerControlApi).getImageById(fakeImageId);
        when(mockCommandService.save(anyListOf(CommandPojo.class))).thenReturn(toReturnList);

        final MockHttpServletRequestBuilder request =
                post(path).param("image", fakeImageId)
                        .with(authentication(ADMIN_AUTH))
                        .with(csrf()).with(testSecurityContext());

        final String responseStr =
                mockMvc.perform(request)
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        final List<CommandEntity> responseList = mapper.readValue(responseStr, new TypeReference<List<CommandEntity>>(){});
        assertThat(responseList, hasSize(1));
        final CommandEntity response = responseList.get(0);

        // "response" will have been saved, so it will not be exactly equal to "expected"
        // Must compare attribute-by-attribute
        assertEquals(expected.name(), response.getName());
        assertEquals(expected.description(), response.getDescription());
        assertEquals(expected.commandLine(), response.getCommandLine());
        assertEquals(expected.image(), response.getImage()); // Did not set image ID on "expected"
        for (final CommandPojo.CommandInputPojo commandInputPojo : expected.inputs()) {
            assertThat(CommandInput.fromPojo(commandInputPojo), isIn(response.getInputs()));
        }

    }

    @Test
    public void testGetImages() throws Exception {
        final String path = "/docker/images";

        final String fakeImageId = "sha256:some godawful hash";
        final String fakeImageName = "xnat/thisisfake";
        final DockerImage fakeDockerImage = DockerImage.create(fakeImageId, null, null);
        fakeDockerImage.addTag(fakeImageName);

        doReturn(Lists.newArrayList(fakeDockerImage)).when(mockContainerControlApi).getAllImages();

        final MockHttpServletRequestBuilder request = get(path)
                .with(authentication(NONADMIN_AUTH))
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
    public void testImageSummariesJsonRoundTrip() throws Exception {
        final String fakeImageId = "sha256:some godawful hash";
        final String fakeImageName = "xnat/thisisfake";
        final DockerImage fakeDockerImage = DockerImage.create(fakeImageId, null, null);
        fakeDockerImage.addTag(fakeImageName);

        final String fakeCommandName = "fake";
        final String fakeCommandWrapperName = "fake-on-thing";
        final XnatCommandWrapper fakeWrapper = new XnatCommandWrapper();
        fakeWrapper.setName(fakeCommandWrapperName);
        final DockerCommandEntity fakeCommand = new DockerCommandEntity();
        fakeCommand.setHash(fakeImageId);
        fakeCommand.setName(fakeCommandName);
        fakeCommand.setImage(fakeImageName);
        fakeCommand.addXnatCommandWrapper(fakeWrapper);

        final String unknownImageName = "unknown";
        final String unknownCommandName = "image-unknown";
        final DockerCommandEntity unknownCommand = new DockerCommandEntity();
        unknownCommand.setName(unknownCommandName);
        unknownCommand.setImage(unknownImageName);

        final DockerImageAndCommandSummary fakeSummary = DockerImageAndCommandSummary.create(fakeImageId, MOCK_CONTAINER_SERVER_NAME, fakeCommand);
        final String fakeSummaryJson = mapper.writeValueAsString(fakeSummary);
        final DockerImageAndCommandSummary deserialized = mapper.readValue(fakeSummaryJson, DockerImageAndCommandSummary.class);
        assertEquals(fakeSummary, deserialized);

        final List<DockerImageAndCommandSummary> expected = Lists.newArrayList(
                DockerImageAndCommandSummary.create(fakeImageId, MOCK_CONTAINER_SERVER_NAME, fakeCommand),
                DockerImageAndCommandSummary.create(unknownCommand)
        );

        final List<DockerImageAndCommandSummary> actual = mapper.readValue(mapper.writeValueAsString(expected), new TypeReference<List<DockerImageAndCommandSummary>>(){});
        assertThat(expected, everyItem(isIn(actual)));
        assertThat(actual, everyItem(isIn(expected)));
    }

    @Test
    public void testGetImageSummaries() throws Exception {
        final String path = "/docker/image-summaries";

        // Image exists on server, command refers to image
        final String imageWithSavedCommand_id = "sha256:some godawful hash";
        final String imageWithSavedCommand_name = "xnat/thisisfake";
        final DockerImage imageWithSavedCommand = DockerImage.create(imageWithSavedCommand_id, Lists.newArrayList(imageWithSavedCommand_name), null);

        final String commandWithImage_name = "fake";
        final String commandWithImage_wrapperName = "fake-on-thing";
        final CommandWrapperPojo wrapper = CommandWrapperPojo.builder().name(commandWithImage_wrapperName).build();
        final CommandPojo commandWithImage = CommandPojo.builder()
                .name(commandWithImage_name)
                .image(imageWithSavedCommand_name)
                .xnatCommandWrappers(Lists.newArrayList(wrapper))
                .build();

        final CommandEntity commandEntityWithImage_entity = CommandEntity.commandPojoToCommand(commandWithImage);

        // Command refers to image that does not exist on server
        final String commandWithUnknownImage_imageName = "unknown";
        final String commandWithUnknownImage_name = "image-unknown";
        final CommandPojo unknownCommand = CommandPojo.builder()
                .name(commandWithUnknownImage_name)
                .image(commandWithUnknownImage_imageName)
                .build();
        final CommandEntity commandEntityWithUnknownImage_entity = CommandEntity.commandPojoToCommand(unknownCommand);

        // Image has command labels, no commands on server
        final String imageWithNonDbCommandLabels_id = "who:cares:not:me";
        final String imageWithNonDbCommandLabels_name = "xnat/thisisanotherfake:3.4.5.6";
        final String imageWithNonDbCommandLabels_commandName = "hi there";
        final CommandPojo toSaveInImageLabels = CommandPojo.builder().name(imageWithNonDbCommandLabels_commandName).build();
        final String imageWithNonDbCommandLabels_labelValue = mapper.writeValueAsString(Lists.newArrayList(toSaveInImageLabels));
        final DockerImage imageWithNonDbCommandLabels = DockerImage.create(
                imageWithNonDbCommandLabels_id,
                Lists.newArrayList(imageWithNonDbCommandLabels_name),
                ImmutableMap.of(LABEL_KEY, imageWithNonDbCommandLabels_labelValue));
 

        // Mock out responses
        doReturn(Lists.newArrayList(imageWithSavedCommand, imageWithNonDbCommandLabels)).when(mockContainerControlApi).getAllImages();
        doReturn(null).when(mockContainerControlApi).getImageById(commandWithUnknownImage_imageName);
        when(mockCommandService.getAll()).thenReturn(Lists.newArrayList(commandEntityWithImage_entity, commandEntityWithUnknownImage_entity));
        when(mockDockerServerPrefsBean.getName()).thenReturn(MOCK_CONTAINER_SERVER_NAME);

        final List<DockerImageAndCommandSummary> expected = Lists.newArrayList(
                DockerImageAndCommandSummary.create(imageWithSavedCommand_id, MOCK_CONTAINER_SERVER_NAME, commandWithImage),
                DockerImageAndCommandSummary.create(unknownCommand),
                DockerImageAndCommandSummary.create(imageWithNonDbCommandLabels, MOCK_CONTAINER_SERVER_NAME)
        );

        final MockHttpServletRequestBuilder request = get(path)
                .with(authentication(NONADMIN_AUTH))
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

    @Test
    public void testListHash() throws Exception {
        // This is to test a question I have.
        // If I put a list into a map, then add an item to the list, does the list's hash change?
        // This would be bad, because the map would "lose" the list

        final Map<List<String>, String> testMap = Maps.newHashMap();
        final List<String> testList = Lists.newArrayList();
        final String mapValue = "foo";
        final String listItem = "bar";

        testMap.put(testList, mapValue);
        assertThat(testMap, hasEntry(testList, mapValue));

        testList.add(listItem);
        assertThat(testMap, hasEntry(testList, mapValue));
    }
}
