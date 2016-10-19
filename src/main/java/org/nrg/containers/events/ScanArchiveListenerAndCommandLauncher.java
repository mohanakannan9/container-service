package org.nrg.containers.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import org.nrg.containers.exceptions.*;
import org.nrg.containers.model.CommandEventMapping;
import org.nrg.containers.model.xnat.Scan;
import org.nrg.containers.services.CommandEventMappingService;
import org.nrg.containers.services.CommandService;
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
public class ScanArchiveListenerAndCommandLauncher implements Consumer<Event<ScanArchiveEventToLaunchCommands>> {
    private static final Logger log = LoggerFactory.getLogger(ScanArchiveListenerAndCommandLauncher.class);
    private static final String EVENT_ID = "ScanArchived";

    @Autowired private ObjectMapper mapper;
    @Autowired private CommandService commandService;
    @Autowired private CommandEventMappingService commandEventMappingService;

     /**
     * Instantiates a new xft item event listener.
     *
     * @param eventBus the event bus
     */
    @Inject public ScanArchiveListenerAndCommandLauncher(EventBus eventBus ){
        eventBus.on(type(ScanArchiveEventToLaunchCommands.class), this);
    }


    @Override
    public void accept(Event<ScanArchiveEventToLaunchCommands> event) {
        final ScanArchiveEventToLaunchCommands scanArchiveEventToLaunchCommands = event.getData();

        // Find commands defined for this event type
        final List<CommandEventMapping> commandEventMappings = commandEventMappingService.findByEventType(EVENT_ID);

        if (commandEventMappings != null && !commandEventMappings.isEmpty()) {
            for (CommandEventMapping commandEventMapping: commandEventMappings) {
                final Long commandId = commandEventMapping.getCommandId();

                final Map<String, String> runtimeValues = Maps.newHashMap();

                final Scan scan = scanArchiveEventToLaunchCommands.getScan();
                String scanString = scan.getId();
                try {
                    scanString = mapper.writeValueAsString(scan);
                } catch (JsonProcessingException e) {
                    log.error(String.format("Could not serialize Scan %s to json.", scan), e);
                    runtimeValues.put("sessionId", scan.getParentId());
                }
                runtimeValues.put("scan", scanString);
                try {
                    if (log.isDebugEnabled()) {
                        final String message = String.format(
                                "Launching command %s for user %s with runtime input values %s",
                                commandId, scanArchiveEventToLaunchCommands.getUser(), runtimeValues
                        );
                        log.debug(message);
                    }
                    commandService.launchCommand(commandId, runtimeValues, scanArchiveEventToLaunchCommands.getUser());
                } catch (NotFoundException | CommandResolutionException | NoServerPrefException | DockerServerException e) {
                    log.error("Error launching command " + commandId, e);
                }
            }
        }
    }
}
