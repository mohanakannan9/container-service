package org.nrg.containers.services;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.nrg.containers.api.ContainerControlApi;
import org.nrg.containers.exceptions.DockerServerException;
import org.nrg.containers.exceptions.NoServerPrefException;
import org.nrg.containers.exceptions.NotFoundException;
import org.nrg.containers.model.DockerHub;
import org.nrg.containers.model.DockerHubPrefs;
import org.nrg.containers.model.DockerImage;
import org.nrg.containers.model.DockerImageDto;
import org.nrg.containers.model.DockerServer;
import org.nrg.framework.exceptions.NrgServiceRuntimeException;
import org.nrg.prefs.exceptions.InvalidPreferenceName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class DockerServiceImpl implements DockerService {
    @Autowired
    private ContainerControlApi controlApi;

    @Autowired
    private DockerImageService imageService;

    @Autowired
    private DockerHubPrefs dockerHubPrefs;

    @Override
    public List<DockerHub> getHubs() {
        return dockerHubPrefs.getDockerHubs();
    }

    @Override
    public void setHub(final DockerHub hub) throws NrgServiceRuntimeException {
        dockerHubPrefs.setDockerHub(hub);
    }

    @Override
    public String pingHub(final DockerHub hub) throws DockerServerException, NoServerPrefException {
        return controlApi.pingHub(hub);
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
    public void setServer(final DockerServer server) throws InvalidPreferenceName {
        controlApi.setServer(server);
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
        final DockerImage dbImage = imageService.retrieve(id);
        if (dbImage == null) {
            throw new NotFoundException("No image with id "+id);
        }

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

    public void createImage(final DockerImageDto dtoRequestIn)
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

        imageService.create(retrievedFromServer);
    }
}
