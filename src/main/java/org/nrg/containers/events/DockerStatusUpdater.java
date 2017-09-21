package org.nrg.containers.events;

import org.nrg.containers.api.ContainerControlApi;
import org.nrg.containers.exceptions.DockerServerException;
import org.nrg.containers.exceptions.NoDockerServerException;
import org.nrg.containers.model.server.docker.DockerServerBase.DockerServer;
import org.nrg.containers.services.DockerServerService;
import org.nrg.framework.exceptions.NotFoundException;
import org.nrg.xft.schema.XFTManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class DockerStatusUpdater implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(DockerStatusUpdater.class);

    private ContainerControlApi controlApi;
    private DockerServerService dockerServerService;

    private boolean haveLoggedDockerConnectFailure = false;
    private boolean haveLoggedNoServerInDb = false;
    private boolean haveLoggedXftInitFailure = false;

    @Autowired
    @SuppressWarnings("SpringJavaAutowiringInspection")
    public DockerStatusUpdater(final ContainerControlApi controlApi,
                               final DockerServerService dockerServerService) {
        this.controlApi = controlApi;
        this.dockerServerService = dockerServerService;
    }

    @Override
    public void run() {
        log.trace("Attempting to update status with docker.");

        final String skipMessage = "Skipping attempt to update status.";

        if (!XFTManager.isInitialized()) {
            if (!haveLoggedXftInitFailure) {
                log.info("XFT is not initialized. " + skipMessage);
                haveLoggedXftInitFailure = true;
            }
            return;
        }

        // Since XFT is up, we should be able to connect to the database and read the docker server
        DockerServer dockerServer = null;
        try {
            dockerServer = dockerServerService.getServer();
        } catch (NotFoundException e) {
            // ignored
        }
        if (dockerServer == null) {
            if (!haveLoggedNoServerInDb) {
                log.info("No docker server has been defined (or enabled) in the database. " + skipMessage);
                haveLoggedNoServerInDb = true;
                haveLoggedXftInitFailure = false;
            }
            return;
        }

        if (!controlApi.canConnect()) {
            if (!haveLoggedDockerConnectFailure) {
                log.info("Cannot ping docker server " + dockerServer.name() + ". " + skipMessage);
                haveLoggedDockerConnectFailure = true;
                haveLoggedXftInitFailure = false;
                haveLoggedNoServerInDb = false;
            }
            return;
        }

        // Now we should be able to check the status
        final Date lastEventCheckTime = dockerServer.lastEventCheckTime();
        final Date since = lastEventCheckTime == null ? new Date(0L) : lastEventCheckTime;

        final Date now = new Date();

        try {
            controlApi.throwContainerEvents(since, now);
            dockerServerService.update(dockerServer.updateEventCheckTime(now));

            // Reset failure flags
            haveLoggedDockerConnectFailure = false;
            haveLoggedXftInitFailure = false;
            haveLoggedNoServerInDb = false;
        } catch (NoDockerServerException e) {
            log.info("Cannot search for Docker container events. No Docker server defined.");
        } catch (DockerServerException e) {
            log.error("Cannot find Docker container events.", e);
        }
    }
}
