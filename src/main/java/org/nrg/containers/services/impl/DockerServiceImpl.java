package org.nrg.containers.services.impl;

import org.nrg.containers.exceptions.DockerServerException;
import org.nrg.containers.exceptions.NoServerPrefException;
import org.nrg.containers.exceptions.NotFoundException;
import org.nrg.containers.model.Command;
import org.nrg.containers.model.DockerCommand;
import org.nrg.containers.model.DockerHub;
import org.nrg.containers.model.DockerImage;
import org.nrg.containers.model.DockerServer;
import org.nrg.containers.api.ContainerControlApi;
import org.nrg.containers.services.CommandService;
import org.nrg.containers.services.DockerHubService;
import org.nrg.containers.services.DockerService;
import org.nrg.prefs.exceptions.InvalidPreferenceName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DockerServiceImpl implements DockerService {
    private static final Logger log = LoggerFactory.getLogger(DockerService.class);

    private ContainerControlApi controlApi;
    private DockerHubService dockerHubService;
    private CommandService commandService;

    @Autowired
    public DockerServiceImpl(final ContainerControlApi controlApi,
                             final DockerHubService dockerHubService,
                             final CommandService commandService) {
        this.controlApi = controlApi;
        this.dockerHubService = dockerHubService;
        this.commandService = commandService;
    }

    @Override
    public List<DockerHub> getHubs() {
        return dockerHubService.getAll();
    }

    @Override
    public DockerHub setHub(final DockerHub hub)  {
        return dockerHubService.create(hub);
    }

    @Override
    public String pingHub(final Long hubId) throws DockerServerException, NoServerPrefException, NotFoundException {
        final DockerHub hub = dockerHubService.retrieve(hubId);
        if (hub == null) {
            throw new NotFoundException(String.format("Hub with it %d not found", hubId));
        }
        return pingHub(hub);
    }

    @Override
    public String pingHub(final DockerHub hub) throws DockerServerException, NoServerPrefException {
        return controlApi.pingHub(hub);
    }

    @Override
    public DockerImage pullFromHub(final Long hubId, final String image, final Boolean saveCommands)
            throws DockerServerException, NoServerPrefException, NotFoundException {
        final DockerHub hub = dockerHubService.retrieve(hubId);
        if (hub == null) {
            throw new NotFoundException("No Docker Hub with id " + hubId);
        }

        final DockerImage dockerImage = controlApi.pullAndReturnImage(image, hub);
        saveFromImageLabels(dockerImage);
        return dockerImage;
    }

    @Override
    public DockerImage pullFromHub(final String image, final Boolean saveCommands)
            throws DockerServerException, NoServerPrefException {
        final DockerImage dockerImage = controlApi.pullAndReturnImage(image);
        saveFromImageLabels(dockerImage);
        return dockerImage;
    }

    @Override
    public DockerServer getServer() throws NotFoundException {
        try {
            return controlApi.getServer();
        } catch (NoServerPrefException e) {
            throw new NotFoundException(e);
        }
    }

    @Override
    public DockerServer setServer(final DockerServer server) throws InvalidPreferenceName {
        return controlApi.setServer(server);
    }

    @Override
    public String pingServer() throws NoServerPrefException, DockerServerException {
        return controlApi.pingServer();
    }

    @Override
    public List<DockerImage> getImages()
            throws NoServerPrefException, DockerServerException {
        return controlApi.getAllImages();
    }

    public DockerImage getImage(final String imageId)
            throws NoServerPrefException, NotFoundException {
        try {
            return controlApi.getImageById(imageId);
        } catch (DockerServerException e) {
            throw new NotFoundException(e);
        }
    }

    public void removeImage(final String imageId, final Boolean force)
            throws NoServerPrefException, DockerServerException {
        controlApi.deleteImageById(imageId, force);
    }

    @Override
    public List<Command> saveFromImageLabels(final String imageId) throws DockerServerException, NotFoundException, NoServerPrefException {
        if (log.isDebugEnabled()) {
            log.debug("Parsing labels for " + imageId);
        }
        final List<Command> parsed = controlApi.parseLabels(imageId);

        if (log.isDebugEnabled()) {
            log.debug("Saving commands from image labels");
        }
        return commandService.save(parsed);
    }

    @Override
    public List<Command> saveFromImageLabels(final DockerImage dockerImage) {
//        commandService.saveFromLabels(imageId);
        final List<Command> parsed = controlApi.parseLabels(dockerImage);
        return commandService.save(parsed);
    }
}
