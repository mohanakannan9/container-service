package org.nrg.actions.services;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.nrg.actions.daos.AceDao;
import org.nrg.actions.model.Action;
import org.nrg.actions.model.ActionContextExecution;
import org.nrg.actions.model.ActionContextExecutionDto;
import org.nrg.actions.model.ActionInput;
import org.nrg.actions.model.ActionResource;
import org.nrg.actions.model.Context;
import org.nrg.actions.model.ItemQueryCacheKey;
import org.nrg.actions.model.Matcher;
import org.nrg.actions.model.ResolvedCommand;
import org.nrg.actions.model.ResolvedCommandMount;
import org.nrg.containers.api.ContainerControlApi;
import org.nrg.containers.exceptions.BadRequestException;
import org.nrg.containers.exceptions.DockerServerException;
import org.nrg.containers.exceptions.NoServerPrefException;
import org.nrg.containers.exceptions.NotFoundException;
import org.nrg.containers.model.DockerServer;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntityService;
import org.nrg.transporter.TransportService;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.base.BaseElement;
import org.nrg.xdat.model.XnatImagescandataI;
import org.nrg.xdat.om.XnatAbstractresource;
import org.nrg.xdat.om.XnatExperimentdata;
import org.nrg.xdat.om.XnatImagescandata;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.XnatResourcecatalog;
import org.nrg.xdat.om.XnatSubjectdata;
import org.nrg.xft.ItemI;
import org.nrg.xft.XFTItem;
import org.nrg.xft.exception.ElementNotFoundException;
import org.nrg.xft.exception.FieldNotFoundException;
import org.nrg.xft.exception.XFTInitException;
import org.nrg.xft.search.ItemSearch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Transactional
public class HibernateAceService
        extends AbstractHibernateEntityService<ActionContextExecution, AceDao>
        implements AceService {
    private static Logger logger = LoggerFactory.getLogger(HibernateAceService.class);

    @Autowired
    private ActionService actionService;

    @Autowired
    private CommandService commandService;

    @Autowired
    private TransportService transporter;

    @Autowired
    private ContainerControlApi containerControlApi;

    public List<ActionContextExecutionDto> resolveAces(final Context context) throws XFTInitException, BadRequestException, NotFoundException {
        final String id = context.get("id");
        if (StringUtils.isBlank(id)) {
            // TODO we should be able to launch actions that don't need an id. But not right now.
            throw new BadRequestException("Must set id in the context");
        }

        final String xsiType = context.get("xsiType");
        if (StringUtils.isBlank(xsiType)) {
            // TODO I hope this is not necessary, but right now it is.
            throw new BadRequestException("Must set xsiType in the context");
        }

        if (StringUtils.isBlank(xsiType)) {
            // TODO This is ok. We can run site-wide Actions.
            return null;
        } else {
            final List<Action> actionCandidates = actionService.findByRootXsiType(xsiType);
            if (actionCandidates == null || actionCandidates.isEmpty()) {
                return null;
            }

            // We know xsiType and id are both not blank.
            // Find the item with the given xsiType and id
            ItemI itemI = null;
            String projectId = "";
            final String rootArchivePath;
            final List<XnatAbstractresource> resources;

            if (xsiType.matches("^[a-zA-Z]+:[a-zA-Z]+[Ss]can[Dd]ata$")) {
                // If we are looking for a scan, assume the id is formatted "sessionid.scanid"
                final String[] splitId = id.split(":");
                final String sessionId;
                final String scanId;
                if (splitId.length < 2) {
                    // TODO scan id must be formatted "sessionid.scanid". throw error.
                    return null;
                } else {
                    sessionId = splitId[0];
                    scanId = StringUtils.join(Arrays.copyOfRange(splitId, 1, splitId.length), ':');
                }

                final XnatImagesessiondata session =
                        XnatImagesessiondata.getXnatImagesessiondatasById(sessionId, XDAT.getUserDetails(), false);
                projectId = session.getProject();
                for (final XnatImagescandataI scan : session.getScans_scan()) {
                    if (scan.getId().equals(scanId)) {
                        itemI = (XnatImagescandata) scan;
                        break;
                    }
                }
            } else {
                try {
                    final XFTItem item = ItemSearch.GetItem(xsiType+".ID", id, XDAT.getUserDetails(), false);
                    itemI = BaseElement.GetGeneratedItem(item);
                } catch (Exception e) {
                    throw new NotFoundException("Could not find " + xsiType + " with id " + id, e);
                }
            }

            if (itemI == null) {
                throw new NotFoundException("Could not find " + xsiType + " with id " + id);
            }

            if (itemI instanceof XnatProjectdata) {
                projectId = id;
                final XnatProjectdata proj = XnatProjectdata.getXnatProjectdatasById(projectId, XDAT.getUserDetails(), false);
                rootArchivePath = proj.getRootArchivePath();
                resources = ((XnatProjectdata) itemI).getResources_resource();
            } else if (itemI instanceof XnatImagesessiondata) {
                projectId = ((XnatImagesessiondata) itemI).getProject();
                final XnatProjectdata proj = XnatProjectdata.getXnatProjectdatasById(projectId, XDAT.getUserDetails(), false);
                rootArchivePath = proj.getRootArchivePath();
                resources = ((XnatImagesessiondata) itemI).getResources_resource();
            } else if (itemI instanceof XnatSubjectdata) {
                projectId = ((XnatSubjectdata) itemI).getProject();
                final XnatProjectdata proj = XnatProjectdata.getXnatProjectdatasById(projectId, XDAT.getUserDetails(), false);
                rootArchivePath = proj.getRootArchivePath();
                resources = ((XnatSubjectdata) itemI).getResources_resource();
            } else if (itemI instanceof XnatExperimentdata) {
                projectId = ((XnatExperimentdata) itemI).getProject();
                final XnatProjectdata proj = XnatProjectdata.getXnatProjectdatasById(projectId, XDAT.getUserDetails(), false);
                rootArchivePath = proj.getRootArchivePath();
                resources = ((XnatExperimentdata) itemI).getResources_resource();
            } else if (itemI instanceof XnatImagescandata) {
                if (StringUtils.isBlank(projectId)) {
                    logger.error("Did not find the project ID for scan with ID " + id);
                    rootArchivePath = "";
                    resources = null;
                } else {
                    final XnatProjectdata proj = XnatProjectdata.getXnatProjectdatasById(projectId, XDAT.getUserDetails(), false);
                    rootArchivePath = proj.getRootArchivePath();
                    resources = ((XnatImagescandata) itemI).getFile();
                }
            } else {
                logger.info("Can't figure out the project for item with id " + id);
                projectId = "";
                rootArchivePath = "";
                resources = null;
            }

            final Map<String, String> resourceLabelToCatalogPath = Maps.newHashMap();
            if (resources != null && StringUtils.isNotBlank(rootArchivePath)) {
                for (final XnatAbstractresource resource : resources) {
                    if (resource instanceof XnatResourcecatalog) {
                        final XnatResourcecatalog resourceCatalog = (XnatResourcecatalog) resource;
                        resourceLabelToCatalogPath.put(resourceCatalog.getLabel(),
                                resourceCatalog.getCatalogFile(rootArchivePath).getParent());
                    }
                }
            } else {
                logger.info("Item with id " + id + " has no resources or a blank archive path");
            }

            final List<ActionContextExecutionDto> resolvedAces = Lists.newArrayList();
            final Map<ItemQueryCacheKey, String> cache = Maps.newHashMap();
            for (final Action candidate : actionCandidates) {
                final ActionContextExecutionDto ace = resolve(candidate, itemI, resourceLabelToCatalogPath, context, cache);
                if (ace != null) {
                    ace.setRootId(id);
                    ace.setProject(projectId);
                    resolvedAces.add(ace);
                }
            }
            return resolvedAces;
        }
    }

    public ActionContextExecution executeAce(final ActionContextExecutionDto aceDto)
            throws NotFoundException, NoServerPrefException, ElementNotFoundException, DockerServerException {
        final ActionContextExecution ace = aceFromDto(aceDto);
        final ResolvedCommand resolvedCommand = ace.getResolvedCommand();

        final DockerServer dockerServer = containerControlApi.getServer();

        // TODO Use Transporter to stage staged resources
        if (!(ace.getResourcesStaged() == null || ace.getResourcesStaged().isEmpty() ||
                resolvedCommand.getMountsIn() == null || resolvedCommand.getMountsIn().isEmpty()) ) {
            final List<ActionResource> staged = ace.getResourcesStaged();
            final List<ResolvedCommandMount> mountsIn = resolvedCommand.getMountsIn();

            final Set<Path> resourcePathsToTransport = Sets.newHashSet();
            final Map<Path, ResolvedCommandMount> localPathToMount = Maps.newHashMap();
            for (final ActionResource resourceToStage : staged) {
                final Path localPath = Paths.get(resourceToStage.getPath());

                resourcePathsToTransport.add(localPath);

                for (final ResolvedCommandMount mountIn : mountsIn) {
                    if (resourceToStage.getMountName().equals(mountIn.getName())) {
                        localPathToMount.put(localPath, mountIn);
                    }
                }
            }
            final Map<Path, Path> localPathToTransportedPath =
                    transporter.transport(dockerServer.getHost(),
                            resourcePathsToTransport.toArray(new Path[resourcePathsToTransport.size()]));

            for (final Path localPath : localPathToMount.keySet()) {
                final ResolvedCommandMount mountIn = localPathToMount.get(localPath);
                final Path transportedResourcePath = localPathToTransportedPath.get(localPath);

                mountIn.setLocalPath(transportedResourcePath.toString());
            }
        }
        // TODO If it's a script command, need to write out the script and transport it

        // TODO Use Transporter to create writable space for output mounts
        if (resolvedCommand.getMountsOut() != null && !resolvedCommand.getMountsOut().isEmpty()) {
            final List<ResolvedCommandMount> mountsOut = resolvedCommand.getMountsOut();
            final List<Path> buildPaths = transporter.getWritableDirectories(dockerServer.getHost(), mountsOut.size());
            for (int i=0; i<mountsOut.size(); i++) {
                final ResolvedCommandMount mountOut = mountsOut.get(i);
                final Path buildPath = buildPaths.get(i);

                mountOut.setLocalPath(buildPath.toString());
            }
        }

        // TODO If it's a script command, need to prepend the script environment's "run" to the command's "run"

        // Save the ace before launching
        final ActionContextExecution created = create(ace);

        final String containerId = commandService.launchCommand(resolvedCommand);

        // Add the container ID after launching
        created.setContainerId(containerId);
        update(created);
        return created;
    }

    public ActionContextExecution aceFromDto(final ActionContextExecutionDto aceDto) throws NotFoundException {
        if (aceDto == null) return null;

        // TODO Check that all required inputs have values
        final Map<String, String> aceInputValues = Maps.newHashMap();
        if (aceDto.getInputs() != null && !aceDto.getInputs().isEmpty()) {
            for (final ActionInput input : aceDto.getInputs()) {
                if (input.isRequired() && StringUtils.isBlank(input.getValue())) {
                    // TODO throw an error. Required input cannot be blank.
                }
                aceInputValues.put(input.getCommandVariableName(), input.getValue());
            }
        }

        // TODO Check that all staged resources have paths
        if (aceDto.getResourcesStaged() != null && !aceDto.getResourcesStaged().isEmpty()) {
            for (final ActionResource resource : aceDto.getResourcesStaged()) {
                if (StringUtils.isBlank(resource.getPath())) {
                    // TODO throw an error. All staged resources need ids. (This should have happened during resolveAce())
                }
            }
        }

        // TODO Check that, if created resources have ids, they are overwritable
        if (aceDto.getResourcesCreated() != null && !aceDto.getResourcesCreated().isEmpty()) {
            for (final ActionResource resource : aceDto.getResourcesCreated()) {
                if (!resource.getOverwrite() && !StringUtils.isBlank(resource.getPath())) {
                    // TODO throw an error. If a "created" resource already exists, we need to be able to overwrite it.
                }
            }
        }

        final ResolvedCommand resolvedCommand =
                commandService.resolveCommand(aceDto.getCommandId(), aceInputValues);

        return new ActionContextExecution(aceDto, resolvedCommand);
    }

    private ActionContextExecutionDto resolve(final Action action,
                                              final ItemI itemI,
                                              final Map<String, String> resourceLabelToCatalogPath,
                                              final Context context,
                                              final Map<ItemQueryCacheKey, String> cache)
            throws XFTInitException {

        if (!doesItemMatchMatchers(itemI, action.getRootMatchers(), cache)) {
            return null;
        }

        final ActionContextExecutionDto ace = new ActionContextExecutionDto(action);

        // Find values for any inputs that we can.
        if (ace.getInputs() != null) {
            for (final ActionInput input : ace.getInputs()) {
                // Try to get inputs of type=property out of the root object
                if (StringUtils.isNotBlank(input.getRootProperty())) {
                    final String property = cacheQuery(itemI, input.getRootProperty(), cache);
                    if (property != null) {
                        input.setValue(property);
                    }
                }

                // Now try to get values from the context.
                // (Even if we already found the value from the item, we want to do this.
                //   Values in the context take precedence over XFTItem properties.)
                if (StringUtils.isNotBlank(context.get(input.getInputName()))) {
                    input.setValue(context.get(input.getInputName()));
                }
            }
        }

        if (ace.getResourcesStaged() != null && !ace.getResourcesStaged().isEmpty()) {
            // Item needs resources

            if (resourceLabelToCatalogPath == null || resourceLabelToCatalogPath.isEmpty()) {
                // Item has no resources, but we need some staged. Action does not work.
                return null;
            } else {
                for (final ActionResource aceResource : ace.getResourcesStaged()) {
                    final String resourceCatalogPath = resourceLabelToCatalogPath.get(aceResource.getResourceName());
                    if (StringUtils.isNotBlank(resourceCatalogPath)) {
                        aceResource.setPath(resourceLabelToCatalogPath.get(aceResource.getResourceName()));
                    } else {
                        if(logger.isDebugEnabled()) {
                            final String message =
                                    String.format("Action %d needed resource, but item has no resource named %s",
                                            action.getId(), aceResource.getResourceName());
                            logger.debug(message);
                        }
                        return null;
                    }
                }
            }
        }

        if (ace.getResourcesCreated() != null && !ace.getResourcesCreated().isEmpty()) {
            // Item needs resources

            if (!(resourceLabelToCatalogPath == null || resourceLabelToCatalogPath.isEmpty())) {
                for (final ActionResource aceResource : ace.getResourcesCreated()) {

                    if (resourceLabelToCatalogPath.containsKey(aceResource.getResourceName()) &&
                            !aceResource.getOverwrite()) {
                        if(logger.isDebugEnabled()) {
                            final String message =
                                    String.format("Action %d will create resource, but item already has " +
                                            "a resource named %s and action cannot overwrite it.",
                                            action.getId(), aceResource.getResourceName());
                            logger.debug(message);
                        }
                        return null;
                    }
                }
            }
        }

        return ace;
    }

    private String cacheQuery(final ItemI itemI, final String propertyName,
                            final Map<ItemQueryCacheKey, String> cache)
            throws XFTInitException {
        final ItemQueryCacheKey query = new ItemQueryCacheKey(itemI, propertyName);
        if (cache.containsKey(query)) {
            return cache.get(query);
        }

        final Object propertyObj;
        try {
            propertyObj = itemI.getProperty(propertyName);
        } catch (ElementNotFoundException | FieldNotFoundException e) {
            logger.debug("Could not find property %s on item %s.",
                    propertyName, itemI.toString());
            return null;
        }

        if (String.class.isAssignableFrom(propertyObj.getClass())) {
            final String propertyString = (String) propertyObj;
            cache.put(query, propertyString);
            return propertyString;
        } else {
            // TODO Do we allow a non-string property here?
            logger.debug("Non-string property found for %s on item %s: %s",
                    propertyName, itemI.toString(), propertyObj.toString());
        }
        return null;
    }

    private Boolean doesItemMatchMatchers(final ItemI itemI, final List<Matcher> matchers,
                                          final Map<ItemQueryCacheKey, String> cache)
            throws XFTInitException {
        if (matchers == null || matchers.isEmpty()) {
            return true;
        }

        for (final Matcher matcher : matchers) {
            if (!doesItemMatchMatchers(itemI, matcher, cache)) {
                return false;
            }
        }
        return true;
    }
    private Boolean doesItemMatchMatchers(final ItemI itemI, final Matcher matcher,
                                          final Map<ItemQueryCacheKey, String> cache)
            throws XFTInitException {
        if (matcher == null || StringUtils.isBlank(matcher.getValue())) {
            return true;
        }

        final String property = cacheQuery(itemI, matcher.getProperty(), cache);
        for (final String matcherOrValue : matcher.getValue().split("|")) {
            if (matcher.getOperator().equalsIgnoreCase("equals")) {
                if (matcherOrValue.equals(property)) {
                    return true;
                }
            } // TODO Add more matcher operators
        }


        return false;
    }

}
