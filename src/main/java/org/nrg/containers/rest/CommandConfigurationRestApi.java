package org.nrg.containers.rest;

import io.swagger.annotations.Api;
import org.apache.commons.lang3.StringUtils;
import org.nrg.config.exceptions.ConfigServiceException;
import org.nrg.containers.exceptions.BadRequestException;
import org.nrg.containers.exceptions.CommandResolutionException;
import org.nrg.containers.exceptions.CommandValidationException;
import org.nrg.containers.exceptions.ContainerException;
import org.nrg.containers.exceptions.DockerServerException;
import org.nrg.containers.exceptions.NoServerPrefException;
import org.nrg.containers.model.configuration.CommandConfiguration;
import org.nrg.containers.model.settings.ContainerServiceSettings;
import org.nrg.containers.services.CommandService;
import org.nrg.containers.services.ContainerConfigService;
import org.nrg.containers.services.ContainerConfigService.CommandConfigurationException;
import org.nrg.framework.annotations.XapiRestController;
import org.nrg.framework.exceptions.NotFoundException;
import org.nrg.xapi.rest.AbstractXapiRestController;
import org.nrg.xapi.rest.XapiRequestMapping;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.base.auto.AutoXnatProjectdata;
import org.nrg.xdat.security.helpers.Permissions;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

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
    private ContainerConfigService containerConfigService;

    @Autowired
    public CommandConfigurationRestApi(final CommandService commandService,
                                       final ContainerConfigService containerConfigService,
                                       final UserManagementServiceI userManagementService,
                                       final RoleHolder roleHolder) {
        super(userManagementService, roleHolder);
        this.commandService = commandService;
        this.containerConfigService = containerConfigService;
    }

    // Configure for site + command wrapper
    @XapiRequestMapping(value = {"/commands/{commandId}/wrappers/{wrapperName}/config"}, method = POST)
    public ResponseEntity<Void> createConfiguration(final @RequestBody CommandConfiguration commandConfiguration,
                                                    final @PathVariable long commandId,
                                                    final @PathVariable String wrapperName,
                                                    final @RequestParam(required = false, defaultValue = "true") boolean enable,
                                                    final @RequestParam(required = false) String reason)
            throws CommandConfigurationException, NotFoundException {
        final UserI userI = XDAT.getUserDetails();
        final HttpStatus status = isAdminUser();
        if (status != null) {
            return new ResponseEntity<>(status);
        }
        commandService.configureForSite(commandConfiguration, commandId, wrapperName, enable, userI.getLogin(), reason);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @XapiRequestMapping(value = {"/wrappers/{wrapperId}/config"}, method = POST)
    public ResponseEntity<Void> createConfiguration(final @RequestBody CommandConfiguration commandConfiguration,
                                                    final @PathVariable long wrapperId,
                                                    final @RequestParam(required = false, defaultValue = "true") boolean enable,
                                                    final @RequestParam(required = false) String reason)
            throws CommandConfigurationException, NotFoundException {
        final UserI userI = XDAT.getUserDetails();
        final HttpStatus status = isAdminUser();
        if (status != null) {
            return new ResponseEntity<>(status);
        }
        commandService.configureForSite(commandConfiguration, wrapperId, enable, userI.getLogin(), reason);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    // Get configuration for site + command wrapper
    @XapiRequestMapping(value = {"/commands/{commandId}/wrappers/{wrapperName}/config"}, method = GET)
    @ResponseBody
    public CommandConfiguration getConfiguration(final @PathVariable long commandId,
                                                 final @PathVariable String wrapperName) throws NotFoundException {
        final HttpStatus status = isAdminUser();
        if (status != null) {
            return null;
        }
        return commandService.getSiteConfiguration(commandId, wrapperName);
    }

    @XapiRequestMapping(value = {"/wrappers/{wrapperId}/config"}, method = GET)
    @ResponseBody
    public CommandConfiguration getConfiguration(final @PathVariable long wrapperId) throws NotFoundException {
        final HttpStatus status = isAdminUser();
        if (status != null) {
            return null;
        }
        return commandService.getSiteConfiguration(wrapperId);
    }

    // Delete configuration for site + command wrapper
    @XapiRequestMapping(value = {"/commands/{commandId}/wrappers/{wrapperName}/config"}, method = DELETE)
    public ResponseEntity<Void> deleteConfiguration(final @PathVariable long commandId,
                                                    final @PathVariable String wrapperName)
            throws CommandConfigurationException, NotFoundException {
        final HttpStatus status = isAdminUser();
        if (status != null) {
            return new ResponseEntity<>(status);
        }
        final UserI userI = XDAT.getUserDetails();
        commandService.deleteSiteConfiguration(commandId, wrapperName, userI.getLogin());
        return ResponseEntity.noContent().build();
    }

    @XapiRequestMapping(value = {"/wrappers/{wrapperId}/config"}, method = DELETE)
    public ResponseEntity<Void> deleteConfiguration(final @PathVariable long wrapperId)
            throws CommandConfigurationException, NotFoundException {
        final HttpStatus status = isAdminUser();
        if (status != null) {
            return new ResponseEntity<>(status);
        }
        final UserI userI = XDAT.getUserDetails();
        commandService.deleteSiteConfiguration(wrapperId, userI.getLogin());
        return ResponseEntity.noContent().build();
    }

    // Configure for project + command wrapper
    @XapiRequestMapping(value = {"/projects/{project}/commands/{commandId}/wrappers/{wrapperName}/config"}, method = POST)
    public ResponseEntity<Void> createConfiguration(final @RequestBody CommandConfiguration commandConfiguration,
                                                    final @PathVariable String project,
                                                    final @PathVariable long commandId,
                                                    final @PathVariable String wrapperName,
                                                    final @RequestParam(required = false, defaultValue = "true") boolean enable,
                                                    final @RequestParam(required = false) String reason)
            throws CommandConfigurationException, NotFoundException {
        final UserI userI = XDAT.getUserDetails();
        final HttpStatus status = isProjectOwnerOrAdmin(project);
        if (status != null) {
            return new ResponseEntity<>(status);
        }
        commandService.configureForProject(commandConfiguration, project, commandId, wrapperName, enable, userI.getLogin(), reason);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @XapiRequestMapping(value = {"/projects/{project}/wrappers/{wrapperId}/config"}, method = POST)
    public ResponseEntity<Void> createConfiguration(final @RequestBody CommandConfiguration commandConfiguration,
                                                    final @PathVariable String project,
                                                    final @PathVariable long wrapperId,
                                                    final @RequestParam(required = false, defaultValue = "true") boolean enable,
                                                    final @RequestParam(required = false) String reason)
            throws CommandConfigurationException, NotFoundException {
        final UserI userI = XDAT.getUserDetails();
        final HttpStatus status = isProjectOwnerOrAdmin(project);
        if (status != null) {
            return new ResponseEntity<>(status);
        }
        commandService.configureForProject(commandConfiguration, project, wrapperId, enable, userI.getLogin(), reason);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    // Get configuration for project + command wrapper
    @XapiRequestMapping(value = {"/projects/{project}/commands/{commandId}/wrappers/{wrapperName}/config"}, method = GET)
    @ResponseBody
    public CommandConfiguration getConfiguration(final @PathVariable String project,
                                                 final @PathVariable long commandId,
                                                 final @PathVariable String wrapperName) throws NotFoundException {
        final HttpStatus status = canReadProjectOrAdmin(project);
        if (status != null) {
            return null;
        }
        return commandService.getProjectConfiguration(project, commandId, wrapperName);
    }

    @XapiRequestMapping(value = {"/projects/{project}/wrappers/{wrapperId}/config"}, method = GET)
    @ResponseBody
    public CommandConfiguration getConfiguration(final @PathVariable String project,
                                                 final @PathVariable long wrapperId) throws NotFoundException {
        final HttpStatus status = canReadProjectOrAdmin(project);
        if (status != null) {
            return null;
        }
        return commandService.getProjectConfiguration(project, wrapperId);
    }

    // Delete configuration for project + command wrapper
    @XapiRequestMapping(value = {"/projects/{project}/commands/{commandId}/wrappers/{wrapperName}/config"}, method = DELETE)
    public ResponseEntity<Void> deleteConfiguration(final @PathVariable String project,
                                                    final @PathVariable long commandId,
                                                    final @PathVariable String wrapperName)
            throws CommandConfigurationException, NotFoundException {
        final HttpStatus status = isProjectOwnerOrAdmin(project);
        if (status != null) {
            return new ResponseEntity<>(status);
        }
        final UserI userI = XDAT.getUserDetails();
        commandService.deleteProjectConfiguration(project, commandId, wrapperName, userI.getLogin());
        return ResponseEntity.noContent().build();
    }

    @XapiRequestMapping(value = {"/projects/{project}/wrappers/{wrapperId}/config"}, method = DELETE)
    public ResponseEntity<Void> deleteConfiguration(final @PathVariable String project,
                                                    final @PathVariable long wrapperId)
            throws CommandConfigurationException, NotFoundException {
        final HttpStatus status = isProjectOwnerOrAdmin(project);
        if (status != null) {
            return new ResponseEntity<>(status);
        }
        final UserI userI = XDAT.getUserDetails();
        commandService.deleteProjectConfiguration(project, wrapperId, userI.getLogin());
        return ResponseEntity.noContent().build();
    }

    /*
    ENABLE/DISABLE
     */
    @XapiRequestMapping(value = {"/commands/{commandId}/wrappers/{wrapperName}/enabled"}, method = GET)
    @ResponseBody
    public Boolean isConfigurationEnabled(final @PathVariable long commandId,
                                          final @PathVariable String wrapperName)
            throws CommandConfigurationException, NotFoundException {
        final HttpStatus status = isAdminUser();
        if (status != null) {
            return false;
        }
        return commandService.isEnabledForSite(commandId, wrapperName);
    }

    @XapiRequestMapping(value = {"/wrappers/{wrapperId}/enabled"}, method = GET)
    @ResponseBody
    public Boolean isConfigurationEnabled(final @PathVariable long wrapperId)
            throws CommandConfigurationException, NotFoundException {
        final HttpStatus status = isAdminUser();
        if (status != null) {
            return false;
        }
        return commandService.isEnabledForSite(wrapperId);
    }

    @XapiRequestMapping(value = {"/commands/{commandId}/wrappers/{wrapperName}/enabled"}, method = PUT)
    public ResponseEntity<Void> enableConfiguration(final @PathVariable long commandId,
                                                    final @PathVariable String wrapperName,
                                                    final @RequestParam(required = false) String reason)
            throws CommandConfigurationException, NotFoundException {
        final UserI userI = XDAT.getUserDetails();
        final HttpStatus status = isAdminUser();
        if (status != null) {
            return new ResponseEntity<>(status);
        }
        commandService.enableForSite(commandId, wrapperName, userI.getLogin(), reason);
        return ResponseEntity.ok().build();
    }

    @XapiRequestMapping(value = {"/wrappers/{wrapperId}/enabled"}, method = PUT)
    public ResponseEntity<Void> enableConfiguration(final @PathVariable long wrapperId,
                                                    final @RequestParam(required = false) String reason)
            throws CommandConfigurationException, NotFoundException {
        final UserI userI = XDAT.getUserDetails();
        final HttpStatus status = isAdminUser();
        if (status != null) {
            return new ResponseEntity<>(status);
        }
        commandService.enableForSite(wrapperId, userI.getLogin(), reason);
        return ResponseEntity.ok().build();
    }

    @XapiRequestMapping(value = {"/commands/{commandId}/wrappers/{wrapperName}/disabled"}, method = PUT)
    public ResponseEntity<Void> disableConfiguration(final @PathVariable long commandId,
                                                     final @PathVariable String wrapperName,
                                                     final @RequestParam(required = false) String reason)
            throws CommandConfigurationException, NotFoundException {
        final UserI userI = XDAT.getUserDetails();
        final HttpStatus status = isAdminUser();
        if (status != null) {
            return new ResponseEntity<>(status);
        }
        commandService.disableForSite(commandId, wrapperName, userI.getLogin(), reason);
        return ResponseEntity.ok().build();
    }

    @XapiRequestMapping(value = {"/wrappers/{wrapperId}/disabled"}, method = PUT)
    public ResponseEntity<Void> disableConfiguration(final @PathVariable long wrapperId,
                                                     final @RequestParam(required = false) String reason)
            throws CommandConfigurationException, NotFoundException {
        final UserI userI = XDAT.getUserDetails();
        final HttpStatus status = isAdminUser();
        if (status != null) {
            return new ResponseEntity<>(status);
        }
        commandService.disableForSite(wrapperId, userI.getLogin(), reason);
        return ResponseEntity.ok().build();
    }

    @XapiRequestMapping(value = {"/projects/{project}/commands/{commandId}/wrappers/{wrapperName}/enabled"}, method = GET)
    @ResponseBody
    public Boolean isConfigurationEnabled(final @PathVariable String project,
                                          final @PathVariable long commandId,
                                          final @PathVariable String wrapperName)
            throws CommandConfigurationException, NotFoundException {
        final HttpStatus status = canReadProjectOrAdmin(project);
        if (status != null) {
            return false;
        }
        return commandService.isEnabledForProject(project, commandId, wrapperName);
    }

    @XapiRequestMapping(value = {"/projects/{project}/wrappers/{wrapperId}/enabled"}, method = GET)
    @ResponseBody
    public Boolean isConfigurationEnabled(final @PathVariable String project,
                                          final @PathVariable long wrapperId)
            throws CommandConfigurationException, NotFoundException {
        final HttpStatus status = canReadProjectOrAdmin(project);
        if (status != null) {
            return false;
        }
        return commandService.isEnabledForProject(project, wrapperId);
    }

    @XapiRequestMapping(value = {"/projects/{project}/commands/{commandId}/wrappers/{wrapperName}/enabled"}, method = PUT)
    public ResponseEntity<Void> enableConfiguration(final @PathVariable String project,
                                                    final @PathVariable long commandId,
                                                    final @PathVariable String wrapperName,
                                                    final @RequestParam(required = false) String reason)
            throws CommandConfigurationException, NotFoundException {
        final UserI userI = XDAT.getUserDetails();
        final HttpStatus status = isProjectOwnerOrAdmin(project);
        if (status != null) {
            return new ResponseEntity<>(status);
        }
        commandService.enableForProject(project, commandId, wrapperName, userI.getLogin(), reason);
        return ResponseEntity.ok().build();
    }

    @XapiRequestMapping(value = {"/projects/{project}/wrappers/{wrapperId}/enabled"}, method = PUT)
    public ResponseEntity<Void> enableConfiguration(final @PathVariable String project,
                                                    final @PathVariable long wrapperId,
                                                    final @RequestParam(required = false) String reason)
            throws CommandConfigurationException, NotFoundException {
        final UserI userI = XDAT.getUserDetails();
        final HttpStatus status = isProjectOwnerOrAdmin(project);
        if (status != null) {
            return new ResponseEntity<>(status);
        }
        commandService.enableForProject(project, wrapperId, userI.getLogin(), reason);
        return ResponseEntity.ok().build();
    }

    @XapiRequestMapping(value = {"/projects/{project}/commands/{commandId}/wrappers/{wrapperName}/disabled"}, method = PUT)
    public ResponseEntity<Void> disableConfiguration(final @PathVariable String project,
                                                     final @PathVariable long commandId,
                                                     final @PathVariable String wrapperName,
                                                     final @RequestParam(required = false) String reason)
            throws CommandConfigurationException, NotFoundException {
        final UserI userI = XDAT.getUserDetails();
        final HttpStatus status = isProjectOwnerOrAdmin(project);
        if (status != null) {
            return new ResponseEntity<>(status);
        }
        commandService.disableForProject(project, commandId, wrapperName, userI.getLogin(), reason);
        return ResponseEntity.ok().build();
    }

    @XapiRequestMapping(value = {"/projects/{project}/wrappers/{wrapperId}/disabled"}, method = PUT)
    public ResponseEntity<Void> disableConfiguration(final @PathVariable String project,
                                                     final @PathVariable long wrapperId,
                                                     final @RequestParam(required = false) String reason)
            throws CommandConfigurationException, NotFoundException {
        final UserI userI = XDAT.getUserDetails();
        final HttpStatus status = isProjectOwnerOrAdmin(project);
        if (status != null) {
            return new ResponseEntity<>(status);
        }
        commandService.disableForProject(project, wrapperId, userI.getLogin(), reason);
        return ResponseEntity.ok().build();
    }

    /*
    SETTINGS
     */
    @XapiRequestMapping(value = {"/container-service/settings"}, method = GET)
    @ResponseBody
    public ContainerServiceSettings getSettings() {
        return containerConfigService.getSettings();
    }

    @XapiRequestMapping(value = {"/container-service/settings/all-enabled"}, method = GET)
    @ResponseBody
    public Boolean allEnabled()
            throws ConfigServiceException {
        final UserI userI = XDAT.getUserDetails();
        final HttpStatus status = isAdminUser();
        if (status != null) {
            return false;
        }
        return containerConfigService.getAllEnabled();
    }

    @XapiRequestMapping(value = {"/container-service/settings/enable-all"}, method = PUT)
    public ResponseEntity<Void> enableAll(final @RequestParam(required = false) String reason)
            throws ConfigServiceException {
        final UserI userI = XDAT.getUserDetails();
        final HttpStatus status = isAdminUser();
        if (status != null) {
            return new ResponseEntity<>(status);
        }
        containerConfigService.enableAll(userI.getLogin(), reason);
        return ResponseEntity.ok().build();
    }

    @XapiRequestMapping(value = {"/container-service/settings/disable-all"}, method = PUT)
    public ResponseEntity<Void> disableAll(final @RequestParam(required = false) String reason)
            throws ConfigServiceException {
        final UserI userI = XDAT.getUserDetails();
        final HttpStatus status = isAdminUser();
        if (status != null) {
            return new ResponseEntity<>(status);
        }
        containerConfigService.disableAll(userI.getLogin(), reason);
        return ResponseEntity.ok().build();
    }

    @XapiRequestMapping(value = {"/container-service/settings/enable-all",
                             "/container-service/settings/disable-all"},
            method = DELETE)
    public ResponseEntity<Void> deleteAllEnabledSetting(final @RequestParam(required = false) String reason)
            throws ConfigServiceException {
        final UserI userI = XDAT.getUserDetails();
        final HttpStatus status = isAdminUser();
        if (status != null) {
            return new ResponseEntity<>(status);
        }
        containerConfigService.deleteAllEnabledSetting(userI.getLogin(), reason);
        return ResponseEntity.noContent().build();
    }

    @XapiRequestMapping(value = {"/projects/{project}/container-service/settings/all-enabled"}, method = GET)
    @ResponseBody
    public Boolean allEnabled(final @PathVariable String project)
            throws ConfigServiceException {
        final UserI userI = XDAT.getUserDetails();
        final HttpStatus status = canReadProjectOrAdmin(project);
        if (status != null) {
            return false;
        }
        return containerConfigService.getAllEnabled(project);
    }

    @XapiRequestMapping(value = {"/projects/{project}/container-service/settings/enable-all"}, method = PUT)
    public ResponseEntity<Void> enableAll(final @PathVariable String project,
                                          final @RequestParam(required = false) String reason)
            throws ConfigServiceException {
        final UserI userI = XDAT.getUserDetails();
        final HttpStatus status = isProjectOwnerOrAdmin(project);
        if (status != null) {
            return new ResponseEntity<>(status);
        }
        containerConfigService.enableAll(project, userI.getLogin(), reason);
        return ResponseEntity.ok().build();
    }

    @XapiRequestMapping(value = {"/projects/{project}/container-service/settings/disable-all"}, method = PUT)
    public ResponseEntity<Void> disableAll(final @PathVariable String project,
                                           final @RequestParam(required = false) String reason)
            throws ConfigServiceException {
        final UserI userI = XDAT.getUserDetails();
        final HttpStatus status = isProjectOwnerOrAdmin(project);
        if (status != null) {
            return new ResponseEntity<>(status);
        }
        containerConfigService.disableAll(project, userI.getLogin(), reason);
        return ResponseEntity.ok().build();
    }

    @XapiRequestMapping(value = {"/projects/{project}/container-service/settings/enable-all",
                             "/projects/{project}/container-service/settings/disable-all"},
            method = DELETE)
    public ResponseEntity<Void> deleteAllEnabledSetting(final @PathVariable String project,
                                                        final @RequestParam(required = false) String reason)
            throws ConfigServiceException {
        final UserI userI = XDAT.getUserDetails();
        final HttpStatus status = isProjectOwnerOrAdmin(project);
        if (status != null) {
            return new ResponseEntity<>(status);
        }
        containerConfigService.deleteAllEnabledSetting(project, userI.getLogin(), reason);
        return ResponseEntity.noContent().build();
    }

    // all above but for projects

    // PUT /container-service/settings/opt-in
    // PUT /container-service/settings/opt-out
    // DELETE /container-service/settings/opt-(in|out)

    // all above but for projects

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

    /**
     * Checks if is permitted.
     *
     * @param projectId the project ID
     *
     * @return the http status
     */
    // TODO: Migrate this to the abstract superclass. Can't right now because XDAT doesn't know about XnatProjectdata, etc.
    private HttpStatus canReadProjectOrAdmin(String projectId) {
        final UserI sessionUser = getSessionUser();
        if (projectId != null) {
            final XnatProjectdata project = AutoXnatProjectdata.getXnatProjectdatasById(projectId, sessionUser, false);
            try {
                return ( Permissions.canRead(sessionUser, project) || getRoleHolder().isSiteAdmin(sessionUser) ) ? null : HttpStatus.FORBIDDEN;
            } catch (Exception e) {
                log.error("Error checking read status for project", e);
                return HttpStatus.INTERNAL_SERVER_ERROR;
            }
        } else {
            return isPermitted() == null ? null : HttpStatus.FORBIDDEN;
        }
    }

    /**
     * Checks if user is owner.
     *
     * @param projectId the project ID
     *
     * @return the http status
     */
    // TODO: Migrate this to the abstract superclass. Can't right now because XDAT doesn't know about XnatProjectdata, etc.
    private HttpStatus isProjectOwnerOrAdmin(String projectId) {
        final UserI sessionUser = getSessionUser();
        if (projectId != null) {
            final XnatProjectdata project = AutoXnatProjectdata.getXnatProjectdatasById(projectId, sessionUser, false);
            try {
                return ( Permissions.isProjectOwner(sessionUser, projectId) || getRoleHolder().isSiteAdmin(sessionUser) ) ? null : HttpStatus.FORBIDDEN;
            } catch (Exception e) {
                log.error("Error checking read status for project", e);
                return HttpStatus.INTERNAL_SERVER_ERROR;
            }
        } else {
            return isPermitted() == null ? null : HttpStatus.FORBIDDEN;
        }
    }

    private HttpStatus isAdminUser() {
        final UserI sessionUser = getSessionUser();
        try {
            return getRoleHolder().isSiteAdmin(sessionUser) ? null : HttpStatus.FORBIDDEN;
        } catch (Exception e) {
            log.error("Error checking whether admin is project owner", e);
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }
}
