package org.nrg.containers.rest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.nrg.containers.exceptions.*;
import org.nrg.containers.model.command.auto.LaunchReport;
import org.nrg.containers.model.command.auto.LaunchReport.Success;
import org.nrg.containers.model.command.auto.LaunchUi;
import org.nrg.containers.model.command.auto.ResolvedCommand.PartiallyResolvedCommand;
import org.nrg.containers.model.configuration.CommandConfiguration;
import org.nrg.containers.model.container.entity.ContainerEntity;
import org.nrg.containers.services.CommandResolutionService;
import org.nrg.containers.services.CommandService;
import org.nrg.containers.services.ContainerService;
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
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import javax.annotation.Nullable;
import java.util.List;
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

    private final CommandService commandService;
    private final ContainerService containerService;
    private final CommandResolutionService commandResolutionService;

    @Autowired
    public LaunchRestApi(final CommandService commandService,
                         final ContainerService containerService,
                         final CommandResolutionService commandResolutionService,
                         final UserManagementServiceI userManagementService,
                         final RoleHolder roleHolder) {
        super(userManagementService, roleHolder);
        this.commandService = commandService;
        this.containerService = containerService;
        this.commandResolutionService = commandResolutionService;
    }

    /*
    GET A LAUNCH UI
     */
    @XapiRequestMapping(value = {"/wrappers/{wrapperId}/launch"}, method = GET)
    @ApiIgnore // Swagger UI does not correctly show this API endpoint
    @ResponseBody
    public LaunchUi getLaunchUi(final @PathVariable long wrapperId,
                                final @RequestParam Map<String, String> allRequestParams)
            throws NotFoundException, CommandResolutionException, UnauthorizedException {
        log.info("Launch UI requested for wrapper {}", wrapperId);

        return getLaunchUi(null, 0L, null, wrapperId, allRequestParams);
    }

    @XapiRequestMapping(value = {"/commands/{commandId}/wrappers/{wrapperName}/launch"}, method = GET)
    @ApiIgnore // Swagger UI does not correctly show this API endpoint
    @ResponseBody
    public LaunchUi getLaunchUi(final @PathVariable long commandId,
                                final @PathVariable String wrapperName,
                                final @RequestParam Map<String, String> allRequestParams)
            throws NotFoundException, CommandResolutionException, UnauthorizedException {
        log.info("Launch UI requested for command {}, wrapper {}", commandId, wrapperName);

        return getLaunchUi(null, commandId, wrapperName, 0L, allRequestParams);
    }

    @XapiRequestMapping(value = {"/projects/{project}/wrappers/{wrapperId}/launch"}, method = GET)
    @ApiIgnore // Swagger UI does not correctly show this API endpoint
    @ResponseBody
    public LaunchUi getLaunchUi(final @PathVariable String project,
                                final @PathVariable long wrapperId,
                                final @RequestParam Map<String, String> allRequestParams)
            throws NotFoundException, CommandResolutionException, UnauthorizedException {
        log.info("Launch UI requested for project {}, wrapper {}", project, wrapperId);

        final HttpStatus status = canEditProjectOrAdmin(project);
        if (status != null) {
            return null;
        }

        return getLaunchUi(project, 0L, null, wrapperId, allRequestParams);
    }

    @XapiRequestMapping(value = {"/projects/{project}/commands/{commandId}/wrappers/{wrapperName}/launch"}, method = GET)
    @ApiIgnore // Swagger UI does not correctly show this API endpoint
    @ResponseBody
    public LaunchUi getLaunchUi(final @PathVariable String project,
                                final @PathVariable long commandId,
                                final @PathVariable String wrapperName,
                                final @RequestParam Map<String, String> allRequestParams)
            throws NotFoundException, CommandResolutionException, UnauthorizedException {
        log.info("Launch UI requested for project {}, command {}, wrapper {}", project, commandId, wrapperName);

        final HttpStatus status = canEditProjectOrAdmin(project);
        if (status != null) {
            return null;
        }

        return getLaunchUi(project, commandId, wrapperName, 0L, allRequestParams);
    }

    private LaunchUi getLaunchUi(final String project,
                                 final long commandId,
                                 final String wrapperName,
                                 final long wrapperId,
                                 final Map<String, String> allRequestParams)
            throws NotFoundException, CommandResolutionException, UnauthorizedException {
        try {
            log.debug("Preparing to pre-resolve command {}, wrapperName {}, wrapperId {}, in project {} with inputs {}.", commandId, wrapperName, wrapperId, project, allRequestParams);
            final UserI userI = XDAT.getUserDetails();
            final PartiallyResolvedCommand partiallyResolvedCommand = preResolve(project, commandId, wrapperName, wrapperId, allRequestParams, userI);
            log.debug("Done pre-resolving command {}, wrapperName {}, wrapperId {}, in project {}.", commandId, wrapperName, project);

            log.debug("Getting {} configuration for command {}, wrapper name {}, wrapper id {}.", project == null ? "site" : "project " + project, commandId, wrapperName, wrapperId);
            final CommandConfiguration commandConfiguration = getCommandConfiguration(project, commandId, wrapperName, wrapperId);

            log.debug("Creating launch UI.");
            return LaunchUi.create(partiallyResolvedCommand, commandConfiguration);
        } catch (Throwable t) {
            log.error("Error getting launch UI.", t);
            if (Exception.class.isAssignableFrom(t.getClass())) {
                // We can re-throw Exceptions, because Spring has methods to catch them.
                throw t;
            }
            return null;
        }
    }

    private PartiallyResolvedCommand preResolve(final String project,
                                                final long commandId,
                                                final String wrapperName,
                                                final long wrapperId,
                                                final Map<String, String> allRequestParams,
                                                final UserI userI)
            throws NotFoundException, CommandResolutionException, UnauthorizedException {
        return project == null ?
                (commandId == 0L && wrapperName == null ?
                        commandResolutionService.preResolve(wrapperId, allRequestParams, userI) :
                        commandResolutionService.preResolve(commandId, wrapperName, allRequestParams, userI)) :
                (commandId == 0L && wrapperName == null ?
                        commandResolutionService.preResolve(project, wrapperId, allRequestParams, userI) :
                        commandResolutionService.preResolve(project, commandId, wrapperName, allRequestParams, userI));
    }

    private CommandConfiguration getCommandConfiguration(final String project,
                                                         final long commandId,
                                                         final String wrapperName,
                                                         final long wrapperId) throws NotFoundException {
        return project == null ?
                (commandId == 0L && wrapperName == null ?
                        commandService.getSiteConfiguration(wrapperId) :
                        commandService.getSiteConfiguration(commandId, wrapperName)) :
                (commandId == 0L && wrapperName == null ?
                        commandService.getProjectConfiguration(project, wrapperId) :
                        commandService.getProjectConfiguration(project, commandId, wrapperName));
    }

    /*
    LAUNCH CONTAINERS
     */
    @XapiRequestMapping(value = {"/wrappers/{wrapperId}/launch"}, method = POST)
    @ApiIgnore // Swagger UI does not correctly show this API endpoint
    @ResponseBody
    public LaunchReport launchCommandWQueryParams(final @PathVariable long wrapperId,
                                                  final @RequestParam Map<String, String> allRequestParams) {
        log.info("Launch requested for command id " + String.valueOf(wrapperId));

        return launchContainer(null, 0L, null, wrapperId, allRequestParams);
    }

    @XapiRequestMapping(value = {"/wrappers/{wrapperId}/launch"}, method = POST, consumes = {JSON})
    @ApiOperation(value = "Resolve a command from the variable values in the request body, and launch it")
    @ResponseBody
    public LaunchReport launchCommandWJsonBody(final @PathVariable long wrapperId,
                                               final @RequestBody Map<String, String> allRequestParams) {
        log.info("Launch requested for command wrapper id " + String.valueOf(wrapperId));

        return launchContainer(null, 0L, null, wrapperId, allRequestParams);
    }

    @XapiRequestMapping(value = {"/projects/{project}/wrapper/{wrapperId}/launch"}, method = POST)
    @ApiIgnore // Swagger UI does not correctly show this API endpoint
    @ResponseBody
    public LaunchReport launchCommandWQueryParams(final @PathVariable String project,
                                                  final @PathVariable long wrapperId,
                                                  final @RequestParam Map<String, String> allRequestParams) {
        log.info("Launch requested for wrapper id " + String.valueOf(wrapperId));

        final HttpStatus status = canEditProjectOrAdmin(project);
        if (status != null) {
            return null;
        }

        return launchContainer(project, 0L, null, wrapperId, allRequestParams);
    }

    @XapiRequestMapping(value = {"/projects/{project}/wrappers/{wrapperId}/launch"}, method = POST, consumes = {JSON})
    @ApiOperation(value = "Resolve a command from the variable values in the request body, and launch it")
    @ResponseBody
    public LaunchReport launchCommandWJsonBody(final @PathVariable String project,
                                               final @PathVariable long wrapperId,
                                               final @RequestBody Map<String, String> allRequestParams) {
        log.info("Launch requested for wrapper id " + String.valueOf(wrapperId));

        final HttpStatus status = canEditProjectOrAdmin(project);
        if (status != null) {
            return null;
        }

        return launchContainer(project, 0L, null, wrapperId, allRequestParams);
    }

    /*
    LAUNCH COMMAND + WRAPPER BY NAME
     */
    @XapiRequestMapping(value = {"/commands/{commandId}/wrappers/{wrapperName}/launch"}, method = POST)
    @ApiIgnore // Swagger UI does not correctly show this API endpoint
    @ResponseBody
    public ResponseEntity<LaunchReport> launchCommandWQueryParams(final @PathVariable long commandId,
                                                                  final @PathVariable String wrapperName,
                                                                  final @RequestParam Map<String, String> allRequestParams)
            throws NoServerPrefException, DockerServerException, NotFoundException, BadRequestException, CommandResolutionException, ContainerException {
        log.info("Launch requested for command {}, wrapper {}", commandId, wrapperName);

        return returnLaunchReportWithStatus(launchContainer(null, commandId, wrapperName, 0L, allRequestParams));
    }

    @XapiRequestMapping(value = {"/commands/{commandId}/wrappers/{wrapperName}/launch"}, method = POST, consumes = {JSON})
    @ApiOperation(value = "Resolve a command from the variable values in the request body, and launch it")
    public ResponseEntity<LaunchReport> launchCommandWJsonBody(final @PathVariable long commandId,
                                                               final @PathVariable String wrapperName,
                                                               final @RequestBody Map<String, String> allRequestParams)
            throws NoServerPrefException, DockerServerException, NotFoundException, BadRequestException, CommandResolutionException, ContainerException {
        log.info("Launch requested for command {}, wrapper {}", commandId, wrapperName);

        return returnLaunchReportWithStatus(launchContainer(null, commandId, wrapperName, 0L, allRequestParams));
    }

    @XapiRequestMapping(value = {"/projects/{project}/commands/{commandId}/wrappers/{wrapperName}/launch"}, method = POST)
    @ApiIgnore // Swagger UI does not correctly show this API endpoint
    public ResponseEntity<LaunchReport> launchCommandWQueryParams(final @PathVariable String project,
                                                                  final @PathVariable long commandId,
                                                                  final @PathVariable String wrapperName,
                                                                  final @RequestParam Map<String, String> allRequestParams) {
        log.info("Launch requested for command {}, wrapper {}", commandId, wrapperName);

        final HttpStatus status = canEditProjectOrAdmin(project);
        if (status != null) {
            return null;
        }

        return returnLaunchReportWithStatus(launchContainer(project, commandId, wrapperName, 0L, allRequestParams));
    }

    @XapiRequestMapping(value = {"/projects/{project}/commands/{commandId}/wrappers/{wrapperName}/launch"}, method = POST, consumes = {JSON})
    @ApiOperation(value = "Resolve a command from the variable values in the request body, and launch it")
    public ResponseEntity<LaunchReport> launchCommandWJsonBody(final @PathVariable String project,
                                                               final @PathVariable long commandId,
                                                               final @PathVariable String wrapperName,
                                                               final @RequestBody Map<String, String> allRequestParams) {
        log.info("Launch requested for command {}, wrapper {}", commandId, wrapperName);

        final HttpStatus status = canEditProjectOrAdmin(project);
        if (status != null) {
            return null;
        }

        final LaunchReport launchReport = launchContainer(project, commandId, wrapperName, 0L, allRequestParams);
        return returnLaunchReportWithStatus(launchReport);
    }

    private LaunchReport launchContainer(@Nullable final String project,
                                         final long commandId,
                                         @Nullable final String wrapperName,
                                         final long wrapperId,
                                         final Map<String, String> allRequestParams) {
        final UserI userI = XDAT.getUserDetails();
        try {
            final ContainerEntity containerEntity =
                    project == null ?
                            (commandId == 0L && wrapperName == null ?
                                    containerService.resolveCommandAndLaunchContainer(wrapperId, allRequestParams, userI) :
                                    containerService.resolveCommandAndLaunchContainer(commandId, wrapperName, allRequestParams, userI)) :
                            (commandId == 0L && wrapperName == null ?
                                    containerService.resolveCommandAndLaunchContainer(project, wrapperId, allRequestParams, userI) :
                                    containerService.resolveCommandAndLaunchContainer(project, commandId, wrapperName, allRequestParams, userI));
            if (log.isInfoEnabled()) {
                log.info("Launched command {}, wrapper {}. Produced container {}.", commandId, wrapperName,
                        containerEntity != null ? containerEntity.getId() : null);
                if (log.isDebugEnabled()) {
                    log.debug(containerEntity != null ? containerEntity.toString() : "Container execution object is null.");
                }
            }
            final String containerId = (containerEntity == null || containerEntity.getContainerId() == null) ? "null" : containerEntity.getContainerId();
            return LaunchReport.Success.create(allRequestParams, containerId, ""+commandId, wrapperName);
        } catch (Throwable t) {
            if (log.isInfoEnabled()) {
                log.error("Launch failed for command wrapper name {}.", wrapperName);
                log.error(mapLogString("Params: ", allRequestParams));
                log.error("Exception: ", t);
            }
            return LaunchReport.Failure.create(allRequestParams, t.getMessage(), ""+commandId, wrapperName);
        }
    }

    private String mapLogString(final String title, final Map<String, String> map) {
        final StringBuilder messageBuilder = new StringBuilder(title);
        for (Map.Entry<String, String> entry : map.entrySet()) {
            messageBuilder.append(entry.getKey());
            messageBuilder.append(": ");
            messageBuilder.append(entry.getValue());
            messageBuilder.append(", ");
        }
        return messageBuilder.substring(0, messageBuilder.length() - 2);
    }

    private ResponseEntity<LaunchReport> returnLaunchReportWithStatus(final LaunchReport launchReport) {
        if (Success.class.isAssignableFrom(launchReport.getClass())) {
            return ResponseEntity.ok(launchReport);
        } else {
            // TODO It would be better to return different stati for the different exception types.
            // But I don't think I want to throw an exception here, because I want the report to
            // be in the body. So it is what it is.
            return ResponseEntity.status(500).body(launchReport);
        }
    }

    /*
    BULK LAUNCH
     */
    @XapiRequestMapping(value = {"/commands/{commandId}/wrappers/{wrapperName}/bulklaunch"}, method = POST, consumes = {JSON})
    @ApiOperation(value = "Resolve a command from the variable values in the request body, and launch it")
    @ResponseBody
    public LaunchReport.BulkLaunchReport bulklaunch(final @PathVariable long commandId,
                                                    final @PathVariable String wrapperName,
                                                    final @RequestBody List<Map<String, String>> allRequestParams) {
        log.info("Launch requested for command {}, wrapper name {}.", commandId, wrapperName);
        return bulkLaunch(null, commandId, wrapperName, 0L, allRequestParams);
    }

    @XapiRequestMapping(value = {"/wrappers/{wrapperId}/bulklaunch"}, method = POST, consumes = {JSON})
    @ApiOperation(value = "Resolve a command from the variable values in the request body, and launch it")
    @ResponseBody
    public LaunchReport.BulkLaunchReport bulklaunch(final @PathVariable long wrapperId,
                                                    final @RequestBody List<Map<String, String>> allRequestParams) {
        log.info("Launch requested for wrapper id {}.", wrapperId);
        return bulkLaunch(null, 0L, null, wrapperId, allRequestParams);
    }

    @XapiRequestMapping(value = {"/projects/{project}/commands/{commandId}/wrappers/{wrapperName}/bulklaunch"}, method = POST, consumes = {JSON})
    @ApiOperation(value = "Resolve a command from the variable values in the request body, and launch it")
    @ResponseBody
    public LaunchReport.BulkLaunchReport bulklaunch(final @PathVariable String project,
                                                    final @PathVariable long commandId,
                                                    final @PathVariable String wrapperName,
                                                    final @RequestBody List<Map<String, String>> allRequestParams) {
        log.info("Launch requested for command {}, wrapper name {}, project {}.", commandId, wrapperName, project);
        return bulkLaunch(project, commandId, wrapperName, 0L, allRequestParams);
    }

    @XapiRequestMapping(value = {"/projects/{project}/wrappers/{wrapperId}/bulklaunch"}, method = POST, consumes = {JSON})
    @ApiOperation(value = "Resolve a command from the variable values in the request body, and launch it")
    @ResponseBody
    public LaunchReport.BulkLaunchReport bulklaunch(final @PathVariable String project,
                                                    final @PathVariable long wrapperId,
                                                    final @RequestBody List<Map<String, String>> allRequestParams) {
        log.info("Launch requested for wrapper id {}, project {}.", wrapperId, project);
        return bulkLaunch(project, 0L, null, wrapperId, allRequestParams);
    }

    private LaunchReport.BulkLaunchReport bulkLaunch(final String project,
                                                     final long commandId,
                                                     final String wrapperName,
                                                     final long wrapperId,
                                                     final List<Map<String, String>> allRequestParams) {

        final LaunchReport.BulkLaunchReport.Builder reportBuilder = LaunchReport.BulkLaunchReport.builder();
        for (final Map<String, String> paramsSet : allRequestParams) {
            reportBuilder.addReport(launchContainer(project, commandId, wrapperName, wrapperId, paramsSet));
        }

        return reportBuilder.build();
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

    /**
     * Checks if is permitted.
     *
     * @param projectId the project ID
     *
     * @return the http status
     */
    // TODO: Migrate this to the abstract superclass. Can't right now because XDAT doesn't know about XnatProjectdata, etc.
    protected HttpStatus canEditProjectOrAdmin(String projectId) {
        final UserI sessionUser = getSessionUser();
        if (projectId != null) {
            final XnatProjectdata project = AutoXnatProjectdata.getXnatProjectdatasById(projectId, sessionUser, false);
            try {
                return ( Permissions.canEdit(sessionUser, project) || getRoleHolder().isSiteAdmin(sessionUser) ) ? null : HttpStatus.FORBIDDEN;
            } catch (Exception e) {
                log.error("Error checking edit status for project", e);
                return HttpStatus.INTERNAL_SERVER_ERROR;
            }
        } else {
            return isPermitted() == null ? null : HttpStatus.FORBIDDEN;
        }
    }
}
