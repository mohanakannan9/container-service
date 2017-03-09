package org.nrg.containers.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerCertificates;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.DockerClient.LogsParam;
import com.spotify.docker.client.EventStream;
import com.spotify.docker.client.LogStream;
import com.spotify.docker.client.exceptions.ContainerNotFoundException;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.ContainerInfo;
import com.spotify.docker.client.messages.Event;
import com.spotify.docker.client.messages.HostConfig;
import com.spotify.docker.client.messages.Image;
import com.spotify.docker.client.messages.ImageInfo;
import com.spotify.docker.client.messages.PortBinding;
import com.spotify.docker.client.messages.RegistryAuth;
import org.apache.commons.lang3.StringUtils;
import org.nrg.containers.events.DockerContainerEvent;
import org.nrg.containers.exceptions.DockerServerException;
import org.nrg.containers.exceptions.NoServerPrefException;
import org.nrg.containers.helpers.CommandLabelHelper;
import org.nrg.containers.model.Container;
import org.nrg.containers.model.ContainerEntityMount;
import org.nrg.containers.model.DockerServer;
import org.nrg.containers.model.DockerServerPrefsBean;
import org.nrg.containers.model.ResolvedDockerCommand;
import org.nrg.containers.model.auto.Command;
import org.nrg.containers.model.auto.DockerHub;
import org.nrg.containers.model.auto.DockerImage;
import org.nrg.framework.exceptions.NotFoundException;
import org.nrg.framework.services.NrgEventService;
import org.nrg.prefs.exceptions.InvalidPreferenceName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.spotify.docker.client.DockerClient.EventsParam.since;
import static com.spotify.docker.client.DockerClient.EventsParam.type;
import static com.spotify.docker.client.DockerClient.EventsParam.until;
import static org.nrg.containers.helpers.CommandLabelHelper.LABEL_KEY;

@Service
public class DockerControlApi implements ContainerControlApi {
    private static final Logger log = LoggerFactory.getLogger(DockerControlApi.class);

    private DockerServerPrefsBean containerServerPref;
    private ObjectMapper objectMapper;
    private NrgEventService eventService;

    @Autowired
    public DockerControlApi(final DockerServerPrefsBean containerServerPref,
                            final ObjectMapper objectMapper,
                            final NrgEventService eventService) {
        this.containerServerPref = containerServerPref;
        this.objectMapper = objectMapper;
        this.eventService = eventService;
    }

    @Override
    @Nonnull
    public DockerServer getServer() throws NoServerPrefException {
        if (containerServerPref == null || containerServerPref.getHost() == null) {
            throw new NoServerPrefException("No container server URI defined in preferences.");
        }
        return containerServerPref.toDto();
    }

    @Override
    public void setServer(final String host) throws InvalidPreferenceName {
        setServer(host, null);
    }

    @Override
    @Nonnull
    public DockerServer setServer(final String host, final String certPath) throws InvalidPreferenceName {
        containerServerPref.setHost(host);
        containerServerPref.setCertPath(certPath);
        return containerServerPref.toDto();
    }

    @Override
    @Nonnull
    public DockerServer setServer(final DockerServer serverBean) throws InvalidPreferenceName {
        containerServerPref.setFromDto(serverBean);
        return serverBean;
    }

    @Override
    public String pingServer() throws NoServerPrefException, DockerServerException {
        try (final DockerClient client = getClient()) {
            return client.ping();
        } catch (DockerException | InterruptedException e) {
            log.error(e.getMessage());
            throw new DockerServerException(e);
        }
    }

    @Override
    public boolean canConnect() {
        try {
            final String pingResult = pingServer();
            return StringUtils.isNotBlank(pingResult) && pingResult.equals("OK");
        } catch (NoServerPrefException | DockerServerException ignored) {
            // Any actual errors have already been logged. We can safely ignore them here.
        }

        return false;
    }

    @Override
    @Nonnull
    public String pingHub(final @Nonnull DockerHub hub) throws DockerServerException, NoServerPrefException {
        return pingHub(hub, null, null);
    }

    @Override
    @Nonnull
    public String pingHub(final @Nonnull DockerHub hub, final @Nullable String username, final @Nullable String password)
            throws DockerServerException, NoServerPrefException {
        try (final DockerClient client = getClient()) {
            client.auth(registryAuth(hub, username, password));
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new DockerServerException(e);
        }
        return "OK";
    }

    @Nullable
    private RegistryAuth registryAuth(final @Nullable DockerHub hub, final @Nullable String username, final @Nullable String password) {
        if (hub == null) {
            return null;
        }
        return RegistryAuth.builder()
                .serverAddress(hub.url())
                .username(username == null ? "" : username)
                .password(password == null ? "" : password)
                .build();
    }

    /**
     * Query Docker server for all images
     *
     * @return Image objects stored on docker server
     **/
    @Override
    @Nonnull
    public List<DockerImage> getAllImages() throws NoServerPrefException, DockerServerException {
        return getImages(null);
    }

    /**
     * Query Docker server for images with parameters
     *
     * @param params Map of query parameters (name = value)
     * @return Image objects stored on docker server meeting the query parameters
     **/
    @Nonnull
    private List<DockerImage> getImages(final Map<String, String> params)
            throws NoServerPrefException, DockerServerException {
        return DockerImageToNrgImage(_getImages(params));
    }

    private List<com.spotify.docker.client.messages.Image> _getImages(final Map<String, String> params)
            throws NoServerPrefException, DockerServerException {
        // Transform param map to ListImagesParam array
        DockerClient.ListImagesParam[] dockerParams;
        if (params != null && params.size() > 0) {
            List<DockerClient.ListImagesParam> dockerParamsList =
                    Lists.transform(Lists.newArrayList(params.entrySet()), imageParamTransformer);
            dockerParams = dockerParamsList.toArray(new DockerClient.ListImagesParam[dockerParamsList.size()]);
        } else {
            dockerParams = new DockerClient.ListImagesParam[] {};
        }

        try (final DockerClient dockerClient = getClient()) {
            return dockerClient.listImages(dockerParams);
        } catch (DockerException | InterruptedException e) {
            log.error("Failed to list images. " + e.getMessage());
            throw new DockerServerException(e);
        } catch (Error e) {
            log.error("Failed to list images. " + e.getMessage());
            throw e;
        }
    }

    /**
     * Query Docker server for image by name
     *
     * @param imageId ID of image
     * @return Image stored on docker server with the given name
     **/
    @Override
    @Nonnull
    public DockerImage getImageById(final String imageId)
        throws NotFoundException, DockerServerException, NoServerPrefException {
        try (final DockerClient client = getClient()) {
            return getImageById(imageId, client);
        }
    }

    private DockerImage getImageById(final String imageId, final DockerClient client)
            throws NoServerPrefException, DockerServerException, NotFoundException {
        final DockerImage image = DockerImageToNrgImage(_getImageById(imageId, client));
        if (image != null) {
            return image;
        }
        throw new NotFoundException(String.format("Could not find image %s", imageId));
    }

    private com.spotify.docker.client.messages.ImageInfo _getImageById(final String imageId, final DockerClient client)
        throws DockerServerException, NoServerPrefException {
        try {
            return client.inspectImage(imageId);
        } catch (DockerException | InterruptedException e) {
            throw new DockerServerException(e);
        }
    }

    /**
     * Launch image on Docker server
     *
     * @param resolvedDockerCommand A ResolvedDockerCommand. All templates are resolved, all mount paths exist.
     * @return ID of created Container
     **/
    @Override
    public String createContainer(final ResolvedDockerCommand resolvedDockerCommand)
            throws NoServerPrefException, DockerServerException {

        final List<String> bindMounts = Lists.newArrayList();
        for (final ContainerEntityMount mount : resolvedDockerCommand.getMounts()) {
            bindMounts.add(mount.toBindMountString());
        }
        final List<String> environmentVariables = Lists.newArrayList();
        for (final Map.Entry<String, String> env : resolvedDockerCommand.getEnvironmentVariables().entrySet()) {
            environmentVariables.add(StringUtils.join(new String[] {env.getKey(), env.getValue()}, "="));
        }

        return createContainer(getServer(),
                resolvedDockerCommand.getImage(),
                resolvedDockerCommand.getCommandLine(),
                bindMounts,
                environmentVariables,
                resolvedDockerCommand.getPorts(),
                StringUtils.isNotBlank(resolvedDockerCommand.getWorkingDirectory()) ?
                        resolvedDockerCommand.getWorkingDirectory() :
                        null
        );
    }

    //    /**
    //     * Launch image on Docker server
    //     *
    //     * @param imageName name of image to launch
    //     * @param runCommand Command string to execute
    //     * @param volumes Volume mounts, in the form "/path/on/server:/path/in/container"
    //     * @return ID of created Container
    //     **/
    //    @Override
    //    public String createContainer(final String imageName, final List<String> runCommand, final List<String> volumes)
    //            throws NoServerPrefException, DockerServerException {
    //        return createContainer(getServer(), imageName, runCommand, volumes);
    //    }

    //    /**
    //     * Launch image on Docker server
    //     *
    //     * @param server DockerServer on which to launch
    //     * @param imageName name of image to launch
    //     * @param runCommand Command string list to execute
    //     * @param volumes Volume mounts, in the form "/path/on/server:/path/in/container"
    //     * @return ID of created Container
    //     **/
    //    @Override
    //    public String createContainer(final DockerServer server,
    //                              final String imageName,
    //                              final List<String> runCommand,
    //                              final List<String> volumes) throws DockerServerException {
    //        return createContainer(server, imageName, runCommand, volumes, null);
    //    }
    /**
     * Launch image on Docker server
     *
     * @param server DockerServer on which to launch
     * @param imageName name of image to launch
     * @param runCommand Command string list to execute
     * @param volumes Volume mounts, in the form "/path/on/server:/path/in/container"
     * @return ID of created Container
     **/
    private String createContainer(final DockerServer server,
                                   final String imageName,
                                   final String runCommand,
                                   final List<String> volumes,
                                   final List<String> environmentVariables,
                                   final Map<String, String> ports,
                                   final String workingDirectory)
            throws DockerServerException {

        final Map<String, List<PortBinding>> portBindings = Maps.newHashMap();
        final List<String> portStringList = Lists.newArrayList();
        if (!(ports == null || ports.isEmpty())) {
            for (final Map.Entry<String, String> portEntry : ports.entrySet()) {
                final String containerPort = portEntry.getKey();
                final String hostPort = portEntry.getValue();

                if (StringUtils.isNotBlank(containerPort) && StringUtils.isNotBlank(hostPort)) {
                    final PortBinding portBinding = PortBinding.create(null, hostPort);
                    portBindings.put(containerPort + "/tcp", Lists.newArrayList(portBinding));

                    portStringList.add("host" + hostPort + "->" + "container" + containerPort);
                } else if (StringUtils.isNotBlank(containerPort)) {
                    // hostPort is blank
                    // TODO log it
                } else if (StringUtils.isNotBlank(hostPort)) {
                    // containerPort is blank
                    // TODO log it
                } else {
                    // both are blank
                    // TODO log it
                }
            }
        }

        final HostConfig hostConfig =
                HostConfig.builder()
                        .binds(volumes)
                        .portBindings(portBindings)
                        .build();
        final ContainerConfig containerConfig =
                ContainerConfig.builder()
                        .hostConfig(hostConfig)
                        .image(imageName)
                        .attachStdout(true)
                        .attachStderr(true)
                        .cmd(Lists.newArrayList("/bin/sh", "-c", runCommand))
                        .env(environmentVariables)
                        .workingDir(workingDirectory)
                        .build();

        if (log.isDebugEnabled()) {
            final String message = String.format(
                    "Starting container:" +
                            "\n\tserver %s" +
                            "\n\timage %s" +
                            "\n\tcommand \"%s\"" +
                            "\n\tworking directory \"%s\"" +
                            "\n\tvolumes [%s]" +
                            "\n\tenvironment variables [%s]" +
                            "\n\texposed ports: {%s}",
                    server,
                    imageName,
                    runCommand,
                    workingDirectory,
                    StringUtils.join(volumes, ", "),
                    StringUtils.join(environmentVariables, ", "),
                    StringUtils.join(portStringList, ", ")
            );
            log.debug(message);
        }

        try (final DockerClient client = getClient(server)) {
            final ContainerCreation container = client.createContainer(containerConfig);

            log.info("Starting container: id " + container.id());

            final List<String> warnings = container.warnings();
            if (warnings != null) {
                for (String warning : warnings) {
                    log.warn(warning);
                }
            }
            // client.startContainer(container.id());

            return container.id();
        } catch (DockerException | InterruptedException e) {
            log.error(e.getMessage());
            throw new DockerServerException("Could not create container from image " + imageName, e);
        }
    }

    @Override
    public void startContainer(final String containerId) throws DockerServerException, NoServerPrefException {
        startContainer(containerId, getServer());
    }

    private void startContainer(final String containerId,
                                final DockerServer server) throws DockerServerException {
        try (final DockerClient client = getClient(server)) {
            client.startContainer(containerId);
        } catch (DockerException | InterruptedException e) {
            log.error(e.getMessage());
            throw new DockerServerException("Could not start container " + containerId, e);
        }
    }

    @Override
    public void deleteImageById(final String id, final Boolean force) throws NoServerPrefException, DockerServerException {
        try (final DockerClient dockerClient = getClient()) {
            dockerClient.removeImage(id, force, false);
        } catch (DockerException|InterruptedException e) {
            throw new DockerServerException(e);
        }
    }

    @Override
    @Nullable
    public DockerImage pullImage(final String name) throws NoServerPrefException, DockerServerException {
        return pullImage(name, null);
    }

    @Override
    @Nullable
    public DockerImage pullImage(final String name, final @Nullable DockerHub hub)
            throws NoServerPrefException, DockerServerException {
        return pullImage(name, hub, null, null);
    }

    @Override
    @Nullable
    public DockerImage pullImage(final String name, final @Nullable DockerHub hub, final @Nullable String username, final @Nullable String password) throws NoServerPrefException, DockerServerException {
        try (final DockerClient client = getClient()) {
            _pullImage(name, registryAuth(hub, username, password), client);
            return getImageById(name, client);
        } catch (NotFoundException e) {
            final String m = String.format("Image \"%s\" was not found", name);
            log.error(m);
            // throw new DockerServerException(m);
        }
        return null;
    }

    private void _pullImage(final @Nonnull String name, final @Nullable RegistryAuth registryAuth, final @Nonnull DockerClient client) throws DockerServerException {
        try {
            if (registryAuth == null) {
                client.pull(name);
            } else {
                client.pull(name, registryAuth);
            }
        }  catch (DockerException | InterruptedException e) {
            log.error(e.getMessage());
            throw new DockerServerException(e);
        }
    }

    @Override
    public List<Command> parseLabels(final String imageName)
            throws DockerServerException, NoServerPrefException, NotFoundException {
        final DockerImage image = getImageById(imageName);
        return CommandLabelHelper.parseLabels(imageName, image, objectMapper);
    }

    /**
     * Query Docker server for all containers
     *
     * @return Container objects stored on docker server
     **/
    @Override
    public List<Container> getAllContainers() throws NoServerPrefException, DockerServerException {
        return getContainers(null);
    }

    /**
     * Query Docker server for containers with parameters
     *
     * @param params Map of query parameters (name = value)
     * @return Container objects stored on docker server meeting the query parameters
     **/
    @Override
    public List<Container> getContainers(final Map<String, String> params)
        throws NoServerPrefException, DockerServerException {
        List<com.spotify.docker.client.messages.Container> containerList;

        // Transform param map to ListImagesParam array
        DockerClient.ListContainersParam[] dockerParams;
        if (params != null && params.size() > 0) {
            List<DockerClient.ListContainersParam> dockerParamsList =
                    Lists.transform(Lists.newArrayList(params.entrySet()), containerParamTransformer);
            dockerParams = dockerParamsList.toArray(new DockerClient.ListContainersParam[dockerParamsList.size()]);
        } else {
            dockerParams = new DockerClient.ListContainersParam[] {};
        }

        try (final DockerClient dockerClient = getClient()) {
            containerList = dockerClient.listContainers(dockerParams);
        } catch (DockerException | InterruptedException e) {
            log.error(e.getMessage());
            throw new DockerServerException(e);
        }
        return DockerContainerToNrgContainer(containerList);
    }

    /**
     * Query Docker server for specific container
     *
     * @param id Container ID
     * @return Container object with specified ID
     **/
    @Override
    public Container getContainer(final String id)
        throws NotFoundException, NoServerPrefException, DockerServerException {
        final Container container = DockerContainerToNrgContainer(_getContainer(id));
        if (container != null) {
            return container;
        }
        throw new NotFoundException(String.format("Could not find container %s", id));
    }

    private ContainerInfo _getContainer(final String id) throws NoServerPrefException, DockerServerException {
        final DockerClient client = getClient();
        try {
            return client.inspectContainer(id);
        } catch (DockerException | InterruptedException e) {
            log.error("Container server error." + e.getMessage());
            throw new DockerServerException(e);
        }
    }

    /**
     * Query Docker server for status of specific container
     *
     * @param id Container ID
     * @return Status of Container object with specified ID
     **/
    @Override
    public String getContainerStatus(final String id)
        throws NotFoundException, NoServerPrefException, DockerServerException {
        final Container container = getContainer(id);

        return container != null ? container.status() : null;
    }

    @Override
    public String getContainerStdoutLog(String id) throws NoServerPrefException, DockerServerException {
        try (final LogStream logStream = getClient().logs(id, LogsParam.stdout())) {
            return logStream.readFully();
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new DockerServerException(e);
        }
    }

    @Override
    public String getContainerStderrLog(String id) throws NoServerPrefException, DockerServerException {
        try (final LogStream logStream = getClient().logs(id, LogsParam.stderr())) {
            return logStream.readFully();
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new DockerServerException(e);
        }
    }

    @VisibleForTesting
    @Nonnull
    DockerClient getClient() throws NoServerPrefException {
        return getClient(getServer());
    }

    @Nonnull
    private DockerClient getClient(final @Nonnull DockerServer server) {

        DefaultDockerClient.Builder clientBuilder =
            DefaultDockerClient.builder()
                .uri(server.getHost());

        if (StringUtils.isNotBlank(server.getCertPath())) {
            try {
                final DockerCertificates certificates =
                    new DockerCertificates(Paths.get(server.getCertPath()));
                clientBuilder = clientBuilder.dockerCertificates(certificates);
            } catch (DockerCertificateException e) {
                log.error("Could not find docker certificates at " + server.getCertPath(), e);
            }
        }

        return clientBuilder.build();
    }

    @Override
    public List<DockerContainerEvent> getContainerEvents(final Date since, final Date until) throws NoServerPrefException, DockerServerException {
        final List<Event> dockerEventList = getDockerContainerEvents(since, until);

        final List<DockerContainerEvent> events = Lists.newArrayList();
        for (final Event dockerEvent : dockerEventList) {
            final Event.Actor dockerEventActor = dockerEvent.actor();
            final Map<String, String> attributes = Maps.newHashMap();
            if (dockerEventActor != null && dockerEventActor.attributes() != null) {
                attributes.putAll(dockerEventActor.attributes());
            }
            if (attributes.containsKey(LABEL_KEY)) {
                attributes.put(LABEL_KEY, "<elided>");
            }
            final DockerContainerEvent containerEvent =
                    new DockerContainerEvent(dockerEvent.action(),
                            dockerEventActor != null? dockerEventActor.id() : null,
                            dockerEvent.time(),
                            dockerEvent.timeNano(),
                            attributes);
            events.add(containerEvent);
        }
        return events;
    }

    @Override
    public List<DockerContainerEvent> getContainerEventsAndThrow(final Date since, final Date until) throws NoServerPrefException, DockerServerException {
        final List<DockerContainerEvent> events = getContainerEvents(since, until);

        for (final DockerContainerEvent event : events) {
            if (log.isDebugEnabled()) {
                log.debug("Throwing docker container event: " + event);
            }
            eventService.triggerEvent(event);
        }

        return events;
    }

    private List<Event> getDockerContainerEvents(final Date since, final Date until) throws NoServerPrefException, DockerServerException {
        try(final DockerClient client = getClient()) {
            if (log.isDebugEnabled()) {
                log.debug("Reading all docker container events from " + since.getTime() + " to " + until.getTime() + ".");
            }
            final List<Event> eventList;
            try (final EventStream eventStream =
                         client.events(since(since.getTime() / 1000),
                                 until(until.getTime() / 1000),
                                 type(Event.Type.CONTAINER))) {

                log.trace("Got a stream of docker events.");

                eventList = Lists.newArrayList(eventStream);
            }

            log.trace("Closed docker event stream.");

            return eventList;
        } catch (InterruptedException | DockerException e) {
            throw new DockerServerException(e);
        }
    }

    @Override
    public void killContainer(final String id) throws NoServerPrefException, DockerServerException, NotFoundException {
        try(final DockerClient client = getClient()) {
            log.info("Killing container " + id);
            client.killContainer(id);
        } catch (InterruptedException e) {
            log.error(e.getMessage());
            throw new DockerServerException(e);
        } catch (DockerException e) {
            if (e.getClass().isAssignableFrom(ContainerNotFoundException.class)) {
                log.error(e.getMessage());
                throw new NotFoundException(e);
            } else {
                log.error(e.getMessage());
                throw new DockerServerException(e);
            }
        }
    }

    // TODO Move everything below to a DAO class
    /**
     * Function to transform image query parameters from key/value to DockerClient.ListImagesParam
     **/
    private static Function<Map.Entry<String, String>, DockerClient.ListImagesParam> imageParamTransformer =
            new Function<Map.Entry<String, String>, DockerClient.ListImagesParam>() {
                @Override
                public DockerClient.ListImagesParam apply(Map.Entry<String, String> stringStringEntry) {
                    return new DockerClient.ListImagesParam(stringStringEntry.getKey(), stringStringEntry.getValue());
                }
            };

    /**
     * Function to transform container query parameters from key/value to DockerClient.ListContainersParam
     **/
    private static Function<Map.Entry<String, String>, DockerClient.ListContainersParam> containerParamTransformer =
            new Function<Map.Entry<String, String>, DockerClient.ListContainersParam>() {
                @Override
                public DockerClient.ListContainersParam apply(Map.Entry<String, String> stringStringEntry) {
                    return new DockerClient.ListContainersParam(stringStringEntry.getKey(), stringStringEntry.getValue());
                }
            };

    /**
     * Convert spotify-docker Image object to xnat-container Image object
     *
     * @param image Spotify-Docker Image object
     * @return NRG Image object
     **/
    private static DockerImage DockerImageToNrgImage(final Image image) {
        if (image == null) {
            return null;
        }

        return DockerImage.create(image.id(), image.repoTags(), image.labels());
    }

    /**
     * Convert spotify-docker Image object to xnat-container Image object
     *
     * @param image Spotify-Docker Image object
     * @return NRG Image object
     **/
    private static DockerImage DockerImageToNrgImage(final ImageInfo image) {
        if (image == null) {
            return null;
        }

        return DockerImage.create(image.id(), null, image.config().labels());
    }

    /**
     * Convert list of spotify-docker Image objects to list of xnat-container Image objects
     *
     * @param dockerImageList List of Spotify-Docker Image objects
     * @return List of NRG Image objects
     **/
    private static List<DockerImage> DockerImageToNrgImage(final List<Image> dockerImageList) {
        return Lists.transform(dockerImageList, DockerImageToNrgImage);
    }

    /**
     * Function to convert list of spotify-docker Image objects to list of xnat-container Image objects
     **/
    private static Function<Image, DockerImage> DockerImageToNrgImage =
            new Function<Image, DockerImage>() {
                @Override
                public DockerImage apply(final Image image) {
                    return DockerImageToNrgImage(image);
                }
            };

    /**
     * Convert spotify-docker Container object to xnat-container Container object
     *
     * @param dockerContainer Spotify-Docker Container object
     * @return NRG Container object
     **/
    private static Container DockerContainerToNrgContainer(final com.spotify.docker.client.messages.Container dockerContainer) {
        Container genericContainer = null;
        if (dockerContainer != null) {
            genericContainer =
                    new Container(dockerContainer.id(), dockerContainer.status());
        }
        return genericContainer;
    }

    /**
     * Convert spotify-docker Container object to xnat-container Container object
     *
     * @param dockerContainer Spotify-Docker ContainerInfo object
     * @return NRG Container object
     **/
    private static Container DockerContainerToNrgContainer(final com.spotify.docker.client.messages.ContainerInfo dockerContainer) {
        Container genericContainer = null;
        if (dockerContainer != null) {
            genericContainer =
                    new Container(
                            dockerContainer.id(),
                            dockerContainer.state().running() ? "Running" :
                                dockerContainer.state().paused() ? "Paused" :
                                dockerContainer.state().restarting() ? "Restarting" :
                                dockerContainer.state().exitCode() != null ? "Exited" :
                                null
                    );
        }
        return genericContainer;
    }

    /**
     * Convert list of spotify-docker Container objects to list of xnat-container Container objects
     *
     * @param dockerContainerList List of Spotify-Docker Container objects
     * @return List of NRG Container objects
     **/
    private static List<Container> DockerContainerToNrgContainer(final List<com.spotify.docker.client.messages.Container> dockerContainerList) {
        return Lists.transform(dockerContainerList, DockerContainerToNrgContainer);
    }

    /**
     * Function to convert list of spotify-docker Container objects to xnat-container Container objects
     **/
    private static Function<com.spotify.docker.client.messages.Container, Container> DockerContainerToNrgContainer =
            new Function<com.spotify.docker.client.messages.Container, Container>() {
                @Override
                public Container apply(com.spotify.docker.client.messages.Container container) {
                    return DockerContainerToNrgContainer(container);
                }
            };

}