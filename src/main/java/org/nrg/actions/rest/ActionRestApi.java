package org.nrg.actions.rest;

import org.nrg.actions.model.Action;
import org.nrg.actions.model.ActionDto;
import org.nrg.actions.model.Command;
import org.nrg.actions.services.ActionService;
import org.nrg.framework.annotations.XapiRestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@XapiRestController
@RequestMapping("/actions")
public class ActionRestApi {
    private static final String JSON = MediaType.APPLICATION_JSON_UTF8_VALUE;
    private static final String TEXT = MediaType.TEXT_PLAIN_VALUE;
    private static final String FORM = MediaType.APPLICATION_FORM_URLENCODED_VALUE;

    @Autowired
    private ActionService actionService;

    @RequestMapping(value = {}, method = GET, produces = {JSON, TEXT})
    public List<Action> getCommands() {
        return actionService.getAll();
    }

    @RequestMapping(value = {}, method = POST, consumes = JSON, produces = TEXT)
    public ResponseEntity<String> createCommand(final @RequestBody ActionDto actionDto) {
        final Action created = actionService.createFromDto(actionDto);
        return new ResponseEntity<>(String.valueOf(created.getId()), HttpStatus.CREATED);
    }

    @RequestMapping(value = {"/{id}"}, method = GET, produces = JSON)
    public Action retrieveAction(final @PathVariable Long id) {
        return actionService.retrieve(id);
    }

    @RequestMapping(value = {"/{id}"}, method = POST, consumes = JSON, produces = TEXT)
    public ResponseEntity<String> updateAction(final @RequestBody ActionDto actionDto,
                                                final @PathVariable Long id) {
        actionDto.setId(id);
        actionService.updateFromDto(actionDto);
        return new ResponseEntity<>(String.valueOf(id), HttpStatus.OK);
    }

    @RequestMapping(value = {"/{id}"}, method = DELETE, produces = JSON)
    public void deleteAction(final @PathVariable Long id) {
        actionService.delete(id);
    }
}
