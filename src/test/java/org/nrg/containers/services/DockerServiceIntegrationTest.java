package org.nrg.containers.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import com.spotify.docker.client.DefaultDockerClient;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.nrg.containers.api.ContainerControlApi;
import org.nrg.containers.config.DockerServiceIntegrationTestConfig;
import org.nrg.containers.model.server.docker.DockerServerPrefsBean;
import org.nrg.containers.model.command.auto.Command;
import org.nrg.xdat.preferences.SiteConfigPreferences;
import org.nrg.xdat.security.services.UserManagementServiceI;
import org.nrg.xft.security.UserI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = DockerServiceIntegrationTestConfig.class)
@Transactional
public class DockerServiceIntegrationTest {
    private UserI mockUser;

    private final String FAKE_USER = "mockUser";
    private final String FAKE_ALIAS = "alias";
    private final String FAKE_SECRET = "secret";
    private final String FAKE_HOST = "mock://url";

    @Autowired private ObjectMapper mapper;
    @Autowired private CommandEntityService commandEntityService;
    @Autowired private ContainerControlApi controlApi;
    @Autowired private DockerService dockerService;
    @Autowired private DockerServerPrefsBean mockDockerServerPrefsBean;
    @Autowired private SiteConfigPreferences mockSiteConfigPreferences;
    @Autowired private UserManagementServiceI mockUserManagementServiceI;

    @Rule public TemporaryFolder folder = new TemporaryFolder(new File("/tmp"));

    @Before
    public void setup() throws Exception {
        // Mock out the prefs bean
        final String containerHost = "unix:///var/run/docker.sock";
        when(mockDockerServerPrefsBean.getHost()).thenReturn(containerHost);
        when(mockDockerServerPrefsBean.toPojo()).thenCallRealMethod();

        // Mock the userI
        mockUser = Mockito.mock(UserI.class);
        when(mockUser.getLogin()).thenReturn(FAKE_USER);

        // Mock the user management service
        when(mockUserManagementServiceI.getUser(FAKE_USER)).thenReturn(mockUser);

        // Mock the site config preferences
        when(mockSiteConfigPreferences.getSiteUrl()).thenReturn(FAKE_HOST);
        when(mockSiteConfigPreferences.getBuildPath()).thenReturn(folder.newFolder().getAbsolutePath()); // transporter makes a directory under build
        when(mockSiteConfigPreferences.getArchivePath()).thenReturn(folder.newFolder().getAbsolutePath()); // container logs get stored under archive
        when(mockSiteConfigPreferences.getProperty("processingUrl", FAKE_HOST)).thenReturn(FAKE_HOST);
    }

    @Test
    public void testSaveCommandFromImageLabels() throws Exception {
        final String imageName = "xnat/testy-test";
        final String dir = Resources.getResource("dockerServiceIntegrationTest").getPath().replace("%20", " ");

        final DefaultDockerClient client = DefaultDockerClient.fromEnv().build();
        client.build(Paths.get(dir), imageName);

        final List<Command> commands = dockerService.saveFromImageLabels(imageName);
        assertThat(commands, hasSize(1));
        final Command command = commands.get(0);
        assertThat(command.id(), not(eq(0L)));

        final List<Command.CommandWrapper> wrappers = command.xnatCommandWrappers();
        assertThat(wrappers.size(), greaterThan(0));
        final Command.CommandWrapper wrapper = wrappers.get(0);
        assertThat(wrapper.id(), not(eq(0L)));

        client.removeImage(imageName);
    }
}
