package org.nrg.containers.services;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nrg.containers.api.ContainerControlApi;
import org.nrg.containers.config.MockContainerControlConfig;
import org.nrg.containers.mocks.MockImages;
import org.nrg.containers.model.Image;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = MockContainerControlConfig.class)
public class DefaultContainerServiceTest {

    @Autowired
    @Qualifier("mockContainerControlApi")
    private ContainerControlApi controlApi;

    @Autowired
    @Qualifier("defaultContainerService")
    private ContainerService service;

    private String server;

    @Before
    public void setup() {
        server = service.getServer();

    }

    @Test
    public void testGetAllImages() throws Exception {
        final List<Image> mockImageList = MockImages.FIRST_AND_SECOND;

        when(controlApi.getAllImages(server)).thenReturn(mockImageList);
        final List<Image> responseImageList = service.getAllImages();
        assertThat(responseImageList, equalTo(mockImageList));
    }

    @Test
    public void testGetImage() throws Exception {
        final String name = MockImages.FOO_NAME;
        final String id = MockImages.FOO_ID;
        final Image mockImage = MockImages.FOO;

        when(controlApi.getImageByName(server, name)).thenReturn(mockImage);
        final Image responseImageByName = service.getImageByName(name);
        assertThat(responseImageByName, equalTo(mockImage));

        when(controlApi.getImageById(server, id)).thenReturn(mockImage);
        final Image responseImageById = service.getImageById(id);
        assertThat(responseImageById, equalTo(mockImage));
    }

/*
    TODO

    List<Container> getAllContainers(final String server);

    List<Container> getContainers(final String server, final Map<String, String> params);

    Container getContainer(final String server, final String id);

    String getContainerStatus(final String server, final String id);

    String launchImage(final String server, final String imageName, final String[] runCommand, final String[] volumes);

 */
}