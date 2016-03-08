package org.nrg.containers.api;

import org.nrg.containers.exceptions.ContainerServerException;
import org.nrg.containers.exceptions.NotFoundException;
import org.nrg.containers.model.Container;
import org.nrg.containers.model.Image;

import java.util.List;
import java.util.Map;

public interface ContainerControlApi {
    List<Image> getAllImages(final String server);

    Image getImageByName(final String server, final String imageName) throws ContainerServerException, NotFoundException;

    Image getImageById(final String server, final String imageId) throws NotFoundException, ContainerServerException;

    List<Container> getAllContainers(final String server);

    List<Container> getContainers(final String server, final Map<String, String> params);

    Container getContainer(final String server, final String id) throws NotFoundException;

    String getContainerStatus(final String server, final String id) throws NotFoundException;

    String launchImage(final String server, final String imageName, final String[] runCommand, final String[] volumes);
}
