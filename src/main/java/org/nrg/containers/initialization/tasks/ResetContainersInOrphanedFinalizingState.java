package org.nrg.containers.initialization.tasks;

import org.nrg.containers.services.ContainerService;
import org.nrg.xnat.initialization.tasks.AbstractInitializingTask;
import org.nrg.xnat.initialization.tasks.InitializingTaskException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Mohana Ramaratnam
 *
 */
@Slf4j
@Component
public class ResetContainersInOrphanedFinalizingState extends AbstractInitializingTask {
    private final ContainerService containerService;


    @Autowired
    public ResetContainersInOrphanedFinalizingState(final ContainerService containerService) {
        this.containerService = containerService;
    }

	  @Override
	    public String getTaskName() {
	        return "Reset Finalizing State to Waiting";
	    }
	  
	    @Override
	    protected void callImpl() throws InitializingTaskException {
	        log.debug("Checking if any containers exist in orphaned Finalizing state  in database. If they do, resetting them to Waiting/Failed");
	    	//MR: 10/30/2018 - If this is the first time the DockerStatusUpdater is running
	    	//Look for all containers which are in Finalizing state
	    	//These are probably in "hung" state
	    	//Change the state of these to Waiting
    		containerService.resetFinalizingStatusToWaitingOrFailed();
    		log.debug("Reset Complete Orphaned Finalizing states to Waiting/Failed State");
	    }
}