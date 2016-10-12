package org.nrg.execution.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import org.nrg.execution.exceptions.CommandInputResolutionException;
import org.nrg.execution.exceptions.DockerServerException;
import org.nrg.execution.exceptions.NoServerPrefException;
import org.nrg.execution.exceptions.NotFoundException;
import org.nrg.execution.model.CommandEventMapping;
import org.nrg.execution.model.xnat.Scan;
import org.nrg.execution.model.xnat.Session;
import org.nrg.execution.services.CommandEventMappingService;
import org.nrg.execution.services.CommandService;
import org.nrg.framework.services.NrgEventService;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xft.exception.XFTInitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.bus.Event;
import reactor.bus.EventBus;
import reactor.fn.Consumer;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;

import static reactor.bus.selector.Selectors.type;


@Service
public class SessionArchiveListenerAndCommandLauncher implements Consumer<Event<SessionArchiveEvent>> {
    private static final Logger log = LoggerFactory.getLogger(SessionArchiveListenerAndCommandLauncher.class);
    private static final String EVENT_ID = "SessionArchived";

    @Autowired private ObjectMapper mapper;
    @Autowired private CommandService commandService;
    @Autowired private CommandEventMappingService commandEventMappingService;
    @Autowired private NrgEventService eventService;

    @Inject public SessionArchiveListenerAndCommandLauncher(EventBus eventBus ){
        eventBus.on(type(SessionArchiveEvent.class), this);
    }

    @Override
    public void accept(Event<SessionArchiveEvent> event) {
        final SessionArchiveEvent sessionArchivedEvent = event.getData();
        final Session session = new Session(sessionArchivedEvent.getSession(), sessionArchivedEvent.getUser());

        // Fire ScanArchiveEvent for each contained scan
        for (final Scan scan : session.getScans()) {
            eventService.triggerEvent(new ScanArchiveEventToLaunchCommands(scan, session.getId(), sessionArchivedEvent.getUser()));
        }

        // Find commands defined for this event type
        List<CommandEventMapping> commandEventMappings = commandEventMappingService.findByEventType(EVENT_ID);

        if (commandEventMappings != null && !commandEventMappings.isEmpty()){
            for (CommandEventMapping commandEventMapping: commandEventMappings) {
                Long commandId = commandEventMapping.getCommandId();

                final Map<String, String> runtimeValues = Maps.newHashMap();
                String sessionString = session.getId();
                try {
                    sessionString = mapper.writeValueAsString(session);
                } catch (JsonProcessingException e) {
                    log.error(String.format("Could not serialize Session %s to json.", session), e);
                }
                runtimeValues.put("session", sessionString);
                try {
                    commandService.launchCommand(commandId, runtimeValues, sessionArchivedEvent.getUser());
                }  catch (CommandInputResolutionException e) {
                    e.printStackTrace();
                } catch (NotFoundException e) {
                    e.printStackTrace();
                } catch (DockerServerException e) {
                    e.printStackTrace();
                } catch (NoServerPrefException e) {
                    e.printStackTrace();
                }
            }
        }
    }


}
