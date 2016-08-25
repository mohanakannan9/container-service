package org.nrg.execution.services;

import org.apache.commons.lang3.StringUtils;
import org.nrg.execution.api.ContainerControlApi;
import org.nrg.execution.daos.ContainerExecutionRepository;
import org.nrg.execution.events.DockerContainerEvent;
import org.nrg.execution.exceptions.DockerServerException;
import org.nrg.execution.exceptions.NoServerPrefException;
import org.nrg.execution.model.ContainerExecution;
import org.nrg.execution.model.ContainerExecutionHistory;
import org.nrg.execution.model.ResolvedCommand;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntityService;
import org.nrg.xdat.preferences.SiteConfigPreferences;
import org.nrg.xdat.security.helpers.Users;
import org.nrg.xdat.security.user.exceptions.UserInitException;
import org.nrg.xdat.security.user.exceptions.UserNotFoundException;
import org.nrg.xft.security.UserI;
import org.nrg.xft.utils.FileUtils;
import org.nrg.xnat.restlet.util.XNATRestConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Service
public class HibernateContainerExecutionService
        extends AbstractHibernateEntityService<ContainerExecution, ContainerExecutionRepository>
        implements ContainerExecutionService {
    private static final Logger log = LoggerFactory.getLogger(HibernateContainerExecutionService.class);

    private ContainerControlApi containerControlApi;
    private SiteConfigPreferences siteConfigPreferences;

    public HibernateContainerExecutionService(final ContainerControlApi containerControlApi,
                                              final SiteConfigPreferences siteConfigPreferences) {
        this.containerControlApi = containerControlApi;
        this.siteConfigPreferences = siteConfigPreferences;
    }

    @Override
    @Transactional
    public void processEvent(final DockerContainerEvent event) {
        if (log.isDebugEnabled()) {
            log.debug("Processing docker container event: " + event);
        }
        final List<ContainerExecution> matchingContainerIds = getDao().findByProperty("containerId", event.getContainerId());

        // Container ID is constrained to be unique, so we can safely take the first element of this list
        if (matchingContainerIds != null && !matchingContainerIds.isEmpty()) {
            final ContainerExecution execution = matchingContainerIds.get(0);
            if (log.isDebugEnabled()) {
                log.debug("Found matching execution: " + execution);
            }

            final ContainerExecutionHistory history = new ContainerExecutionHistory(event.getStatus(), event.getTime());
            if (log.isDebugEnabled()) {
                log.debug("Adding history entry: " + history);
            }
            execution.addToHistory(history);
            update(execution);

            if (StringUtils.isNotBlank(event.getStatus()) &&
                    event.getStatus().matches("kill|die|oom")) {
                finalize(execution);
            }
        }
    }

    @Override
    @Transactional
    public void finalize(final ContainerExecution execution) {
        if (log.isDebugEnabled()) {
            log.debug("Finalizing ContainerExecution for container %s", execution.getContainerId());
        }
        final String userLogin = execution.getUserId();
        try {
            final UserI user = Users.getUser(userLogin);
            uploadLogs(execution, user);
        } catch (UserInitException | UserNotFoundException e) {
            log.error("Could not finalize container execution. Could not get user details for user " + userLogin, e);
        }



        // TODO upload output files
    }

    private void uploadLogs(final ContainerExecution execution, final UserI user) {
        String stdoutLogStr = "";
        String stderrLogStr = "";
        try {
            stdoutLogStr = containerControlApi.getContainerStdoutLog(execution.getContainerId());
            stderrLogStr = containerControlApi.getContainerStderrLog(execution.getContainerId());
        } catch (DockerServerException | NoServerPrefException e) {
            log.error("Could not get container logs for container with id " + execution.getContainerId(), e);
        }

        if (StringUtils.isNotBlank(stdoutLogStr) || StringUtils.isNotBlank(stderrLogStr)) {

//            final String containerExecDir = "";
//            PersistentWorkflowI wrk = null;
//            String rootPath = "";
//
//            CatCatalogBean cat = new CatCatalogBean();
//            cat.setId("LOG");
//
//
//
//
//            final String rootType = execution.getRootObjectXsiType();
//            if (StringUtils.isBlank(rootType)) {
//                // Execution was at the site level. Add logs to sitewide log resource.
//                rootPath = siteConfigPreferences.getArchivePath();
//            } else {
//                final XnatResourcecatalog catResource = new XnatResourcecatalog();
//
////                rootPath = item.
//
//
//                try {
//                    wrk = PersistentWorkflowUtils.buildOpenWorkflow(user, BaseElement.GetGeneratedItem(catResource).getItem(),
//                            EventUtils.newEventInstance(EventUtils.CATEGORY.DATA, EventUtils.TYPE.PROCESS, EventUtils.CREATE_RESOURCE));
//                } catch (PersistentWorkflowUtils.JustificationAbsent | PersistentWorkflowUtils.ActionNameAbsent | PersistentWorkflowUtils.IDAbsent e) {
//                    log.error("Could not create persistent workflow for resource upload.", e);
//                }
//            }

            String rootPath = siteConfigPreferences.getArchivePath(); // TODO find a place to upload this thing. Root of the archive if sitewide, else under the archive path of the root object

            final SimpleDateFormat formatter = new SimpleDateFormat(XNATRestConstants.PREARCHIVE_TIMESTAMP);
            final String datestamp = formatter.format(new Date());
            final String containerExecPath = FileUtils.AppendRootPath(rootPath, "CONTAINER_EXEC/");
            final String destinationPath = containerExecPath + datestamp + "/LOGS/";
            final File destination = new File(destinationPath);
            destination.mkdirs();

            if (StringUtils.isNotBlank(stdoutLogStr)) {
                final File stdoutFile = new File(destination, "stdout.log");
                FileUtils.OutputToFile(stdoutLogStr, stdoutFile.getAbsolutePath());
            }

            if (StringUtils.isNotBlank(stderrLogStr)) {
                final File stderrFile = new File(destination, "stderr.log");
                FileUtils.OutputToFile(stderrLogStr, stderrFile.getAbsolutePath());
            }

            // TODO Save a resource if possible.
            // session or subj or proj
            // .setResources_resource(catResource)

//            SaveItemHelper.authorizedSave(something, user, false, false, ci);
//            try {
//                WorkflowUtils.complete(wrk, ci);
//            } catch (Exception e) {
//                log.error("", e);
//            }
        }
    }

    @Override
    @Transactional
    public ContainerExecution save(final ResolvedCommand resolvedCommand,
                                   final String containerId,
                                   final String rootObjectId,
                                   final String rootObjectXsiType,
                                   final UserI userI) {
        final ContainerExecution execution = new ContainerExecution(resolvedCommand, containerId, rootObjectId, rootObjectXsiType, userI.getLogin());
        return create(execution);
    }
}
