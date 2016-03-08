package org.nrg.containers.api;

import org.nrg.containers.exceptions.ContainerServerException;
import org.nrg.containers.exceptions.NoServerPrefException;
import org.nrg.containers.exceptions.NotFoundException;
import org.nrg.containers.model.Container;
import org.nrg.containers.model.Image;

import java.util.List;
import java.util.Map;

public interface ContainerControlApi {
    String server() throws NoServerPrefException;

    List<Image> getAllImages();

    Image getImageByName(final String imageName) throws ContainerServerException, NotFoundException;

    Image getImageById(final String imageId) throws NotFoundException, ContainerServerException;

    List<Container> getAllContainers();

    List<Container> getContainers(final Map<String, String> params);

    Container getContainer(final String id) throws NotFoundException;

    String getContainerStatus(final String id) throws NotFoundException;

    String launchImage(final String imageName, final String[] runCommand, final String[] volumes);
}
