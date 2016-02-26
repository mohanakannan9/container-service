//package org.nrg.containers.api;
//
//import com.spotify.docker.client.DockerClient;
//import com.spotify.docker.client.DockerException;
//import org.junit.After;
//import org.junit.Before;
//import org.junit.Test;
//import org.nrg.containers.model.Image;
//import org.nrg.containers.services.ContainerService;
//
//import javax.inject.Inject;
//
//import static org.hamcrest.Matchers.containsString;
//import static org.junit.Assert.assertThat;
//
//public class DockerControlApiTest {
//    @Inject
//    private ContainerService _service;
//
//    private static String SERVER;
//    private static DockerClient client;
//
//    private static final String BUSYBOX = "busybox";
//
//    @Before
//    public void setup() throws Exception {
//        SERVER = _service.getServer();
//        client = DockerControlApi.getClient(SERVER);
//    }
//
//    @After
//    public void tearDown() throws Exception {
//        return;
//    }
//
//    @Test
//    public void testGetImageByName() throws DockerException, InterruptedException {
//        client.pull(BUSYBOX);
//        final Image image = DockerControlApi.getImageByName(SERVER, BUSYBOX);
//        assertThat(image.getName(), containsString(BUSYBOX));
//    }
//}
