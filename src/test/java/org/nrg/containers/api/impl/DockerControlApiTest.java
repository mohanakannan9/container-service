package org.nrg.containers.api.impl;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import com.spotify.docker.client.DockerClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.nrg.containers.config.DockerControlApiTestConfig;
import org.nrg.containers.model.ContainerServer;
import org.nrg.containers.model.Image;
import org.nrg.prefs.entities.Preference;
import org.nrg.prefs.services.PreferenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.isIn;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = DockerControlApiTestConfig.class)
public class DockerControlApiTest {

    final static String SERVER_PREF_TOOL_ID = "container";
    final static String SERVER_PREF_NAME = "server";
    final static String CERT_PATH_PREF_NAME = "certpath";

    // Local docker-machine based VM
//    final static String MOCK_CONTAINER_HOST = "https://192.168.99.100:2376";
    final static String MOCK_CONTAINER_HOST = System.getenv("DOCKER_HOST").replace("tcp", "https");
//    final static String MOCK_CERT_PATH = "/Users/Kelsey/.docker/machine/machines/testDocker";
    final static String MOCK_CERT_PATH = System.getenv("DOCKER_CERT_PATH");

    final static Preference MOCK_PREFERENCE_ENTITY_HOST = new Preference();
    final static Preference MOCK_PREFERENCE_ENTITY_CERT = new Preference();

    private static DockerClient client;

    private static final String BUSYBOX_LATEST = "busybox:latest";
    private static final String UBUNTU_LATEST = "ubuntu:latest";
    private static final String KELSEYM_PYDICOM = "kelseym/pydicom:latest";

    @Autowired
    private DockerControlApi controlApi;

    @Autowired
    private PreferenceService mockPrefsService;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setup() throws Exception {
        MOCK_PREFERENCE_ENTITY_HOST.setValue(MOCK_CONTAINER_HOST);
        when(mockPrefsService.getPreference(
                SERVER_PREF_TOOL_ID,
                SERVER_PREF_NAME)).thenReturn(MOCK_PREFERENCE_ENTITY_HOST);

        MOCK_PREFERENCE_ENTITY_CERT.setValue(MOCK_CERT_PATH);
        when(mockPrefsService.getPreference(
                SERVER_PREF_TOOL_ID,
                CERT_PATH_PREF_NAME)).thenReturn(MOCK_PREFERENCE_ENTITY_CERT);

        client = controlApi.getClient();
    }

    @After
    public void tearDown() throws Exception {
        return;
    }

    @Test
    public void testGetServer() throws Exception {
        final ContainerServer expectedServer =
            new ContainerServer(MOCK_CONTAINER_HOST, MOCK_CERT_PATH);

        final ContainerServer server = controlApi.getServer();

        assertEquals(expectedServer, server);
    }

    @Test
    public void testGetImageByName() throws Exception {
        client.pull(BUSYBOX_LATEST);
        final Image image = controlApi.getImageByName(BUSYBOX_LATEST);
        assertThat(image.getName(), containsString(BUSYBOX_LATEST));
    }

    @Test
    public void testGetAllImages() throws Exception {
        client.pull(BUSYBOX_LATEST);
        client.pull(UBUNTU_LATEST);
        final List<Image> images = controlApi.getAllImages();

        final List<String> imageNames = imagesToTags(images);
        assertThat(BUSYBOX_LATEST, isIn(imageNames));
        assertThat(UBUNTU_LATEST, isIn(imageNames));
    }

    private List<String> imagesToTags(final List<Image> images) {
        final List<String> tags = Lists.newArrayList();
        for (Image image : images) {
            if (image.getRepoTags() != null) {
                tags.addAll(image.getRepoTags());
            }
        }
        return tags;
    }

    private List<String> imagesToNames(final List<Image> images) {
        final Function<Image, String> imageToName = new Function<Image, String>() {
            @Override
            public String apply(final Image image) {
                return image.getName();
            }
        };
        return Lists.transform(images, imageToName);
    }

    @Test
    public void testLaunchImage() throws Exception {
        List cmd = new ArrayList<String>();
        cmd.add("ls");
        cmd.add("/data/pyscript.py");
        List vol = new ArrayList<String>();
        vol.add("/Users/Kelsey/Projects/XNAT/1.7/pydicomDocker/data:/data");

        client.pull(KELSEYM_PYDICOM);
        String containerId = controlApi.launchImage(KELSEYM_PYDICOM, cmd, vol);
    }

    @Test
    public void testLaunchPythonScript() throws Exception {
       // python pyscript.py -h <hostname> -u <user> -p <password> -s <session_id>
        List cmd = new ArrayList<String>();
        cmd.add("python");
        cmd.add("/data/pyscript.py");
        cmd.add("-h");
        cmd.add("https://central.xnat.org");
        cmd.add("-u");
        cmd.add("admin");
        cmd.add("-p");
        cmd.add("admin");
        cmd.add("s");
        cmd.add("CENTRAL_E07096");
        List vol = new ArrayList<String>();
        vol.add("/Users/Kelsey/Projects/XNAT/1.7/pydicomDocker/data:/data");

        client.pull(KELSEYM_PYDICOM);
        String containerId = controlApi.launchImage(KELSEYM_PYDICOM, cmd, vol);
    }
}


