package org.nrg.containers.services;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.DockerException;
import com.spotify.docker.client.DockerRequestException;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.ContainerInfo;
import org.apache.commons.lang.NotImplementedException;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TestName;
import org.nrg.containers.api.DockerControlApi;
import org.nrg.containers.model.Container;
import org.nrg.containers.model.Image;
import org.nrg.containers.services.impl.DefaultContainerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static java.lang.Long.toHexString;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isIn;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

public class DefaultContainerServiceTest {
    private static final Logger _log = LoggerFactory.getLogger(DefaultContainerServiceTest.class);
    private final DefaultContainerService service = new DefaultContainerService();

    private static DockerClient client;

    private static final String BUSYBOX = "busybox";
    private static final String BUSYBOX_LATEST = BUSYBOX + ":latest";


    @Rule public final TestName testName = new TestName();
    @Rule public ExpectedException thrown = ExpectedException.none();

    private final String nameTag = toHexString(ThreadLocalRandom.current().nextLong());

    @Before
    public void setup() {
        System.out.printf("- %s\n", testName.getMethodName());

        // TODO: Add all necessary 'client' operations to DockerControlApi; remove 'client' from tests
        client = DockerControlApi.getClient(service.getServer());
    }

    @After
    public void tearDown() throws DockerException, InterruptedException {
        // Stolen from docker-client DefaultDockerClientTest.

        // Remove containers
        final List<com.spotify.docker.client.messages.Container> containers = client.listContainers();
        for (com.spotify.docker.client.messages.Container container : containers) {
            final ContainerInfo info = client.inspectContainer(container.id());
            if (info != null && info.name().contains(nameTag)) {
                try {
                    client.killContainer(info.id());
                } catch (DockerRequestException e) {
                    // Docker 1.6 sometimes fails to kill a container because it disappears.
                    // https://github.com/docker/docker/issues/12738
                    _log.warn("Failed to kill container {}", info.id(), e);
                }
            }
        }

        // Close the client
        client.close();
    }

    @Test
    public void testGetAllImages() throws DockerException, InterruptedException {
        client.pull(BUSYBOX_LATEST);
        final List<Image> images = service.getAllImages();
        assertThat(BUSYBOX_LATEST, isIn(imagesToNames(images)));
    }

    @Test
    public void testGetImageByName() throws DockerException, InterruptedException {
        client.pull(BUSYBOX);
        final Image image = service.getImageByName(BUSYBOX);
        assertThat(image.getName(), containsString(BUSYBOX));
    }

    @Test
    public void testGetAllContainers() throws DockerException, InterruptedException {
        client.pull(BUSYBOX_LATEST);
        final ContainerConfig containerConfig = ContainerConfig.builder()
                .image(BUSYBOX_LATEST)
                .cmd("sh", "-c", "while :; do sleep 1; done")
                .build();
        final String containerName = randomName();
        final ContainerCreation containerCreation = client.createContainer(containerConfig, containerName);
        final String containerId = containerCreation.id();

        client.startContainer(containerId);

        final List<Container> containers = service.getAllContainers();
        assertThat(containers, is(not(empty())));
        assertThat(containerId, isIn(containersToIds(containers)));
    }

    @Test
    public void testGetContainerStatus() {
        // Create a container
        // Get status. Assert "created"
        // Start container
        // Get status. Assert "running"
        // Pause container
        // Get status. Assert "paused"
        // Unpause container
        // Get status. Assert "restarted" or "running", not sure which
        // Stop container
        // Get status. Assert "exited"
        thrown.expect(NotImplementedException.class);
        throw new NotImplementedException("TODO: "+testName);
    }

    @Test
    public void testGetContainer() {
        // Create a container with some given name or labels or something
        // Get that container
        // Assert that it has the attribute(s) we gave it
        thrown.expect(NotImplementedException.class);
        throw new NotImplementedException("TODO: "+testName);
    }

    public void testLaunch() {
        // Test most basic launching.
        // I'm sure we will fill out more tests as we fill out the API for launching
        thrown.expect(NotImplementedException.class);
        throw new NotImplementedException("TODO: "+testName);
    }

    private String randomName() {
        return nameTag + '-' + toHexString(ThreadLocalRandom.current().nextLong());
    }

    private List<String> imagesToNames(final List<Image> images) {
        final Function<Image, String> imageToName = new Function<Image, String>() {
            @Override
            public String apply(final Image image) {
                return image.getName();
            }
        };
        return Lists.transform(images, imageToName);
    }

    private List<String> containersToIds(final List<Container> containers) {
        final Function<Container, String> containerToId = new Function<Container, String>() {
            @Override
            public String apply(final Container container) {
                return container.id();
            }
        };
        return Lists.transform(containers, containerToId);
    }
}
