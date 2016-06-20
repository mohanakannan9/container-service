package org.nrg.containers.services;

import org.nrg.containers.exceptions.DockerServerException;
import org.nrg.containers.exceptions.NoServerPrefException;
import org.nrg.containers.exceptions.NotFoundException;
import org.nrg.containers.model.DockerHub;
import org.nrg.containers.model.DockerImage;
import org.nrg.containers.model.DockerServer;
import org.nrg.prefs.exceptions.InvalidPreferenceName;

import java.util.List;

public interface DockerService {
    List<DockerHub> getHubs();
    DockerHub setHub(DockerHub hub);
    String pingHub(Long hubId) throws DockerServerException, NoServerPrefException, NotFoundException;
    String pingHub(DockerHub hub) throws DockerServerException, NoServerPrefException;
    DockerImage pullFromHub(Long hubId, String image) throws DockerServerException, NoServerPrefException, NotFoundException;
    DockerImage pullFromHub(String image) throws DockerServerException, NoServerPrefException, NotFoundException;

    DockerServer getServer() throws NotFoundException;
    DockerServer setServer(DockerServer server) throws InvalidPreferenceName;
    String pingServer() throws NoServerPrefException, DockerServerException;

    List<DockerImage> getImages() throws NoServerPrefException, DockerServerException;
    DockerImage getImage(String imageId) throws NoServerPrefException, NotFoundException;
    void removeImage(String imageId, Boolean force) throws NotFoundException, NoServerPrefException, DockerServerException;
}
