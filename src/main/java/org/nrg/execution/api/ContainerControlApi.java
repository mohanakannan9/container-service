package org.nrg.execution.api;

import org.nrg.execution.events.DockerContainerEvent;
import org.nrg.execution.exceptions.DockerServerException;
import org.nrg.execution.exceptions.NoServerPrefException;
import org.nrg.execution.exceptions.NotFoundException;
import org.nrg.execution.model.Command;
import org.nrg.execution.model.Container;
import org.nrg.execution.model.ContainerExecution;
import org.nrg.execution.model.DockerHub;
import org.nrg.execution.model.DockerImage;
import org.nrg.execution.model.DockerServer;
import org.nrg.execution.model.ResolvedCommand;
import org.nrg.prefs.exceptions.InvalidPreferenceName;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface ContainerControlApi {
    String LABEL_KEY = "org.nrg.commands";

    DockerServer getServer() throws NoServerPrefException;
    DockerServer setServer(String host, String certPath) throws InvalidPreferenceName;
    DockerServer setServer(DockerServer server) throws InvalidPreferenceName;
    void setServer(String host) throws InvalidPreferenceName;
    String pingServer() throws NoServerPrefException, DockerServerException;

    String pingHub(DockerHub hub) throws DockerServerException, NoServerPrefException;

    List<DockerImage> getAllImages() throws NoServerPrefException, DockerServerException;
    DockerImage getImageById(final String imageId) throws NotFoundException, DockerServerException, NoServerPrefException;
    void deleteImageById(String id, Boolean force) throws NoServerPrefException, DockerServerException;
    void pullImage(String name) throws NoServerPrefException, DockerServerException;
    void pullImage(String name, DockerHub hub) throws NoServerPrefException, DockerServerException;
    DockerImage pullAndReturnImage(String name) throws NoServerPrefException, DockerServerException;
    DockerImage pullAndReturnImage(String name, DockerHub hub) throws NoServerPrefException, DockerServerException;

    ContainerExecution launchImage(final ResolvedCommand command) throws NoServerPrefException, DockerServerException;
//    String launchImage(final String imageName, final List<String> runCommand, final List <String> volumes) throws NoServerPrefException, DockerServerException;
//    String launchImage(final DockerServer server, final String imageName,
//                       final List<String> runCommand, final List <String> volumes) throws DockerServerException;
//    String launchImage(final DockerServer server, final String imageName,
//                       final List<String> runCommand, final List <String> volumes,
//                       final List<String> environmentVariables) throws DockerServerException;

    List<Command> parseLabels(final String imageId)
            throws DockerServerException, NoServerPrefException, NotFoundException;
    List<Command> parseLabels(final DockerImage dockerImage);

    List<Container> getAllContainers() throws NoServerPrefException, DockerServerException;
    List<Container> getContainers(final Map<String, String> params) throws NoServerPrefException, DockerServerException;
    Container getContainer(final String id) throws NotFoundException, NoServerPrefException, DockerServerException;
    String getContainerStatus(final String id) throws NotFoundException, NoServerPrefException, DockerServerException;
    String getContainerLogs(String id) throws NoServerPrefException, DockerServerException;

    List<DockerContainerEvent> getContainerEvents(final Date since, final Date until) throws NoServerPrefException, DockerServerException;
    List<DockerContainerEvent> getContainerEventsAndThrow(final Date since, final Date until) throws NoServerPrefException, DockerServerException;
}
