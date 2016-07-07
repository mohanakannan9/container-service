package org.nrg.execution.rest;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.commons.lang3.StringUtils;
import org.nrg.execution.exceptions.DockerServerException;
import org.nrg.execution.exceptions.NoServerPrefException;
import org.nrg.execution.exceptions.NotFoundException;
import org.nrg.execution.model.DockerHub;
import org.nrg.execution.model.DockerServer;
import org.nrg.execution.services.DockerService;
import org.nrg.framework.annotations.XapiRestController;
import org.nrg.framework.exceptions.NrgServiceRuntimeException;
import org.nrg.prefs.exceptions.InvalidPreferenceName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@XapiRestController
@RequestMapping(value = "/docker")
public class DockerRestApi {
    private static final Logger log = LoggerFactory.getLogger(DockerRestApi.class);

    private static final String JSON = MediaType.APPLICATION_JSON_UTF8_VALUE;
    private static final String TEXT = MediaType.TEXT_PLAIN_VALUE;

    @Autowired
    private DockerService dockerService;

    @ApiOperation(value = "Docker server", notes = "Returns Docker server configuration values",
            response = DockerServer.class)
    @ApiResponses({
            @ApiResponse(code = 200, message = "The Docker server configuration"),
            @ApiResponse(code = 400, message = "The server has not been configured"),
            @ApiResponse(code = 500, message = "Unexpected error")})
    @RequestMapping(value = "/server", method = GET, produces = JSON)
    @ResponseBody
    public DockerServer getServer() throws NotFoundException {
        return dockerService.getServer();
    }

    @ApiOperation(value = "Set Docker server configuration",
            notes = "Save new Docker server configuration values")
    @ApiResponses({
            @ApiResponse(code = 202, message = "The Docker server configuration was saved"),
            @ApiResponse(code = 400, message = "Must set the \"host\" property in request body"),
            @ApiResponse(code = 500, message = "Unexpected error")})
    @RequestMapping(value = "/server", method = POST)
    public ResponseEntity<String> setServer(final @RequestBody DockerServer dockerServer)
            throws InvalidPreferenceName {
        if (StringUtils.isBlank(dockerServer.getHost())) {
            return new ResponseEntity<>("Must set the \"host\" property in request body.",
                    HttpStatus.BAD_REQUEST);
        }

        dockerService.setServer(dockerServer);

        return new ResponseEntity<>("", HttpStatus.ACCEPTED);
    }

    @RequestMapping(value = "/server/ping", method = GET)
    @ResponseBody
    public String pingServer()
            throws NoServerPrefException, DockerServerException {
        return dockerService.pingServer();
    }

    @RequestMapping(value = "/hubs", method = GET)
    @ResponseBody
    public List<DockerHub> getHubs() {
        return dockerService.getHubs();
    }

    @RequestMapping(value = "/hubs", method = POST)
    @ResponseBody
    public ResponseEntity<DockerHub> setHub(final @RequestBody DockerHub hub) throws NrgServiceRuntimeException {
        return new ResponseEntity<>(dockerService.setHub(hub), HttpStatus.CREATED);
    }

    @RequestMapping(value = "/hubs/{id}/ping", method = GET)
    @ResponseBody
    public String pingHub(final @PathVariable Long id)
            throws NoServerPrefException, DockerServerException, NotFoundException {
        return dockerService.pingHub(id);
    }

    @RequestMapping(value = "/hubs/{id}/pull", params = {"image"}, method = POST)
    public void pullImageFromHub(final @PathVariable Long hubId,
                          final @RequestParam(value = "image") String image)
            throws DockerServerException, NotFoundException, NoServerPrefException {
        dockerService.pullFromHub(hubId, image);
    }

    @RequestMapping(value = "/pull", params = {"image"}, method = POST)
    public void pullImageFromDefaultHub(final @RequestParam(value = "image") String image)
            throws DockerServerException, NotFoundException, NoServerPrefException {
        dockerService.pullFromHub(image);
    }

    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(value = {InvalidPreferenceName.class})
    public String handleInvalidPreferenceName(final Exception e) {
        return e.getMessage();
    }

    @ResponseStatus(value = HttpStatus.NOT_FOUND)
    @ExceptionHandler(value = {NotFoundException.class})
    public String handleNotFound(final Exception e) {
        return e.getMessage();
    }

    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(value = {DockerServerException.class})
    public String handleDockerServerError(final Exception e) {
        return "The Docker server returned an error:\n" + e.getMessage();
    }

    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    @ExceptionHandler(value = {NrgServiceRuntimeException.class})
    public String handleBadRequest() {
        return "Body was not a valid Docker Hub.";
    }

    @ResponseStatus(value = HttpStatus.FAILED_DEPENDENCY)
    @ExceptionHandler(value = {NoServerPrefException.class})
    public String handleFailedDependency() {
        return "Set up Docker server before using this REST endpoint.";
    }
}

