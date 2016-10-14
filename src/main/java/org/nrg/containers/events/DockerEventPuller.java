package org.nrg.containers.events;

import org.apache.commons.lang3.StringUtils;
import org.nrg.containers.api.ContainerControlApi;
import org.nrg.containers.exceptions.DockerServerException;
import org.nrg.containers.exceptions.NoServerPrefException;
import org.nrg.containers.model.DockerServerPrefsBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class DockerEventPuller implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(DockerEventPuller.class);

    private ContainerControlApi controlApi;
    private DockerServerPrefsBean dockerServerPrefs;

    @Autowired
    @SuppressWarnings("SpringJavaAutowiringInspection")
    public DockerEventPuller(final ContainerControlApi controlApi,
                             final DockerServerPrefsBean dockerServerPrefs) {
        this.controlApi = controlApi;
        this.dockerServerPrefs = dockerServerPrefs;
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
            final Date since = lastEventCheckTime == null ? new Date(0L) : lastEventCheckTime;

            try {
                final Date now = new Date();
                controlApi.getContainerEventsAndThrow(since, now);
                dockerServerPrefs.setLastEventCheckTime(now);
            } catch (NoServerPrefException e) {
                log.info("Cannot search for Docker container events. No Docker server defined.");
            } catch (DockerServerException e) {
                log.error("Cannot find Docker container events.", e);
            }
        }
    }
}
