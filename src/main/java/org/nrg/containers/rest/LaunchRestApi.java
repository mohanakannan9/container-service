package org.nrg.containers.rest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.nrg.containers.exceptions.BadRequestException;
import org.nrg.containers.exceptions.CommandInputResolutionException;
import org.nrg.containers.exceptions.CommandResolutionException;
import org.nrg.containers.exceptions.CommandValidationException;
import org.nrg.containers.exceptions.ContainerException;
import org.nrg.containers.exceptions.DockerServerException;
import org.nrg.containers.exceptions.NoServerPrefException;
import org.nrg.containers.model.command.auto.BulkLaunchReport;
import org.nrg.containers.model.command.auto.LaunchUi;
import org.nrg.containers.model.command.auto.ResolvedCommand;
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
            throws NotFoundException, CommandResolutionException {
        log.info("Launch UI requested for wrapper {}", wrapperId);
        final UserI userI = XDAT.getUserDetails();
        final PartiallyResolvedCommand partiallyResolvedCommand =
                commandResolutionService.preResolve(wrapperId, allRequestParams, userI);
        final CommandConfiguration commandConfiguration = commandService.getSiteConfiguration(wrapperId);
        return LaunchUi.create(partiallyResolvedCommand, commandConfiguration);
    }

    @XapiRequestMapping(value = {"/commands/{commandId}/wrappers/{wrapperName}/launch"}, method = GET)
    @ApiIgnore // Swagger UI does not correctly show this API endpoint
    @ResponseBody
    public LaunchUi getLaunchUi(final @PathVariable long commandId,
                                final @PathVariable String wrapperName,
                                final @RequestParam Map<String, String> allRequestParams)
            throws NotFoundException, CommandResolutionException {
        log.info("Launch UI requested for command {}, wrapper {}", commandId, wrapperName);
        final UserI userI = XDAT.getUserDetails();
        final PartiallyResolvedCommand partiallyResolvedCommand =
                commandResolutionService.preResolve(commandId, wrapperName, allRequestParams, userI);
        final CommandConfiguration commandConfiguration = commandService.getSiteConfiguration(commandId, wrapperName);
        return LaunchUi.create(partiallyResolvedCommand, commandConfiguration);
    }

    @XapiRequestMapping(value = {"/projects/{project}/wrappers/{wrapperId}/launch"}, method = GET)
    @ApiIgnore // Swagger UI does not correctly show this API endpoint
    @ResponseBody
    public LaunchUi getLaunchUi(final @PathVariable String project,
                                final @PathVariable long wrapperId,
                                final @RequestParam Map<String, String> allRequestParams)
            throws NotFoundException, CommandResolutionException {
        log.info("Launch UI requested for project {}, wrapper {}", project, wrapperId);
        final UserI userI = XDAT.getUserDetails();
        final PartiallyResolvedCommand partiallyResolvedCommand =
                commandResolutionService.preResolve(project, wrapperId, allRequestParams, userI);
        final CommandConfiguration commandConfiguration = commandService.getProjectConfiguration(project, wrapperId);
        return LaunchUi.create(partiallyResolvedCommand, commandConfiguration);
    }

    @XapiRequestMapping(value = {"/projects/{project}/commands/{commandId}/wrappers/{wrapperName}/launch"}, method = GET)
    @ApiIgnore // Swagger UI does not correctly show this API endpoint
    @ResponseBody
    public LaunchUi getLaunchUi(final @PathVariable String project,
                                final @PathVariable long commandId,
                                final @PathVariable String wrapperName,
                                final @RequestParam Map<String, String> allRequestParams)
            throws NotFoundException, CommandResolutionException {
        log.info("Launch UI requested for project {}, command {}, wrapper {}", project, commandId, wrapperName);
        final UserI userI = XDAT.getUserDetails();
        final PartiallyResolvedCommand partiallyResolvedCommand =
                commandResolutionService.preResolve(project, commandId, wrapperName, allRequestParams, userI);
        final CommandConfiguration commandConfiguration = commandService.getProjectConfiguration(project, commandId, wrapperName);
        return LaunchUi.create(partiallyResolvedCommand, commandConfiguration);
    }

    /*
    LAUNCH CONTAINERS
     */
    @XapiRequestMapping(value = {"/commands/launch"}, method = POST)
    @ApiOperation(value = "Launch a container from a resolved command")
    @ResponseBody
    public String launchContainer(final @RequestBody ResolvedCommand resolvedCommand)
            throws NoServerPrefException, DockerServerException, ContainerException {
        final UserI userI = XDAT.getUserDetails();
        final ContainerEntity executed = containerService.launchResolvedCommand(resolvedCommand, userI);
        return executed.getContainerId();
    }

    @XapiRequestMapping(value = {"/wrappers/{wrapperId}/launch"}, method = POST)
    @ApiIgnore // Swagger UI does not correctly show this API endpoint
    @ResponseBody
    public String launchCommandWQueryParams(final @PathVariable long wrapperId,
                                            final @RequestParam Map<String, String> allRequestParams)
            throws NoServerPrefException, DockerServerException, NotFoundException, BadRequestException, CommandResolutionException, ContainerException {
        log.info("Launch requested for command id " + String.valueOf(wrapperId));
        final ContainerEntity executed = launchContainer(wrapperId, allRequestParams);
        return executed.getContainerId();
    }

    @XapiRequestMapping(value = {"/wrappers/{wrapperId}/launch"}, method = POST, consumes = {JSON})
    @ApiOperation(value = "Resolve a command from the variable values in the request body, and launch it")
    @ResponseBody
    public String launchCommandWJsonBody(final @PathVariable long wrapperId,
                                         final @RequestBody Map<String, String> allRequestParams)
            throws NoServerPrefException, DockerServerException, NotFoundException, BadRequestException, CommandResolutionException, ContainerException {
        log.info("Launch requested for command wrapper id " + String.valueOf(wrapperId));
        final ContainerEntity executed = launchContainer(wrapperId, allRequestParams);
        return executed.getContainerId();
    }

    private ContainerEntity launchContainer(final long wrapperId,
                                            final Map<String, String> allRequestParams)
            throws NoServerPrefException, DockerServerException, NotFoundException, CommandResolutionException, BadRequestException, ContainerException {
        final UserI userI = XDAT.getUserDetails();
        try {
            final ContainerEntity containerEntity = containerService.resolveCommandAndLaunchContainer(wrapperId, allRequestParams, userI);
            if (log.isInfoEnabled()) {
                log.info(String.format("Launched command wrapper id %d. Produced container %d.", wrapperId,
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

    @XapiRequestMapping(value = {"/projects/{project}/wrapper/{wrapperId}/launch"}, method = POST)
    @ApiIgnore // Swagger UI does not correctly show this API endpoint
    @ResponseBody
    public String launchCommandWQueryParams(final @PathVariable String project,
                                            final @PathVariable long wrapperId,
                                            final @RequestParam Map<String, String> allRequestParams)
            throws NoServerPrefException, DockerServerException, NotFoundException, BadRequestException, CommandResolutionException, ContainerException {
        log.info("Launch requested for command id " + String.valueOf(wrapperId));
        final ContainerEntity executed = launchContainer(project, wrapperId, allRequestParams);
        return executed.getContainerId();
    }

    @XapiRequestMapping(value = {"/projects/{project}/wrappers/{wrapperId}/launch"}, method = POST, consumes = {JSON})
    @ApiOperation(value = "Resolve a command from the variable values in the request body, and launch it")
    @ResponseBody
    public String launchCommandWJsonBody(final @PathVariable String project,
                                         final @PathVariable long wrapperId,
                                         final @RequestBody Map<String, String> allRequestParams)
            throws NoServerPrefException, DockerServerException, NotFoundException, BadRequestException, CommandResolutionException, ContainerException {
        log.info("Launch requested for command wrapper id " + String.valueOf(wrapperId));
        final ContainerEntity executed = launchContainer(project, wrapperId, allRequestParams);
        return executed.getContainerId();
    }

    private ContainerEntity launchContainer(final String project,
                                            final long wrapperId,
                                            final Map<String, String> allRequestParams)
            throws NoServerPrefException, DockerServerException, NotFoundException, CommandResolutionException, BadRequestException, ContainerException {
        final UserI userI = XDAT.getUserDetails();
        try {
            final ContainerEntity containerEntity = containerService.resolveCommandAndLaunchContainer(project, wrapperId, allRequestParams, userI);
            if (log.isInfoEnabled()) {
                log.info(String.format("Launched command wrapper id %d. Produced container %d.", wrapperId,
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
    LAUNCH COMMAND + WRAPPER BY NAME
     */
    @XapiRequestMapping(value = {"/commands/{commandId}/wrappers/{wrapperName}/launch"}, method = POST)
    @ApiIgnore // Swagger UI does not correctly show this API endpoint
    @ResponseBody
    public String launchCommandWQueryParams(final @PathVariable long commandId,
                                            final @PathVariable String wrapperName,
                                            final @RequestParam Map<String, String> allRequestParams)
            throws NoServerPrefException, DockerServerException, NotFoundException, BadRequestException, CommandResolutionException, ContainerException {
        log.info("Launch requested for command {}, wrapper {}", commandId, wrapperName);
        final ContainerEntity executed = launchContainer(commandId, wrapperName, allRequestParams);
        return executed.getContainerId();
    }

    @XapiRequestMapping(value = {"/commands/{commandId}/wrappers/{wrapperName}/launch"}, method = POST, consumes = {JSON})
    @ApiOperation(value = "Resolve a command from the variable values in the request body, and launch it")
    @ResponseBody
    public String launchCommandWJsonBody(final @PathVariable long commandId,
                                         final @PathVariable String wrapperName,
                                         final @RequestBody Map<String, String> allRequestParams)
            throws NoServerPrefException, DockerServerException, NotFoundException, BadRequestException, CommandResolutionException, ContainerException {
        log.info("Launch requested for command {}, wrapper {}", commandId, wrapperName);
        final ContainerEntity executed = launchContainer(commandId, wrapperName, allRequestParams);
        return executed.getContainerId();
    }

    private ContainerEntity launchContainer(final long commandId,
                                            final String wrapperName,
                                            final Map<String, String> allRequestParams)
            throws NoServerPrefException, DockerServerException, NotFoundException, CommandResolutionException, BadRequestException, ContainerException {
        final UserI userI = XDAT.getUserDetails();
        try {
            final ContainerEntity containerEntity = containerService.resolveCommandAndLaunchContainer(commandId, wrapperName, allRequestParams, userI);
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

    @XapiRequestMapping(value = {"/projects/{project}/commands/{commandId}/wrappers/{wrapperName}/launch"}, method = POST)
    @ApiIgnore // Swagger UI does not correctly show this API endpoint
    @ResponseBody
    public String launchCommandWQueryParams(final @PathVariable String project,
                                            final @PathVariable long commandId,
                                            final @PathVariable String wrapperName,
                                            final @RequestParam Map<String, String> allRequestParams)
            throws NoServerPrefException, DockerServerException, NotFoundException, BadRequestException, CommandResolutionException, ContainerException {
        log.info("Launch requested for command {}, wrapper {}", commandId, wrapperName);
        final ContainerEntity executed = launchContainer(project, commandId, wrapperName, allRequestParams);
        return executed.getContainerId();
    }

    @XapiRequestMapping(value = {"/projects/{project}/commands/{commandId}/wrappers/{wrapperName}/launch"}, method = POST, consumes = {JSON})
    @ApiOperation(value = "Resolve a command from the variable values in the request body, and launch it")
    @ResponseBody
    public String launchCommandWJsonBody(final @PathVariable String project,
                                         final @PathVariable long commandId,
                                         final @PathVariable String wrapperName,
                                         final @RequestBody Map<String, String> allRequestParams)
            throws NoServerPrefException, DockerServerException, NotFoundException, BadRequestException, CommandResolutionException, ContainerException {
        log.info("Launch requested for command {}, wrapper {}", commandId, wrapperName);
        final ContainerEntity executed = launchContainer(project, commandId, wrapperName, allRequestParams);
        return executed.getContainerId();
    }

    private ContainerEntity launchContainer(final String project,
                                            final long commandId,
                                            final String wrapperName,
                                            final Map<String, String> allRequestParams)
            throws NoServerPrefException, DockerServerException, NotFoundException, CommandResolutionException, BadRequestException, ContainerException {
        final UserI userI = XDAT.getUserDetails();
        try {
            final ContainerEntity containerEntity = containerService.resolveCommandAndLaunchContainer(project, commandId, wrapperName, allRequestParams, userI);
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
    BULK LAUNCH
     */
    @XapiRequestMapping(value = {"/wrappers/{wrapperId}/bulklaunch"}, method = POST, consumes = {JSON})
    @ApiOperation(value = "Resolve a command from the variable values in the request body, and launch it")
    @ResponseBody
    public BulkLaunchReport bulklaunchWJsonBody(final @PathVariable long wrapperId,
                                                final @RequestBody List<Map<String, String>> allRequestParams)
            throws NoServerPrefException, DockerServerException, NotFoundException, BadRequestException, CommandResolutionException, ContainerException {
        log.info("Launch requested for command wrapper id " + String.valueOf(wrapperId));
        return bulkLaunch(wrapperId, allRequestParams);
    }

    private BulkLaunchReport bulkLaunch(final long wrapperId,
                                        final List<Map<String, String>> allRequestParams) {
        final UserI userI = XDAT.getUserDetails();

        final BulkLaunchReport.Builder reportBuilder = BulkLaunchReport.builder();
        for (final Map<String, String> paramsSet : allRequestParams) {
            try {
                final ContainerEntity containerEntity = containerService.resolveCommandAndLaunchContainer(wrapperId, paramsSet, userI);

                log.info("Launched command wrapper id {}. Produced container {}.", wrapperId, containerEntity.getId());
                if (log.isDebugEnabled()) {
                    log.debug(mapLogString("Params: ", paramsSet));
                    log.debug(containerEntity.toString());
                }

                final String containerId = containerEntity.getContainerId() == null ? "null" : containerEntity.getContainerId();
                reportBuilder.addSuccess(BulkLaunchReport.Success.create(paramsSet, containerId));
            } catch (Exception e) {
                if (log.isInfoEnabled()) {
                    log.error("Launch failed for command wrapper id {}.", wrapperId);
                    log.error(mapLogString("Params: ", paramsSet));
                    log.error("Exception: ", e);
                }
                reportBuilder.addFailure(BulkLaunchReport.Failure.create(paramsSet, e.getMessage()));
            }
        }

        return reportBuilder.build();
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
