package org.nrg.execution.events;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.nrg.execution.api.ContainerControlApi;
import org.nrg.execution.exceptions.DockerServerException;
import org.nrg.execution.exceptions.NoServerPrefException;
import org.nrg.execution.model.DockerServerPrefsBean;
import org.nrg.framework.services.NrgEventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

@Component
public class DockerEventPuller implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(DockerEventPuller.class);

    private ContainerControlApi controlApi;
    private DockerServerPrefsBean dockerServerPrefs;
    private NrgEventService eventService;

    @Autowired
    @SuppressWarnings("SpringJavaAutowiringInspection")
    public DockerEventPuller(final ContainerControlApi controlApi,
                             final DockerServerPrefsBean dockerServerPrefs,
                             final NrgEventService eventService) {
        this.controlApi = controlApi;
        this.dockerServerPrefs = dockerServerPrefs;
        this.eventService = eventService;


    }

    @Override
    public void run() {
        if (log.isDebugEnabled()) {
            log.debug("Attempting to read docker events.");
        }
        if (StringUtils.isBlank(dockerServerPrefs.getHost())) {
            log.info("No docker server host set. Skipping attempt to read events.");
        } else {
            final Date lastEventCheckTime = dockerServerPrefs.getLastEventCheckTime();
            final Long since = lastEventCheckTime == null ? 0L : lastEventCheckTime.getTime();

            List<DockerContainerEvent> eventsToThrow = Lists.newArrayList();
            try {
                final Date now = new Date();
                eventsToThrow = controlApi.getContainerEvents(since);
                dockerServerPrefs.setLastEventCheckTime(now); // Set last event check time to just before we checked. Could produce repeat events next time.
            } catch (NoServerPrefException e) {
                log.info("Cannot search for Docker container events. No Docker server defined.");
            } catch (DockerServerException e) {
                log.error("Cannot find Docker container events.", e);
            }

            if (log.isDebugEnabled()) {
                log.debug("Throwing docker events as xnat events.");
            }
            for (final DockerContainerEvent event : eventsToThrow) {
                eventService.triggerEvent(event);
            }
        }
    }
}
