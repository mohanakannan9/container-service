package org.nrg.containers.services;

import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.DockerException;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.nrg.containers.api.DockerControlApi;
import org.nrg.containers.model.Image;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

public class DefaultContainerServiceTest {
    private final DefaultContainerService _service = new DefaultContainerService();

    private static DockerClient client;

    private static final String BUSYBOX = "busybox";

    @Rule
    public final TestName testName = new TestName();

    @Before
    public void setup() {
        System.out.printf("- %s\n", testName.getMethodName());

        client = DockerControlApi.getClient(_service.getServer());
    }

    @After
    public void tearDown() {
        return;
    }

    @Test
    public void testGetImageByName() throws DockerException, InterruptedException {
        client.pull(BUSYBOX);
        final Image image = _service.getImageByName(BUSYBOX);
        assertThat(image.getName(), containsString(BUSYBOX));
    }
}
