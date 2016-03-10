package org.nrg.containers.services;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.nrg.containers.api.ContainerControlApi;
import org.nrg.containers.config.DefaultContainerServiceTestConfig;
import org.nrg.containers.model.ContainerServer;
import org.nrg.containers.model.Image;
import org.nrg.containers.model.ImageMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = DefaultContainerServiceTestConfig.class)
public class DefaultContainerServiceTest {
    final static String MOCK_CONTAINER_HOST = "fake://host.url";
    final static String MOCK_CONTAINER_CERT_PATH = "/path/to/file";
    final static ContainerServer MOCK_CONTAINER_SERVER = new ContainerServer(MOCK_CONTAINER_HOST, MOCK_CONTAINER_CERT_PATH);

    @Autowired
    private ContainerControlApi mockContainerControlApi;

    @Autowired
    private ContainerService service;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setup() throws Exception {
        when(mockContainerControlApi.getServer())
                .thenReturn(MOCK_CONTAINER_SERVER);
    }

    @Test
    public void testGetServer() throws Exception {
        // No need to mock method here, because we mocked it in setup()
        final ContainerServer containerServer = service.getServer();
        assertEquals(containerServer, MOCK_CONTAINER_SERVER);
    }

//    @Test
//    public void testGetServerBlankPrefValue() throws Exception {
//        final Preference BLANK_PREFERENCE = new Preference();
//        when(mockPrefsService.getPreference(
//                DockerControlApi.SERVER_PREF_TOOL_ID,
//                DockerControlApi.SERVER_PREF_NAME))
//                .thenReturn(BLANK_PREFERENCE);
//
//        thrown.expect(NoServerPrefException.class);
//        thrown.expectMessage("No container server URI defined in preferences.");
//
//        service.getServer();
//    }

//    @Test
//    public void testGetServerNullPref() throws Exception {
//        when(mockPrefsService.getPreference(
//                DockerControlApi.SERVER_PREF_TOOL_ID,
//                DockerControlApi.SERVER_PREF_NAME))
//                .thenReturn(null);
//
//        thrown.expect(NoServerPrefException.class);
//        thrown.expectMessage("No container server URI defined in preferences.");
//
//        service.getServer();
//    }

    @Test
    public void testSetServer() throws Exception {
        // TODO
    }

    @Test
    public void testGetAllImages() throws Exception {
        final List<Image> mockImageList = ImageMocks.FIRST_AND_SECOND;

        when(mockContainerControlApi.getAllImages()).thenReturn(mockImageList);
        final List<Image> responseImageList = service.getAllImages();
        assertThat(responseImageList, equalTo(mockImageList));
    }

    @Test
    public void testGetImageByName() throws Exception {
        final String name = ImageMocks.FOO_NAME;
        final Image mockImage = ImageMocks.FOO;

        when(mockContainerControlApi.getImageByName(name)).thenReturn(mockImage);
        final Image responseImageByName = service.getImageByName(name);
        assertThat(responseImageByName, equalTo(mockImage));
    }

    @Test
    public void testGetImageById() throws Exception {
        final String id = ImageMocks.FOO_ID;
        final Image mockImage = ImageMocks.FOO;

        when(mockContainerControlApi.getImageById(id)).thenReturn(mockImage);
        final Image responseImageById = service.getImageById(id);
        assertThat(responseImageById, equalTo(mockImage));
    }

    @Test
    public void testDeleteImageById() throws Exception {
        // TODO
    }

    @Test
    public void testDeleteImageByName() throws Exception {
        // TODO
    }

    @Test
    public void testGetAllContainers() throws Exception {
        // TODO
    }

    @Test
    public void testGetContainerStatus() throws Exception {
        // TODO
    }

    @Test
    public void testGetContainer() throws Exception {
        // TODO
    }

    @Test
    public void testLaunch() throws Exception {
        // TODO
    }
}