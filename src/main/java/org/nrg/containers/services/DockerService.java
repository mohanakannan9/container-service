package org.nrg.containers.services;

import org.nrg.containers.exceptions.DockerServerException;
import org.nrg.containers.exceptions.NoServerPrefException;
import org.nrg.containers.exceptions.NotFoundException;
import org.nrg.containers.model.DockerHub;
import org.nrg.containers.model.DockerImageDto;
import org.nrg.containers.model.DockerServer;
import org.nrg.framework.exceptions.NrgServiceRuntimeException;
import org.nrg.prefs.exceptions.InvalidPreferenceName;

import java.io.IOException;
import java.util.List;

public interface DockerService {
    List<DockerHub> getHubs();
    void setHub(DockerHub hub) throws NrgServiceRuntimeException;
    String pingHub(DockerHub hub) throws DockerServerException, NoServerPrefException;

    DockerServer getServer() throws NotFoundException;
    void setServer(DockerServer server) throws InvalidPreferenceName;
    String pingServer() throws NoServerPrefException, DockerServerException;

    List<DockerImageDto> getImages(Boolean fromDb, Boolean fromDockerServer) throws NoServerPrefException, DockerServerException;
    DockerImageDto getImage(Long id, Boolean fromDockerServer);
    void createImage(DockerImageDto dockerImageDto) throws DockerServerException, NoServerPrefException, NotFoundException;
}
