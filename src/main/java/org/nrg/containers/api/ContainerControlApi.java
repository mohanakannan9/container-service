package org.nrg.containers.api;

import org.nrg.containers.exceptions.DockerServerException;
import org.nrg.containers.exceptions.NoServerPrefException;
import org.nrg.containers.exceptions.NotFoundException;
import org.nrg.containers.model.Container;
import org.nrg.containers.model.DockerHub;
import org.nrg.containers.model.DockerImageDto;
import org.nrg.containers.model.DockerServer;
import org.nrg.prefs.exceptions.InvalidPreferenceName;

import java.util.List;
import java.util.Map;

public interface ContainerControlApi {
    DockerServer getServer() throws NoServerPrefException;

    DockerServer setServer(String host, String certPath) throws InvalidPreferenceName;

    DockerServer setServer(DockerServer server) throws InvalidPreferenceName;

    String pingServer() throws NoServerPrefException, DockerServerException;

    String pingHub(DockerHub hub) throws DockerServerException, NoServerPrefException;

    void pullImage(String name) throws NoServerPrefException, DockerServerException;

    void pullImage(String name, DockerHub hub) throws NoServerPrefException, DockerServerException;

    List<DockerImageDto> getAllImages() throws NoServerPrefException, DockerServerException;

    DockerImageDto getImageByName(final String imageName) throws DockerServerException, NotFoundException, NoServerPrefException;

    DockerImageDto getImageById(final String imageId) throws NotFoundException, DockerServerException, NoServerPrefException;

    List<Container> getAllContainers() throws NoServerPrefException, DockerServerException;

    List<Container> getContainers(final Map<String, String> params) throws NoServerPrefException, DockerServerException;

    Container getContainer(final String id) throws NotFoundException, NoServerPrefException, DockerServerException;

    void setServer(String host) throws InvalidPreferenceName;

    String getContainerStatus(final String id) throws NotFoundException, NoServerPrefException, DockerServerException;

    String launchImage(final String imageName, final List<String> runCommand, final List <String> volumes) throws NoServerPrefException;

    String launchImage(final DockerServer server, final String imageName,
                       final List<String> runCommand, final List <String> volumes);

    String getContainerLogs(String id) throws NoServerPrefException, DockerServerException;

    void deleteImageByName(String name) throws NoServerPrefException, DockerServerException;

    void deleteImageById(String id) throws NoServerPrefException, DockerServerException;
}
