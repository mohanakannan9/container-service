package org.nrg.containers.api;

import org.nrg.containers.model.Container;
import org.nrg.containers.model.Image;

import java.util.List;
import java.util.Map;

public interface ContainerControlApi {
    List<Image> getAllImages(final String server);

    Image getImageByName(final String server, final String imageName);

    Image getImageById(final String server, final String imageId);

    List<Container> getAllContainers(final String server);

    List<Container> getContainers(final String server, final Map<String, String> params);

    Container getContainer(final String server, final String id);

    String getContainerStatus(final String server, final String id);

    String launchImage(final String server, final String imageName, final String[] runCommand, final String[] volumes);
}
