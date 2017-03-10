package org.nrg.containers.rest;

import org.nrg.containers.exceptions.DockerServerException;
import org.nrg.containers.exceptions.NoServerPrefException;
import org.nrg.containers.model.ContainerEntity;
import org.nrg.containers.services.ContainerEntityService;
import org.nrg.containers.services.ContainerService;
import org.nrg.framework.annotations.XapiRestController;
import org.nrg.framework.exceptions.NotFoundException;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.rest.AbstractXapiRestController;
import org.nrg.xdat.security.services.RoleHolder;
import org.nrg.xdat.security.services.UserManagementServiceI;
import org.nrg.xft.security.UserI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;

import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@XapiRestController
@RequestMapping(value = "/containers")
public class ContainerRestApi extends AbstractXapiRestController {
    private static final String JSON = MediaType.APPLICATION_JSON_UTF8_VALUE;

    private ContainerService containerService;
    private ContainerEntityService containerEntityService;

    @Autowired
    public ContainerRestApi(final ContainerEntityService containerEntityService,
                            final ContainerService containerService,
                            final UserManagementServiceI userManagementService,
                            final RoleHolder roleHolder) {
        super(userManagementService, roleHolder);
        this.containerEntityService = containerEntityService;
        this.containerService = containerService;
    }

    @RequestMapping(method = GET)
    @ResponseBody
    public List<ContainerEntity> getAll() {
        return containerEntityService.getAll();
    }

    @RequestMapping(value = "/{id}", method = GET)
    @ResponseBody
    public ContainerEntity getOne(final @PathVariable Long id) throws NotFoundException {
        final ContainerEntity containerEntity = containerEntityService.retrieve(id);
        if (containerEntity == null) {
            throw new NotFoundException("ContainerExecution " + id + " not found.");
        }
        return containerEntity;
    }

    @RequestMapping(value = "/{id}", method = DELETE)
    public ResponseEntity<String> delete(final @PathVariable Long id) {
        containerEntityService.delete(id);
        return new ResponseEntity<>("", HttpStatus.NO_CONTENT);
    }

    @RequestMapping(value = "/{id}/finalize", method = POST, produces = JSON)
    public void finalize(final @PathVariable Long id) throws NotFoundException {
        final UserI userI = XDAT.getUserDetails();
        containerService.finalize(id, userI);
    }

    @RequestMapping(value = "/{id}/kill", method = POST)
    @ResponseBody
    public String kill(final @PathVariable Long id)
            throws NotFoundException, NoServerPrefException, DockerServerException {
        final UserI userI = XDAT.getUserDetails();
        return containerService.kill(id, userI);
    }

    @ResponseStatus(value = HttpStatus.NOT_FOUND)
    @ExceptionHandler(value = {NotFoundException.class})
    public String handleNotFound(final Exception e) {
        return e.getMessage();
    }

    @ResponseStatus(value = HttpStatus.FAILED_DEPENDENCY)
    @ExceptionHandler(value = {NoServerPrefException.class})
    public String handleFailedDependency() {
        return "Set up Docker server before using this REST endpoint.";
    }

    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(value = {DockerServerException.class})
    public String handleDockerServerException(final Exception e) {
        return e.getMessage();
    }
}
