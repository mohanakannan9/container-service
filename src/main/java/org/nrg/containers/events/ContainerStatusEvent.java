package org.nrg.containers.events;

import org.nrg.containers.model.container.auto.Container;
import org.nrg.framework.event.XnatEventServiceEvent;
import org.nrg.xnat.eventservice.events.CombinedEventServiceEvent;
import org.nrg.xnat.eventservice.listeners.EventServiceListener;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@XnatEventServiceEvent(name="ContainerStatusEvent")
public class ContainerStatusEvent extends CombinedEventServiceEvent<ContainerStatusEvent, Container> {
    final String displayName = "Container Status Update";
    final String description = "Container execution status has been updated.";

    public enum Status {Created, Starting, Running, Complete, Failed, Killed}

    public ContainerStatusEvent(){};

    public ContainerStatusEvent(final Container payload, final String eventUser, final Status status, final String projectId){
        super(payload, eventUser, status, projectId);}

    public ContainerStatusEvent(final Container payload, final String eventUser, final String status, final String projectId){
        this(payload,eventUser,Status.valueOf(status),projectId);
    }

    @Override
    public String getDisplayName() { return displayName; }

    @Override
    public String getDescription() { return description; }

    @Override
    public String getPayloadXnatType() { return "org.nrg.containers.model.container.auto.Container"; }

    @Override
    public Boolean isPayloadXsiType() { return false; }

    @Override
    public List<String> getStatiStates() {
        return Arrays.stream(Status.values()).map(Status::name).collect(Collectors.toList()); }

    @Override
    public EventServiceListener getInstance() { return new ContainerStatusEvent(); }
}
