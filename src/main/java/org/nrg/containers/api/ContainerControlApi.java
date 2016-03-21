package org.nrg.containers.api;

import com.spotify.docker.client.DockerException;
import org.nrg.containers.exceptions.ContainerServerException;
import org.nrg.containers.exceptions.NoServerPrefException;
import org.nrg.containers.exceptions.NotFoundException;
import org.nrg.containers.model.Container;
import org.nrg.containers.model.ContainerServer;
import org.nrg.containers.model.Image;
import org.nrg.prefs.exceptions.InvalidPreferenceName;

import java.util.List;
import java.util.Map;

public interface ContainerControlApi {
    ContainerServer getServer() throws NoServerPrefException;

    void setServer(String host, String certPath) throws InvalidPreferenceName;

    List<Image> getAllImages() throws NoServerPrefException;

    Image getImageByName(final String imageName) throws ContainerServerException, NotFoundException, NoServerPrefException;

    Image getImageById(final String imageId) throws NotFoundException, ContainerServerException, NoServerPrefException;

    List<Container> getAllContainers() throws NoServerPrefException;

    List<Container> getContainers(final Map<String, String> params) throws NoServerPrefException;

    Container getContainer(final String id) throws NotFoundException, NoServerPrefException;

    void setServer(String host) throws InvalidPreferenceName;

    String getContainerStatus(final String id) throws NotFoundException, NoServerPrefException;

    String launchImage(final String imageName, final List<String> runCommand, final List <String> volumes) throws NoServerPrefException;

    String getContainerLogs(String id) throws NoServerPrefException, DockerException, InterruptedException;
}
