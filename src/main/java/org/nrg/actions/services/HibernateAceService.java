package org.nrg.actions.services;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.nrg.actions.daos.AceDao;
import org.nrg.actions.model.Action;
import org.nrg.actions.model.ActionContextExecution;
import org.nrg.actions.model.ActionContextExecutionDto;
import org.nrg.actions.model.ActionInput;
import org.nrg.actions.model.ActionResource;
import org.nrg.actions.model.Context;
import org.nrg.actions.model.ItemQueryCacheKey;
import org.nrg.actions.model.ResolvedCommand;
import org.nrg.actions.model.matcher.Matcher;
import org.nrg.containers.exceptions.NotFoundException;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntityService;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.base.BaseElement;
import org.nrg.xdat.model.XnatImagescandataI;
import org.nrg.xdat.om.XnatAbstractresource;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xft.XFTItem;
import org.nrg.xft.exception.ElementNotFoundException;
import org.nrg.xft.exception.FieldNotFoundException;
import org.nrg.xft.exception.XFTInitException;
import org.nrg.xft.security.UserI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class HibernateAceService
        extends AbstractHibernateEntityService<ActionContextExecution, AceDao>
        implements AceService {
    private static Logger logger = LoggerFactory.getLogger(HibernateAceService.class);

    @Autowired
    private ActionService actionService;

    @Autowired
    private CommandService commandService;

    public List<ActionContextExecutionDto> resolveAces(final Context context) throws XFTInitException {
        final String xsiType = context.get("xsiType");
        final String id = context.get("id");

        if (StringUtils.isNotBlank(xsiType) && StringUtils.isBlank(id)) {
            // TODO non-blank xsiType and blank id is an error
            return null;
        }

        final List<Action> actionCandidates = actionService.findByRootXsiType(xsiType);
        if (actionCandidates == null || actionCandidates.isEmpty()) {
            return null;
        }

        if (StringUtils.isBlank(xsiType)) {
            // TODO This is ok. We can run site-wide Actions.
            return null;
        } else {
            // We know xsiType and id are both not blank.
            // Find the item with the given xsiType and id
            XFTItem item = null;
            final UserI user = XDAT.getUserDetails();
            if (xsiType.matches("^[a-zA-Z]+:[a-zA-Z]+ScanData$")) {
                // If we are looking for a scan, assume the id is formatted "sessionid.scanid"
                final String[] splitId = id.split(".");
                if (splitId.length != 2) {
                    // TODO scan id must be formatted "sessionid.scanid". throw error.
                    return null;
                }
                final String sessionId = splitId[0];
                final String scanId = splitId[1];
                final XnatImagesessiondata session =
                        XnatImagesessiondata.getXnatImagesessiondatasById(sessionId, user, false);
                for (final XnatImagescandataI scan : session.getScans_scan()) {
                    if (scan.getId().equals(scanId)) {
                        item = (XFTItem) scan;
                        break;
                    }
                }
            } else {
                // TODO get a non-scan
                // ItemSearch.GetAllItems(id, user, false)
                return null;
            }

            if (item == null) {
                // TODO couldn't find the item. Error.
                return null;
            }

            final List<ActionContextExecutionDto> resolvedAces = Lists.newArrayList();
            final Map<ItemQueryCacheKey, String> cache = Maps.newHashMap();
            for (final Action candidate : actionCandidates) {
                final ActionContextExecutionDto ace = resolve(candidate, item, context, cache);
                if (ace != null) {
                    resolvedAces.add(ace);
                }
            }
            return resolvedAces;
        }
    }

    public ActionContextExecution launchAce(final ActionContextExecutionDto aceDto) throws NotFoundException {
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

        // TODO Check that all staged resources have ids
        if (aceDto.getResourcesStaged() != null && !aceDto.getResourcesStaged().isEmpty()) {
            for (final ActionResource resource : aceDto.getResourcesStaged()) {
                if (resource.getResourceId() == null || resource.getResourceId().equals(0)) {
                    // TODO throw an error. All staged resources need ids. (This should have happened during resolveAce())
                }
            }
        }

        // TODO Check that, if created resources have ids, they are overwritable
        if (aceDto.getResourcesCreated() != null && !aceDto.getResourcesCreated().isEmpty()) {
            for (final ActionResource resource : aceDto.getResourcesCreated()) {
                if (!resource.getOverwrite() &&
                        !(resource.getResourceId() == null || resource.getResourceId().equals(0))) {
                    // TODO throw an error. If a "created" resource already exists, we need to be able to overwrite it.
                }
            }
        }

        final ResolvedCommand resolvedCommand =
                commandService.resolveCommand(aceDto.getCommandId(), aceInputValues);

        final ActionContextExecution ace = new ActionContextExecution(aceDto, resolvedCommand);

        // TODO Use Transporter to stage staged resources
        // TODO Use Transporter to create writable space for created resources

        return null;
    }

    private ActionContextExecutionDto resolve(final Action action,
                                           final XFTItem item,
                                           final Context context,
                                           final Map<ItemQueryCacheKey, String> cache)
            throws XFTInitException {

        if (!doesItemMatchMatchers(item, action.getRoot().getMatchers(), cache)) {
            return null;
        }

        final ActionContextExecutionDto ace = new ActionContextExecutionDto(action);

        // Find values for any inputs that we can.
        if (ace.getInputs() != null) {
            for (final ActionInput input : ace.getInputs()) {
                // Try to get inputs of type=property out of the root object
                if (StringUtils.isNotBlank(input.getType())) {
                    if (input.getType().equals("property")) {
                        final String property = cacheQuery(item, input.getRootProperty(), cache);
                        if (property != null) {
                            input.setValue(property);
                        }
                    } else {
                        // TODO Are there other types of input we need to handle specially?
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


        // Get all the item's resources, and figure out which ones the ACE needs.
        List<XnatAbstractresource> resources = null;
        try {
            // Stolen from XnatImagescandata.getFile()
            resources = BaseElement.WrapItems(item.getChildItems("File"));
        } catch (ElementNotFoundException | FieldNotFoundException e) {
            // No problem, necessarily. Item just has no resources
        }
        if (resources == null || resources.isEmpty()) {
            if (ace.getResourcesStaged() != null && !ace.getResourcesStaged().isEmpty()) {
                // Now there is a problem. item has no resources, but we need some staged.
                return null;
            }
        } else {
            if(ace.getResourcesStaged() != null) {
                setResourceIds(ace.getResourcesStaged(), resources);
                for (final ActionResource actionResource : ace.getResourcesStaged()) {
                    if (actionResource.getResourceId() == null) {
                        // We needed to find an item resource to mount, but we didn't.
                        return null;
                    }
                }
            }
            if(ace.getResourcesCreated() != null) {
                setResourceIds(ace.getResourcesCreated(), resources);
                // It's ok if there are null ids. These will be created, so they
                // don't have to exist yet.
            }
        }

        return ace;
    }

    private void setResourceIds(final List<ActionResource> aceResources,
                                final List<XnatAbstractresource> itemResources) {
        for (final ActionResource aceResource : aceResources) {
            for (final XnatAbstractresource itemResource : itemResources) {
                if (aceResource.getResourceName().equals(itemResource.getLabel())) {
                    aceResource.setResourceId(itemResource.getXnatAbstractresourceId());
                }
            }
        }
    }

    private String cacheQuery(final XFTItem item, final String propertyName,
                            final Map<ItemQueryCacheKey, String> cache)
            throws XFTInitException {
        final ItemQueryCacheKey query = new ItemQueryCacheKey(item, propertyName);
        if (cache.containsKey(query)) {
            return cache.get(query);
        }

        final Object propertyObj;
        try {
            propertyObj = item.getProperty(propertyName);
        } catch (ElementNotFoundException | FieldNotFoundException e) {
            logger.debug("Could not find property %s on item %s.",
                    propertyName, item.toString());
            return null;
        }

        if (String.class.isAssignableFrom(propertyObj.getClass())) {
            final String propertyString = (String) propertyObj;
            cache.put(query, propertyString);
            return propertyString;
        } else {
            // TODO Do we allow a non-string property here?
            logger.debug("Non-string property found for %s on item %s: %s",
                    propertyName, item.toString(), propertyObj.toString());
        }
        return null;
    }

    private Boolean doesItemMatchMatchers(final XFTItem item, final List<Matcher> matchers,
                                          final Map<ItemQueryCacheKey, String> cache)
            throws XFTInitException {
        if (matchers == null || matchers.isEmpty()) {
            return true;
        }

        for (final Matcher matcher : matchers) {
            if (!doesItemMatchMatchers(item, matcher, cache)) {
                return false;
            }
        }
        return true;
    }
    private Boolean doesItemMatchMatchers(final XFTItem item, final Matcher matcher,
                                          final Map<ItemQueryCacheKey, String> cache)
            throws XFTInitException {
        if (matcher == null || StringUtils.isBlank(matcher.getValue())) {
            return true;
        }

        final String property = cacheQuery(item, matcher.getProperty(), cache);
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
