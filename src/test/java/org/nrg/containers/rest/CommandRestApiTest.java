package org.nrg.containers.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.nrg.containers.api.ContainerControlApi;
import org.nrg.containers.config.CommandRestApiTestConfig;
import org.nrg.containers.model.CommandEntity;
import org.nrg.containers.model.CommandWrapperEntity;
import org.nrg.containers.model.ContainerExecution;
import org.nrg.containers.model.DockerServer;
import org.nrg.containers.model.DockerServerPrefsBean;
import org.nrg.containers.model.ResolvedCommand;
import org.nrg.containers.model.ResolvedDockerCommand;
import org.nrg.containers.model.auto.Command;
import org.nrg.containers.model.auto.Command.CommandWrapper;
import org.nrg.containers.services.CommandEntityService;
import org.nrg.containers.services.CommandService;
import org.nrg.containers.services.ContainerExecutionService;
import org.nrg.xdat.entities.AliasToken;
import org.nrg.xdat.preferences.SiteConfigPreferences;
import org.nrg.xdat.security.services.RoleServiceI;
import org.nrg.xdat.security.services.UserManagementServiceI;
import org.nrg.xdat.services.AliasTokenService;
import org.nrg.xft.security.UserI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.io.File;
import java.util.List;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.testSecurityContext;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@Transactional
@ContextConfiguration(classes = CommandRestApiTestConfig.class)
public class CommandRestApiTest {
    private UserI mockAdmin;
    private Authentication authentication;
    private MockMvc mockMvc;

    private final String FAKE_URL = "mock://url";
    private final String FAKE_USERNAME = "fakeuser";
    private final String FAKE_PASSWORD = "fakepass";
    private final String FAKE_ALIAS = "fakealias";
    private final String FAKE_SECRET = "fakesecret";
    private final String FAKE_DOCKER_IMAGE = "abc123";
    private final MediaType JSON = MediaType.APPLICATION_JSON_UTF8;
    private final MediaType XML = MediaType.APPLICATION_XML;

    @Autowired private WebApplicationContext wac;
    @Autowired private ObjectMapper mapper;
    @Autowired private CommandService commandService;
    @Autowired private RoleServiceI mockRoleService;
    @Autowired private ContainerControlApi mockDockerControlApi;
    @Autowired private ContainerExecutionService mockContainerExecutionService;
    @Autowired private AliasTokenService mockAliasTokenService;
    @Autowired private DockerServerPrefsBean mockDockerServerPrefsBean;
    @Autowired private SiteConfigPreferences mockSiteConfigPreferences;
    @Autowired private UserManagementServiceI mockUserManagementServiceI;

    @Rule public TemporaryFolder folder = new TemporaryFolder(new File("/tmp"));

    @Before
    public void setup() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).apply(springSecurity()).build();

        Configuration.setDefaults(new Configuration.Defaults() {

            private final JsonProvider jsonProvider = new JacksonJsonProvider();
            private final MappingProvider mappingProvider = new JacksonMappingProvider();

            @Override
            public JsonProvider jsonProvider() {
                return jsonProvider;
            }

            @Override
            public MappingProvider mappingProvider() {
                return mappingProvider;
            }

            @Override
            public Set<Option> options() {
                return Sets.newHashSet(Option.DEFAULT_PATH_LEAF_TO_NULL);
            }
        });

        // Mock out the prefs bean
        final String containerServerName = "testy test";
        final String containerHost = "unix:///var/run/docker.sock";
        final DockerServer dockerServer = new DockerServer(containerServerName, containerHost, null);
        when(mockDockerServerPrefsBean.getName()).thenReturn(containerServerName);
        when(mockDockerServerPrefsBean.getHost()).thenReturn(containerHost);
        when(mockDockerServerPrefsBean.toDto()).thenReturn(dockerServer);
        when(mockDockerControlApi.getServer()).thenReturn(dockerServer);

        // Mock the userI
        mockAdmin = Mockito.mock(UserI.class);
        when(mockAdmin.getLogin()).thenReturn(FAKE_USERNAME);
        when(mockAdmin.getPassword()).thenReturn(FAKE_PASSWORD);
        when(mockRoleService.isSiteAdmin(mockAdmin)).thenReturn(true);

        authentication = new TestingAuthenticationToken(mockAdmin, FAKE_PASSWORD);

        // Mock the user management service
        when(mockUserManagementServiceI.getUser(FAKE_USERNAME)).thenReturn(mockAdmin);

        // Mock the aliasTokenService
        final AliasToken mockAliasToken = new AliasToken();
        mockAliasToken.setAlias(FAKE_ALIAS);
        mockAliasToken.setSecret(FAKE_SECRET);
        when(mockAliasTokenService.issueTokenForUser(mockAdmin)).thenReturn(mockAliasToken);

        // Mock the site config preferences
        when(mockSiteConfigPreferences.getSiteUrl()).thenReturn(FAKE_URL);
        when(mockSiteConfigPreferences.getProperty("processingUrl", FAKE_URL)).thenReturn(FAKE_URL);
        when(mockSiteConfigPreferences.getBuildPath()).thenReturn(folder.newFolder().getAbsolutePath()); // transporter makes a directory under build
        when(mockSiteConfigPreferences.getArchivePath()).thenReturn(folder.newFolder().getAbsolutePath()); // container logs get stored under archive
    }

    @Test
    @DirtiesContext
    public void testGetAll() throws Exception {
        final String path = "/commands";

        final String commandJson =
                "{\"name\": \"one\", \"type\": \"docker\", \"image\":\"" + FAKE_DOCKER_IMAGE + "\"}";
        final Command created = commandService.create(mapper.readValue(commandJson, Command.class));

        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        final MockHttpServletRequestBuilder request = get(path)
                .with(authentication(authentication))
                .with(csrf())
                .with(testSecurityContext());

        final String response =
                mockMvc.perform(request)
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(JSON))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        final List<Command> commands = mapper.readValue(response, new TypeReference<List<Command>>() {});
        assertThat(commands, hasSize(1));
        final Command command = commands.get(0);
        assertNotEquals(0L, command.id());
        assertEquals(created.id(), command.id());
        assertEquals("one", command.name());
        assertEquals(FAKE_DOCKER_IMAGE, command.image());
    }

    @Test
    @DirtiesContext
    public void testGet() throws Exception {
        final String pathTemplate = "/commands/%d";

        final String commandJson =
                "{\"name\": \"one\", \"type\": \"docker\", \"image\":\"" + FAKE_DOCKER_IMAGE + "\"}";
        final Command created = commandService.create(mapper.readValue(commandJson, Command.class));
        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        final String path = String.format(pathTemplate, created.id());

        final MockHttpServletRequestBuilder request = get(path)
                .with(authentication(authentication))
                .with(csrf())
                .with(testSecurityContext());

        final String response =
                mockMvc.perform(request)
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(JSON))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        final Command command = mapper.readValue(response, Command.class);
        assertNotEquals(0L, command.id());
        assertEquals(created, command);
    }

    @Test
    @DirtiesContext
    public void testCreate() throws Exception {
        final String path = "/commands";

        final String commandJson =
                "{\"name\": \"toCreate\", \"type\": \"docker\", \"image\":\"" + FAKE_DOCKER_IMAGE + "\"}";

        final MockHttpServletRequestBuilder request =
                post(path).content(commandJson).contentType(JSON)
                        .with(authentication(authentication))
                        .with(csrf())
                        .with(testSecurityContext());

        final String response =
                mockMvc.perform(request)
                        .andExpect(status().isCreated())
                        .andExpect(content().contentType(JSON))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        final Long idResponse = Long.parseLong(response);
        assertNotEquals(Long.valueOf(0L), idResponse);

        final Command retrieved = commandService.retrieve(idResponse);
        assertNotEquals(0L, retrieved.id());
        assertEquals((Long) retrieved.id(), idResponse);
        assertEquals("toCreate", retrieved.name());
        assertEquals(FAKE_DOCKER_IMAGE, retrieved.image());

        // Errors
        // No 'Content-type' header
        final MockHttpServletRequestBuilder noContentType =
                post(path).content(commandJson)
                        .with(authentication(authentication))
                        .with(csrf())
                        .with(testSecurityContext());
        mockMvc.perform(noContentType)
                .andExpect(status().isUnsupportedMediaType());

        // Bad 'Accepts' header
        final MockHttpServletRequestBuilder badAccept =
                post(path).content(commandJson)
                        .contentType(JSON)
                        .accept(XML)
                        .with(authentication(authentication))
                        .with(csrf())
                        .with(testSecurityContext());
        mockMvc.perform(badAccept)
                .andExpect(status().isNotAcceptable());

        // Blank command
        final String blankCommand = "{\"type\": \"docker\"}";
        final MockHttpServletRequestBuilder blankCommandRequest =
                post(path).content(blankCommand).contentType(JSON)
                        .with(authentication(authentication))
                        .with(csrf())
                        .with(testSecurityContext());
        final String blankCommandResponse =
                mockMvc.perform(blankCommandRequest)
                        .andExpect(status().isBadRequest())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        assertEquals("Invalid command:\n\tCommand name cannot be blank.\n\tCommand \"\" - image name cannot be blank.", blankCommandResponse);
    }

    @Test
    @DirtiesContext
    public void testDelete() throws Exception {
        final String pathTemplate = "/commands/%d";

        final String commandJson =
                "{\"name\": \"toDelete\", \"type\": \"docker\", \"image\":\"" + FAKE_DOCKER_IMAGE + "\"}";
        final Command command = commandService.create(mapper.readValue(commandJson, Command.class));
        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        final String path = String.format(pathTemplate, command.id());

        final MockHttpServletRequestBuilder request = delete(path)
                .with(authentication(authentication))
                .with(csrf())
                .with(testSecurityContext());

        mockMvc.perform(request)
                .andExpect(status().isNoContent());

        assertNull(commandService.retrieve(command.id()));
    }

    @Test
    @DirtiesContext
    public void testAddWrapper() throws Exception {
        final String pathTemplate = "/commands/%d/wrappers";

        final String commandWrapperJson = "{\"name\": \"empty wrapper\"}";

        final String commandJson =
                "{\"name\": \"toCreate\", \"type\": \"docker\", \"image\":\"" + FAKE_DOCKER_IMAGE + "\"}";
        final Command command = commandService.create(mapper.readValue(commandJson, Command.class));
        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        final String path = String.format(pathTemplate, command.id());

        final MockHttpServletRequestBuilder request =
                post(path).content(commandWrapperJson).contentType(JSON)
                        .with(authentication(authentication))
                        .with(csrf())
                        .with(testSecurityContext());

        final String response =
                mockMvc.perform(request)
                        .andExpect(status().isCreated())
                        .andExpect(content().contentType(JSON))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        final Long idResponse = Long.parseLong(response);
        assertNotEquals(Long.valueOf(0L), idResponse);

        CommandWrapper retrieved = null;
        final Command retrievedCommand = commandService.retrieve(command.id());
        for (final CommandWrapper wrapper : retrievedCommand.xnatCommandWrappers()) {
            if (wrapper.id() == idResponse) {
                retrieved = wrapper;
                break;
            }
        }
        assertNotNull(retrieved);
        assertNotEquals(0L, retrieved.id());
        assertEquals((Long) retrieved.id(), idResponse);
        assertEquals("empty wrapper", retrieved.name());

        // Errors

        // Blank command
        final String blankWrapper = "{}";
        final MockHttpServletRequestBuilder blankCommandRequest =
                post(path).content(blankWrapper).contentType(JSON)
                        .with(authentication(authentication))
                        .with(csrf())
                        .with(testSecurityContext());
        final String blankCommandResponse =
                mockMvc.perform(blankCommandRequest)
                        .andExpect(status().isBadRequest())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        assertEquals("Invalid command:\n\tName cannot be blank.", blankCommandResponse);
    }

    @Test
    @DirtiesContext
    public void testUpdateWrapper() throws Exception {
        final String pathTemplate = "/commands/%d/wrappers/%d";

        final String commandJson =
                "{\"name\": \"toCreate\", \"type\": \"docker\", \"image\":\"" + FAKE_DOCKER_IMAGE + "\"," +
                        "\"xnat\":[{\"name\": \"a name\"," +
                        "\"description\": \"ORIGINAL\"}]}";
        final Command command = commandService.create(mapper.readValue(commandJson, Command.class));
        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        final CommandWrapper created = command.xnatCommandWrappers().get(0);
        final long commandId = command.id();
        final long wrapperId = created.id();
        final String path = String.format(pathTemplate, commandId, wrapperId);

        final String newDescription = "UPDATED";
        final CommandWrapper updates = created.toBuilder().description(newDescription).build();

        final MockHttpServletRequestBuilder request =
                post(path).content(mapper.writeValueAsString(updates))
                        .contentType(JSON)
                        .with(authentication(authentication))
                        .with(csrf())
                        .with(testSecurityContext());

        mockMvc.perform(request)
                .andExpect(status().isOk());
        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        assertEquals(updates, commandService.retrieve(commandId, wrapperId));
    }

    @Test
    @DirtiesContext
    public void testDeleteWrapper() throws Exception {
        final String pathTemplate = "/commands/%d/wrappers/%d";

        final String commandJson =
                "{\"name\": \"toCreate\", \"type\": \"docker\", \"image\":\"" + FAKE_DOCKER_IMAGE + "\"," +
                        "\"xnat\":[{\"name\": \"a name\"}]}";
        final Command command = commandService.create(mapper.readValue(commandJson, Command.class));
        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        final CommandWrapper created = command.xnatCommandWrappers().get(0);
        final long commandId = command.id();
        final long wrapperId = created.id();
        final String path = String.format(pathTemplate, commandId, wrapperId);

        final MockHttpServletRequestBuilder request =
                delete(path)
                        .with(authentication(authentication))
                        .with(csrf())
                        .with(testSecurityContext());

        mockMvc.perform(request)
                .andExpect(status().isNoContent());
        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        assertNull(commandService.retrieve(commandId, wrapperId));
    }

    @Test
    @DirtiesContext
    public void testLaunchWithQueryParams() throws Exception {
        final String pathTemplate = "/commands/%d/launch";

        final String fakeContainerId = "098zyx";
        final String inputName = "stringInput";
        final String inputValue = "the super cool value";
        final String inputJson = "{\"" + inputName + "\": \"" + inputValue + "\"}";
        final String commandJson =
                "{\"name\": \"toLaunch\"," +
                        "\"type\": \"docker\", " +
                        "\"image\": \"" + FAKE_DOCKER_IMAGE + "\"," +
                        "\"inputs\": [{\"name\": \"" + inputName + "\"}]}";
        final Command command = commandService.create(mapper.readValue(commandJson, Command.class));
        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        final long id = command.id();

        // This ResolvedCommand will be used in an internal method to "launch" a container
        final String environmentVariablesJson = "{" +
                "\"XNAT_HOST\": \"" + FAKE_URL + "\"," +
                "\"XNAT_USER\": \"" + FAKE_ALIAS + "\"," +
                "\"XNAT_PASS\": \"" + FAKE_SECRET + "\"" +
                "}";
        final String preparedResolvedCommandJson =
                "{\"command-id\": " + String.valueOf(id) +"," +
                        "\"image\": \"" + FAKE_DOCKER_IMAGE + "\"," +
                        "\"env\": " + environmentVariablesJson + "," +
                        "\"raw-input-values\": " + inputJson + "," +
                        "\"mounts\": []," +
                        "\"outputs\": []," +
                        "\"ports\": {}" +
                        "}";
        final ResolvedDockerCommand preparedResolvedCommand = mapper.readValue(preparedResolvedCommandJson, ResolvedDockerCommand.class);
        final ContainerExecution containerExecution = new ContainerExecution(preparedResolvedCommand, fakeContainerId, FAKE_USERNAME);

        // We have to match any resolved command because spring will add a csrf token to the inputs. I don't know how to get that token in advance.
        when(mockDockerControlApi.launchImage(any(ResolvedDockerCommand.class))).thenReturn(fakeContainerId);
        when(mockContainerExecutionService.save(any(ResolvedCommand.class), eq(fakeContainerId), eq(mockAdmin)))
                .thenReturn(containerExecution);

        final String path = String.format(pathTemplate, id);
        final MockHttpServletRequestBuilder request =
                post(path).param(inputName, inputValue)
                        .with(authentication(authentication))
                        .with(csrf())
                        .with(testSecurityContext());

        final String response = mockMvc.perform(request)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertEquals(fakeContainerId, response);
    }

    @Test
    @DirtiesContext
    public void testLaunchWithParamsInBody() throws Exception {
        final String pathTemplate = "/commands/%d/launch";

        final String fakeContainerId = "098zyx";
        final String inputName = "stringInput";
        final String inputValue = "the super cool value";
        final String inputJson = "{\"" + inputName + "\": \"" + inputValue + "\"}";
        final String commandInput = "{\"name\": \"" + inputName + "\"}";
        final String commandJson = "{" +
                "\"name\": \"toLaunch\"," +
                "\"type\": \"docker\", " +
                "\"image\": \"" + FAKE_DOCKER_IMAGE + "\"," +
                "\"inputs\": [" + commandInput + "]" +
                "}";
        final Command command = commandService.create(mapper.readValue(commandJson, Command.class));
        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        final long id = command.id();

        // This ResolvedCommand will be used in an internal method to "launch" a container
        final String environmentVariablesJson = "{" +
                "\"XNAT_HOST\": \"" + FAKE_URL + "\"," +
                "\"XNAT_USER\": \"" + FAKE_ALIAS + "\"," +
                "\"XNAT_PASS\": \"" + FAKE_SECRET + "\"" +
                "}";
        final String preparedResolvedCommandJson = "{" +
                "\"command-id\": " + String.valueOf(id) +"," +
                "\"image\": \"" + FAKE_DOCKER_IMAGE + "\"," +
                "\"env\": " + environmentVariablesJson + "," +
                "\"raw-input-values\": " + inputJson + "," +
                "\"mounts\": []," +
                "\"outputs\": []," +
                "\"ports\": {}" +
                "}";
        final ResolvedDockerCommand preparedResolvedCommand = mapper.readValue(preparedResolvedCommandJson, ResolvedDockerCommand.class);
        final ContainerExecution containerExecution = new ContainerExecution(preparedResolvedCommand, fakeContainerId, FAKE_USERNAME);

        // We have to match any resolved command because spring will add a csrf token to the inputs. I don't know how to get that token in advance.
        when(mockDockerControlApi.launchImage(any(ResolvedDockerCommand.class))).thenReturn(fakeContainerId);
        when(mockContainerExecutionService.save(any(ResolvedCommand.class), eq(fakeContainerId), eq(mockAdmin)))
                .thenReturn(containerExecution);

        final String path = String.format(pathTemplate, id);
        final MockHttpServletRequestBuilder request =
                post(path).content(inputJson).contentType(JSON)
                        .with(authentication(authentication))
                        .with(csrf())
                        .with(testSecurityContext());

        final String response = mockMvc.perform(request)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertEquals(fakeContainerId, response);
    }


    @Test
    public void testCreateEcatHeaderDump() throws Exception {
        // A User was attempting to create the command in this resource.
        // Spring didn't tell us why. See CS-70.

        final String path = "/commands";

        final String dir = Resources.getResource("ecatHeaderDump").getPath().replace("%20", " ");
        final String commandJsonFile = dir + "/command.json";
        final Command ecatHeaderDump = mapper.readValue(new File(commandJsonFile), Command.class);
        final String commandJson = mapper.writeValueAsString(ecatHeaderDump);

        final MockHttpServletRequestBuilder request =
                post(path).content(commandJson).contentType(JSON)
                        .with(authentication(authentication))
                        .with(csrf())
                        .with(testSecurityContext());

        final String response =
                mockMvc.perform(request)
                        .andExpect(status().isCreated())
                        .andExpect(content().contentType(JSON))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        assertNotEquals(response, "0");
    }

    @Test
    public void testSpringUnhelpfulError() throws Exception {
        // Deliberately trigger Spring's 400 'syntactically incorrect' error

        final String path = "/commands";

        final String badInputType = "fig newton";
        final String badInputTypeJson = "{" +
                "\"name\": \"a command name\", " +
                "\"type\": \"docker\", " +
                "\"image\": \"an image\", " +
                "\"inputs\": [" +
                    "{\"name\": \"an input name\", \"type\": \"" + badInputType + "\"}" +
                "}";

        final MockHttpServletRequestBuilder badInputTypeCommandRequest =
                post(path).content(badInputTypeJson).contentType(JSON)
                        .with(authentication(authentication))
                        .with(csrf())
                        .with(testSecurityContext());

        final String badInputTypeCommandResponse =
                mockMvc.perform(badInputTypeCommandRequest)
                        .andExpect(status().isBadRequest())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        assertEquals("", badInputTypeCommandResponse);
    }
}
