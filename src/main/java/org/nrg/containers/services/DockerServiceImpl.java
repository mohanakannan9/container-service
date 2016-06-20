package org.nrg.containers.services;

import org.nrg.containers.exceptions.DockerServerException;
import org.nrg.containers.exceptions.NoServerPrefException;
import org.nrg.containers.exceptions.NotFoundException;
import org.nrg.containers.model.DockerHub;
import org.nrg.containers.model.DockerImage;
import org.nrg.containers.model.DockerServer;
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
    public DockerImage pullFromHub(final Long hubId, final String image)
            throws DockerServerException, NoServerPrefException, NotFoundException {
        final DockerHub hub = dockerHubService.retrieve(hubId);
        if (hub == null) {
            throw new NotFoundException("No Docker Hub with id " + hubId);
        }

        return controlApi.pullAndReturnImage(image, hub);
    }

    @Override
    public DockerImage pullFromHub(final String image)
            throws DockerServerException, NoServerPrefException {
        return controlApi.pullAndReturnImage(image);
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
