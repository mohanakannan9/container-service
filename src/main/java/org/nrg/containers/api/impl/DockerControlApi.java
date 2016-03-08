package org.nrg.containers.api.impl;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.DockerException;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.ContainerInfo;
import com.spotify.docker.client.messages.HostConfig;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.nrg.containers.api.ContainerControlApi;
import org.nrg.containers.exceptions.ContainerServerException;
import org.nrg.containers.exceptions.NotFoundException;
import org.nrg.containers.model.Container;
import org.nrg.containers.model.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

@Service
public class DockerControlApi implements ContainerControlApi {
    private static final Logger _log = LoggerFactory.getLogger(DockerControlApi.class);

    /**
     * Query Docker server for all images
     *
     * @param server Server URI
     * @return Image objects stored on docker server
     **/
    public List<org.nrg.containers.model.Image> getAllImages(final String server) {
        return getImages(server, null);
    }


    /**
     * Query Docker server for images with parameters
     *
     * @param server Server URI
     * @param params Map of query parameters (name = value)
     * @return Image objects stored on docker server meeting the query parameters
     **/
    public List<Image> getImages(final String server, final Map<String, String> params) {
        if (_log.isDebugEnabled()) {
            _log.debug("method getImages server "+ server + "; params "+ params);
        }
        return DockerImageToNrgImage(_getImages(server, params));
    }

    private static List<com.spotify.docker.client.messages.Image> _getImages(final String server, final Map<String, String> params) {
        final DockerClient dockerClient = getClient(server);

        // Transform param map to ListImagesParam array
        DockerClient.ListImagesParam[] dockerParams;
        if (params != null && params.size() > 0) {
            List<DockerClient.ListImagesParam> dockerParamsList =
                    Lists.transform(Lists.newArrayList(params.entrySet()), imageParamTransformer);
            dockerParams = dockerParamsList.toArray(new DockerClient.ListImagesParam[dockerParamsList.size()]);
        } else {
            dockerParams = new DockerClient.ListImagesParam[] {};
        }

        try {
            return dockerClient.listImages(dockerParams);
        } catch (DockerException | InterruptedException e) {
            _log.error("Failed to list images. "+e.getMessage());
        } catch (Error e) {
            _log.error("Failed to list images. "+e.getMessage());
            throw e;
        }
        return null;
    }

    /**
     * Query Docker server for image by name
     *
     * @param server Server URI
     * @param imageName Name of image
     * @return Image stored on docker server with the given name
     **/
    public Image getImageByName(final String server, final String imageName) throws ContainerServerException, NotFoundException {
        if (_log.isDebugEnabled()) {
            _log.debug("method getImages server "+ server + "; imageName "+ imageName);
        }
        final Image image = DockerImageToNrgImage(_getImageByName(server, imageName));
        if (image != null) {
            return image;
        }
        throw new NotFoundException(String.format("Could not find image %s on server %s", imageName, server));
    }

    private static com.spotify.docker.client.messages.Image _getImageByName(final String server, final String imageName) throws ContainerServerException {
        final DockerClient client = getClient(server);

        List<com.spotify.docker.client.messages.Image> images = null;
        try {
            images = client.listImages(DockerClient.ListImagesParam.byName(imageName));
        } catch (DockerException | InterruptedException e) {
            throw new ContainerServerException(e);
        }

        if (images != null && !images.isEmpty()) {
            if (images.size() > 1) {
                String warn = "Found multiple images with name "+imageName + ": ";
                for (final com.spotify.docker.client.messages.Image image : images) {
                    warn += image.id() + " ";
                }
                warn += ". Returning "+images.get(0).id()+".";

                _log.warn(warn);
            }
            return images.get(0);
        }
        return null;
    }

    /**
     * Query Docker server for image by name
     *
     * @param server Server URI
     * @param imageId ID of image
     * @return Image stored on docker server with the given name
     **/
    public Image getImageById(final String server, final String imageId) throws NotFoundException, ContainerServerException {
        if (_log.isDebugEnabled()) {
            _log.debug("method getImages server "+ server + "; imageId "+ imageId);
        }
        final Image image = DockerImageToNrgImage(_getImageById(server, imageId));
        if (image != null) {
            return image;
        }
        throw new NotFoundException(String.format("Could not find image %s on server %s", imageId, server));
    }

    private static com.spotify.docker.client.messages.Image _getImageById(final String server, final String imageId) throws ContainerServerException {
//        TODO: Make this work
        final DockerClient client = getClient(server);

        List<com.spotify.docker.client.messages.Image> images;
        try {
            images = client.listImages(DockerClient.ListImagesParam.byName(imageId));
        } catch (DockerException | InterruptedException e) {
            throw new ContainerServerException(e);
        }

        if (images != null && !images.isEmpty()) {
            if (images.size() > 1) {
                String warn = "Found multiple images with name "+ imageId + ": ";
                for (final com.spotify.docker.client.messages.Image image : images) {
                    warn += image.id() + " ";
                }
                warn += ". Returning "+images.get(0).id()+".";

                _log.warn(warn);
            }
            return images.get(0);
        }
        return null;
    }

    /**
     * Query Docker server for all containers
     *
     * @param server Server URI
     * @return Container objects stored on docker server
     **/
    public List<Container> getAllContainers(final String server) {
        return getContainers(server, null);
    }

    /**
     * Query Docker server for containers with parameters
     *
     * @param server Server URI
     * @param params Map of query parameters (name = value)
     * @return Container objects stored on docker server meeting the query parameters
     **/
    public List<Container> getContainers(final String server, final Map<String, String> params) {
        if (_log.isDebugEnabled()) {
            _log.debug("method getContainers server "+ server + "; params "+ params);
        }

        DockerClient dockerClient = getClient(server);
        List<com.spotify.docker.client.messages.Container> containerList = null;

        // Transform param map to ListImagesParam array
        DockerClient.ListContainersParam[] dockerParams;
        if (params != null && params.size() > 0) {
            List<DockerClient.ListContainersParam> dockerParamsList =
                    Lists.transform(Lists.newArrayList(params.entrySet()), containerParamTransformer);
            dockerParams = dockerParamsList.toArray(new DockerClient.ListContainersParam[dockerParamsList.size()]);
        } else {
            dockerParams = new DockerClient.ListContainersParam[] {};
        }

        try {
            containerList = dockerClient.listContainers(dockerParams);
        } catch (DockerException | InterruptedException e) {
            _log.error(e.getMessage());
        }
        return DockerContainerToNrgContainer(containerList);
    }

    /**
     * Query Docker server for specific container
     *
     * @param server Server URI
     * @param id Container ID
     * @return Container object with specified ID
     **/
    public Container getContainer(final String server, final String id) throws NotFoundException {
        final Container container = DockerContainerToNrgContainer(_getContainer(server, id));
        if (container != null) {
            return container;
        }
        throw new NotFoundException(String.format("Could not find container %s on server %s", id, server));
    }

    private static ContainerInfo _getContainer(final String server, final String id) {
        final DockerClient client = getClient(server);
        try {
            return client.inspectContainer(id);
        } catch (DockerException | InterruptedException e) {
            _log.error("Container server error." + e.getMessage());
        }
        return null;
    }

    /**
     * Query Docker server for status of specific container
     *
     * @param server Server URI
     * @param id Container ID
     * @return Status of Container object with specified ID
     **/
    public String getContainerStatus(final String server, final String id) throws NotFoundException {
        final Container container = getContainer(server, id);

        return container != null ? container.status() : null;
    }

    /**
     * Launch image on Docker server
     *
     * @param server Server URI
     * @param imageName name of image to launch
     * @param runCommand Command string to execute
     * @param volumes Volume mounts, in the form "/path/on/server:/path/in/container"
     * @return ID of created Container
     **/
    public String launchImage(final String server, final String imageName, final String[] runCommand, final String[] volumes) {
        final HostConfig hostConfig =
                HostConfig.builder()
                .binds(volumes)
                .build();
        final ContainerConfig containerConfig =
                ContainerConfig.builder()
                .hostConfig(hostConfig)
                .image(imageName)
                .attachStdout(true)
                .attachStderr(true)
                .cmd(runCommand)
                .build();

        if (_log.isDebugEnabled()) {
            final String message = String.format(
                    "Starting container: server %s, image %s, command \"%s\", volumes [%s]",
                    server,
                    imageName,
                    StringUtils.join(runCommand, " "),
                    StringUtils.join(volumes, ", ")
            );
            _log.debug(message);
        }

        final DockerClient client = getClient(server);
        final ContainerCreation container;
        try {
            container = client.createContainer(containerConfig);

            _log.info("Starting container: id "+container.id());
            if (container.getWarnings() != null) {
                for (String warning : container.getWarnings()) {
                    _log.warn(warning);
                }
            }
            client.startContainer(container.id());

            return container.id();
        } catch (DockerException | InterruptedException e) {
            _log.error(e.getMessage());
        }

        return "";
    }

    /**
     * Create a client connection to a Docker server
     *
     * @param server Server URI
     * @return DockerClient object
     **/
    public static DockerClient getClient(final String server) {
        if (_log.isDebugEnabled()) {
            _log.debug("method getClient, Create server connection, server " + server);
        }

        DefaultDockerClient.Builder builder = DefaultDockerClient.builder();
        builder.uri(server);
        return builder.build();
    }


    private String _httpGet(final String url, final String requestParams) throws IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(url + '?' + requestParams);
        CloseableHttpResponse response = httpclient.execute(httpGet);
        try {
            System.out.println(response.getStatusLine());
            HttpEntity entity1 = response.getEntity();
            EntityUtils.consume(entity1);
        } finally {
            response.close();
        }
        return response.toString();
    }

    private String _httpPost(final String url, final List<NameValuePair> nameValuePairs) throws UnsupportedEncodingException, IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(url);
        httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
        CloseableHttpResponse response = httpclient.execute(httpPost);

        try {
            System.out.println(response.getStatusLine());
            HttpEntity entity = response.getEntity();
            EntityUtils.consume(entity);
        } finally {
            response.close();
        }
        return response.toString();
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
     * @param dockerImage Spotify-Docker Image object
     * @return NRG Image object
     **/
    private static org.nrg.containers.model.Image DockerImageToNrgImage(final com.spotify.docker.client.messages.Image dockerImage) {
        org.nrg.containers.model.Image genericImage = null;
        if (dockerImage != null) {
            genericImage =
                    new org.nrg.containers.model.Image(
                            dockerImage.repoTags() != null && dockerImage.repoTags().size() > 0 ? dockerImage.repoTags().get(0) : "null",
                            dockerImage.id(),
                            dockerImage.size(),
                            dockerImage.repoTags(),
                            dockerImage.labels()
                    );
        }
        return genericImage;
    }

    /**
     * Convert list of spotify-docker Image objects to list of xnat-container Image objects
     *
     * @param dockerImageList List of Spotify-Docker Image objects
     * @return List of NRG Image objects
     **/
    private static List<org.nrg.containers.model.Image> DockerImageToNrgImage(final List<com.spotify.docker.client.messages.Image> dockerImageList) {
        return Lists.transform(dockerImageList, DockerImageToNrgImage);
    }

    /**
     * Function to convert list of spotify-docker Image objects to list of xnat-container Image objects
     **/
    private static Function<com.spotify.docker.client.messages.Image, org.nrg.containers.model.Image> DockerImageToNrgImage =
            new Function<com.spotify.docker.client.messages.Image, Image>() {
                @Override
                public Image apply(com.spotify.docker.client.messages.Image image) {
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
        org.nrg.containers.model.Container genericContainer = null;
        if (dockerContainer != null) {
            genericContainer =
                    new Container(
                            // TODO container fields
//                            dockerContainer.repoTags() != null && dockerContainer.repoTags().size() > 0 ? dockerContainer.repoTags().get(0) : "null",
//                            dockerContainer.id(),
//                            dockerContainer.size(),
//                            dockerContainer.repoTags(),
//                            dockerContainer.labels()
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
    private static List<org.nrg.containers.model.Container> DockerContainerToNrgContainer(final List<com.spotify.docker.client.messages.Container> dockerContainerList) {
        return Lists.transform(dockerContainerList, DockerContainerToNrgContainer);
    }

    /**
     * Function to convert list of spotify-docker Container objects to xnat-container Container objects
     **/
    private static Function<com.spotify.docker.client.messages.Container, org.nrg.containers.model.Container> DockerContainerToNrgContainer =
            new Function<com.spotify.docker.client.messages.Container, Container>() {
                @Override
                public Container apply(com.spotify.docker.client.messages.Container container) {
                    return DockerContainerToNrgContainer(container);
                }
            };
}