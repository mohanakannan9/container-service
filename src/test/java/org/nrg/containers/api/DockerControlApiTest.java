package org.nrg.containers.api;

import com.google.common.collect.Lists;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.HostConfig;
import com.spotify.docker.client.messages.Info;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.CustomTypeSafeMatcher;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.nrg.containers.config.DockerControlApiTestConfig;
import org.nrg.containers.events.DockerContainerEvent;
import org.nrg.containers.exceptions.DockerServerException;
import org.nrg.containers.exceptions.NoServerPrefException;
import org.nrg.containers.model.server.docker.DockerServer;
import org.nrg.containers.model.dockerhub.DockerHub;
import org.nrg.containers.model.auto.DockerImage;
import org.nrg.framework.scope.EntityId;
import org.nrg.prefs.services.NrgPreferenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Date;
import java.util.List;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.isIn;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = DockerControlApiTestConfig.class)
public class DockerControlApiTest {

    static String CONTAINER_HOST;
    static String CERT_PATH ;

    private static DockerClient client;

    private static final String BUSYBOX_LATEST = "busybox:latest";
    private static final String ALPINE_LATEST = "alpine:latest";
    // private static final String KELSEYM_PYDICOM = "kelseym/pydicom:latest";
    private static final String BUSYBOX_ID = "sha256:47bcc53f74dc94b1920f0b34f6036096526296767650f223433fe65c35f149eb";
    private static final String BUSYBOX_NAME = "busybox:1.24.2-uclibc";
    private static final DockerHub DOCKER_HUB = DockerHub.DEFAULT;

    @Autowired private DockerControlApi controlApi;
    @Autowired private NrgPreferenceService mockPrefsService;

    @Rule public ExpectedException exception = ExpectedException.none();

    @Before
    public void setup() throws Exception {

        final String defaultHost = "unix:///var/run/docker.sock";
        final String hostEnv = System.getenv("DOCKER_HOST");
        CONTAINER_HOST = StringUtils.isBlank(hostEnv) ? defaultHost : hostEnv;

        final String tlsVerify = System.getenv("DOCKER_TLS_VERIFY");
        final String certPathEnv = System.getenv("DOCKER_CERT_PATH");
        if (tlsVerify != null && tlsVerify.equals("1")) {
            if (StringUtils.isBlank(certPathEnv)) {
                throw new Exception("Must set DOCKER_CERT_PATH if DOCKER_TLS_VERIFY=1.");
            }
            CERT_PATH = certPathEnv;
        } else {
            CERT_PATH = "";
        }

        final Date timeZero = new Date(0L);
        final String timeZeroString = String.valueOf(timeZero.getTime());

        // Set up mock prefs service for all the calls that will initialize
        // the ContainerServerPrefsBean
        when(mockPrefsService.getPreferenceValue("docker-server", "host"))
            .thenReturn(CONTAINER_HOST);
        when(mockPrefsService.getPreferenceValue("docker-server", "certPath"))
            .thenReturn(CERT_PATH);
        when(mockPrefsService.getPreferenceValue("docker-server", "lastEventCheckTime", EntityId.Default.getScope(), EntityId.Default.getEntityId()))
            .thenReturn(timeZeroString);
        doNothing().when(mockPrefsService)
            .setPreferenceValue(Mockito.eq("docker-server"), Mockito.anyString(), Mockito.anyString());
        when(mockPrefsService.hasPreference(Mockito.eq("docker-server"), Mockito.anyString())).thenReturn(true);

        client = controlApi.getClient();
    }

    @Test
    public void testGetServer() throws Exception {
        final DockerServer server = controlApi.getServer();
        assertEquals(CONTAINER_HOST, server.getHost());
        assertEquals(CERT_PATH, server.getCertPath());
    }

    @Test
    public void testGetAllImages() throws Exception {
        client.pull(BUSYBOX_LATEST);
        client.pull(ALPINE_LATEST);
        final List<DockerImage> images = controlApi.getAllImages();

        final List<String> imageNames = imagesToTags(images);
        assertThat(BUSYBOX_LATEST, isIn(imageNames));
        assertThat(ALPINE_LATEST, isIn(imageNames));
    }

    private List<String> imagesToTags(final List<DockerImage> images) {
        final List<String> tags = Lists.newArrayList();
        for (final DockerImage image : images) {
            if (image.tags() != null) {
                tags.addAll(image.tags());
            }
        }
        return tags;
    }

//    @Test
//    public void testLaunchImage() throws Exception {
//        final List<String> cmd = Lists.newArrayList("ls", "/data/pyscript.py");
//        final List<String> vol =
//            Lists.newArrayList("/Users/Kelsey/Projects/XNAT/1.7/pydicomDocker/data:/data");
//
//        client.pull(KELSEYM_PYDICOM);
//        String containerId = controlApi.launchImage(KELSEYM_PYDICOM, cmd, vol);
//        assertThat(containerId, not(isEmptyOrNullString()));
//    }

//    @Test
//    public void testLaunchPythonScript() throws Exception {
//       // python pyscript.py -h <hostname> -u <user> -p <password> -s <session_id>
//        final List<String> cmd = Lists.newArrayList(
//            "python", "/data/pyscript.py",
//            "-h", "https://central.xnat.org",
//            "-u", "admin",
//            "-p", "admin",
//            "-s", "CENTRAL_E07096"
//        );
//        final List<String> vol =
//            Lists.newArrayList("/Users/Kelsey/Projects/XNAT/1.7/pydicomDocker/data:/data");
//
//        client.pull(KELSEYM_PYDICOM);
//        String containerId = controlApi.launchImage(KELSEYM_PYDICOM, cmd, vol);
//    }

    @Test
    public void testPingServer() throws Exception {
        assertEquals("OK", controlApi.pingServer());
    }

    @Test
    public void testPingHub() throws Exception {
        assertEquals("OK", controlApi.pingHub(DOCKER_HUB));
    }

    @Test
    public void testPullImage() throws Exception {
        controlApi.pullImage(BUSYBOX_LATEST, DOCKER_HUB);
    }

    @Test
    public void testPullPrivateImage() throws Exception {
        final String privateImageName = "xnattest/private";
        exception.expect(imageNotFoundException(privateImageName));
        controlApi.pullImage(privateImageName, DOCKER_HUB);

        final DockerImage test = controlApi.pullImage(privateImageName, DOCKER_HUB, "xnattest", "windmill susanna portico");
        assertNotNull(test);
    }

    @Test
    public void testDeleteImage() throws DockerException, InterruptedException, NoServerPrefException, DockerServerException {
        client.pull(BUSYBOX_NAME);
        int beforeImageCount = client.listImages().size();
        controlApi.deleteImageById(BUSYBOX_ID, true);
        List<com.spotify.docker.client.messages.Image> images = client.listImages();
        int afterImageCount = images.size();
        assertEquals(beforeImageCount, afterImageCount+1);
        for(com.spotify.docker.client.messages.Image image:images){
            assertNotEquals(BUSYBOX_ID, image.id());
        }
    }

    @Test(timeout = 10000)
    public void testEventPolling() throws Exception {
        final Info dockerInfo = client.info();
        if (dockerInfo.kernelVersion().contains("moby")) {
            // If we are running docker in the moby VM, then it isn't running natively
            //   on the host machine. Sometimes the clocks on the host and VM can get out
            //   out sync, and this test will fail. This especially happens on laptops that
            //   have docker running and go to sleep.
            // We can run this container command:
            //     docker run --rm --privileged alpine hwclock -s
            // to sync up the clocks. It requires 'privileged' mode, which may cause problems
            // running in a CI environment.
            final ContainerConfig containerConfig = ContainerConfig.builder()
                    .image("alpine")
                    .cmd(new String[]{"hwclock", "-s"})
                    .hostConfig(HostConfig.builder()
                            .privileged(true)
                            .autoRemove(true)
                            .build())
                    .build();
            final ContainerCreation containerCreation = client.createContainer(containerConfig);
            client.startContainer(containerCreation.id());
        }

        final Date start = new Date();
        Thread.sleep(1000); // Wait to ensure we get some events

        controlApi.pullImage(BUSYBOX_LATEST);

        // Create container, to ensure we have some events to read
        final ContainerConfig config = ContainerConfig.builder()
                .image(BUSYBOX_LATEST)
                .cmd("sh", "-c", "echo Hello world")
                .build();

        final ContainerCreation creation = client.createContainer(config);
        client.startContainer(creation.id());

        Thread.sleep(1000); // Wait to ensure we get some events
        final Date end = new Date();

        final List<DockerContainerEvent> events = controlApi.getContainerEvents(start, end);

        // The fact that we have a list of events and not a timeout failure is already a victory
        assertThat(events, not(empty()));

        // TODO assert more things about the events
    }

    private Matcher<DockerServerException> imageNotFoundException(final String name) {
        final String exceptionMessage = "Image not found: " + name;
        final String description = "for image name " + name;
        return new CustomTypeSafeMatcher<DockerServerException>(description) {
            @Override
            protected boolean matchesSafely(final DockerServerException ex) {
                return ex.getMessage().contains(exceptionMessage);
            }
        };
    }
}


