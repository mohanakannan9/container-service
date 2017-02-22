package org.nrg.containers.events;

import org.nrg.containers.api.ContainerControlApi;
import org.nrg.containers.exceptions.DockerServerException;
import org.nrg.containers.exceptions.NoServerPrefException;
import org.nrg.containers.model.DockerServerPrefsBean;
import org.nrg.xft.schema.XFTManager;
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

    private boolean haveLoggedDockerConnectFailure = false;
    private boolean haveLoggedXftInitFailure = false;

    @Autowired
    @SuppressWarnings("SpringJavaAutowiringInspection")
    public DockerEventPuller(final ContainerControlApi controlApi,
                             final DockerServerPrefsBean dockerServerPrefs) {
        this.controlApi = controlApi;
        this.dockerServerPrefs = dockerServerPrefs;
    }

    @Override
    public void run() {
        log.trace("Attempting to read docker events.");

        if (!controlApi.canConnect()) {
            if (!haveLoggedDockerConnectFailure) {
                log.info("Cannot ping docker server. Skipping attempt to read events.");
                haveLoggedDockerConnectFailure = true;
            }
        } else if (!XFTManager.isInitialized()) {
            if (!haveLoggedXftInitFailure) {
                log.info("XFT is not initialized. Skipping attempt to read events.");
                haveLoggedXftInitFailure = true;
            }
        } else {
            final Date lastEventCheckTime = dockerServerPrefs.getLastEventCheckTime();
            final Date since = lastEventCheckTime == null ? new Date(0L) : lastEventCheckTime;

            final Date now = new Date();

            try {
                controlApi.getContainerEventsAndThrow(since, now);
                dockerServerPrefs.setLastEventCheckTime(now);

                // Reset failure flags
                haveLoggedDockerConnectFailure = false;
                haveLoggedXftInitFailure = false;
            } catch (NoServerPrefException e) {
                log.info("Cannot search for Docker container events. No Docker server defined.");
            } catch (DockerServerException e) {
                log.error("Cannot find Docker container events.", e);
            }

        }
    }
}
