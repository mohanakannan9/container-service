package org.nrg.containers.rest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.nrg.containers.exceptions.*;
import org.nrg.containers.model.Command;
import org.nrg.containers.model.ContainerExecution;
import org.nrg.containers.model.ResolvedCommand;
import org.nrg.containers.services.CommandService;
import org.nrg.framework.annotations.XapiRestController;
import org.nrg.framework.exceptions.NrgRuntimeException;
import org.nrg.xdat.XDAT;
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

import java.util.List;
import java.util.Map;

import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@XapiRestController
@RequestMapping("/commands")
@Api("Command API for XNAT Action/Context Execution service")
public class CommandRestApi {
    private static final Logger log = LoggerFactory.getLogger(CommandRestApi.class);
    private static final String JSON = MediaType.APPLICATION_JSON_UTF8_VALUE;
    private static final String TEXT = MediaType.TEXT_PLAIN_VALUE;
    private static final String FORM = MediaType.APPLICATION_FORM_URLENCODED_VALUE;

    private CommandService commandService;

    @Autowired
    public CommandRestApi(final CommandService commandService) {
        this.commandService = commandService;
    }

    @RequestMapping(value = {}, method = GET)
    @ApiOperation(value = "Get all Commands")
    @ResponseBody
    public List<Command> getCommands() {
        return commandService.getAll();
    }

    @RequestMapping(value = {"/{id}"}, method = GET)
    @ApiOperation(value = "Get a Command")
    @ResponseBody
    public Command retrieveCommand(final @PathVariable Long id) {
        return commandService.retrieve(id);
    }

    @RequestMapping(value = {}, method = POST, produces = JSON)
    @ApiOperation(value = "Create a Command", code = 201)
    @ApiResponses({
            @ApiResponse(code = 201, message = "Created", response = Command.class),
            @ApiResponse(code = 415, message = "Set the Content-type header on the request")
    })
    public ResponseEntity<Command> createCommand(final @RequestBody Command command)
            throws BadRequestException {
        try {
            final Command created = commandService.create(command);
            return new ResponseEntity<>(created, HttpStatus.CREATED);
        } catch (NrgRuntimeException e) {
            throw new BadRequestException(e);
        }
    }

    @RequestMapping(value = {"/{id}"}, method = POST)
    @ApiOperation(value = "Update a Command")
    @ResponseBody
    public Command updateCommand(final @RequestBody Command command,
                                 final @PathVariable Long id) {
        command.setId(id);
        commandService.update(command);
        return command;
    }

    @RequestMapping(value = {"/{id}"}, method = DELETE)
    @ApiOperation(value = "Delete a Command", code = 204)
    public ResponseEntity<String> deleteCommand(final @PathVariable Long id) {
        commandService.delete(id);
        return new ResponseEntity<>("", HttpStatus.NO_CONTENT);
    }

    @RequestMapping(value = {"/launch"}, method = POST)
    @ApiOperation(value = "Launch a container from a resolved command")
    @ResponseBody
    public ContainerExecution launchCommand(final @RequestBody ResolvedCommand resolvedCommand)
            throws NoServerPrefException, DockerServerException {
        final UserI userI = XDAT.getUserDetails();
        return commandService.launchResolvedCommand(resolvedCommand, userI);
    }

    @RequestMapping(value = {"/{id}/launch"}, method = POST)
    @ApiOperation(value = "Resolve a command from the variable values in the query string, and launch it")
    @ResponseBody
    public ContainerExecution launchCommand(final @PathVariable Long id,
                                            final @RequestParam Map<String, String> allRequestParams)
            throws NoServerPrefException, DockerServerException, NotFoundException, BadRequestException, CommandResolutionException {
        final UserI userI = XDAT.getUserDetails();
        try {
            return commandService.resolveAndLaunchCommand(id, allRequestParams, userI);
        } catch (CommandInputResolutionException e) {
            throw new BadRequestException("Must provide value for variable " + e.getInput().getName() + ".", e);
        }
    }

    // HACKY TEST API ENDPOINTS
//    @RequestMapping(value = "/{id}/resolve", method = POST)
//    @ResponseBody
//    public ResolvedCommand resolve(final @PathVariable Long id,
//                                   final @RequestParam Map<String, String> allRequestParams)
//            throws NotFoundException, CommandInputResolutionException, NoServerPrefException, XFTInitException {
//        final UserI userI = XDAT.getUserDetails();
//        final Command command = commandService.retrieve(id);
//        if (command == null) {
//            throw new NotFoundException("Could not find Command with id " + id);
//        }
//
//        if (allRequestParams.containsKey("id")) {
//            final String itemId = allRequestParams.get("id");
//            if (itemId.contains(":")) {
//                final String sessionId = StringUtils.substringBeforeLast(itemId, ":");
//                final String scanId = StringUtils.substringAfterLast(itemId, ":");
//
//                final XnatImagesessiondata session = XnatImagesessiondata.getXnatImagesessiondatasById(sessionId, userI, false);
//                final XnatImagescandata scan = session.getScanById(scanId);
//
//                return commandService.prepareToLaunchScan(command, session, scan, userI);
//            } else {
//                log.error("Haven't tested anything other than scan yet.");
//            }
//        }
//        return null;
//    }

//    @RequestMapping(value = "/{id}/launchtest", method = POST)
//    @ResponseBody
//    public ContainerExecution launchTest(final @PathVariable Long id,
//                                   final @RequestParam Map<String, String> allRequestParams)
//            throws NotFoundException, CommandInputResolutionException, NoServerPrefException, XFTInitException, DockerServerException {
//        final UserI userI = XDAT.getUserDetails();
//
//        if (allRequestParams.containsKey("id")) {
//            final String itemId = allRequestParams.get("id");
//            if (itemId.contains(":")) {
//                final String sessionId = StringUtils.substringBeforeLast(itemId, ":");
//                final String scanId = StringUtils.substringAfterLast(itemId, ":");
//
//                final XnatImagesessiondata session = XnatImagesessiondata.getXnatImagesessiondatasById(sessionId, userI, false);
//                if (session == null) {
//                    throw new NotFoundException(String.format("No session with id %s.", sessionId));
//                }
//                final XnatImagescandata scan = session.getScanById(scanId);
//                if (scan == null) {
//                    throw new NotFoundException(String.format("No scan with id %s on session with id %s.", scanId, sessionId));
//                }
//
//                return commandService.resolveAndLaunchCommand(id, userI, session, scan);
//            } else {
//                log.error("Haven't tested anything other than scan yet.");
//            }
//        }
//        return null;
//    }

    @ResponseStatus(value = HttpStatus.FAILED_DEPENDENCY)
    @ExceptionHandler(value = {NoServerPrefException.class})
    public String handleFailedDependency(final Exception ignored) {
        return "Set up Docker server before using this REST endpoint.";
    }

    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(value = {DockerServerException.class})
    public String handleDockerServerError(final Exception e) {
        return "The Docker server returned an error:\n" + e.getMessage();
    }
}
