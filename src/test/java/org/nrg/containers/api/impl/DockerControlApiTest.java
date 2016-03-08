package org.nrg.containers.api.impl;

import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.DockerException;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.nrg.containers.config.DockerControlApiTestConfig;
import org.nrg.containers.config.RestApiTestConfig;
import org.nrg.containers.exceptions.ContainerServerException;
import org.nrg.containers.exceptions.NotFoundException;
import org.nrg.containers.model.ContainerServer;
import org.nrg.containers.model.Image;
import org.nrg.containers.services.ContainerService;
import org.nrg.prefs.entities.Preference;
import org.nrg.prefs.services.PreferenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


import java.util.List;

import static org.hamcrest.Matchers.containsString;
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
    final static String MOCK_CONTAINER_HOST = "https://192.168.99.100:2376";
    final static String MOCK_CERT_PATH = "/Users/Kelsey/.docker/machine/machines/testDocker";

    final static Preference MOCK_PREFERENCE_ENTITY_HOST = new Preference();
    final static Preference MOCK_PREFERENCE_ENTITY_CERT = new Preference();

    private static DockerClient client;

    private static final String IMAGE1 = "busybox";
    private static final String IMAGE2 = "ubuntu";

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
        client.pull(IMAGE1);
        client.pull(IMAGE2);

    }


    @After
    public void tearDown() throws Exception {
        return;
    }

    @Test
    public void testGetServer() throws Exception {
        ContainerServer server = controlApi.getServer();
        assertThat(server.host(), containsString(MOCK_CONTAINER_HOST));
        assertThat(server.certPath(), containsString(MOCK_CERT_PATH));


    }
    @Test
    public void testGetImageByName() throws Exception {
        final Image image = controlApi.getImageByName(IMAGE1);
        assertThat(image.getName(), containsString(IMAGE1));
    }

    @Test
    public void testGetAllImages() throws Exception {
        final List<Image> images = controlApi.getAllImages();
        assertEquals(images.size(), 2);
        assertThat(images.get(0).getName(), containsString(IMAGE1));
        assertThat(images.get(1).getName(), containsString(IMAGE2));
    }

}


