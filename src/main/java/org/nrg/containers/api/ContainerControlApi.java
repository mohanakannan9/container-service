package org.nrg.containers.api;

import org.nrg.containers.events.model.DockerContainerEvent;
import org.nrg.containers.exceptions.ContainerException;
import org.nrg.containers.exceptions.DockerServerException;
import org.nrg.containers.exceptions.NoServerPrefException;
import org.nrg.containers.model.command.auto.ResolvedCommand;
import org.nrg.containers.model.command.auto.Command;
import org.nrg.containers.model.container.auto.ContainerMessage;
import org.nrg.containers.model.dockerhub.DockerHubBase.DockerHub;
import org.nrg.containers.model.image.docker.DockerImage;
import org.nrg.containers.model.server.docker.DockerServerBase.DockerServer;
import org.nrg.containers.model.server.docker.DockerServerBase.DockerServerWithPing;
import org.nrg.framework.exceptions.NotFoundException;
import org.nrg.prefs.exceptions.InvalidPreferenceName;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface ContainerControlApi {
    DockerServer getServer() throws NoServerPrefException;
    DockerServerWithPing getServerAndPing() throws NoServerPrefException;
    DockerServer setServer(String host, String certPath) throws InvalidPreferenceName;
    DockerServer setServer(DockerServer server) throws InvalidPreferenceName;
    DockerServerWithPing setServerAndPing(DockerServer server) throws InvalidPreferenceName;
    void setServer(String host) throws InvalidPreferenceName;
    String pingServer() throws NoServerPrefException, DockerServerException;
    boolean canConnect();

    String pingHub(DockerHub hub) throws DockerServerException, NoServerPrefException;
    String pingHub(DockerHub hub, String username, String password) throws DockerServerException, NoServerPrefException;

    List<DockerImage> getAllImages() throws NoServerPrefException, DockerServerException;
    DockerImage getImageById(final String imageId) throws NotFoundException, DockerServerException, NoServerPrefException;
    void deleteImageById(String id, Boolean force) throws NoServerPrefException, DockerServerException;

    DockerImage pullImage(String name) throws NoServerPrefException, DockerServerException, NotFoundException;
    DockerImage pullImage(String name, DockerHub hub) throws NoServerPrefException, DockerServerException, NotFoundException;
    DockerImage pullImage(String name, DockerHub hub, String username, String password) throws NoServerPrefException, DockerServerException, NotFoundException;

    String createContainer(final ResolvedCommand dockerCommand) throws NoServerPrefException, DockerServerException, ContainerException;
    //    String createContainer(final String imageName, final List<String> runCommand, final List <String> volumes) throws NoServerPrefException, DockerServerException;
//    String createContainer(final DockerServer server, final String imageName,
//                       final List<String> runCommand, final List <String> volumes) throws DockerServerException;
//    String createContainer(final DockerServer server, final String imageName,
//                       final List<String> runCommand, final List <String> volumes,
//                       final List<String> environmentVariables) throws DockerServerException;
    void startContainer(final String containerId) throws NoServerPrefException, DockerServerException;

    List<Command> parseLabels(final String imageName)
            throws DockerServerException, NoServerPrefException, NotFoundException;

    List<ContainerMessage> getAllContainers() throws NoServerPrefException, DockerServerException;
    List<ContainerMessage> getContainers(final Map<String, String> params) throws NoServerPrefException, DockerServerException;
    ContainerMessage getContainer(final String id) throws NotFoundException, NoServerPrefException, DockerServerException;
    String getContainerStatus(final String id) throws NotFoundException, NoServerPrefException, DockerServerException;
    String getContainerStdoutLog(String id) throws NoServerPrefException, DockerServerException;
    String getContainerStderrLog(String id) throws NoServerPrefException, DockerServerException;

    List<DockerContainerEvent> getContainerEvents(final Date since, final Date until) throws NoServerPrefException, DockerServerException;
    List<DockerContainerEvent> getContainerEventsAndThrow(final Date since, final Date until) throws NoServerPrefException, DockerServerException;

    void killContainer(final String id) throws NoServerPrefException, DockerServerException, NotFoundException;
}
