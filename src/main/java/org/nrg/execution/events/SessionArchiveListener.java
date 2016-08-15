package org.nrg.execution.events;

import org.nrg.execution.events.ScanArchiveEvent;
import org.nrg.execution.events.SessionArchiveEvent;
import org.nrg.execution.exceptions.BadRequestException;
import org.nrg.execution.exceptions.CommandVariableResolutionException;
import org.nrg.execution.exceptions.DockerServerException;
import org.nrg.execution.exceptions.NoServerPrefException;
import org.nrg.execution.exceptions.NotFoundException;
import org.nrg.execution.model.CommandEventMapping;
import org.nrg.execution.services.CommandEventMappingService;
import org.nrg.execution.services.CommandService;
import org.nrg.framework.services.NrgEventService;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.om.XnatImagescandata;
import org.nrg.xft.exception.ElementNotFoundException;
import org.nrg.xft.exception.XFTInitException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.bus.Event;
import reactor.bus.EventBus;
import reactor.fn.Consumer;

import javax.inject.Inject;
import java.util.List;

import static reactor.bus.selector.Selectors.type;


@Service
public class SessionArchiveListener implements Consumer<Event<SessionArchiveEvent>> {

    @Autowired
    CommandService commandService;

    @Autowired
    CommandEventMappingService commandEventMappingService;

    @Inject public SessionArchiveListener( EventBus eventBus ){
        eventBus.on(type(SessionArchiveEvent.class), this);
    }

    @Override
    public void accept(Event<SessionArchiveEvent> event) {
        final SessionArchiveEvent sessionArchivedEvent = event.getData();

        // Fire ScanArchiveEvent for each contained scan
        final List<XnatImagescandata> scans =  sessionArchivedEvent.getSession().getScans_scan();
        final NrgEventService eventService = XDAT.getContextService().getBean(NrgEventService.class);
        for (XnatImagescandata scan : scans) {
            eventService.triggerEvent(new ScanArchiveEvent(scan, sessionArchivedEvent.getUser()));
        }

        // Find commands defined for this event type
        List<CommandEventMapping> commandEventMappings = commandEventMappingService.findByEventType(sessionArchivedEvent.getEventId());

        if (commandEventMappings != null && !commandEventMappings.isEmpty()){
            for (CommandEventMapping commandEventMapping: commandEventMappings) {
                Long commandId = commandEventMapping.getCommandId();
                try {
                    commandService.launchCommand(commandId, sessionArchivedEvent.getUser(), sessionArchivedEvent.getSession());
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
                }
            }
        }
    }


}
