package org.nrg.containers.events.listeners;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.nrg.containers.events.model.ScanArchiveEventToLaunchCommands;
import org.nrg.containers.events.model.SessionArchiveEvent;
import org.nrg.containers.exceptions.CommandResolutionException;
import org.nrg.containers.exceptions.ContainerException;
import org.nrg.containers.exceptions.DockerServerException;
import org.nrg.containers.exceptions.NoServerPrefException;
import org.nrg.containers.model.CommandEventMapping;
import org.nrg.containers.model.xnat.Scan;
import org.nrg.containers.model.xnat.Session;
import org.nrg.containers.services.CommandEventMappingService;
import org.nrg.containers.services.ContainerService;
import org.nrg.framework.exceptions.NotFoundException;
import org.nrg.framework.services.NrgEventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.bus.Event;
import reactor.bus.EventBus;
import reactor.fn.Consumer;

import java.util.List;
import java.util.Map;

import static reactor.bus.selector.Selectors.type;

@Service
@SuppressWarnings("unused")
public class SessionArchiveListenerAndCommandLauncher implements Consumer<Event<SessionArchiveEvent>> {
    private static final Logger log = LoggerFactory.getLogger(SessionArchiveListenerAndCommandLauncher.class);
    private static final String EVENT_ID = "SessionArchived";

    private ObjectMapper mapper;
    private ContainerService containerService;
    private CommandEventMappingService commandEventMappingService;
    private NrgEventService eventService;

    @Autowired
    public SessionArchiveListenerAndCommandLauncher(final EventBus eventBus,
                                                    final ObjectMapper mapper,
                                                    final ContainerService containerService,
                                                    final CommandEventMappingService commandEventMappingService,
                                                    final NrgEventService eventService) {
        eventBus.on(type(SessionArchiveEvent.class), this);
        this.mapper = mapper;
        this.containerService = containerService;
        this.commandEventMappingService = commandEventMappingService;
        this.eventService = eventService;
    }

    @Override
    public void accept(Event<SessionArchiveEvent> event) {
        final SessionArchiveEvent sessionArchivedEvent = event.getData();
        final Session session = new Session(sessionArchivedEvent.getSession());

        // Fire ScanArchiveEvent for each contained scan
        for (final Scan scan : session.getScans()) {
            eventService.triggerEvent(new ScanArchiveEventToLaunchCommands(scan, sessionArchivedEvent.getUser()));
        }

        // Find commands defined for this event type
        List<CommandEventMapping> commandEventMappings = commandEventMappingService.findByEventType(EVENT_ID);

        if (commandEventMappings != null && !commandEventMappings.isEmpty()){
            for (CommandEventMapping commandEventMapping: commandEventMappings) {
                final Long commandId = commandEventMapping.getCommandId();
                final String xnatCommandWrapperName = commandEventMapping.getXnatCommandWrapperName();

                final Map<String, String> runtimeValues = Maps.newHashMap();
                String sessionString = session.getUri();
                try {
                    sessionString = mapper.writeValueAsString(session);
                } catch (JsonProcessingException e) {
                    log.error(String.format("Could not serialize Session %s to json.", session), e);
                }
                runtimeValues.put("session", sessionString);
                try {
                    if (log.isInfoEnabled()) {
                        final String wrapperMessage = StringUtils.isNotBlank(xnatCommandWrapperName) ?
                                String.format("wrapper \"%s\"", xnatCommandWrapperName) :
                                "identity wrapper";
                        final String message = String.format(
                                "Launching command %s, %s, for user \"%s\"", commandId, wrapperMessage, sessionArchivedEvent.getUser().getLogin());
                        log.info(message);
                    }
                    if (log.isDebugEnabled()) {
                        log.debug("Runtime parameter values:");
                        for (final Map.Entry<String, String> paramEntry : runtimeValues.entrySet()) {
                            log.debug(paramEntry.getKey() + ": " + paramEntry.getValue());
                        }
                    }
                    containerService.resolveAndLaunchCommand(xnatCommandWrapperName, commandId, runtimeValues, sessionArchivedEvent.getUser());
                } catch (NotFoundException | CommandResolutionException | NoServerPrefException | DockerServerException | ContainerException e) {
                    log.error("Error launching command " + commandId, e);
                }
            }
        }
    }


}