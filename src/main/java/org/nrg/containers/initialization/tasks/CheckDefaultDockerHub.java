package org.nrg.containers.initialization.tasks;

import org.nrg.containers.exceptions.NotFoundException;
import org.nrg.containers.model.auto.DockerHub;
import org.nrg.containers.services.DockerHubService;
import org.nrg.xdat.preferences.SiteConfigPreferences;
import org.nrg.xnat.initialization.tasks.AbstractInitializingTask;
import org.nrg.xnat.initialization.tasks.InitializingTaskException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CheckDefaultDockerHub extends AbstractInitializingTask {
    private final DockerHubService dockerHubService;
    private final SiteConfigPreferences siteConfigPreferences;

    @Autowired
    public CheckDefaultDockerHub(final DockerHubService dockerHubService,
                                 final SiteConfigPreferences siteConfigPreferences) {
        this.dockerHubService = dockerHubService;
        this.siteConfigPreferences = siteConfigPreferences;
    }

    @Override
    public String getTaskName() {
        return "Check that a default docker hub is stored";
    }

    @Override
    protected void callImpl() throws InitializingTaskException {
        // Check if a default is returned by the service
        DockerHub defaultDockerHub = null;
        try {
            defaultDockerHub = dockerHubService.getDefault();
        } catch (NotFoundException ignored) {
            // ignored
        }

        if (defaultDockerHub == null || defaultDockerHub.id() == 0L) {
            // Default is not initialized.
            dockerHubService.setDefault(DockerHub.DEFAULT, siteConfigPreferences.getPrimaryAdminUsername(), "Initializing default docker hub");
        }
    }
}
