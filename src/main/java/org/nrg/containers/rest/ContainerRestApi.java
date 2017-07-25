package org.nrg.containers.rest;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.nrg.containers.exceptions.DockerServerException;
import org.nrg.containers.exceptions.NoServerPrefException;
import org.nrg.containers.model.container.auto.Container;
import org.nrg.containers.model.container.entity.ContainerEntity;
import org.nrg.containers.services.ContainerEntityService;
import org.nrg.containers.services.ContainerService;
import org.nrg.framework.annotations.XapiRestController;
import org.nrg.framework.exceptions.NotFoundException;
import org.nrg.xapi.rest.XapiRequestMapping;
import org.nrg.xdat.XDAT;
import org.nrg.xapi.rest.AbstractXapiRestController;
import org.nrg.xdat.security.helpers.AccessLevel;
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

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

import static org.nrg.xdat.security.helpers.AccessLevel.Admin;
import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@XapiRestController
@RequestMapping(value = "/containers")
public class ContainerRestApi extends AbstractXapiRestController {
    private static final String JSON = MediaType.APPLICATION_JSON_UTF8_VALUE;

    private ContainerService containerService;

    @Autowired
    public ContainerRestApi(final ContainerService containerService,
                            final UserManagementServiceI userManagementService,
                            final RoleHolder roleHolder) {
        super(userManagementService, roleHolder);
        this.containerService = containerService;
    }

    @XapiRequestMapping(method = GET, restrictTo = Admin)
    @ResponseBody
    public List<Container> getAll() {
        return Lists.transform(containerService.getAll(), new Function<Container, Container>() {
            @Override
            public Container apply(final Container input) {
                return scrubPasswordEnv(input);
            }
        });
    }

    @XapiRequestMapping(value = "/{id}", method = GET, restrictTo = Admin)
    @ResponseBody
    public Container get(final @PathVariable String id) throws NotFoundException {
        return scrubPasswordEnv(containerService.get(id));
    }

    @XapiRequestMapping(value = "/{id}", method = DELETE, restrictTo = Admin)
    public ResponseEntity<Void> delete(final @PathVariable String id) throws NotFoundException {
        containerService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @XapiRequestMapping(value = "/{id}/finalize", method = POST, produces = JSON, restrictTo = Admin)
    public void finalize(final @PathVariable String id) throws NotFoundException {
        final UserI userI = XDAT.getUserDetails();
        containerService.finalize(id, userI);
    }

    @XapiRequestMapping(value = "/{id}/kill", method = POST, restrictTo = Admin)
    @ResponseBody
    public String kill(final @PathVariable String id)
            throws NotFoundException, NoServerPrefException, DockerServerException {
        final UserI userI = XDAT.getUserDetails();
        return containerService.kill(id, userI);
    }

    private Container scrubPasswordEnv(final Container container) {
        final Map<String, String> scrubbedEnvironmentVariables = Maps.newHashMap();
        for (final Map.Entry<String, String> env : container.environmentVariables().entrySet()) {
            scrubbedEnvironmentVariables.put(env.getKey(),
                    env.getKey().equals("XNAT_PASS") ? "******" : env.getValue());
        }
        return container.toBuilder().environmentVariables(scrubbedEnvironmentVariables).build();
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
