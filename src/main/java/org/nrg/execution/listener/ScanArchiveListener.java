package org.nrg.execution.listener;

import org.nrg.execution.events.ScanArchiveEvent;
import org.nrg.execution.exceptions.*;
import org.nrg.execution.model.CommandEventMapping;
import org.nrg.execution.services.CommandEventMappingService;
import org.nrg.execution.services.CommandService;
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
public class ScanArchiveListener implements Consumer<Event<ScanArchiveEvent>> {

    @Autowired
    CommandService commandService;

    @Autowired
    CommandEventMappingService commandEventMappingService;

     /**
     * Instantiates a new xft item event listener.
     *
     * @param eventBus the event bus
     */
    @Inject public ScanArchiveListener(EventBus eventBus ){
        eventBus.on(type(ScanArchiveEvent.class), this);
    }


    @Override
    public void accept(Event<ScanArchiveEvent> event) {
        final ScanArchiveEvent scanArchiveEvent = event.getData();

        // Find commands defined for this event type
        List<CommandEventMapping> commandEventMappings = commandEventMappingService.findByEventType(scanArchiveEvent.getEventId());

        if (commandEventMappings != null && !commandEventMappings.isEmpty()){
            for (CommandEventMapping commandEventMapping: commandEventMappings) {
                Long commandId = commandEventMapping.getCommandId();
                try {
                    commandService.launchCommand(commandId, scanArchiveEvent.getUser(), scanArchiveEvent.getScan());
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
