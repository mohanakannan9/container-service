package org.nrg.execution.services;

import org.nrg.execution.exceptions.DockerServerException;
import org.nrg.execution.exceptions.NoServerPrefException;
import org.nrg.execution.exceptions.NotFoundException;
import org.nrg.execution.model.Command;
import org.nrg.execution.model.DockerHub;
import org.nrg.execution.model.DockerImage;
import org.nrg.execution.model.DockerServer;
import org.nrg.execution.api.ContainerControlApi;
import org.nrg.prefs.exceptions.InvalidPreferenceName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class DockerServiceImpl implements DockerService {
    @Autowired
    private ContainerControlApi controlApi;

    @Autowired
    private DockerHubService dockerHubService;

    @Autowired
    private CommandService commandService;

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
        commandService.saveFromLabels(dockerImage.getLabels());
        return dockerImage;
    }

    @Override
    public DockerImage pullFromHub(final String image, final Boolean saveCommands)
            throws DockerServerException, NoServerPrefException {
        final DockerImage dockerImage = controlApi.pullAndReturnImage(image);
        commandService.saveFromLabels(dockerImage.getLabels());
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
}
