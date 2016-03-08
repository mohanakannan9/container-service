package org.nrg.containers.services;

import org.nrg.containers.exceptions.ContainerServerException;
import org.nrg.containers.exceptions.NoServerPrefException;
import org.nrg.containers.exceptions.NotFoundException;
import org.nrg.containers.model.Container;
import org.nrg.containers.model.ContainerHub;
import org.nrg.containers.model.ContainerServer;
import org.nrg.containers.model.Image;
import org.nrg.containers.model.ImageParameters;
import org.nrg.prefs.exceptions.InvalidPreferenceName;

import java.util.List;

public interface ContainerService {

    List<Image> getAllImages() throws NoServerPrefException;

    Image getImageByName(String name) throws NoServerPrefException, NotFoundException, ContainerServerException;

    Image getImageById(String id) throws NoServerPrefException, NotFoundException, ContainerServerException;

    String deleteImageById(String id, Boolean onServer) throws NoServerPrefException, NotFoundException, ContainerServerException;

    String deleteImageByName(String name, Boolean onServer) throws NoServerPrefException, NotFoundException, ContainerServerException;

    List<Container> getAllContainers() throws NoServerPrefException, ContainerServerException;

    String getContainerStatus(String id) throws NoServerPrefException, NotFoundException, ContainerServerException;

    Container getContainer(String id) throws NoServerPrefException, NotFoundException, ContainerServerException;

    String launch(String imageName, ImageParameters params) throws NoServerPrefException, NotFoundException, ContainerServerException;

    String getContainerLogs(String id);

    String verbContainer(String id, String status);

    ContainerHub getHub(String hub, Boolean verbose);

    List<ContainerHub> getHubs(Boolean verbose);

    void setHub(ContainerHub hub, Boolean overwrite, Boolean ignoreBlank);

    String search(String term);

    Image pullByName(String image, String hub, String name);

    Image pullFromSource(String source, String name);
}
