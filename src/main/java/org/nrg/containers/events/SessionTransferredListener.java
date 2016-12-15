package org.nrg.containers.events;

//import org.apache.commons.codec.binary.StringUtils;
import org.apache.commons.lang3.StringUtils;
import org.nrg.framework.services.NrgEventService;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.security.helpers.Users;
import org.nrg.xdat.security.user.exceptions.UserInitException;
import org.nrg.xdat.security.user.exceptions.UserNotFoundException;
import org.nrg.xft.event.entities.WorkflowStatusEvent;
import org.nrg.xft.security.UserI;
import org.springframework.stereotype.Service;
import reactor.bus.Event;
import reactor.bus.EventBus;
import reactor.fn.Consumer;

import javax.inject.Inject;

import static reactor.bus.selector.Selectors.type;


@Service
public class SessionTransferredListener implements Consumer<Event<WorkflowStatusEvent>> {


    @Inject public SessionTransferredListener(EventBus eventBus ){ eventBus.on(type(WorkflowStatusEvent.class), this); }

    //*
    // Translate "Transferred" workflow event into SessionArchivedEvent for workflow events containing Session type
    //*
    @Override
    public void accept(Event<WorkflowStatusEvent> event) {
        final WorkflowStatusEvent wfsEvent = event.getData();

        if (StringUtils.equals(wfsEvent.getEventId(), "Transferred") && wfsEvent.getEntityType().contains("Session")) {
            try {
                final UserI user = Users.getUser(wfsEvent.getUserId());
                final XnatImagesessiondata session = XnatImagesessiondata.getXnatImagesessiondatasById(wfsEvent.getEntityId(), user, true);
                final NrgEventService eventService = XDAT.getContextService().getBean(NrgEventService.class);
                eventService.triggerEvent(new SessionArchiveEvent(session, user));

            } catch (UserNotFoundException | UserInitException e) {
                e.printStackTrace();
            }
        }
    }
}
