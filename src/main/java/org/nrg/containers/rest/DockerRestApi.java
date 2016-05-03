package org.nrg.containers.rest;

import org.apache.commons.lang3.StringUtils;
import org.nrg.containers.exceptions.DockerServerException;
import org.nrg.containers.exceptions.NoServerPrefException;
import org.nrg.containers.exceptions.NotFoundException;
import org.nrg.containers.model.DockerHub;
import org.nrg.containers.model.DockerServer;
import org.nrg.containers.services.DockerService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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

    @RequestMapping(value = "/server", method = GET, produces = JSON)
    @ResponseBody
    public DockerServer getServer() throws NotFoundException {
        return dockerService.getServer();
    }

    @RequestMapping(value = "/server", method = POST)
    public ResponseEntity<String> setServer(final @RequestBody DockerServer dockerServer)
            throws InvalidPreferenceName {
        if (StringUtils.isBlank(dockerServer.host())) {
            return new ResponseEntity<>("Must set the \"host\" property in request body.",
                    HttpStatus.BAD_REQUEST);
        }

        dockerService.setServer(dockerServer);

        return new ResponseEntity<>("", HttpStatus.CREATED);
    }

    @RequestMapping(value = "/server/ping", method = GET)
    @ResponseBody
    public String pingServer()
            throws NoServerPrefException, DockerServerException {
        return dockerService.pingServer();
    }

    @RequestMapping(value = "/hubs", method = GET, produces = {JSON, TEXT})
    @ResponseBody
    public List<DockerHub> getHubs() {
        return dockerService.getHubs();
    }

    @RequestMapping(value = "/hubs", method = POST, consumes = JSON)
    @ResponseBody
    public ResponseEntity<String> setHub(final @RequestBody DockerHub hub) throws NrgServiceRuntimeException {
        dockerService.setHub(hub);
        return new ResponseEntity<>("", HttpStatus.CREATED);
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

