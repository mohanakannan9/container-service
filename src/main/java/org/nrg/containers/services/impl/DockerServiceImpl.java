package org.nrg.containers.services.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.nrg.containers.exceptions.DockerServerException;
import org.nrg.containers.exceptions.NoServerPrefException;
import org.nrg.containers.exceptions.NotFoundException;
import org.nrg.containers.helpers.CommandLabelHelper;
import org.nrg.containers.model.Command;
import org.nrg.containers.model.auto.DockerImage;
import org.nrg.containers.exceptions.NotUniqueException;
import org.nrg.containers.model.DockerHubEntity;
import org.nrg.containers.model.DockerServerPrefsBean;
import org.nrg.containers.model.auto.CommandPojo;
import org.nrg.containers.model.auto.DockerHub;
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
    public List<DockerHubEntity> getHubs() {
        return dockerHubService.getAll();
    }

    @Override
    public DockerHubEntity setHub(final DockerHubEntity hub)  {
        return dockerHubService.create(hub);
    }

    @Override
    public String pingHub(final Long hubId) throws DockerServerException, NoServerPrefException, NotFoundException {
        final DockerHub hub = dockerHubService.getHub(hubId);
        return pingHub(hub);
    }

    @Override
    public String pingHub(final String hubName) throws DockerServerException, NoServerPrefException, NotUniqueException {
        final DockerHub hub = dockerHubService.getHub(hubName);

        return pingHub(hub);
    }

    private String pingHub(final DockerHub hub) throws DockerServerException, NoServerPrefException {
        return controlApi.pingHub(hub);
    }

    @Override
    public DockerImage pullFromHub(final Long hubId, final String imageName, final boolean saveCommands)
            throws DockerServerException, NoServerPrefException, NotFoundException {
        final DockerHub hub = dockerHubService.getHub(hubId);
        final DockerImage dockerImage = controlApi.pullAndReturnImage(imageName, hub);
        if (saveCommands) {
            saveFromImageLabels(imageName, dockerImage);
        }
        return dockerImage;
    }

    @Override
    public DockerImage pullFromHub(final String imageName, final boolean saveCommands)
            throws DockerServerException, NoServerPrefException {
        // TODO migrate this to use pullfromHub(defaultHubId, imageName, saveCommands)
        final DockerImage dockerImage = controlApi.pullAndReturnImage(imageName);
        if (saveCommands) {
            saveFromImageLabels(imageName, dockerImage);
        }
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
        final Map<String, DockerImage> imagesByIdUniqueValues = Maps.newHashMap();
        final Map<String, String> imageIdsByNameDuplicateValues = Maps.newHashMap();

        // Store the summaries indexed by image id
        final Map<String, DockerImageAndCommandSummary> imageSummariesByImageId = Maps.newHashMap();
        for (final DockerImage image : rawImages) {

            if (image.tags() != null && !image.tags().isEmpty()) {
                for (final String tag : image.tags()) {
                    imageIdsByNameDuplicateValues.put(tag, image.imageId());
                }
            }

            if (StringUtils.isNotBlank(image.imageId())) {
                imagesByIdUniqueValues.put(image.imageId(), image);

                imageSummariesByImageId.put(image.imageId(), DockerImageAndCommandSummary.create(image, server));
            }
        }

        // final Map<DockerImage, DockerImageAndCommandSummary> imageToImageSummaryMap = Maps.newHashMap();

        // Go through all commands, update the images with command info (or add new images if we can't find them)
        final List<Command> commands = commandService.getAll();
        if (commands != null) {
            for (final Command command : commands) {
                final String imageNameUsedByTheCommand = command.getImage();
                if (StringUtils.isNotBlank(imageNameUsedByTheCommand)) {
                    if (imageIdsByNameDuplicateValues.containsKey(imageNameUsedByTheCommand)) {

                        // We do recognize the image by this name, so either make a new summary for it or add this command to an existing summary
                        final String dockerImageId = imageIdsByNameDuplicateValues.get(imageNameUsedByTheCommand);
                        imageSummariesByImageId.get(dockerImageId).addOrUpdateCommand(command);

                    } else {
                        // the command refers to some image that either
                        //   A. we have not cached by that name, or
                        //   B. does not exist on the docker server
                        DockerImage dockerImage = null;
                        try {
                            dockerImage = controlApi.getImageById(imageNameUsedByTheCommand);
                        } catch (NotFoundException ignored) {
                            // ignored
                        }

                        if (dockerImage != null) {
                            // This means A: we have the image on the server, just not by this name
                            final String dockerImageId = dockerImage.imageId();

                            imageIdsByNameDuplicateValues.put(imageNameUsedByTheCommand, dockerImageId);

                            final DockerImageAndCommandSummary summary = imageSummariesByImageId.get(dockerImageId);
                            summary.addOrUpdateCommand(command);
                        } else {
                            // This means B: the command refers to some image that we do not have on the docker server
                            // Create a placeholder image summary object
                            final DockerImageAndCommandSummary summary = DockerImageAndCommandSummary.create(command);

                            imageIdsByNameDuplicateValues.put(imageNameUsedByTheCommand, summary.imageId());
                            imageSummariesByImageId.put(summary.imageId(), summary);
                        }
                    }
                } else {
                    // TODO command does not refer to an image? Should not be possible...
                }
            }
        }

        return Lists.newArrayList(imageSummariesByImageId.values());
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
    public List<Command> saveFromImageLabels(final String imageName) throws DockerServerException, NotFoundException, NoServerPrefException {
        return saveFromImageLabels(imageName, controlApi.getImageById(imageName));
    }

    private List<Command> saveFromImageLabels(final String imageName, final DockerImage dockerImage) {
        if (log.isDebugEnabled()) {
            log.debug("Parsing labels for " + imageName);
        }
        final List<CommandPojo> parsed = CommandLabelHelper.parseLabels(imageName, dockerImage);
        if (log.isDebugEnabled()) {
            log.debug("Saving commands from image labels");
        }
        return commandService.save(parsed);
    }
}
