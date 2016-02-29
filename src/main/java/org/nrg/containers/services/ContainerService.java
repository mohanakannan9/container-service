package org.nrg.containers.services;

import org.nrg.containers.exceptions.NoServerPrefException;
import org.nrg.containers.model.Container;
import org.nrg.containers.model.ContainerServer;
import org.nrg.containers.model.Image;
import org.nrg.containers.model.ImageParameters;
import org.nrg.prefs.exceptions.InvalidPreferenceName;

import java.util.List;

public interface ContainerService {
    String CONTAINER_SERVICE_REST_PATH_PREFIX = "/containers";
    String CONTAINERS_REST_PATH = "/container";
    String IMAGES_REST_PATH = "/images";
    String SERVER_REST_PATH = "/server";

    String SERVER_PREF_TOOL_ID = "container";
    String SERVER_PREF_NAME = "server";

    ContainerServer getServer() throws NoServerPrefException;

    void setServer(final String host) throws NoServerPrefException, InvalidPreferenceName;

    List<Image> getAllImages() throws NoServerPrefException;

    Image getImageByName(final String name) throws NoServerPrefException;

    Image getImageById(final String id) throws NoServerPrefException;

    String deleteImageById(final String id) throws NoServerPrefException;

    String deleteImageByName(final String name) throws NoServerPrefException;

    List<Container> getAllContainers() throws NoServerPrefException;

    String getContainerStatus(final String id) throws NoServerPrefException;

    Container getContainer(final String id) throws NoServerPrefException;

    String launch(final String imageName, final ImageParameters params) throws NoServerPrefException;
}
