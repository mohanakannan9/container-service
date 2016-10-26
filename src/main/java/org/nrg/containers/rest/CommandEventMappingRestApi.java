package org.nrg.containers.rest;


import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.nrg.containers.exceptions.BadRequestException;
import org.nrg.containers.model.CommandEventMapping;
import org.nrg.containers.services.CommandEventMappingService;
import org.nrg.framework.annotations.XapiRestController;
import org.nrg.framework.exceptions.NrgRuntimeException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@XapiRestController
@RequestMapping("/commandeventmapping")
@Api("Command Event Mapping API for XNAT Container Service")
public class CommandEventMappingRestApi {
    private static final String JSON = MediaType.APPLICATION_JSON_UTF8_VALUE;
    private static final String TEXT = MediaType.TEXT_PLAIN_VALUE;
    private static final String FORM = MediaType.APPLICATION_FORM_URLENCODED_VALUE;

    private CommandEventMappingService commandEventMappingService;

    @Autowired
    public CommandEventMappingRestApi(final CommandEventMappingService commandEventMappingService) {
        this.commandEventMappingService = commandEventMappingService;
    }

    @RequestMapping(value = {}, method = GET)
    @ApiOperation(value = "Get all Commands Event Mappings")
    @ResponseBody
    public List<CommandEventMapping> getMappings() {
        return commandEventMappingService.getAll();
    }


    @RequestMapping(value = {}, method = POST)
    public ResponseEntity<CommandEventMapping> createCommand(final @RequestBody CommandEventMapping commandEventMapping)
            throws BadRequestException {
        try {
            final CommandEventMapping created = commandEventMappingService.create(commandEventMapping);
            return new ResponseEntity<>(created, HttpStatus.CREATED);
        } catch (NrgRuntimeException e) {
            throw new BadRequestException(e);
        }
    }


}
