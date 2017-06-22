package org.nrg.containers.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.nrg.containers.api.ContainerControlApi;
import org.nrg.containers.config.CommandRestApiTestConfig;
import org.nrg.containers.model.command.auto.Command;
import org.nrg.containers.model.command.auto.Command.CommandWrapper;
import org.nrg.containers.model.server.docker.DockerServer;
import org.nrg.containers.model.server.docker.DockerServerPrefsBean;
import org.nrg.containers.services.CommandService;
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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
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
    @Autowired private AliasTokenService mockAliasTokenService;
    @Autowired private DockerServerPrefsBean mockDockerServerPrefsBean;
    @Autowired private SiteConfigPreferences mockSiteConfigPreferences;
    @Autowired private UserManagementServiceI mockUserManagementServiceI;

    @Rule public TemporaryFolder folder = new TemporaryFolder(new File("/tmp"));

    @Before
    public void setup() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).apply(springSecurity()).build();

        // Mock out the prefs bean
        final String containerServerName = "testy test";
        final String containerHost = "unix:///var/run/docker.sock";
        final DockerServer dockerServer = DockerServer.create(containerServerName, containerHost, null);
        when(mockDockerServerPrefsBean.getName()).thenReturn(containerServerName);
        when(mockDockerServerPrefsBean.getHost()).thenReturn(containerHost);
        when(mockDockerServerPrefsBean.toPojo()).thenReturn(dockerServer);
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
        assertThat(command.id(), is(not(0L)));
        assertThat(command.id(), is(created.id()));
        assertThat(command.name(), is("one"));
        assertThat(command.image(), is(FAKE_DOCKER_IMAGE));
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
        assertThat(command.id(), is(not(0L)));
        assertThat(command, is(created));
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
        assertThat(idResponse, is(not(0L)));

        final Command retrieved = commandService.retrieve(idResponse);
        assertThat(retrieved.id(), is(not(0L)));
        assertThat(idResponse, is(retrieved.id()));
        assertThat(retrieved.name(), is("toCreate"));
        assertThat(retrieved.image(), is(FAKE_DOCKER_IMAGE));

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
        assertThat(blankCommandResponse,
                is("Invalid command:\n\tCommand name cannot be blank.\n\tCommand \"null\" - image name cannot be blank."));
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

        assertThat(commandService.retrieve(command.id()), is(nullValue()));
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
        assertThat(idResponse, is(not(0L)));

        CommandWrapper retrieved = null;
        final Command retrievedCommand = commandService.retrieve(command.id());
        for (final CommandWrapper wrapper : retrievedCommand.xnatCommandWrappers()) {
            if (wrapper.id() == idResponse) {
                retrieved = wrapper;
                break;
            }
        }
        assertThat(retrieved, is(not(nullValue())));
        assertThat(retrieved.id(), is(not(0L)));
        assertThat(idResponse, is(retrieved.id()));
        assertThat(retrieved.name(), is("empty wrapper"));

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
        assertThat(blankCommandResponse,
                is("Invalid command:\n\tCommand \"toCreate\" - Command wrapper name cannot be blank."));
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

        assertThat(commandService.retrieveWrapper(wrapperId), is(updates));
    }

    @Test
    @DirtiesContext
    public void testDeleteWrapper() throws Exception {
        final String pathTemplate = "/wrappers/%d";

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

        assertThat(commandService.retrieveWrapper(wrapperId), is(nullValue()));
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

        assertThat(response, is(not("0")));
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

        assertThat(badInputTypeCommandResponse, is(""));
    }
}
