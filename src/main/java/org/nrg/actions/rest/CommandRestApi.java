package org.nrg.actions.rest;

import org.nrg.actions.model.Command;
import org.nrg.actions.services.CommandService;
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
@RequestMapping("/commands")
public class CommandRestApi {
    private static final String JSON = MediaType.APPLICATION_JSON_UTF8_VALUE;
    private static final String TEXT = MediaType.TEXT_PLAIN_VALUE;
    private static final String FORM = MediaType.APPLICATION_FORM_URLENCODED_VALUE;

    @Autowired
    private CommandService commandService;

    @RequestMapping(value = {}, method = GET, produces = {JSON, TEXT})
    public List<Command> getCommands() {
        return commandService.getAll();
    }

    @RequestMapping(value = {}, method = POST, consumes = JSON, produces = TEXT)
    public ResponseEntity<String> createCommand(final @RequestBody Command command) {
        final Command created = commandService.create(command);
        return new ResponseEntity<>(String.valueOf(created.getId()), HttpStatus.CREATED);
    }

    @RequestMapping(value = {"/{id}"}, method = GET, produces = JSON)
    public Command retrieveCommand(final @PathVariable Long id) {
        return commandService.retrieve(id);
    }

    @RequestMapping(value = {"/{id}"}, method = POST, consumes = JSON, produces = TEXT)
    public ResponseEntity<String> updateCommand(final @RequestBody Command command,
                                                final @PathVariable Long id) {
        command.setId(id);
        commandService.update(command);
        return new ResponseEntity<>(String.valueOf(id), HttpStatus.OK);
    }

    @RequestMapping(value = {"/{id}"}, method = DELETE, produces = JSON)
    public void deleteCommand(final @PathVariable Long id) {
        commandService.delete(id);
    }
}
