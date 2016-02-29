package org.nrg.containers.services;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.nrg.containers.api.ContainerControlApi;
import org.nrg.containers.config.DefaultContainerServiceTestConfig;
import org.nrg.containers.exceptions.NoServerPrefException;
import org.nrg.containers.mocks.MockImages;
import org.nrg.containers.model.ContainerServer;
import org.nrg.containers.model.Image;
import org.nrg.prefs.entities.Preference;
import org.nrg.prefs.services.PreferenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = DefaultContainerServiceTestConfig.class)
public class DefaultContainerServiceTest {
    final static String MOCK_CONTAINER_HOST = "fake://host.url";

    final static Preference MOCK_PREFERENCE_ENTITY = new Preference();

    @Autowired
    private ContainerControlApi mockContainerControlApi;

    @Autowired
    private PreferenceService mockPrefsService;

    @Autowired
    private ContainerService service;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setup() {
        MOCK_PREFERENCE_ENTITY.setValue(MOCK_CONTAINER_HOST);
        when(mockPrefsService.getPreference(
                ContainerService.SERVER_PREF_TOOL_ID,
                ContainerService.SERVER_PREF_NAME)).thenReturn(MOCK_PREFERENCE_ENTITY);
    }

    @Test
    public void testGetServer() throws Exception {
        // No need to mock method here, because we mocked it in setup()
        final ContainerServer containerServer = service.getServer();
        assertThat(containerServer.host(), equalTo(MOCK_CONTAINER_HOST));
    }

    @Test
    public void testGetServerBlankPrefValue() throws Exception {
        final Preference BLANK_PREFERENCE = new Preference();
        when(mockPrefsService.getPreference(
                ContainerService.SERVER_PREF_TOOL_ID,
                ContainerService.SERVER_PREF_NAME))
                .thenReturn(BLANK_PREFERENCE);

        thrown.expect(NoServerPrefException.class);
        thrown.expectMessage("No container server URI defined in preferences.");

        service.getServer();
    }

    @Test
    public void testGetServerNullPref() throws Exception {
        when(mockPrefsService.getPreference(
                ContainerService.SERVER_PREF_TOOL_ID,
                ContainerService.SERVER_PREF_NAME))
                .thenReturn(null);

        thrown.expect(NoServerPrefException.class);
        thrown.expectMessage("No container server URI defined in preferences.");

        service.getServer();
    }

    @Test
    public void testSetServer() throws Exception {
        // TODO
    }

    @Test
    public void testGetAllImages() throws Exception {
        final List<Image> mockImageList = MockImages.FIRST_AND_SECOND;

        when(mockContainerControlApi.getAllImages(MOCK_CONTAINER_HOST)).thenReturn(mockImageList);
        final List<Image> responseImageList = service.getAllImages();
        assertThat(responseImageList, equalTo(mockImageList));
    }

    @Test
    public void testGetImageByName() throws Exception {
        final String name = MockImages.FOO_NAME;
        final Image mockImage = MockImages.FOO;

        when(mockContainerControlApi.getImageByName(MOCK_CONTAINER_HOST, name)).thenReturn(mockImage);
        final Image responseImageByName = service.getImageByName(name);
        assertThat(responseImageByName, equalTo(mockImage));
    }

    @Test
    public void testGetImageById() throws Exception {
        final String id = MockImages.FOO_ID;
        final Image mockImage = MockImages.FOO;

        when(mockContainerControlApi.getImageById(MOCK_CONTAINER_HOST, id)).thenReturn(mockImage);
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