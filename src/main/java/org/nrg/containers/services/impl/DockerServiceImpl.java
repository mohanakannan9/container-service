package org.nrg.containers.services.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.nrg.containers.exceptions.DockerServerException;
import org.nrg.containers.exceptions.NoServerPrefException;
import org.nrg.containers.exceptions.NotFoundException;
import org.nrg.containers.model.Command;
import org.nrg.containers.model.DockerHub;
import org.nrg.containers.model.DockerImage;
import org.nrg.containers.model.DockerServerPrefsBean;
import org.nrg.containers.model.auto.DockerImageAndCommandSummary;
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

import java.util.List;
import java.util.Map;

@Service
public class DockerServiceImpl implements DockerService {
    private static final Logger log = LoggerFactory.getLogger(DockerService.class);

    private ContainerControlApi controlApi;
    private DockerHubService dockerHubService;
    private CommandService commandService;
    private DockerServerPrefsBean dockerServerPrefsBean;

    @Autowired
    public DockerServiceImpl(final ContainerControlApi controlApi,
                             final DockerHubService dockerHubService,
                             final CommandService commandService,
                             final DockerServerPrefsBean dockerServerPrefsBean) {
        this.controlApi = controlApi;
        this.dockerHubService = dockerHubService;
        this.commandService = commandService;
        this.dockerServerPrefsBean = dockerServerPrefsBean;
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

    @Override
    public List<DockerImageAndCommandSummary> getImageSummaries()
            throws NoServerPrefException, DockerServerException {
        // TODO once I have multiple docker servers, I will have to go ask all of them for their images
        final String server = dockerServerPrefsBean.getName();

        final List<DockerImage> rawImages = controlApi.getAllImages();

        // Store the images by every name that someone might call them: all tags and id
        final Map<String, DockerImage> imagesByName = Maps.newHashMap();
        for (final DockerImage image : rawImages) {
            // final DockerImageWithCommandSummary imageWithCommandSummary = new DockerImageWithCommandSummary(image);
            if (image.getTags() != null && !image.getTags().isEmpty()) {
                for (final String tag : image.getTags()) {
                    imagesByName.put(tag, image);
                }
            }

            if (StringUtils.isNotBlank(image.getImageId())) {
                imagesByName.put(image.getImageId(), image);
            }
        }

        // And store the summaries indexed by image
        final Map<DockerImage, DockerImageAndCommandSummary> imageToImageSummaryMap = Maps.newHashMap();
        final List<Command> commands = commandService.getAll();
        if (commands != null) {
            for (final Command command : commands) {
                final String imageNameUsedByTheCommand = command.getImage();
                if (StringUtils.isNotBlank(imageNameUsedByTheCommand)) {
                    final DockerImage dockerImage = imagesByName.get(imageNameUsedByTheCommand);
                    if (dockerImage == null) {
                        // the command refers to some image we do not have
                        // Make a placeholder image
                        final DockerImage unknownImage = new DockerImage();
                        unknownImage.addTag(imageNameUsedByTheCommand);
                        imagesByName.put(imageNameUsedByTheCommand, unknownImage);

                        // Now add the placeholder to the summary list, with server = null
                        imageToImageSummaryMap.put(unknownImage, DockerImageAndCommandSummary.create(command, null));
                    } else {
                        // We do have the image, so either make a new summary for it or add this command to an existing summary
                        if (imageToImageSummaryMap.containsKey(dockerImage)) {
                            imageToImageSummaryMap.get(dockerImage).addCommandSummary(command);
                        } else {
                            imageToImageSummaryMap.put(dockerImage, DockerImageAndCommandSummary.create(command, server));
                        }
                    }
                }
            }
        }

        return Lists.newArrayList(imageToImageSummaryMap.values());
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
