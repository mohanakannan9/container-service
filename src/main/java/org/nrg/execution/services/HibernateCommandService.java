package org.nrg.execution.services;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Hibernate;
import org.hibernate.exception.ConstraintViolationException;
import org.nrg.execution.api.ContainerControlApi;
import org.nrg.execution.daos.CommandDao;
import org.nrg.execution.exceptions.CommandVariableResolutionException;
import org.nrg.execution.exceptions.DockerServerException;
import org.nrg.execution.exceptions.NoServerPrefException;
import org.nrg.execution.exceptions.NotFoundException;
import org.nrg.execution.model.Command;
import org.nrg.execution.model.CommandMount;
import org.nrg.execution.model.CommandVariable;
import org.nrg.execution.model.ContainerExecution;
import org.nrg.execution.model.Context;
import org.nrg.execution.model.ResolvedCommand;
import org.nrg.framework.exceptions.NrgRuntimeException;
import org.nrg.framework.exceptions.NrgServiceRuntimeException;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntityService;
import org.nrg.framework.orm.hibernate.BaseHibernateService;
import org.nrg.framework.orm.hibernate.HibernateUtils;
import org.nrg.transporter.TransportService;
import org.nrg.xdat.entities.AliasToken;
import org.nrg.xdat.om.XnatAbstractresource;
import org.nrg.xdat.om.XnatImagescandata;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.XnatResourcecatalog;
import org.nrg.xdat.om.base.BaseXnatExperimentdata.UnknownPrimaryProjectException;
import org.nrg.xdat.preferences.SiteConfigPreferences;
import org.nrg.xdat.services.AliasTokenService;
import org.nrg.xft.ItemI;
import org.nrg.xft.exception.ElementNotFoundException;
import org.nrg.xft.exception.FieldNotFoundException;
import org.nrg.xft.exception.XFTInitException;
import org.nrg.xft.security.UserI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Transactional
public class HibernateCommandService extends AbstractHibernateEntityService<Command, CommandDao>
        implements CommandService {
    private static final Logger log = LoggerFactory.getLogger(HibernateCommandService.class);

    @Autowired private ContainerControlApi controlApi;
    @Autowired private AliasTokenService aliasTokenService;
    @Autowired private SiteConfigPreferences siteConfigPreferences;
    @Autowired private TransportService transporter;
    @Autowired private ContainerExecutionService containerExecutionService;

    private static final Pattern TEMPLATE_PATTERN = Pattern.compile("(?<=\\s|\\A)#(\\w+)#(?=\\s|\\z)"); // Match #varname#

    @Override
    public void initialize(final Command command) {
        Hibernate.initialize(command);
        Hibernate.initialize(command.getEnvironmentVariables());
        Hibernate.initialize(command.getRunTemplate());
        Hibernate.initialize(command.getMountsIn());
        Hibernate.initialize(command.getMountsOut());
        Hibernate.initialize(command.getVariables());
    }

    @Override
    public Command create(final Command command) throws NrgRuntimeException {
        try {
            return super.create(command);
        } catch (ConstraintViolationException e) {
            throw new NrgServiceRuntimeException("A command already exists with this name and docker image ID.");
        }
    }

    @Override
    @Transactional
    public Command retrieve(long id) {
        if (log.isDebugEnabled()) {
            log.debug("Retrieving entity for ID: " + id);
        }
        final Command entity;
        if (HibernateUtils.isAuditable(getParameterizedType())) {
            entity = getDao().findEnabledById(id);
        } else {
            entity = getDao().retrieve(id);
        }

        if (entity != null) {
            initialize(entity);
        }
        return entity;
    }

    @Override
    @Transactional
    public Command retrieve(final String name, final String dockerImageId) {
        final Command command = getDao().retrieve(name, dockerImageId);
        if (command != null) {
            initialize(command);
        }
        return command;
    }

    @Override
    @Transactional
    public List<Command> getAll() {
        log.debug("Getting all enabled entities");
        final List<Command> list = getDao().findAllEnabled();
        for (final Command entity : list) {
            initialize(entity);
        }

        return list;
    }

    @Override
    public List<Command> save(final List<Command> commands) {
        final List<Command> saved = Lists.newArrayList();
        if (!(commands == null || commands.isEmpty())) {
            for (final Command command : commands) {
                try {
                    create(command);
                    saved.add(command);
                } catch (NrgServiceRuntimeException e) {
                    // TODO: should I "update" instead of erroring out if command already exists?
                    log.error("Could not save command: " + command, e);
                }
            }
        }
        getDao().flush();
        return saved;
    }

    @Override
    public ResolvedCommand resolveCommand(final Long commandId)
            throws NotFoundException, CommandVariableResolutionException {
        return resolveCommand(commandId, Maps.<String, String>newHashMap());
    }

    @Override
    public ResolvedCommand resolveCommand(final Long commandId,
                                          final Map<String, String> variableValuesProvidedAtRuntime)
            throws NotFoundException, CommandVariableResolutionException {
        final Command command = retrieve(commandId);
        if (command == null) {
            throw new NotFoundException("Could not find Command with id " + commandId);
        }
        return resolveCommand(command, variableValuesProvidedAtRuntime);
    }

    @Override
    public ResolvedCommand resolveCommand(final Command command)
            throws NotFoundException, CommandVariableResolutionException {
        return resolveCommand(command, Maps.<String, String>newHashMap());
    }

    @Override
    public ResolvedCommand resolveCommand(final Command command,
                                          final Map<String, String> variableValuesProvidedAtRuntime)
            throws NotFoundException, CommandVariableResolutionException {

        if (variableValuesProvidedAtRuntime == null) {
            return resolveCommand(command);
        }

        final Map<String, String> resolvedVariableValues = Maps.newHashMap();
        final Map<String, String> resolvedVariableValuesAsRunTemplateArgs = Maps.newHashMap();
        if (command.getVariables() != null) {
            for (final CommandVariable variable : command.getVariables()) {
                // raw value is runtime value if provided, else default value
                String variableRawValueCouldBeNull = variable.getDefaultValue();
                if (StringUtils.isNotBlank(variable.getValue())) {
                    variableRawValueCouldBeNull = variable.getValue();
                }
                if (variableValuesProvidedAtRuntime.containsKey(variable.getName())) {
                    variableRawValueCouldBeNull = variableValuesProvidedAtRuntime.get(variable.getName());
                }
                final String variableRawValue = variableRawValueCouldBeNull == null ? "" : variableRawValueCouldBeNull;

                // If there is no raw value, and variable is required, that is an error
                if (StringUtils.isBlank(variableRawValue) && variable.isRequired()) {
                    throw new CommandVariableResolutionException(variable);
                }

                // value is either = raw value, or if "type" is "boolean" then value = trueValue if rawvalue=true or falseValue if rawvalue=false
                final String variableValue =
                        variable.getType() != null && variable.getType().equalsIgnoreCase("boolean") ?
                                (Boolean.valueOf(variableRawValue) ? variable.getTrueValue() : variable.getFalseValue()) :
                                variableRawValue;

                // to get the value as run template arg, we replace any "#value#" tokens in argTemplate
                final String variableValueAsRunTemplateArg =
                        StringUtils.isNotBlank(variable.getArgTemplate()) ?
                                variable.getArgTemplate().replaceAll("#value#", variableValue) :
                                variableValue;

                resolvedVariableValues.put(variable.getName(), variableValue);
                resolvedVariableValuesAsRunTemplateArgs.put(variable.getName(), variableValueAsRunTemplateArg);
            }
        }

        // Replace variable names in runTemplate, mounts, and environment variables
        final ResolvedCommand resolvedCommand = new ResolvedCommand(command);
        resolvedCommand.setRun(resolveTemplateList(command.getRunTemplate(), resolvedVariableValuesAsRunTemplateArgs));
        resolvedCommand.setMountsIn(resolveCommandMounts(command.getMountsIn(), resolvedVariableValues));
        resolvedCommand.setMountsOut(resolveCommandMounts(command.getMountsOut(), resolvedVariableValues));
        resolvedCommand.setEnvironmentVariables(resolveTemplateMap(command.getEnvironmentVariables(), resolvedVariableValues, true));

        // TODO What else do I need to do to resolve the command?

        return resolvedCommand;
    }

    private ResolvedCommand resolve(final Command command,
                                    final ItemI itemI,
                                    final Map<String, String> resourceLabelToCatalogPath,
                                    final Context context)
            throws XFTInitException, NotFoundException, CommandVariableResolutionException {

//        if (!doesItemMatchMatchers(itemI, action.getRootMatchers(), cache)) {
//            return null;
//        }
        String itemId = "<not found>";
        try {
            itemId = itemI.getItem().getIDValue(); // TODO this is null for some reason?
        } catch (ElementNotFoundException ignored) {
            // Can't get ID.
        }

        // Find values for any inputs that we can.
        if (command.getVariables() != null) {
            for (final CommandVariable variable : command.getVariables()) {
                // Try to get inputs of type=property out of the root object
                if (StringUtils.isNotBlank(variable.getRootProperty())) {
                    try {

                        final String property = (String)itemI.getProperty(variable.getRootProperty());
                        if (property != null) {
                            variable.setValue(property);
                        }
                    } catch (ElementNotFoundException | FieldNotFoundException e) {
                        log.info("Field %s not found on item with id %s",
                                variable.getRootProperty(), itemId);
                    }
                }

                // Now try to get values from the context.
                // (Even if we already found the value from the item, we want to do this.
                //   Values in the context take precedence over XFTItem properties.)
                if (StringUtils.isNotBlank(context.get(variable.getName()))) {
                    variable.setValue(context.get(variable.getName()));
                }
            }
        }

        if (command.getMountsIn() != null && !command.getMountsIn().isEmpty()) {
            // Item needs resources

            if (resourceLabelToCatalogPath == null || resourceLabelToCatalogPath.isEmpty()) {
                // Item has no resources, but we need some staged. Action does not work.
                return null;
            } else {
                for (final CommandMount mount : command.getMountsIn()) {
                    final String resourceCatalogPath = resourceLabelToCatalogPath.get(mount.getName());
                    if (StringUtils.isNotBlank(resourceCatalogPath)) {
                        mount.setHostPath(resourceLabelToCatalogPath.get(mount.getName()));
                    } else {
                        if(log.isDebugEnabled()) {
                            final String message =
                                    String.format("Command %d needed resource, but item has no resource named %s",
                                            command.getId(), mount.getName());
                            log.debug(message);
                        }
                        return null;
                    }
                }
            }
        }

        if (command.getMountsOut() != null && !command.getMountsOut().isEmpty()) {
            // Item needs resources

            if (!(resourceLabelToCatalogPath == null || resourceLabelToCatalogPath.isEmpty())) {
                for (final CommandMount mount : command.getMountsOut()) {

                    if (resourceLabelToCatalogPath.containsKey(mount.getName()) &&
                            !mount.getOverwrite()) {
                        if(log.isDebugEnabled()) {
                            final String message =
                                    String.format("Action %d will create resource, but item already has " +
                                                    "a resource named %s and action cannot overwrite it.",
                                            command.getId(), mount.getName());
                            log.debug(message);
                        }
                        return null;
                    }
                }
            }
        }

        return resolveCommand(command);
    }

    @Override
    public ContainerExecution launchCommand(final ResolvedCommand resolvedCommand, final UserI userI)
            throws NoServerPrefException, DockerServerException {
        return launchCommand(resolvedCommand, null, null, userI);
    }

    @Override
    public ContainerExecution launchCommand(final ResolvedCommand resolvedCommand,
                                            final String rootObjectId,
                                            final String rootObjectXsiType,
                                            final UserI userI)
            throws NoServerPrefException, DockerServerException {
        final String containerId = controlApi.launchImage(resolvedCommand);
        return containerExecutionService.save(resolvedCommand, containerId, rootObjectId, rootObjectXsiType, userI);
    }

    @Override
    public ContainerExecution launchCommand(final Long commandId, final UserI userI)
            throws NoServerPrefException, DockerServerException, NotFoundException, CommandVariableResolutionException {
        return launchCommand(commandId, Maps.<String, String>newHashMap(), userI);
    }

    @Override
    public ContainerExecution launchCommand(final Long commandId, final Map<String, String> variableRuntimeValues, final UserI userI)
            throws NoServerPrefException, DockerServerException, NotFoundException, CommandVariableResolutionException {
        final ResolvedCommand resolvedCommand = resolveCommand(commandId, variableRuntimeValues);
        return launchCommand(resolvedCommand, userI);
    }

    @Override
    public ContainerExecution launchCommand(final Long commandId, final UserI userI, final XnatImagesessiondata session)
            throws NotFoundException, XFTInitException, CommandVariableResolutionException, NoServerPrefException, DockerServerException {
        final Command command = retrieve(commandId);
        if (command == null) {
            throw new NotFoundException("No command with ID " + commandId);
        }

        final String projectId = session.getProject();
        final XnatProjectdata proj = XnatProjectdata.getXnatProjectdatasById(projectId, userI, false);
        final String rootArchivePath = proj.getRootArchivePath();
        final List<XnatAbstractresource> resources = session.getResources_resource();

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
            log.info("Session with id " + session.getId() + " has no resources or a blank archive path");
        }

        try {
            resourceLabelToCatalogPath.put("root", session.getRelativeArchivePath());
        } catch (UnknownPrimaryProjectException e) {
            log.info("Could not get session's archive path", e);
        }

        final ResolvedCommand resolvedCommand =
                resolve(command, session, resourceLabelToCatalogPath, Context.newContext());
        if (resolvedCommand == null) {
            // TODO throw an error
            return null;
        }

        // Add default environment variables
        final Map<String, String> defaultEnv = Maps.newHashMap();
//        siteConfigPreferences.getBuildPath()
        defaultEnv.put("XNAT_HOST", siteConfigPreferences.getSiteUrl());

        final AliasToken token = aliasTokenService.issueTokenForUser(userI);
        defaultEnv.put("XNAT_USER", token.getAlias());
        defaultEnv.put("XNAT_PASS", token.getSecret());

        resolvedCommand.addEnvironmentVariables(defaultEnv);

        // Transport mounts
        if (resolvedCommand.getMountsIn() != null) {
            final String dockerHost = controlApi.getServer().getHost();
            for (final CommandMount mountIn : resolvedCommand.getMountsIn()) {
                final Path pathOnXnatHost = Paths.get(mountIn.getHostPath());
                final Path pathOnDockerHost = transporter.transport(dockerHost, pathOnXnatHost);
                mountIn.setHostPath(pathOnDockerHost.toString());
            }
        }
        if (resolvedCommand.getMountsOut() != null) {
            final String dockerHost = controlApi.getServer().getHost();
            final List<CommandMount> mountsOut = resolvedCommand.getMountsOut();
            final List<Path> buildPaths = transporter.getWritableDirectories(dockerHost, mountsOut.size());
            for (int i=0; i < mountsOut.size(); i++) {
                final CommandMount mountOut = mountsOut.get(i);
                final Path buildPath = buildPaths.get(i);

                mountOut.setHostPath(buildPath.toString());
            }
        }

        return launchCommand(resolvedCommand, session.getId(), session.getXSIType(), userI);
    }

    @Override
    public ContainerExecution launchCommand(Long commandId, UserI userI, XnatImagesessiondata session, XnatImagescandata scan)
            throws NotFoundException, XFTInitException, CommandVariableResolutionException, NoServerPrefException, DockerServerException {
        final Command command = retrieve(commandId);
        if (command == null) {
            throw new NotFoundException("No command with ID " + commandId);
        }

        final String projectId = session.getProject();
        final XnatProjectdata proj = XnatProjectdata.getXnatProjectdatasById(projectId, userI, false);
        final String rootArchivePath = proj.getRootArchivePath();
        final List<XnatAbstractresource> resources = scan.getFile();

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
            log.info("Session with id " + session.getId() + " has a blank archive path, or scan " + scan.getId() + " has no resources.");
        }

        final Context context = Context.newContext();
        context.put("scanId", scan.getId());
        context.put("sessionId", session.getId());

        final ResolvedCommand resolvedCommand =
                resolve(command, scan, resourceLabelToCatalogPath, context);
        if (resolvedCommand == null) {
            // TODO throw an error
            return null;
        }

        // Add default environment variables
        final Map<String, String> defaultEnv = Maps.newHashMap();
//        siteConfigPreferences.getBuildPath()
        defaultEnv.put("XNAT_HOST", siteConfigPreferences.getSiteUrl());

        final AliasToken token = aliasTokenService.issueTokenForUser(userI);
        defaultEnv.put("XNAT_USER", token.getAlias());
        defaultEnv.put("XNAT_PASS", token.getSecret());

        resolvedCommand.addEnvironmentVariables(defaultEnv);

        // Transport mounts
        if (resolvedCommand.getMountsIn() != null) {
            final String dockerHost = controlApi.getServer().getHost();
            for (final CommandMount mountIn : resolvedCommand.getMountsIn()) {
                final Path pathOnXnatHost = Paths.get(mountIn.getHostPath());
                final Path pathOnDockerHost = transporter.transport(dockerHost, pathOnXnatHost);
                mountIn.setHostPath(pathOnDockerHost.toString());
            }
        }
        if (resolvedCommand.getMountsOut() != null) {
            final String dockerHost = controlApi.getServer().getHost();
            final List<CommandMount> mountsOut = resolvedCommand.getMountsOut();
            final List<Path> buildPaths = transporter.getWritableDirectories(dockerHost, mountsOut.size());
            for (int i=0; i < mountsOut.size(); i++) {
                final CommandMount mountOut = mountsOut.get(i);
                final Path buildPath = buildPaths.get(i);

                mountOut.setHostPath(buildPath.toString());
            }
        }

        final String concattenatedSessionScanId = session.getId() + ":" + scan.getId();
        return launchCommand(resolvedCommand, concattenatedSessionScanId, scan.getXSIType(), userI);
    }

//    @Override
//    public List<Command> parseLabels(final Map<String, String> labels) {
//        if (labels != null && !labels.isEmpty() && labels.containsKey(LABEL_KEY)) {
//            final String labelValue = labels.get(LABEL_KEY);
//            if (StringUtils.isNotBlank(labelValue)) {
//                try {
//                    return objectMapper.readValue(labelValue, new TypeReference<List<Command>>() {});
//                } catch (IOException e) {
//                    log.info("Could not parse Commands from label: %s", labelValue);
//                }
//            }
//        }
//        return null;
//    }


//    @Override
//    public List<Command> saveFromLabels(final String imageId) throws DockerServerException, NotFoundException, NoServerPrefException {
//        final List<Command> commands = controlApi.parseLabels(imageId);
//        return save(commands);
//    }
//
//    @Override
//    public List<Command> saveFromLabels(final DockerImage dockerImage) {
//        final List<Command> commands = controlApi.parseLabels(dockerImage);
//        return save(commands);
//    }

    private List<String> resolveTemplateList(final List<String> template,
                                             final Map<String, String> variableValues) {
        return Lists.transform(template, new Function<String, String>() {
            @Nullable
            @Override
            public String apply(@Nullable final String input) {
                return resolveTemplate(input, variableValues);
            }
        });
    }

    private String resolveTemplate(final String template,
                                   final Map<String, String> variableValues) {
        String toResolve = template;
        final Set<String> matches = Sets.newHashSet();

        final Matcher m = TEMPLATE_PATTERN.matcher(toResolve);
        while (m.find()) {
            matches.add(m.group(1));
        }

        for (final String varName : matches) {
            if (variableValues.get(varName) != null) {
                toResolve = toResolve.replaceAll("#"+ varName +"#", variableValues.get(varName));
            }
        }

        return toResolve;
    }

    private Map<String, String> resolveTemplateMap(final Map<String, String> templateMap,
                                                   final Map<String, String> variableValues,
                                                   final boolean keysAreTemplates) {
        if (templateMap == null) {
            return null;
        }

        final Map<String, String> resolvedMap = Maps.newHashMap();
        for (final Map.Entry<String, String> templateEntry : templateMap.entrySet()) {
            final String resolvedKey = keysAreTemplates ?
                    resolveTemplate(templateEntry.getKey(), variableValues) :
                    templateEntry.getKey();
            final String resolvedValue = resolveTemplate(templateEntry.getValue(), variableValues);
            resolvedMap.put(resolvedKey, resolvedValue);
        }
        return resolvedMap;
    }

    private List<CommandMount> resolveCommandMounts(final List<CommandMount> commandMounts,
                                                    final Map<String, String> variableValues) {
        if (commandMounts == null || commandMounts.isEmpty()) {
            return Lists.newArrayList();
        }

        for (final CommandMount mount : commandMounts) {
            mount.setRemotePath(resolveTemplate(mount.getRemotePath(), variableValues));
        }

        return commandMounts;
    }
}
