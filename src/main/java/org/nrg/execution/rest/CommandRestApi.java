package org.nrg.execution.rest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.nrg.execution.exceptions.BadRequestException;
import org.nrg.execution.exceptions.CommandVariableResolutionException;
import org.nrg.execution.exceptions.DockerServerException;
import org.nrg.execution.exceptions.NoServerPrefException;
import org.nrg.execution.exceptions.NotFoundException;
import org.nrg.execution.model.Command;
import org.nrg.execution.model.ResolvedCommand;
import org.nrg.execution.services.CommandService;
import org.nrg.framework.annotations.XapiRestController;
import org.nrg.framework.exceptions.NrgRuntimeException;
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
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

@XapiRestController
@RequestMapping("/commands")
@Api("Command API for XNAT Action/Context Execution service")
public class CommandRestApi {
    private static final String JSON = MediaType.APPLICATION_JSON_UTF8_VALUE;
    private static final String TEXT = MediaType.TEXT_PLAIN_VALUE;
    private static final String FORM = MediaType.APPLICATION_FORM_URLENCODED_VALUE;

    @Autowired
    private CommandService commandService;

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
    public String launchCommand(final @RequestBody ResolvedCommand resolvedCommand)
            throws NoServerPrefException, DockerServerException {
        return commandService.launchCommand(resolvedCommand);
    }

    @RequestMapping(value = {"/{id}/launch"}, method = PUT)
    @ApiOperation(value = "Resolve a command from the variable values in the query string, and launch it")
    @ResponseBody
    public String launchCommand(final @PathVariable Long id,
                                final @RequestParam Map<String, String> allRequestParams)
            throws NoServerPrefException, DockerServerException, NotFoundException, BadRequestException {
        try {
            return commandService.launchCommand(id, allRequestParams);
        } catch (CommandVariableResolutionException e) {
            throw new BadRequestException("Must provide value for variable " + e.getVariable().getName() + ".", e);
        }
    }

    @RequestMapping(value = {"/{id}/launch"}, method = POST)
    @ResponseBody
    public String launchCommand(final @PathVariable Long id)
            throws NoServerPrefException, DockerServerException, NotFoundException, BadRequestException {
        try {
            return commandService.launchCommand(id);
        } catch (CommandVariableResolutionException e) {
            throw new BadRequestException("Must provide value for variable " + e.getVariable().getName() + " in request body.", e);
        }
    }

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
