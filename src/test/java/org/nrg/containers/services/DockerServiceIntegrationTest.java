package org.nrg.containers.services;

import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerException;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.nrg.containers.api.DockerControlApi;
import org.nrg.containers.config.DockerServiceIntegrationTestConfig;
import org.nrg.containers.model.command.auto.Command;
import org.nrg.xdat.preferences.SiteConfigPreferences;
import org.nrg.xdat.security.services.UserManagementServiceI;
import org.nrg.xft.security.UserI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Pattern;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;
import static org.mockito.Mockito.when;
import static org.nrg.containers.model.server.docker.DockerServerBase.DockerServer.DockerServer;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = DockerServiceIntegrationTestConfig.class)
@Transactional
public class DockerServiceIntegrationTest {
    private static final Logger log = LoggerFactory.getLogger(DockerServiceIntegrationTest.class);

    private UserI mockUser;

    private final String FAKE_USER = "mockUser";
    private final String FAKE_ALIAS = "alias";
    private final String FAKE_SECRET = "secret";
    private final String FAKE_HOST = "mock://url";

    private static DockerClient CLIENT;

    @Autowired private DockerControlApi controlApi;
    @Autowired private DockerService dockerService;
    @Autowired private DockerServerService dockerServerService;
    @Autowired private SiteConfigPreferences mockSiteConfigPreferences;
    @Autowired private UserManagementServiceI mockUserManagementServiceI;

    @Rule public TemporaryFolder folder = new TemporaryFolder(new File(System.getProperty("user.dir") + "/build"));

    @Before
    public void setup() throws Exception {
        // Mock out the prefs bean
        final String defaultHost = "unix:///var/run/docker.sock";
        final String hostEnv = System.getenv("DOCKER_HOST");
        final String certPathEnv = System.getenv("DOCKER_CERT_PATH");
        final String tlsVerify = System.getenv("DOCKER_TLS_VERIFY");

        final boolean useTls = tlsVerify != null && tlsVerify.equals("1");
        final String certPath;
        if (useTls) {
            if (StringUtils.isBlank(certPathEnv)) {
                throw new Exception("Must set DOCKER_CERT_PATH if DOCKER_TLS_VERIFY=1.");
            }
            certPath = certPathEnv;
        } else {
            certPath = "";
        }

        final String containerHost;
        if (StringUtils.isBlank(hostEnv)) {
            containerHost = defaultHost;
        } else {
            final Pattern tcpShouldBeHttpRe = Pattern.compile("tcp://.*");
            final java.util.regex.Matcher tcpShouldBeHttpMatch = tcpShouldBeHttpRe.matcher(hostEnv);
            if (tcpShouldBeHttpMatch.matches()) {
                // Must switch out tcp:// for either http:// or https://
                containerHost = hostEnv.replace("tcp://", "http" + (useTls ? "s" : "") + "://");
            } else {
                containerHost = hostEnv;
            }
        }

        dockerServerService.setServer(DockerServer.create(0L, "name", containerHost, certPath, false));

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

        CLIENT = controlApi.getClient();
    }

    private boolean canConnectToDocker() {
        try {
            return CLIENT.ping().equals("OK");
        } catch (InterruptedException | DockerException e) {
            log.warn("Could not connect to docker.", e);
        }
        return false;
    }

    @Test
    public void testSaveCommandFromImageLabels() throws Exception {
        assumeThat(canConnectToDocker(), is(true));

        final String imageName = "xnat/testy-test";
        final String dir = Paths.get(ClassLoader.getSystemResource("dockerServiceIntegrationTest").toURI()).toString().replace("%20", " ");

        CLIENT.build(Paths.get(dir), imageName);

        final List<Command> commands = dockerService.saveFromImageLabels(imageName);
        assertThat(commands, hasSize(1));
        final Command command = commands.get(0);
        assertThat(command.id(), not(0L));

        final List<Command.CommandWrapper> wrappers = command.xnatCommandWrappers();
        assertThat(wrappers.size(), greaterThan(0));
        final Command.CommandWrapper wrapper = wrappers.get(0);
        assertThat(wrapper.id(), not(0L));

        CLIENT.removeImage(imageName);
    }
}
