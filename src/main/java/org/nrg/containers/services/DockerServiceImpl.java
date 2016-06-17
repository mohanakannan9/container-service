package org.nrg.containers.services;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.nrg.execution.api.ContainerControlApi;
import org.nrg.containers.exceptions.DockerServerException;
import org.nrg.containers.exceptions.NoServerPrefException;
import org.nrg.containers.exceptions.NotFoundException;
import org.nrg.containers.model.DockerHub;
import org.nrg.containers.model.DockerImage;
import org.nrg.containers.model.DockerImageDto;
import org.nrg.containers.model.DockerServer;
import org.nrg.prefs.exceptions.InvalidPreferenceName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@Transactional
public class DockerServiceImpl implements DockerService {
    @Autowired
    private ContainerControlApi controlApi;

    @Autowired
    private DockerImageService imageService;

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
    public DockerImageDto pullFromHub(Long hubId, String image)
            throws DockerServerException, NoServerPrefException, NotFoundException {
        final DockerHub hub = dockerHubService.retrieve(hubId);
        if (hub == null) {
            throw new NotFoundException("No Docker Hub with id " + hubId);
        }

        return controlApi.pullAndReturnImage(image, hub);
    }

    @Override
    public DockerImageDto pullFromHub(String image)
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
    public List<DockerImageDto> getImages(final Boolean fromDb, final Boolean fromDockerServer)
            throws NoServerPrefException, DockerServerException {
        if (!fromDb && !fromDockerServer) {
            return null;
        }

        final List<DockerImage> dbList = fromDb ? imageService.getAll() : Lists.<DockerImage>newArrayList();

        final List<DockerImageDto> dockerServerList = fromDockerServer ?
                controlApi.getAllImages() :
                Lists.<DockerImageDto>newArrayList();

        return combine(dbList, dockerServerList);
    }

    private List<DockerImageDto> combine(final List<DockerImage> dbList, final List<DockerImageDto> dockerServerList) {
        final Map<String, DockerImageDto> combined = Maps.newHashMap();
        for (final DockerImage dockerImage : dbList) {
            if (dockerImage != null) {
                combined.put(dockerImage.getImageId(), DockerImageDto.fromDbImage(dockerImage));
            }
        }

        for (final DockerImageDto dockerImageDto : dockerServerList) {
            if (dockerImageDto != null) {
                if (!combined.containsKey(dockerImageDto.getImageId())) {
                    dockerImageDto.setInDatabase(false);
                    combined.put(dockerImageDto.getImageId(), dockerImageDto);
                } else {
                    combined.get(dockerImageDto.getImageId()).setOnDockerServer(true);
                }
            }
        }

        return Lists.newArrayList(combined.values());
    }

    public DockerImageDto getImage(final Long id, final Boolean fromDockerServer) throws NotFoundException {
        // We have an image from the database, and we need to know whether it is
        // present on the docker server.
        final DockerImage dbImage = imageService.getByDbId(id);

        if (!fromDockerServer) {
            // The user does not want us to check the docker server
            return DockerImageDto.fromDbImage(dbImage, null);
        }

        if (StringUtils.isBlank(dbImage.getImageId())) {
            // We can't check whether the image is on the server or not
            return DockerImageDto.fromDbImage(dbImage, null);
        }

        try {
            final DockerImageDto dockerServerImage = controlApi.getImageById(dbImage.getImageId());

            // If the image on the server is not null, we can say it is there
            return DockerImageDto.fromDbImage(dbImage, dockerServerImage != null);
        } catch (DockerServerException | NoServerPrefException e) {
            // We can't verify if the image is on the docker server or not
            return DockerImageDto.fromDbImage(dbImage, null);
        } catch (NotFoundException e) {
            // The image is not on the docker server
            return DockerImageDto.fromDbImage(dbImage, false);
        }
    }

    @Transactional
    public void removeImage(final Long id, final Boolean fromDockerServer)
            throws NotFoundException, NoServerPrefException, DockerServerException {
        final DockerImage dbImage = imageService.getByDbId(id);
        final String imageId = dbImage.getImageId();

        if (fromDockerServer) {
            controlApi.deleteImageById(imageId);
        }

        imageService.delete(dbImage);
    }

    public DockerImageDto createImage(final DockerImageDto dtoRequestIn)
            throws DockerServerException, NoServerPrefException, NotFoundException {

        final String imageId = dtoRequestIn.getImageId();
        final DockerImageDto retrievedFromServer = controlApi.getImageById(imageId);

        String name = "";
        if (StringUtils.isNotBlank(dtoRequestIn.getName())) {
            name = dtoRequestIn.getName();
        } else if (!retrievedFromServer.getRepoTags().isEmpty()) {
            for (final String tag : retrievedFromServer.getRepoTags()) {
                if (StringUtils.isNotBlank(tag)) {
                    name = tag;
                    break;
                }
            }
        }

        if (StringUtils.isBlank(name)) {
            name = imageId;
        }

        retrievedFromServer.setName(name);

        return imageService.create(retrievedFromServer, true);
    }
}
