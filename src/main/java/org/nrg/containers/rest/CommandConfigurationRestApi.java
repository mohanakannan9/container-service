package org.nrg.containers.rest;

import io.swagger.annotations.Api;
import org.apache.commons.lang3.StringUtils;
import org.nrg.containers.exceptions.BadRequestException;
import org.nrg.containers.exceptions.CommandResolutionException;
import org.nrg.containers.exceptions.CommandValidationException;
import org.nrg.containers.exceptions.ContainerException;
import org.nrg.containers.exceptions.DockerServerException;
import org.nrg.containers.exceptions.NoServerPrefException;
import org.nrg.containers.model.CommandConfiguration;
import org.nrg.containers.services.CommandService;
import org.nrg.containers.services.ContainerConfigService.CommandConfigurationException;
import org.nrg.framework.annotations.XapiRestController;
import org.nrg.framework.exceptions.NotFoundException;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.rest.AbstractXapiRestController;
import org.nrg.xdat.security.services.RoleHolder;
import org.nrg.xdat.security.services.UserManagementServiceI;
import org.nrg.xft.security.UserI;
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

import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@XapiRestController
@Api("Command Configuration API for XNAT Container service")
public class CommandConfigurationRestApi extends AbstractXapiRestController {
    private static final Logger log = LoggerFactory.getLogger(CommandConfigurationRestApi.class);

    private static final String JSON = MediaType.APPLICATION_JSON_UTF8_VALUE;
    private static final String TEXT = MediaType.TEXT_PLAIN_VALUE;
    private static final String FORM = MediaType.APPLICATION_FORM_URLENCODED_VALUE;

    private static final String ID_REGEX = "\\d+";
    private static final String NAME_REGEX = "\\d*[^\\d]+\\d*";

    private CommandService commandService;

    @Autowired
    public CommandConfigurationRestApi(final CommandService commandService,
                                       final UserManagementServiceI userManagementService,
                                       final RoleHolder roleHolder) {
        super(userManagementService, roleHolder);
        this.commandService = commandService;
    }

    // Configure for site + command wrapper
    @RequestMapping(value = {"/commands/{commandId}/wrappers/{wrapperName}/config"}, method = POST)
    public ResponseEntity<Void> createConfiguration(final @RequestBody CommandConfiguration commandConfiguration,
                                                    final @PathVariable long commandId,
                                                    final @PathVariable String wrapperName,
                                                    final @RequestParam(required = false, defaultValue = "true") boolean enable,
                                                    final @RequestParam(required = false) String reason)
            throws CommandConfigurationException, NotFoundException {
        final UserI userI = XDAT.getUserDetails();
        // TODO Check: can user create?
        commandService.configureForSite(commandConfiguration, commandId, wrapperName, enable, userI.getLogin(), reason);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    // Get configuration for site + command wrapper
    @RequestMapping(value = {"/commands/{commandId}/wrappers/{wrapperName}/config"}, method = GET)
    @ResponseBody
    public CommandConfiguration getConfiguration(final @PathVariable long commandId,
                                                 final @PathVariable String wrapperName) throws NotFoundException {
        // TODO Check: can user read?
        // final UserI userI = XDAT.getUserDetails();
        return commandService.getSiteConfiguration(commandId, wrapperName);
    }

    // Delete configuration for site + command wrapper
    @RequestMapping(value = {"/commands/{commandId}/wrappers/{wrapperName}/config"}, method = DELETE)
    public ResponseEntity<Void> deleteConfiguration(final @PathVariable long commandId,
                                                    final @PathVariable String wrapperName)
            throws CommandConfigurationException {
        // TODO Check: can user delete?
        final UserI userI = XDAT.getUserDetails();
        commandService.deleteSiteConfiguration(commandId, wrapperName, userI.getLogin());
        return ResponseEntity.noContent().build();
    }

    // Configure for project + command wrapper
    @RequestMapping(value = {"/projects/{project}/commands/{commandId}/wrappers/{wrapperName}/config"}, method = POST)
    public ResponseEntity<Void> createConfiguration(final @RequestBody CommandConfiguration commandConfiguration,
                                                    final @PathVariable String project,
                                                    final @PathVariable long commandId,
                                                    final @PathVariable String wrapperName,
                                                    final @RequestParam(required = false, defaultValue = "true") boolean enable,
                                                    final @RequestParam(required = false) String reason)
            throws CommandConfigurationException, NotFoundException {
        final UserI userI = XDAT.getUserDetails();
        // TODO Check: can user create?
        commandService.configureForProject(commandConfiguration, project, commandId, wrapperName, enable, userI.getLogin(), reason);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    // Get configuration for project + command wrapper
    @RequestMapping(value = {"/projects/{project}/commands/{commandId}/wrappers/{wrapperName}/config"}, method = GET)
    @ResponseBody
    public CommandConfiguration getConfiguration(final @PathVariable String project,
                                                 final @PathVariable long commandId,
                                                 final @PathVariable String wrapperName) throws NotFoundException {
        // TODO Check: can user read?
        // final UserI userI = XDAT.getUserDetails();
        return commandService.getProjectConfiguration(project, commandId, wrapperName);
    }

    // Delete configuration for project + command wrapper
    @RequestMapping(value = {"/projects/{project}/commands/{commandId}/wrappers/{wrapperName}/config"}, method = DELETE)
    public ResponseEntity<Void> deleteConfiguration(final @PathVariable String project,
                                                    final @PathVariable long commandId,
                                                    final @PathVariable String wrapperName)
            throws CommandConfigurationException {
        // TODO Check: can user delete?
        final UserI userI = XDAT.getUserDetails();
        commandService.deleteProjectConfiguration(project, commandId, wrapperName, userI.getLogin());
        return ResponseEntity.noContent().build();
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
