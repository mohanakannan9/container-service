package org.nrg.containers.rest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.nrg.containers.exceptions.BadRequestException;
import org.nrg.containers.exceptions.CommandInputResolutionException;
import org.nrg.containers.exceptions.CommandResolutionException;
import org.nrg.containers.exceptions.CommandValidationException;
import org.nrg.containers.exceptions.ContainerException;
import org.nrg.containers.exceptions.ContainerMountResolutionException;
import org.nrg.containers.exceptions.DockerServerException;
import org.nrg.containers.exceptions.NoServerPrefException;
import org.nrg.containers.model.PartiallyResolvedCommand;
import org.nrg.containers.model.ResolvedDockerCommand;
import org.nrg.containers.model.container.entity.ContainerEntity;
import org.nrg.containers.services.ContainerService;
import org.nrg.framework.annotations.XapiRestController;
import org.nrg.framework.exceptions.NotFoundException;
import org.nrg.xapi.rest.AbstractXapiRestController;
import org.nrg.xapi.rest.XapiRequestMapping;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.security.services.RoleHolder;
import org.nrg.xdat.security.services.UserManagementServiceI;
import org.nrg.xft.security.UserI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import springfox.documentation.annotations.ApiIgnore;

import java.util.Map;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@XapiRestController
@Api("API for Launching Containers with XNAT Container service")
public class LaunchRestApi extends AbstractXapiRestController {
    private static final Logger log = LoggerFactory.getLogger(LaunchRestApi.class);

    private static final String JSON = MediaType.APPLICATION_JSON_UTF8_VALUE;
    private static final String TEXT = MediaType.TEXT_PLAIN_VALUE;
    private static final String FORM = MediaType.APPLICATION_FORM_URLENCODED_VALUE;

    private static final String ID_REGEX = "\\d+";
    private static final String NAME_REGEX = "\\d*[^\\d]+\\d*";

    private ContainerService containerService;

    @Autowired
    public LaunchRestApi(final ContainerService containerService,
                         final UserManagementServiceI userManagementService,
                         final RoleHolder roleHolder) {
        super(userManagementService, roleHolder);
        this.containerService = containerService;
    }

    /*
    GET A LAUNCH UI
     */
    @XapiRequestMapping(value = {"/commands/{commandId}/wrappers/{wrapperId:" + ID_REGEX + "}/launch"}, method = GET)
    @ApiIgnore // Swagger UI does not correctly show this API endpoint
    @ResponseBody
    public String getLaunchUi(final @PathVariable long commandId,
                              final @PathVariable long wrapperId,
                              final @RequestParam Map<String, String> allRequestParams)
            {
        log.info("Launch UI requested for command {}, wrapper {}", commandId, wrapperId);
        final PartiallyResolvedCommand partiallyResolvedCommand =
                containerService.partiallyResolveCommand(commandId, wrapperId, allRequestParams);
        return null; // TODO
    }

    @XapiRequestMapping(value = {"/commands/{commandId}/wrappers/{wrapperName:" + NAME_REGEX + "}/launch"}, method = GET)
    @ApiIgnore // Swagger UI does not correctly show this API endpoint
    @ResponseBody
    public String getLaunchUi(final @PathVariable long commandId,
                              final @PathVariable String wrapperName,
                              final @RequestParam Map<String, String> allRequestParams)
            {
        log.info("Launch UI requested for command {}, wrapper {}", commandId, wrapperName);
        final PartiallyResolvedCommand partiallyResolvedCommand =
                containerService.partiallyResolveCommand(commandId, wrapperName, allRequestParams);
        return null; // TODO
    }

    /*
    LAUNCH COMMANDS
     */
    @XapiRequestMapping(value = {"/commands/launch"}, method = POST)
    @ApiOperation(value = "Launch a container from a resolved command")
    @ResponseBody
    public String launchCommand(final @RequestBody ResolvedDockerCommand resolvedDockerCommand)
            throws NoServerPrefException, DockerServerException, ContainerMountResolutionException, ContainerException {
        final UserI userI = XDAT.getUserDetails();
        final ContainerEntity executed = containerService.launchResolvedDockerCommand(resolvedDockerCommand, userI);
        return executed.getContainerId();
    }

    @XapiRequestMapping(value = {"/commands/{id}/launch"}, method = POST)
    @ApiIgnore // Swagger UI does not correctly show this API endpoint
    @ResponseBody
    public String launchCommandWQueryParams(final @PathVariable long id,
                                            final @RequestParam Map<String, String> allRequestParams)
            throws NoServerPrefException, DockerServerException, NotFoundException, BadRequestException, CommandResolutionException, ContainerException {
        log.info("Launch requested for command id " + String.valueOf(id));
        final ContainerEntity executed = launchCommand(id, allRequestParams);
        return executed.getContainerId();
    }

    @XapiRequestMapping(value = {"/commands/{id}/launch"}, method = POST, consumes = {JSON})
    @ApiOperation(value = "Resolve a command from the variable values in the request body, and launch it")
    @ResponseBody
    public String launchCommandWJsonBody(final @PathVariable long id,
                                         final @RequestBody Map<String, String> allRequestParams)
            throws NoServerPrefException, DockerServerException, NotFoundException, BadRequestException, CommandResolutionException, ContainerException {
        log.info("Launch requested for command id " + String.valueOf(id));
        final ContainerEntity executed = launchCommand(id, allRequestParams);
        return executed.getContainerId();
    }

    private ContainerEntity launchCommand(final long id, final Map<String, String> allRequestParams)
            throws NoServerPrefException, DockerServerException, NotFoundException, CommandResolutionException, BadRequestException, ContainerException {
        final UserI userI = XDAT.getUserDetails();
        try {
            final ContainerEntity containerEntity = containerService.resolveAndLaunchCommand(id, allRequestParams, userI);
            if (log.isInfoEnabled()) {
                log.info(String.format("Launched command id %d. Produced container %d.", id,
                        containerEntity != null ? containerEntity.getId() : null));
                if (log.isDebugEnabled()) {
                    log.debug(containerEntity != null ? containerEntity.toString() : "Container execution object is null.");
                }
            }
            return containerEntity;
        } catch (CommandInputResolutionException e) {
            throw new BadRequestException("Must provide value for variable " + e.getInput().name() + ".", e);
        }
    }

    /*
    LAUNCH COMMAND + WRAPPER BY ID
     */
    @XapiRequestMapping(value = {"/commands/{commandId}/wrappers/{wrapperId:" + ID_REGEX + "}/launch"}, method = POST)
    @ApiIgnore // Swagger UI does not correctly show this API endpoint
    @ResponseBody
    public String launchCommandWQueryParams(final @PathVariable long commandId,
                                            final @PathVariable long wrapperId,
                                            final @RequestParam Map<String, String> allRequestParams)
            throws NoServerPrefException, DockerServerException, NotFoundException, BadRequestException, CommandResolutionException, ContainerException {
        log.info("Launch requested for command {}, wrapper {}", commandId, wrapperId);
        final ContainerEntity executed = launchCommandAndWrapper(commandId, wrapperId, allRequestParams);
        return executed.getContainerId();
    }

    @XapiRequestMapping(value = {"/commands/{commandId}/wrappers/{wrapperId:" + ID_REGEX + "}/launch"}, method = POST, consumes = {JSON})
    @ApiOperation(value = "Resolve a command from the variable values in the request body, and launch it")
    @ResponseBody
    public String launchCommandWJsonBody(final @PathVariable long commandId,
                                         final @PathVariable long wrapperId,
                                         final @RequestBody Map<String, String> allRequestParams)
            throws NoServerPrefException, DockerServerException, NotFoundException, BadRequestException, CommandResolutionException, ContainerException {
        log.info("Launch requested for command {}, wrapper {}", commandId, wrapperId);
        final ContainerEntity executed = launchCommandAndWrapper(commandId, wrapperId, allRequestParams);
        return executed.getContainerId();
    }

    private ContainerEntity launchCommandAndWrapper(final long commandId,
                                                    final long wrapperId,
                                                    final Map<String, String> allRequestParams)
            throws NoServerPrefException, DockerServerException, NotFoundException, CommandResolutionException, BadRequestException, ContainerException {
        final UserI userI = XDAT.getUserDetails();
        try {
            final ContainerEntity containerEntity = containerService.resolveAndLaunchCommand(wrapperId, commandId, allRequestParams, userI);
            if (log.isInfoEnabled()) {
                log.info("Launched command {}, wrapper {}. Produced container {}.", commandId, wrapperId,
                        containerEntity != null ? containerEntity.getId() : null);
                if (log.isDebugEnabled()) {
                    log.debug(containerEntity != null ? containerEntity.toString() : "Container execution object is null.");
                }
            }
            return containerEntity;
        } catch (CommandInputResolutionException e) {
            throw new BadRequestException("Must provide value for variable " + e.getInput().name() + ".", e);
        }
    }

    /*
    LAUNCH COMMAND + WRAPPER BY NAME
     */
    @XapiRequestMapping(value = {"/commands/{commandId}/wrappers/{wrapperName:" + NAME_REGEX + "}/launch"}, method = POST)
    @ApiIgnore // Swagger UI does not correctly show this API endpoint
    @ResponseBody
    public String launchCommandWQueryParams(final @PathVariable long commandId,
                                            final @PathVariable String wrapperName,
                                            final @RequestParam Map<String, String> allRequestParams)
            throws NoServerPrefException, DockerServerException, NotFoundException, BadRequestException, CommandResolutionException, ContainerException {
        log.info("Launch requested for command {}, wrapper {}", commandId, wrapperName);
        final ContainerEntity executed = launchCommandAndWrapper(commandId, wrapperName, allRequestParams);
        return executed.getContainerId();
    }

    @XapiRequestMapping(value = {"/commands/{commandId}/wrappers/{wrapperName:" + NAME_REGEX + "}/launch"}, method = POST, consumes = {JSON})
    @ApiOperation(value = "Resolve a command from the variable values in the request body, and launch it")
    @ResponseBody
    public String launchCommandWJsonBody(final @PathVariable long commandId,
                                         final @PathVariable String wrapperName,
                                         final @RequestBody Map<String, String> allRequestParams)
            throws NoServerPrefException, DockerServerException, NotFoundException, BadRequestException, CommandResolutionException, ContainerException {
        log.info("Launch requested for command {}, wrapper {}", commandId, wrapperName);
        final ContainerEntity executed = launchCommandAndWrapper(commandId, wrapperName, allRequestParams);
        return executed.getContainerId();
    }

    private ContainerEntity launchCommandAndWrapper(final long commandId,
                                                    final String wrapperName,
                                                    final Map<String, String> allRequestParams)
            throws NoServerPrefException, DockerServerException, NotFoundException, CommandResolutionException, BadRequestException, ContainerException {
        final UserI userI = XDAT.getUserDetails();
        try {
            final ContainerEntity containerEntity = containerService.resolveAndLaunchCommand(wrapperName, commandId, allRequestParams, userI);
            if (log.isInfoEnabled()) {
                log.info("Launched command {}, wrapper {}. Produced container {}.", commandId, wrapperName,
                        containerEntity != null ? containerEntity.getId() : null);
                if (log.isDebugEnabled()) {
                    log.debug(containerEntity != null ? containerEntity.toString() : "Container execution object is null.");
                }
            }
            return containerEntity;
        } catch (CommandInputResolutionException e) {
            throw new BadRequestException("Must provide value for variable " + e.getInput().name() + ".", e);
        }
    }

    /*
    EXCEPTION HANDLING
     */
    @ResponseStatus(value = HttpStatus.NOT_FOUND)
    @ExceptionHandler(value = {NotFoundException.class})
    public String handleNotFound(final Exception e) {
        final String message = e.getMessage();
        log.debug(message);
        return message;
    }

    @ResponseStatus(value = HttpStatus.FAILED_DEPENDENCY)
    @ExceptionHandler(value = {NoServerPrefException.class})
    public String handleFailedDependency(final Exception ignored) {
        final String message = "Set up Docker server before using this REST endpoint.";
        log.debug(message);
        return message;
    }

    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(value = {DockerServerException.class})
    public String handleDockerServerError(final Exception e) {
        final String message = "The Docker server returned an error:\n" + e.getMessage();
        log.debug(message);
        return message;
    }

    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(value = {ContainerException.class})
    public String handleContainerException(final Exception e) {
        final String message = "There was a problem with the container:\n" + e.getMessage();
        log.debug(message);
        return message;
    }

    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(value = {CommandResolutionException.class})
    public String handleCommandResolutionException(final CommandResolutionException e) {
        final String message = "The command could not be resolved.\n" + e.getMessage();
        log.debug(message);
        return message;
    }

    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    @ExceptionHandler(value = {BadRequestException.class})
    public String handleBadRequest(final Exception e) {
        final String message = "Bad request:\n" + e.getMessage();
        log.debug(message);
        return message;
    }

    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    @ExceptionHandler(value = {CommandValidationException.class})
    public String handleBadCommand(final CommandValidationException e) {
        String message = "Invalid command";
        if (e != null && e.getErrors() != null && !e.getErrors().isEmpty()) {
            message += ":\n\t";
            message += StringUtils.join(e.getErrors(), "\n\t");
        }
        log.debug(message);
        return message;
    }
}
