package org.nrg.containers.services;

import org.nrg.containers.exceptions.DockerServerException;
import org.nrg.containers.exceptions.NoServerPrefException;
import org.nrg.containers.exceptions.NotUniqueException;
import org.nrg.containers.model.command.auto.Command;
import org.nrg.containers.model.dockerhub.DockerHub;
import org.nrg.containers.model.auto.DockerImage;
import org.nrg.containers.model.auto.DockerImageAndCommandSummary;
import org.nrg.containers.model.server.docker.DockerServer;
import org.nrg.containers.services.DockerHubService.DockerHubDeleteDefaultException;
import org.nrg.framework.exceptions.NotFoundException;
import org.nrg.prefs.exceptions.InvalidPreferenceName;

import java.util.List;

public interface DockerService {
    List<DockerHub> getHubs();
    DockerHub getHub(long id) throws NotFoundException;
    DockerHub getHub(String name) throws NotFoundException, NotUniqueException;
    DockerHub createHub(DockerHub hub);
    DockerHub createHubAndSetDefault(DockerHub hub, String username, String reason);
    void updateHub(DockerHub hub);
    void updateHubAndSetDefault(DockerHub hub, String username, String reason);
    void setDefaultHub(long id, String username, String reason);
    void deleteHub(long id) throws DockerHubDeleteDefaultException;
    void deleteHub(String name) throws DockerHubDeleteDefaultException, NotUniqueException;
    String pingHub(long hubId) throws DockerServerException, NoServerPrefException, NotFoundException;
    String pingHub(long hubId, String username, String password) throws DockerServerException, NoServerPrefException, NotFoundException;
    String pingHub(String hubName) throws DockerServerException, NoServerPrefException, NotUniqueException, NotFoundException;
    String pingHub(String hubName, String username, String password)
            throws DockerServerException, NoServerPrefException, NotUniqueException, NotFoundException;
    DockerImage pullFromHub(long hubId, String imageName, boolean saveCommands)
            throws DockerServerException, NoServerPrefException, NotFoundException;
    DockerImage pullFromHub(long hubId, String imageName, boolean saveCommands, String username, String password)
            throws DockerServerException, NoServerPrefException, NotFoundException;
    DockerImage pullFromHub(String hubName, String imageName, boolean saveCommands)
            throws DockerServerException, NoServerPrefException, NotFoundException, NotUniqueException;
    DockerImage pullFromHub(String hubName, String imageName, boolean saveCommands, String username, String password)
            throws DockerServerException, NoServerPrefException, NotFoundException, NotUniqueException;
    DockerImage pullFromHub(String imageName, boolean saveCommands)
            throws DockerServerException, NoServerPrefException, NotFoundException;

    DockerServer getServer() throws NotFoundException;
    DockerServer setServer(DockerServer server) throws InvalidPreferenceName;
    String pingServer() throws NoServerPrefException, DockerServerException;

    List<DockerImage> getImages() throws NoServerPrefException, DockerServerException;
    List<DockerImageAndCommandSummary> getImageSummaries() throws NoServerPrefException, DockerServerException;
    DockerImage getImage(String imageId) throws NoServerPrefException, NotFoundException;
    void removeImage(String imageId, Boolean force) throws NotFoundException, NoServerPrefException, DockerServerException;
    List<Command> saveFromImageLabels(String imageName) throws DockerServerException, NotFoundException, NoServerPrefException;
}
