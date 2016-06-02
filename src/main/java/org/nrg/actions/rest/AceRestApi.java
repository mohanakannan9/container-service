package org.nrg.actions.rest;

import org.nrg.actions.model.ActionContextExecution;
import org.nrg.actions.model.ActionContextExecutionDto;
import org.nrg.actions.model.Context;
import org.nrg.actions.services.AceService;
import org.nrg.containers.exceptions.DockerServerException;
import org.nrg.containers.exceptions.NoServerPrefException;
import org.nrg.containers.exceptions.NotFoundException;
import org.nrg.framework.annotations.XapiRestController;
import org.nrg.xft.exception.ElementNotFoundException;
import org.nrg.xft.exception.XFTInitException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;
import java.util.Map;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@XapiRestController
@RequestMapping("/aces")
public class AceRestApi {
    private static final String JSON = MediaType.APPLICATION_JSON_UTF8_VALUE;
    private static final String TEXT = MediaType.TEXT_PLAIN_VALUE;
    private static final String FORM = MediaType.APPLICATION_FORM_URLENCODED_VALUE;

    @Autowired
    private AceService aceService;

    @RequestMapping(value = {}, method = GET, produces = JSON)
    public List<ActionContextExecutionDto> getAcesWArgsFromQuery(final @RequestParam Map<String,String> allRequestParams)
            throws XFTInitException, ElementNotFoundException {
        return aceService.resolveAces(Context.fromMap(allRequestParams));
    }

    @RequestMapping(value = {}, method = GET, consumes = FORM, produces = JSON)
    public List<ActionContextExecutionDto> getAcesWArgsInBodyAsForm(final @RequestParam MultiValueMap<String,String> allRequestParams)
            throws XFTInitException, ElementNotFoundException {
        return aceService.resolveAces(Context.fromMap(allRequestParams.toSingleValueMap()));
    }

    @RequestMapping(value = {}, method = GET, consumes = JSON, produces = JSON)
    public List<ActionContextExecutionDto> getAcesWArgsInBodyAsJson(final @RequestParam Map<String,String> allRequestParams)
            throws XFTInitException, ElementNotFoundException {
        return aceService.resolveAces(Context.fromMap(allRequestParams));
    }

    @RequestMapping(value = {}, method = POST, consumes = JSON, produces = JSON)
    public ResponseEntity<ActionContextExecution> executeAce(final @RequestBody ActionContextExecutionDto aceDto)
            throws NoServerPrefException, ElementNotFoundException, NotFoundException, DockerServerException {
        final ActionContextExecution ace = aceService.executeAce(aceDto);
        return new ResponseEntity<>(ace, HttpStatus.CREATED);
    }
//
//    @RequestMapping(value = {"/{id}"}, method = GET, produces = JSON)
//    public Action retrieveAction(final @PathVariable Long id) {
//        return actionService.retrieve(id);
//    }
//
//    @RequestMapping(value = {"/{id}"}, method = POST, consumes = JSON, produces = TEXT)
//    public ResponseEntity<String> updateAction(final @RequestBody ActionDto actionDto,
//                                                final @PathVariable Long id) {
//        actionDto.setId(id);
//        actionService.updateFromDto(actionDto);
//        return new ResponseEntity<>(String.valueOf(id), HttpStatus.OK);
//    }
//
//    @RequestMapping(value = {"/{id}"}, method = DELETE, produces = JSON)
//    public void deleteAction(final @PathVariable Long id) {
//        actionService.delete(id);
//    }

    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(value = {XFTInitException.class})
    public String internalServerError(final Exception e) {
        return "There was an error:\n" + e.getMessage();
    }

    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(value = {DockerServerException.class})
    public String handleDockerServerError(final Exception e) {
        return "The Docker server returned an error:\n" + e.getMessage();
    }

    @ResponseStatus(value = HttpStatus.FAILED_DEPENDENCY)
    @ExceptionHandler(value = {NoServerPrefException.class})
    public String handleFailedDependency() {
        return "Set up Docker server before using this REST endpoint.";
    }
}
