package org.nrg.execution.listener;

import org.apache.commons.codec.binary.StringUtils;
import org.apache.turbine.om.security.User;
import org.nrg.execution.exceptions.*;
import org.nrg.execution.model.CommandEventMapping;
import org.nrg.execution.services.CommandEventMappingService;
import org.nrg.execution.services.CommandService;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.entities.UserAuthI;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.om.base.auto.AutoXnatImagesessiondata;
import org.nrg.xdat.security.helpers.Users;
import org.nrg.xdat.security.user.exceptions.UserInitException;
import org.nrg.xdat.security.user.exceptions.UserNotFoundException;
import org.nrg.xft.event.XftItemEvent;
import org.nrg.xft.event.entities.WorkflowStatusEvent;
import org.nrg.xft.event.persist.PersistentWorkflowUtils;
import org.nrg.xft.exception.ElementNotFoundException;
import org.nrg.xft.exception.MetaDataException;
import org.nrg.xft.exception.XFTInitException;
import org.nrg.xft.security.UserI;
import org.nrg.xnat.event.listeners.PipelineEmailHandlerAbst;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import reactor.bus.Event;
import reactor.bus.EventBus;
import reactor.fn.Consumer;

import javax.inject.Inject;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import static reactor.bus.selector.Selectors.R;
import static reactor.bus.selector.Selectors.type;

/**
 * Created by Kelsey on 7/19/16.
 */

@Service
public class SessionArchiveListener implements Consumer<Event<WorkflowStatusEvent>> {

    @Autowired
    CommandService commandService;

    @Autowired
    CommandEventMappingService commandEventMappingService;

     /**
     * Instantiates a new xft item event listener.
     *
     * @param eventBus the event bus
     */
    @Inject public SessionArchiveListener( EventBus eventBus ){
        eventBus.on(type(WorkflowStatusEvent.class), this);
    }


    @Override
    public void accept(Event<WorkflowStatusEvent> event) {
        final WorkflowStatusEvent wfsEvent = event.getData();

        if (StringUtils.equals(wfsEvent.getEventId(), "Transferred") && wfsEvent.getEntityType().contains("Session")) {

            // Find commands defined for this event type
            List<CommandEventMapping> commandEventMappings = commandEventMappingService.findByEventType(wfsEvent.getEventId());


            if (commandEventMappings != null && !commandEventMappings.isEmpty()){
                UserI user = null;
                XnatImagesessiondata session = null;
                try {
                    user = Users.getUser(wfsEvent.getUserId());
                } catch (UserNotFoundException e) {
                    e.printStackTrace();
                } catch (UserInitException e) {
                    e.printStackTrace();
                }
                String sessionId = wfsEvent.getEntityId();
                session = XnatImagesessiondata.getXnatImagesessiondatasById(sessionId, user, true);
                for (CommandEventMapping commandEventMapping: commandEventMappings) {

                    Long commandId = commandEventMapping.getCommandId();

                    try {
                        commandService.launchCommand(commandId, user, session);
                    } catch (NotFoundException e) {
                        e.printStackTrace();
                    } catch (CommandVariableResolutionException e) {
                        e.printStackTrace();
                    } catch (NoServerPrefException e) {
                        e.printStackTrace();
                    } catch (DockerServerException e) {
                        e.printStackTrace();
                    } catch (BadRequestException e) {
                        e.printStackTrace();
                    } catch (XFTInitException e) {
                        e.printStackTrace();
                    } catch (ElementNotFoundException e) {
                        e.printStackTrace();
                    } catch (AceInputException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
