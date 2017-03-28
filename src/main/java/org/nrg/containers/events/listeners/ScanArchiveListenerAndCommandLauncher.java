package org.nrg.containers.events.listeners;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.nrg.containers.events.ScanArchiveEventToLaunchCommands;
import org.nrg.containers.exceptions.CommandResolutionException;
import org.nrg.containers.exceptions.ContainerException;
import org.nrg.containers.exceptions.DockerServerException;
import org.nrg.containers.exceptions.NoServerPrefException;
import org.nrg.containers.model.CommandEventMapping;
import org.nrg.containers.model.xnat.Scan;
import org.nrg.containers.services.CommandEventMappingService;
import org.nrg.containers.services.ContainerService;
import org.nrg.framework.exceptions.NotFoundException;
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
public class ScanArchiveListenerAndCommandLauncher implements Consumer<Event<ScanArchiveEventToLaunchCommands>> {
    private static final Logger log = LoggerFactory.getLogger(ScanArchiveListenerAndCommandLauncher.class);
    private static final String EVENT_ID = "ScanArchived";

    private ObjectMapper mapper;
    private ContainerService containerService;
    private CommandEventMappingService commandEventMappingService;

    @Autowired
    public ScanArchiveListenerAndCommandLauncher(final EventBus eventBus,
                                                 final ObjectMapper mapper,
                                                 final ContainerService containerService,
                                                 final CommandEventMappingService commandEventMappingService) {
        eventBus.on(type(ScanArchiveEventToLaunchCommands.class), this);
        this.mapper = mapper;
        this.containerService = containerService;
        this.commandEventMappingService = commandEventMappingService;
    }


    @Override
    public void accept(Event<ScanArchiveEventToLaunchCommands> event) {
        final ScanArchiveEventToLaunchCommands scanArchiveEventToLaunchCommands = event.getData();

        // Find commands defined for this event type
        final List<CommandEventMapping> commandEventMappings = commandEventMappingService.findByEventType(EVENT_ID);

        if (commandEventMappings != null && !commandEventMappings.isEmpty()) {
            for (CommandEventMapping commandEventMapping: commandEventMappings) {
                final Long commandId = commandEventMapping.getCommandId();
                final String xnatCommandWrapperName = commandEventMapping.getXnatCommandWrapperName();

                final Map<String, String> runtimeValues = Maps.newHashMap();

                final Scan scan = scanArchiveEventToLaunchCommands.getScan();
                String scanString = scan.getUri();
                try {
                    scanString = mapper.writeValueAsString(scan);
                } catch (JsonProcessingException e) {
                    log.error(String.format("Could not serialize Scan %s to json.", scan), e);
                }
                runtimeValues.put("scan", scanString);
                try {
                    if (log.isInfoEnabled()) {
                        final String wrapperMessage = StringUtils.isNotBlank(xnatCommandWrapperName) ?
                                String.format("wrapper \"%s\"", xnatCommandWrapperName) :
                                "identity wrapper";
                        final String message = String.format(
                                "Launching command %s, %s, for user \"%s\".", commandId, wrapperMessage, scanArchiveEventToLaunchCommands.getUser().getLogin()
                        );
                        log.info(message);
                        if (log.isDebugEnabled()) {
                            log.debug("Runtime parameter values:");
                            for (final Map.Entry<String, String> paramEntry : runtimeValues.entrySet()) {
                                log.debug(paramEntry.getKey() + ": " + paramEntry.getValue());
                            }
                        }
                    }
                    containerService.resolveAndLaunchCommand(xnatCommandWrapperName, commandId, runtimeValues, scanArchiveEventToLaunchCommands.getUser());
                } catch (NotFoundException | CommandResolutionException | NoServerPrefException | DockerServerException | ContainerException e) {
                    log.error("Error launching command " + commandId, e);
                }
            }
        }
    }
}
