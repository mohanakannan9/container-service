package org.nrg.containers.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.nrg.config.entities.Configuration;
import org.nrg.config.services.ConfigService;
import org.nrg.containers.config.CommandConfigurationRestApiTestConfig;
import org.nrg.containers.model.CommandConfiguration;
import org.nrg.containers.model.CommandConfiguration.CommandInputConfiguration;
import org.nrg.containers.model.CommandConfiguration.CommandOutputConfiguration;
import org.nrg.containers.model.CommandConfigurationInternalRepresentation;
import org.nrg.containers.services.ContainerConfigService;
import org.nrg.containers.services.impl.ContainerConfigServiceImpl;
import org.nrg.framework.constants.Scope;
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
import org.springframework.web.context.WebApplicationContext;

import java.io.File;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
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
@ContextConfiguration(classes = CommandConfigurationRestApiTestConfig.class)
public class CommandConfigurationRestApiTest {
    private UserI mockAdmin;
    private Authentication authentication;
    private MockMvc mockMvc;
    private CommandConfiguration commandConfiguration;
    private String commandConfigurationJson;
    private CommandConfigurationInternalRepresentation commandConfigurationInternalRepresentation;
    private String commandConfigurationInternalRepresentationJson;
    private Configuration mockConfig;
    private String configPath;
    private String siteConfigRestPath;
    private String projectConfigRestPath;

    private final String project = "some-project";
    private final String FAKE_USERNAME = "fakeuser";
    private final String FAKE_PASSWORD = "fakepass";
    private final MediaType JSON = MediaType.APPLICATION_JSON_UTF8;
    private final MediaType XML = MediaType.APPLICATION_XML;

    @Autowired private WebApplicationContext wac;
    @Autowired private ObjectMapper mapper;
    @Autowired private ConfigService mockConfigService;
    @Autowired private RoleServiceI mockRoleService;
    @Autowired private UserManagementServiceI mockUserManagementServiceI;

    @Rule public TemporaryFolder folder = new TemporaryFolder(new File("/tmp"));

    @Before
    public void setup() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).apply(springSecurity()).build();

        // Mock the userI
        mockAdmin = Mockito.mock(UserI.class);
        when(mockAdmin.getLogin()).thenReturn(FAKE_USERNAME);
        when(mockAdmin.getPassword()).thenReturn(FAKE_PASSWORD);
        when(mockRoleService.isSiteAdmin(mockAdmin)).thenReturn(true);

        authentication = new TestingAuthenticationToken(mockAdmin, FAKE_PASSWORD);

        // Mock the user management service
        when(mockUserManagementServiceI.getUser(FAKE_USERNAME)).thenReturn(mockAdmin);

        // Create a command configuration
        final Map<String, CommandInputConfiguration> inputs =
                ImmutableMap.of(
                        "input", CommandInputConfiguration.builder()
                                .defaultValue("whatever")
                                .matcher("anything")
                                .userSettable(true)
                                .advanced(false)
                                .build()
                );
        final Map<String, CommandOutputConfiguration> outputs =
                ImmutableMap.of(
                        "output", CommandOutputConfiguration.create(null)
                );
        commandConfiguration = CommandConfiguration.create(inputs, outputs);
        commandConfigurationJson = mapper.writeValueAsString(commandConfiguration);

        commandConfigurationInternalRepresentation = CommandConfigurationInternalRepresentation.create(true, commandConfiguration);
        commandConfigurationInternalRepresentationJson = mapper.writeValueAsString(commandConfigurationInternalRepresentation);

        // Command and wrapper
        final long commandId = 0L;
        final String wrapperName = "aWrapper";

        // mock out a org.nrg.config.Configuration
        mockConfig = mock(Configuration.class);
        when(mockConfig.getContents()).thenReturn(commandConfigurationInternalRepresentationJson);

        configPath = String.format(ContainerConfigServiceImpl.COMMAND_CONFIG_PATH_TEMPLATE, commandId, wrapperName);

        // REST paths
        final String siteConfigRestPathTemplate = "/commands/%d/wrappers/%s/config";
        siteConfigRestPath = String.format(siteConfigRestPathTemplate, commandId, wrapperName);

        final String projectConfigRestPathTemplate = "/projects/%s/commands/%d/wrappers/%s/config";
        projectConfigRestPath = String.format(projectConfigRestPathTemplate, project, commandId, wrapperName);
    }

    @Test
    public void testCreateSiteConfig() throws Exception {
        when(mockConfigService
                .replaceConfig(
                        eq(FAKE_USERNAME),
                        anyString(),
                        eq(ContainerConfigService.TOOL_ID),
                        eq(configPath),
                        eq(commandConfigurationInternalRepresentationJson),
                        eq(Scope.Site),
                        isNull(String.class)))
                .thenReturn(null);

        final MockHttpServletRequestBuilder request =
                post(siteConfigRestPath)
                        .content(commandConfigurationJson)
                        .contentType(JSON)
                        .with(authentication(authentication))
                        .with(csrf())
                        .with(testSecurityContext());

        mockMvc.perform(request)
                .andExpect(status().isCreated());
    }

    @Test
    public void testGetSiteConfig() throws Exception {
        when(mockConfigService
                .getConfig(
                        eq(ContainerConfigService.TOOL_ID),
                        eq(configPath),
                        eq(Scope.Site),
                        isNull(String.class)
                ))
                .thenReturn(mockConfig);

        final MockHttpServletRequestBuilder request =
                get(siteConfigRestPath)
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

        final CommandConfiguration commandConfigurationResponse = mapper.readValue(response, CommandConfiguration.class);
        assertThat(commandConfigurationResponse, is(commandConfiguration));
    }

    @Test
    public void testDeleteSiteConfig() throws Exception {
        when(mockConfigService
                .getConfig(
                        eq(ContainerConfigService.TOOL_ID),
                        eq(configPath),
                        eq(Scope.Site),
                        isNull(String.class)
                ))
                .thenReturn(mockConfig);
        doNothing().when(mockConfigService).delete(mockConfig);

        final MockHttpServletRequestBuilder request =
                delete(siteConfigRestPath)
                        .with(authentication(authentication))
                        .with(csrf())
                        .with(testSecurityContext());

        mockMvc.perform(request)
                .andExpect(status().isNoContent());
    }

    @Test
    public void testCreateProjectConfig() throws Exception {
        when(mockConfigService
                .replaceConfig(
                        eq(FAKE_USERNAME),
                        anyString(),
                        eq(ContainerConfigService.TOOL_ID),
                        eq(configPath),
                        eq(commandConfigurationInternalRepresentationJson),
                        eq(Scope.Project),
                        eq(project)))
                .thenReturn(null);

        final MockHttpServletRequestBuilder request =
                post(projectConfigRestPath)
                        .content(commandConfigurationJson)
                        .contentType(JSON)
                        .with(authentication(authentication))
                        .with(csrf())
                        .with(testSecurityContext());

        mockMvc.perform(request)
                .andExpect(status().isCreated());
    }

    @Test
    public void testGetProjectConfig() throws Exception {
        when(mockConfigService
                .getConfig(
                        eq(ContainerConfigService.TOOL_ID),
                        eq(configPath),
                        eq(Scope.Project),
                        eq(project)
                ))
                .thenReturn(mockConfig);

        final MockHttpServletRequestBuilder request =
                get(projectConfigRestPath)
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

        final CommandConfiguration commandConfigurationResponse = mapper.readValue(response, CommandConfiguration.class);
        assertThat(commandConfigurationResponse, is(commandConfiguration));
    }

    @Test
    public void testDeleteProjectConfig() throws Exception {
        when(mockConfigService
                .getConfig(
                        eq(ContainerConfigService.TOOL_ID),
                        eq(configPath),
                        eq(Scope.Project),
                        eq(project)
                ))
                .thenReturn(mockConfig);
        doNothing().when(mockConfigService).delete(mockConfig);

        final MockHttpServletRequestBuilder request =
                delete(projectConfigRestPath)
                        .with(authentication(authentication))
                        .with(csrf())
                        .with(testSecurityContext());

        mockMvc.perform(request)
                .andExpect(status().isNoContent());
    }
}
