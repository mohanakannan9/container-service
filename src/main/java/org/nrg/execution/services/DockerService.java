package org.nrg.execution.services;

import org.nrg.execution.exceptions.DockerServerException;
import org.nrg.execution.exceptions.NoServerPrefException;
import org.nrg.execution.exceptions.NotFoundException;
import org.nrg.execution.model.Command;
import org.nrg.execution.model.DockerHub;
import org.nrg.execution.model.DockerImage;
import org.nrg.execution.model.DockerServer;
import org.nrg.prefs.exceptions.InvalidPreferenceName;

import java.util.List;

public interface DockerService {
    List<DockerHub> getHubs();
    DockerHub setHub(DockerHub hub);
    String pingHub(Long hubId) throws DockerServerException, NoServerPrefException, NotFoundException;
    String pingHub(DockerHub hub) throws DockerServerException, NoServerPrefException;
    DockerImage pullFromHub(Long hubId, String image, Boolean saveCommands)
            throws DockerServerException, NoServerPrefException, NotFoundException;
    DockerImage pullFromHub(String image, Boolean saveCommands)
            throws DockerServerException, NoServerPrefException, NotFoundException;

    DockerServer getServer() throws NotFoundException;
    DockerServer setServer(DockerServer server) throws InvalidPreferenceName;
    String pingServer() throws NoServerPrefException, DockerServerException;

    List<DockerImage> getImages() throws NoServerPrefException, DockerServerException;
    DockerImage getImage(String imageId) throws NoServerPrefException, NotFoundException;
    void removeImage(String imageId, Boolean force) throws NotFoundException, NoServerPrefException, DockerServerException;
    List<Command> saveFromImageLabels(String imageId) throws DockerServerException, NotFoundException, NoServerPrefException;
    List<Command> saveFromImageLabels(DockerImage dockerImage);
}
