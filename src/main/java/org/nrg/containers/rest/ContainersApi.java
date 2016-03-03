package org.nrg.containers.rest;

import io.swagger.annotations.ApiParam;
import org.nrg.containers.exceptions.ContainerServerException;
import org.nrg.containers.exceptions.NoServerPrefException;
import org.nrg.containers.exceptions.NotFoundException;
import org.nrg.containers.model.Container;
import org.nrg.containers.model.ContainerHub;
import org.nrg.containers.model.ContainerServer;
import org.nrg.containers.model.Image;
import org.nrg.containers.model.ImageParameters;
import org.nrg.containers.services.ContainerService;
import org.nrg.prefs.exceptions.InvalidPreferenceName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

//@Api(description = "XNAT Container Services REST API")
@RestController
@RequestMapping(value = "/containers")
public class ContainersApi {
    private static final Logger _log = LoggerFactory.getLogger(ContainersApi.class);

    public static final String JSON = MediaType.APPLICATION_JSON_UTF8_VALUE;
    public static final String PLAIN_TEXT = MediaType.TEXT_PLAIN_VALUE;

    @Autowired
    @SuppressWarnings("SpringJavaAutowiringInspection") // IntelliJ does not process the excludeFilter in ContainersSpringConfig @ComponentScan, erroneously marks this red
    private ContainerService service;

    //    @ApiOperation(value = "Get list of images.", notes = "Returns a list of all images on the container server.", response = Image.class, responseContainer = "List")
//    @ApiResponses({
//            @ApiResponse(code = 200, message = "A list of images on the server"),
//            @ApiResponse(code = 500, message = "Unexpected error")})
    @RequestMapping(value = {}, method = GET, produces = {JSON, PLAIN_TEXT})
    @ResponseBody
    public List<Container> getAllContainers()
            throws NoServerPrefException, ContainerServerException {
        return service.getAllContainers();
    }

    //    @ApiOperation(value = "Gets the container with the specified id.", notes = "Returns the serialized container object with the specified id.", response = Container.class)
//    @ApiResponses({
//            @ApiResponse(code = 200, message = "Container successfully retrieved."),
//            @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
//            @ApiResponse(code = 403, message = "Not authorized to view this container."),
//            @ApiResponse(code = 404, message = "Container not found."),
//            @ApiResponse(code = 500, message = "Unexpected error")})
    @RequestMapping(value = "/{id}", method = GET, produces = {JSON, PLAIN_TEXT})
    @ResponseBody
    public Container getContainer(@ApiParam(value = "ID of the container to fetch", required = true)
                                      final @PathVariable("id") String id)
            throws NoServerPrefException, NotFoundException, ContainerServerException {
        return service.getContainer(id);
    }

    //    @ApiOperation(value = "Gets the image with the specified name.", notes = "Returns the serialized image object with the specified name.", response = String.class)
//    @ApiResponses({
//            @ApiResponse(code = 200, message = "Container successfully retrieved."),
//            @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
//            @ApiResponse(code = 403, message = "Not authorized to view this container."),
//            @ApiResponse(code = 404, message = "Container not found."),
//            @ApiResponse(code = 500, message = "Unexpected error")})
    @RequestMapping(value = "/{id}/status", method = GET, produces = PLAIN_TEXT)
    @ResponseBody
    public String getContainerStatus(@ApiParam(value = "Get status of container with this ID", required = true)
                                         final @PathVariable("id") String id)
            throws NotFoundException, NoServerPrefException, ContainerServerException {
            return service.getContainerStatus(id);
    }

    @RequestMapping(value = "/{id}/{verb:(start|stop|restart|pause|unpause|kill)}", method = PUT, produces = PLAIN_TEXT)
    @ResponseBody
    public String verbContainer(@ApiParam(value = "Change status of container with this ID", required = true)
                                         final @PathVariable("id") String id,
                                     @ApiParam(value = "Perform this verb on container", required = true)
                                         final @PathVariable("verb") String verb)
            throws NotFoundException, NoServerPrefException, ContainerServerException {
        return service.verbContainer(id, verb);
    }

    @RequestMapping(value = "/{id}/logs", method = GET, produces = PLAIN_TEXT)
    @ResponseBody
    public String getContainerLogs(@ApiParam(value = "Get logs of container with this ID", required = true)
                                       final @PathVariable("id") String id)
            throws NotFoundException, NoServerPrefException, ContainerServerException {
        return service.getContainerLogs(id);
    }

    //    @ApiOperation(value = "Launches a container.", notes = "Returns the updated serialized image object with the specified image name.", response = Image.class)
//    @ApiResponses({
//            @ApiResponse(code = 200, message = "Image successfully created or updated."),
//            @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
//            @ApiResponse(code = 403, message = "Not authorized to create or update this image."),
//            @ApiResponse(code = 404, message = "Image not found."),
//            @ApiResponse(code = 500, message = "Unexpected error")})
    @RequestMapping(value = "/launch", method = POST, params = "name", produces = PLAIN_TEXT)
    @ResponseBody
    public String launch(@ApiParam(value = "The name of the image to launch.", required = true)
                             final @RequestParam("name") String name,
                         final @RequestBody ImageParameters launchArguments,
                         final @RequestParam(name = "wait", defaultValue = "false") Boolean wait)
            throws NoServerPrefException, NotFoundException, ContainerServerException {
        return service.launch(name, launchArguments);
    }

    @RequestMapping(value = "/server", method = GET, produces = {JSON, PLAIN_TEXT})
    @ResponseBody
    public ContainerServer getServer() throws NotFoundException {
        try {
            return service.getServer();
        } catch (NoServerPrefException e) {
            throw new NotFoundException(e);
        }
    }

    @RequestMapping(value = "/server", method = POST)
    @ResponseBody
    public void setServer(final @RequestBody ContainerServer containerServer) throws InvalidPreferenceName {
        service.setServer(containerServer.host());
    }

    @RequestMapping(value = "/hubs", method = GET, produces = {JSON, PLAIN_TEXT})
    @ResponseBody
    public List<ContainerHub> getHubs(final @RequestParam(name = "verbose", defaultValue = "false") Boolean verbose) {
        // Hard-code non-verbose until I get user permissions sorted out
        return service.getHubs(Boolean.FALSE);
    }

    @RequestMapping(value = "/hubs/{hub}", method = GET, produces = {JSON, PLAIN_TEXT})
    @ResponseBody
    public ContainerHub getHub(final @PathVariable("hub") String hub,
                               final @RequestParam(name = "verbose", defaultValue = "false") Boolean verbose) {
        // Hard-code non-verbose until I get user permissions sorted out
        return service.getHub(hub, Boolean.FALSE);
    }

    @RequestMapping(value = "/hubs", method = POST, consumes = JSON)
    @ResponseBody
    public void setHub(final @RequestBody ContainerHub hub,
                       final @RequestParam(name = "overwrite", defaultValue = "false") Boolean overwrite,
                       final @RequestParam(name = "ignoreBlank", defaultValue = "false") Boolean ignoreBlank) {
        service.setHub(hub, overwrite, ignoreBlank);
    }

    @RequestMapping(value = "/search", method = GET)
    @ResponseBody
    public String search(final @RequestParam(name = "term") String term) {
        return service.search(term);
    }

    @RequestMapping(value = "/pull", method = GET, params = "image")
    @ResponseBody
    public Image pullByName(final @RequestParam("image") String image,
                            final @RequestParam(value = "hub", required = false) String hub,
                            final @RequestParam(name = "name", required = false) String name) {
        return service.pullByName(image, hub, name);
    }

    @RequestMapping(value = "/pull", method = GET, params = "source")
    @ResponseBody
    public Image pullFromSource(final @RequestParam("source") String source,
                        final @RequestParam(name = "name", required = false) String name) {
        return service.pullFromSource(source, name);
    }

    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(value = {InvalidPreferenceName.class})
    public void handleInvalidPreferenceNameException() {
        // Do nothing. HTTP 500 will be returned.
    }
}
