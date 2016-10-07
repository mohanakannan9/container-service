package org.nrg.execution.services;

import com.google.common.annotations.VisibleForTesting;
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
import org.nrg.execution.model.CommandInput;
import org.nrg.execution.model.CommandRun;
import org.nrg.execution.model.ContainerExecution;
import org.nrg.execution.model.Context;
import org.nrg.execution.model.ResolvedCommand;
import org.nrg.framework.exceptions.NrgRuntimeException;
import org.nrg.framework.exceptions.NrgServiceRuntimeException;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntityService;
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

    @Override
    public Command get(final Long id) throws NotFoundException {
        final Command command = retrieve(id);
        if (command == null) {
            throw new NotFoundException("Could not find Command with id " + id);
        }
        return command;
    }

    @Override
    public void initialize(final Command command) {
        if (command == null) {
            return;
        }
        Hibernate.initialize(command);
        Hibernate.initialize(command.getInputs());
        Hibernate.initialize(command.getOutputs());

        final CommandRun run = command.getRun();
        if (run != null) {
            Hibernate.initialize(run.getEnvironmentVariables());
            Hibernate.initialize(run.getCommandLine());
            Hibernate.initialize(run.getMounts());
        }
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
    public ResolvedCommand resolveCommand(final Long commandId,
                                          final Map<String, String> variableValuesProvidedAtRuntime)
            throws NotFoundException, CommandVariableResolutionException {
        final Command command = get(commandId);
        return resolveCommand(command, variableValuesProvidedAtRuntime);
    }

    @Override
    public ResolvedCommand resolveCommand(final Command command)
            throws NotFoundException, CommandVariableResolutionException {
        return resolveCommand(command, Maps.<String, String>newHashMap());
    }

    @Override
    public ResolvedCommand resolveCommand(final Command command,
                                          final Map<String, String> inputValuesProvidedAtRuntime)
            throws NotFoundException, CommandVariableResolutionException {

        if (inputValuesProvidedAtRuntime == null) {
            return resolveCommand(command);
        }

        final Map<String, String> resolvedInputValues = Maps.newHashMap();
        final Map<String, String> resolvedInputValuesAsCommandLineArgs = Maps.newHashMap();
        if (command.getInputs() != null) {
            for (final CommandInput input : command.getInputs()) {
                final String runtimeValue = inputValuesProvidedAtRuntime.get(input.getName());

                final String resolvedValue = getResolvedInputValue(input, runtimeValue);

                // If resolved value is null, and input is required, that is an error
                if (resolvedValue == null && input.isRequired()) {
                    throw new CommandVariableResolutionException(input);
                }

                // Only substitute the input into the command line if a replacementKey is set
                // TODO This will be changed later, as we will allow pro-active searching with JSONPath
                final String replacementKey = input.getReplacementKey();
                if (StringUtils.isBlank(replacementKey)) {
                    continue;
                }
                resolvedInputValues.put(replacementKey, resolvedValue);
                resolvedInputValuesAsCommandLineArgs.put(replacementKey, getValueForCommandLine(input, resolvedValue));
            }
        }

        // Replace variable names in command line, mounts, and environment variables
        final ResolvedCommand resolvedCommand = new ResolvedCommand(command);
        final CommandRun run = command.getRun();
        resolvedCommand.setCommandLine(resolveTemplate(run.getCommandLine(), resolvedInputValuesAsCommandLineArgs));
        resolvedCommand.setMounts(resolveCommandMounts(run.getMounts(), resolvedInputValues));
        resolvedCommand.setEnvironmentVariables(resolveTemplateMap(run.getEnvironmentVariables(), resolvedInputValues, true));

        // TODO What else do I need to do to resolve the command?

        return resolvedCommand;
    }

    private String getResolvedInputValue(final CommandInput input, final String runtimeValue) {
        final String rawValue;
        if (runtimeValue != null) {
            rawValue = runtimeValue;
        } else {
            rawValue = input.getValueOrDefaultValue();
        }
        if (rawValue == null) {
            return null;
        }

        if (input.getType() != null && input.getType().equals(CommandInput.Type.BOOLEAN)) {
            if (Boolean.parseBoolean(rawValue)) {
                return input.getTrueValue() != null ? input.getTrueValue() : "";
            } else {
                return input.getFalseValue() != null ? input.getFalseValue() : "";
            }
        } else {
            return rawValue;
        }
    }

    private String getValueForCommandLine(final CommandInput input, final String resolvedInputValue) {
        if (StringUtils.isBlank(input.getCommandLineFlag())) {
            return resolvedInputValue;
        } else {
            return input.getCommandLineFlag() +
                    (input.getCommandLineSeparator() == null ? " " : input.getCommandLineSeparator()) +
                    resolvedInputValue;
        }
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
        if (command.getInputs() != null) {
            for (final CommandInput variable : command.getInputs()) {
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

        final CommandRun run = command.getRun();
        if (run.getMounts() != null && !run.getMounts().isEmpty()) {
            // Item needs resources
            final List<CommandMount> mountsIn = Lists.newArrayList();
            final List<CommandMount> mountsOut = Lists.newArrayList();
            for (final CommandMount mount : run.getMounts()) {
                if (mount.isInput()) {
                    mountsIn.add(mount);
                } else {
                    mountsOut.add(mount);
                }
            }

            if (!mountsIn.isEmpty() && (resourceLabelToCatalogPath == null || resourceLabelToCatalogPath.isEmpty())) {
                // Item has no resources, but we need some staged. Action does not work.
                return null;
            } else {
                for (final CommandMount mount : mountsIn) {
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

            // If there are no resources, then we don't need to check if they can be overwritten
            if (!(resourceLabelToCatalogPath == null || resourceLabelToCatalogPath.isEmpty())) {
                // There are resources, so check if we will need to overwrite any
                for (final CommandMount mount : mountsOut) {

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

        return resolveCommand(command, context);
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
    public ContainerExecution launchCommand(final Long commandId, final Map<String, String> variableRuntimeValues, final UserI userI)
            throws NoServerPrefException, DockerServerException, NotFoundException, CommandVariableResolutionException {
        final ResolvedCommand resolvedCommand = resolveCommand(commandId, variableRuntimeValues);
        return launchCommand(resolvedCommand, userI);
    }

    @Override
    public ContainerExecution launchCommand(final Long commandId, final UserI userI, final XnatImagesessiondata session)
            throws NotFoundException, XFTInitException, CommandVariableResolutionException, NoServerPrefException, DockerServerException {
        final Command command = get(commandId);

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

        final ResolvedCommand preparedToLaunch = prelaunch(resolvedCommand, userI);

        return launchCommand(preparedToLaunch, session.getId(), session.getXSIType(), userI);
    }

    @Override
    public ContainerExecution launchCommand(Long commandId, UserI userI, XnatImagesessiondata session, XnatImagescandata scan)
            throws NotFoundException, XFTInitException, CommandVariableResolutionException, NoServerPrefException, DockerServerException {
        final Command command = get(commandId);

        final ResolvedCommand preparedToLaunch = prepareToLaunchScan(command, session, scan, userI);

        final String concattenatedSessionScanId = session.getId() + ":" + scan.getId();
        return launchCommand(preparedToLaunch, concattenatedSessionScanId, scan.getXSIType(), userI);
    }

    @VisibleForTesting
    @Override
    public ResolvedCommand prepareToLaunchScan(final Command command,
                                                final XnatImagesessiondata session,
                                                final XnatImagescandata scan,
                                                final UserI userI)
            throws CommandVariableResolutionException, NotFoundException, XFTInitException, NoServerPrefException {
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

        return prelaunch(resolvedCommand, userI);
    }

    private ResolvedCommand prelaunch(final ResolvedCommand resolvedCommand, final UserI userI) throws NoServerPrefException {
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

        return resolvedCommand;
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

        for (final String replacementKey : variableValues.keySet()) {
            final String replacementValue = variableValues.get(replacementKey);
            toResolve = toResolve.replaceAll(replacementKey, replacementValue);
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
