package org.nrg.containers.rest;


import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.nrg.containers.exceptions.BadRequestException;
import org.nrg.containers.model.CommandEventMapping;
import org.nrg.containers.services.CommandEventMappingService;
import org.nrg.framework.annotations.XapiRestController;
import org.nrg.framework.exceptions.NrgRuntimeException;
import org.nrg.xapi.rest.AbstractXapiRestController;
import org.nrg.xapi.rest.XapiRequestMapping;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.security.services.RoleHolder;
import org.nrg.xdat.security.services.UserManagementServiceI;
import org.nrg.xft.security.UserI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.nrg.xdat.security.helpers.Permissions;
import java.util.ArrayList;
import java.util.List;
import org.nrg.xft.security.UserI;
import org.apache.commons.lang3.StringUtils;
import static org.nrg.xdat.security.helpers.AccessLevel.Admin;
import static org.springframework.web.bind.annotation.RequestMethod.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@XapiRestController
@RequestMapping("/commandeventmapping")
@Api("Command Event Mapping API for XNAT Container Service")
public class CommandEventMappingRestApi extends AbstractXapiRestController {
    private static final String JSON = MediaType.APPLICATION_JSON_UTF8_VALUE;
    private static final String TEXT = MediaType.TEXT_PLAIN_VALUE;
    private static final String FORM = MediaType.APPLICATION_FORM_URLENCODED_VALUE;
    private static final Logger log = LoggerFactory.getLogger(CommandEventMappingRestApi.class);

    private CommandEventMappingService commandEventMappingService;

    @Autowired
    public CommandEventMappingRestApi(final CommandEventMappingService commandEventMappingService,
                                      final UserManagementServiceI userManagementService,
                                      final RoleHolder roleHolder) {
        super(userManagementService, roleHolder);
        this.commandEventMappingService = commandEventMappingService;
    }

    @XapiRequestMapping(method = GET)
    @ApiOperation(value = "Get all Commands-Event-Mappings")
    @ResponseBody
    public List<CommandEventMapping> getMappings() {
        List<CommandEventMapping> mappings = commandEventMappingService.getAll();
        List<CommandEventMapping> mappingsCanRead = new ArrayList<CommandEventMapping>();
        try{
            UserI userObj = getSessionUser();
            for(CommandEventMapping mapping: mappings){
                String cProject = mapping.getProjectId();
                if(Permissions.canReadProject(userObj, cProject)) {
                    mappingsCanRead.add(mapping);
                }
            }
        }
        catch(Exception e){
            log.error("", e);
        }
        return mappingsCanRead;
    }


    @XapiRequestMapping(method = POST, restrictTo = Admin)
    public ResponseEntity<CommandEventMapping> createCommand(final @RequestBody CommandEventMapping commandEventMapping)
            throws BadRequestException {
        try {
            if(StringUtils.isBlank(commandEventMapping.getEventType())){
                throw new BadRequestException("Event type must be defined and cannot be empty.");
            }
            final UserI userI = XDAT.getUserDetails();
            commandEventMapping.setSubscriptionUserName(userI.getUsername());
            final CommandEventMapping created = commandEventMappingService.create(commandEventMapping);
            return new ResponseEntity<>(created, HttpStatus.CREATED);
        } catch (NrgRuntimeException e) {
            throw new BadRequestException(e);
        }
    }


    @XapiRequestMapping(value = {"/{id}"}, method = GET)
    @ApiOperation(value = "Get a Command-Event-Mapping")
    @ResponseBody
    public CommandEventMapping retrieve(final @PathVariable Long id) {
        try {
            CommandEventMapping mapping = commandEventMappingService.retrieve(id);

            String cProject = mapping.getProjectId();
            if (Permissions.canReadProject(getSessionUser(), cProject)) {
                return commandEventMappingService.retrieve(id);
            }
        }
        catch(Exception e){
            log.error("", e);
        }
        return null;
    }

    @XapiRequestMapping(value = {"/{id}"}, method = DELETE, restrictTo = Admin)
    @ApiOperation(value = "Delete a CommandEventMapping", code = 204)
    public ResponseEntity<String> delete(final @PathVariable Long id) {
        commandEventMappingService.delete(id);
        return new ResponseEntity<>("", HttpStatus.NO_CONTENT);
    }



}
