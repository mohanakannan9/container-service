package org.nrg.containers.services;

import org.apache.commons.lang3.StringUtils;
import org.nrg.xft.event.persist.PersistentWorkflowI;
import org.nrg.xft.security.UserI;
import org.nrg.xnat.utils.WorkflowUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContainerUtils {
    private static final Logger log = LoggerFactory.getLogger(ContainerUtils.class);

    public static void updateWorkflowStatus(final String workflowId, final String status, final UserI userI) {
        if (StringUtils.isNotBlank(workflowId)) {
            log.debug("Updating status of workflow {}.", workflowId);
            final PersistentWorkflowI workflow = WorkflowUtils.getUniqueWorkflow(userI, workflowId);
            if (workflow != null) {
                log.debug("Found workflow {}. Updating status to \"{}\".", workflow.getWorkflowId(), status);

                workflow.setStatus(status);
                try {
                    WorkflowUtils.save(workflow, workflow.buildEvent());
                } catch (Exception e) {
                    log.error("Could not update workflow status.", e);
                }
            } else {
                log.debug("Could not find workflow.");
            }
        } else {
            log.debug("Container has no workflow ID. Not attempting to update workflow.");
        }
    }
}
