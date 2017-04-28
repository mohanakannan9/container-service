package org.nrg.containers.services.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.InvalidJsonException;
import com.jayway.jsonpath.InvalidPathException;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.TypeRef;
import com.jayway.jsonpath.spi.mapper.MappingException;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.nrg.config.services.ConfigService;
import org.nrg.containers.exceptions.CommandInputResolutionException;
import org.nrg.containers.exceptions.CommandMountResolutionException;
import org.nrg.containers.exceptions.CommandResolutionException;
import org.nrg.containers.exceptions.CommandWrapperInputResolutionException;
import org.nrg.containers.exceptions.ContainerMountResolutionException;
import org.nrg.containers.model.command.auto.Command.CommandInput;
import org.nrg.containers.model.command.auto.Command.CommandMount;
import org.nrg.containers.model.command.auto.Command.CommandOutput;
import org.nrg.containers.model.command.auto.Command.CommandWrapper;
import org.nrg.containers.model.command.auto.Command.CommandWrapperDerivedInput;
import org.nrg.containers.model.command.auto.Command.CommandWrapperInput;
import org.nrg.containers.model.command.auto.Command.CommandWrapperOutput;
import org.nrg.containers.model.command.auto.Command.ConfiguredCommand;
import org.nrg.containers.model.command.auto.ResolvedCommand;
import org.nrg.containers.model.command.auto.ResolvedCommand.PartiallyResolvedCommand;
import org.nrg.containers.model.command.auto.ResolvedCommand.PartiallyResolvedCommandMount;
import org.nrg.containers.model.command.auto.ResolvedCommand.ResolvedCommandMount;
import org.nrg.containers.model.command.auto.ResolvedCommand.ResolvedCommandMountFiles;
import org.nrg.containers.model.command.auto.ResolvedCommand.ResolvedCommandOutput;
import org.nrg.containers.model.xnat.Assessor;
import org.nrg.containers.model.xnat.Project;
import org.nrg.containers.model.xnat.Resource;
import org.nrg.containers.model.xnat.Scan;
import org.nrg.containers.model.xnat.Session;
import org.nrg.containers.model.xnat.Subject;
import org.nrg.containers.model.xnat.XnatFile;
import org.nrg.containers.model.xnat.XnatModelObject;
import org.nrg.containers.services.CommandResolutionService;
import org.nrg.containers.services.CommandService;
import org.nrg.framework.constants.Scope;
import org.nrg.framework.exceptions.NotFoundException;
import org.nrg.xdat.preferences.SiteConfigPreferences;
import org.nrg.xft.security.UserI;
import org.nrg.xnat.helpers.uri.URIManager;
import org.nrg.xnat.helpers.uri.URIManager.ArchiveItemURI;
import org.nrg.xnat.helpers.uri.UriParserUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.nrg.containers.model.command.entity.CommandWrapperInputType.ASSESSOR;
import static org.nrg.containers.model.command.entity.CommandWrapperInputType.BOOLEAN;
import static org.nrg.containers.model.command.entity.CommandWrapperInputType.CONFIG;
import static org.nrg.containers.model.command.entity.CommandWrapperInputType.DIRECTORY;
import static org.nrg.containers.model.command.entity.CommandWrapperInputType.FILE;
import static org.nrg.containers.model.command.entity.CommandWrapperInputType.FILES;
import static org.nrg.containers.model.command.entity.CommandWrapperInputType.NUMBER;
import static org.nrg.containers.model.command.entity.CommandWrapperInputType.PROJECT;
import static org.nrg.containers.model.command.entity.CommandWrapperInputType.RESOURCE;
import static org.nrg.containers.model.command.entity.CommandWrapperInputType.SCAN;
import static org.nrg.containers.model.command.entity.CommandWrapperInputType.SESSION;
import static org.nrg.containers.model.command.entity.CommandWrapperInputType.STRING;
import static org.nrg.containers.model.command.entity.CommandWrapperInputType.SUBJECT;

@Service
public class CommandResolutionServiceImpl implements CommandResolutionService {
    private final Logger log = LoggerFactory.getLogger(CommandResolutionService.class);

    private final CommandService commandService;
    private final ConfigService configService;
    private final SiteConfigPreferences siteConfigPreferences;
    private final ObjectMapper mapper;

    @Autowired
    public CommandResolutionServiceImpl(final CommandService commandService,
                                        final ConfigService configService,
                                        final SiteConfigPreferences siteConfigPreferences,
                                        final ObjectMapper mapper) {
        this.commandService = commandService;
        this.configService = configService;
        this.siteConfigPreferences = siteConfigPreferences;
        this.mapper = mapper;
    }

    @Override
    public PartiallyResolvedCommand preResolve(final long wrapperId,
                                               final Map<String, String> inputValues,
                                               final UserI userI)
            throws NotFoundException, CommandResolutionException {
        return preResolve(commandService.getAndConfigure(wrapperId), inputValues, userI);
    }

    @Override
    public PartiallyResolvedCommand preResolve(final long commandId,
                                               final String wrapperName,
                                               final Map<String, String> inputValues,
                                               final UserI userI)
            throws NotFoundException, CommandResolutionException {
        return preResolve(commandService.getAndConfigure(commandId, wrapperName), inputValues, userI);
    }

    @Override
    public PartiallyResolvedCommand preResolve(final String project,
                                               final long wrapperId,
                                               final Map<String, String> inputValues,
                                               final UserI userI)
            throws NotFoundException, CommandResolutionException {
        return preResolve(commandService.getAndConfigure(project, wrapperId), inputValues, userI);
    }

    @Override
    public PartiallyResolvedCommand preResolve(final String project,
                                               final long commandId,
                                               final String wrapperName,
                                               final Map<String, String> inputValues,
                                               final UserI userI)
            throws NotFoundException, CommandResolutionException {
        return preResolve(commandService.getAndConfigure(project, commandId, wrapperName), inputValues, userI);
    }

    @Override
    public PartiallyResolvedCommand preResolve(final ConfiguredCommand configuredCommand, final Map<String, String> inputValues, final UserI userI) throws CommandResolutionException {
        final CommandResolutionHelper helper = new CommandResolutionHelper(configuredCommand, inputValues, userI);
        return helper.preResolve();
    }

    @Override
    public ResolvedCommand resolve(final long commandId,
                                   final String wrapperName,
                                   final Map<String, String> inputValues,
                                   final UserI userI)
            throws NotFoundException, CommandResolutionException {
        return resolve(commandService.getAndConfigure(commandId, wrapperName), inputValues, userI);
    }

    @Override
    @Nonnull
    public ResolvedCommand resolve(final long wrapperId,
                                   final Map<String, String> inputValues,
                                   final UserI userI)
            throws NotFoundException, CommandResolutionException {
        return resolve(commandService.getAndConfigure(wrapperId), inputValues, userI);
    }

    @Override
    @Nonnull
    public ResolvedCommand resolve(final String project,
                                   final long wrapperId,
                                   final Map<String, String> inputValues,
                                   final UserI userI)
            throws NotFoundException, CommandResolutionException {
        return resolve(commandService.getAndConfigure(project, wrapperId), inputValues, userI);
    }

    @Override
    public ResolvedCommand resolve(final String project,
                                   final long commandId,
                                   final String wrapperName,
                                   final Map<String, String> inputValues,
                                   final UserI userI)
            throws NotFoundException, CommandResolutionException {
        return resolve(commandService.getAndConfigure(project, commandId, wrapperName), inputValues, userI);
    }

    // @Override
    // @Nonnull
    // public PartiallyResolvedCommand resolve(final Command command,
    //                                                final Map<String, String> runtimeInputValues,
    //                                                final UserI userI)
    //         throws NotFoundException, CommandResolutionException {
    //     // I was not given a wrapper.
    //     // TODO what should I do here? Should I...
    //     //  1. Use the "passthrough" wrapper, no matter what
    //     //  2. Use the "passthrough" wrapper only if the command has no outputs
    //     //  3. check if the command has any wrappers, and use one if it exists
    //     //  4. Something else
    //     //
    //     // I guess for now I'll do 2.
    //
    //     if (!command.outputs().isEmpty()) {
    //         throw new CommandResolutionException("Cannot resolve command without an XNAT wrapper. Command has outputs that will not be handled.");
    //     }
    //
    //     final CommandWrapper commandWrapperToResolve = CommandWrapper.passthrough(command);
    //
    //     return resolve(commandWrapperToResolve, command, runtimeInputValues, userI);
    // }

    @Override
    @Nonnull
    public ResolvedCommand resolve(final ConfiguredCommand configuredCommand,
                                   final Map<String, String> inputValues,
                                   final UserI userI)
            throws NotFoundException, CommandResolutionException {
        final CommandResolutionHelper helper = new CommandResolutionHelper(configuredCommand, inputValues, userI);
        return helper.resolve();
    }

    private class CommandResolutionHelper {
        private final String JSONPATH_SUBSTRING_REGEX = "\\^(wrapper:)?(.+)\\^";

        private final CommandWrapper commandWrapper;
        private final ConfiguredCommand command;
        private final ResolvedCommand.Builder resolvedCommandBuilder;
        private final Map<String, CommandWrapperInput> resolvedWrapperInputObjects = Maps.newHashMap();
        private final Map<String, String> resolvedXnatInputValuesByCommandInputName = Maps.newHashMap();
        private final Map<String, List<CommandWrapperInput>> commandMountsToReceiveFilesFromXnatInputs = Maps.newHashMap();
        private final Map<String, String> resolvedInputValuesByReplacementKey = Maps.newHashMap();
        private final Map<String, String> resolvedInputCommandLineValuesByReplacementKey = Maps.newHashMap();
        private final UserI userI;
        private final Map<String, String> inputValues;
        private final Pattern jsonpathSubstringPattern;
        private final DocumentContext commandJsonpathSearchContext;
        private final DocumentContext commandWrapperJsonpathSearchContext;
        private String containerHost;

        // Caches
        private Map<CommandInput, String> commandInputValues =  Maps.newHashMap();
        private Map<CommandWrapperInput, String> commandWrapperInputValues =  Maps.newHashMap();
        private Map<CommandWrapperInput, String> commandWrapperInputJsonValues =  Maps.newHashMap();

        private CommandResolutionHelper(final ConfiguredCommand configuredCommand,
                                        final Map<String, String> inputValues,
                                        final UserI userI) throws CommandResolutionException {
            this.commandWrapper = configuredCommand.wrapper();
            this.command = configuredCommand;

            // Set up JSONPath search contexts
            final Configuration c = Configuration.defaultConfiguration().addOptions(Option.ALWAYS_RETURN_LIST);
            try {
                final String commandJson = mapper.writeValueAsString(command);
                commandJsonpathSearchContext = JsonPath.using(c).parse(commandJson);
            } catch (JsonProcessingException e) {
                throw new CommandResolutionException("Could not serialize command to JSON.", e);
            }

            try {
                final String commandWrapperJson = mapper.writeValueAsString(commandWrapper);
                commandWrapperJsonpathSearchContext = JsonPath.using(c).parse(commandWrapperJson);
            } catch (JsonProcessingException e) {
                throw new CommandResolutionException("Could not serialize command to JSON.", e);
            }
            // this.resolvedXnatInputObjects =
            // this.resolvedXnatInputValuesByCommandInputName = Maps.newHashMap();
            // this.resolvedInputValuesByReplacementKey = Maps.newHashMap();
            // this.resolvedInputCommandLineValuesByReplacementKey = Maps.newHashMap();
            this.userI = userI;
            this.jsonpathSubstringPattern = Pattern.compile(JSONPATH_SUBSTRING_REGEX);

            this.inputValues = inputValues == null ?
                    Maps.<String, String>newHashMap() :
                    inputValues;

            resolvedCommandBuilder = ResolvedCommand.builder()
                    .wrapperId(commandWrapper.id())
                    .wrapperName(commandWrapper.name())
                    .wrapperDescription(commandWrapper.description())
                    .commandId(command.id())
                    .commandName(command.name())
                    .commandDescription(command.description())
                    .image(command.image())
                    .rawInputValues(this.inputValues);
        }

        private PartiallyResolvedCommand preResolve() {
            return null; // TODO
        }

        private ResolvedCommand resolve() throws CommandResolutionException {
            log.info("Resolving command.");
            if (log.isDebugEnabled()) {
                log.debug(command.toString());
            }

            final ResolvedCommand resolvedCommand =
                    resolvedCommandBuilder.wrapperInputValues(resolveXnatWrapperInputs())
                            .commandInputValues(resolveInputs())
                            .outputs(resolveOutputs())
                            .commandLine(resolveCommandLine())
                            .environmentVariables(resolveEnvironmentVariables())
                            .workingDirectory(resolveTemplate(command.workingDirectory()))
                            .ports(resolvePorts())
                            .mounts(resolveCommandMounts())
                            .build();

            log.info("Done resolving command.");
            if (log.isDebugEnabled()) {
                log.debug("Resolved command: \n" + resolvedCommand);
            }
            return resolvedCommand;
        }

        private Map<String, String> resolveXnatWrapperInputs() throws CommandResolutionException {
            log.info("Resolving xnat wrapper inputs.");

            final boolean hasExternalInputs = !(commandWrapper.externalInputs() == null || commandWrapper.externalInputs().isEmpty());
            final boolean hasDerivedInputs = !(commandWrapper.derivedInputs() == null || commandWrapper.derivedInputs().isEmpty());

            if (!hasExternalInputs) {
                if (hasDerivedInputs) {
                    // TODO this should be fixed at validation
                    final String message = "Cannot resolve inputs. There are no external inputs, but there are inputs that need to be derived from external inputs.";
                    log.error(message);
                    throw new CommandResolutionException(message);
                } else {
                    log.info("No xnat wrapper inputs.");
                    return null;
                }
            }

            final Map<String, String> resolvedXnatWrapperInputValuesByName = Maps.newHashMap();
            log.info("Resolving external xnat wrapper inputs.");
            for (final CommandWrapperInput externalInput : commandWrapper.externalInputs()) {
                log.info(String.format("Resolving input \"%s\".", externalInput.name()));

                String resolvedValue = null;
                String jsonRepresentation = null;

                // Give the input its default value
                if (log.isDebugEnabled()) {
                    log.debug("Default value: " + externalInput.defaultValue());
                }
                if (externalInput.defaultValue() != null) {
                    resolvedValue = externalInput.defaultValue();
                }

                // If a value was provided at runtime, use that over the default
                if (inputValues.containsKey(externalInput.name()) && inputValues.get(externalInput.name()) != null) {
                    if (log.isDebugEnabled()) {
                        log.debug("Runtime value: " + inputValues.get(externalInput.name()));
                    }
                    resolvedValue = inputValues.get(externalInput.name());
                }

                // Check for JSONPath substring in input value
                resolvedValue = resolveJsonpathSubstring(resolvedValue);

                // Resolve the matcher, if one was provided
                if (log.isDebugEnabled()) {
                    log.debug("Matcher: " + externalInput.matcher());
                }
                final String resolvedMatcher = externalInput.matcher() != null ? resolveTemplate(externalInput.matcher()) : null;

                if (StringUtils.isNotBlank(resolvedValue)) {
                    // Process the input based on its type
                    if (log.isDebugEnabled()) {
                        log.debug("Processing input value as a " + externalInput.type());
                    }
                    final String type = externalInput.type();
                    if (type.equals(PROJECT.getName())) {

                        // We were either given, A. an archive-style URI, or B. the project id
                        final Project aProject;
                        try {
                            aProject = resolveXnatObjectUri(resolvedValue, resolvedMatcher, Project.class,
                                    Project.uriToModelObject(), Project.idToModelObject(userI));
                        } catch (CommandWrapperInputResolutionException e) {
                            throw new CommandWrapperInputResolutionException(e.getMessage(), externalInput);
                        }

                        if (aProject != null) {
                            if (log.isDebugEnabled()) {
                                log.debug("Setting resolvedValue to uri " + aProject.getUri());
                            }
                            resolvedValue = aProject.getUri();
                            try {
                                jsonRepresentation = mapper.writeValueAsString(aProject);
                            } catch (JsonProcessingException e) {
                                String message = "Could not serialize project";
                                if (log.isDebugEnabled()) {
                                    message += ": " + aProject;
                                } else {
                                    message += ".";
                                }
                                log.error(message, e);
                            }
                        }
                    } else if (type.equals(SUBJECT.getName())) {
                        // We were either given, A. an archive-style URI, or B. the subject id
                        final Subject aSubject;
                        try {
                            aSubject = resolveXnatObjectUri(resolvedValue, resolvedMatcher, Subject.class,
                                    Subject.uriToModelObject(), Subject.idToModelObject(userI));
                        } catch (CommandWrapperInputResolutionException e) {
                            throw new CommandWrapperInputResolutionException(e.getMessage(), externalInput);
                        }

                        if (aSubject != null) {
                            if (log.isDebugEnabled()) {
                                log.debug("Setting resolvedValue to uri " + aSubject.getUri());
                            }
                            resolvedValue = aSubject.getUri();
                            try {
                                jsonRepresentation = mapper.writeValueAsString(aSubject);
                            } catch (JsonProcessingException e) {
                                String message = "Could not serialize subject";
                                if (log.isDebugEnabled()) {
                                    message += ": " + aSubject;
                                } else {
                                    message += ".";
                                }
                                log.error(message, e);
                            }
                        }
                    } else if (type.equals(SESSION.getName())) {
                        // We were either given, A. an archive-style URI, or B. the session id
                        final Session aSession;
                        try {
                            aSession = resolveXnatObjectUri(resolvedValue, resolvedMatcher, Session.class,
                                    Session.uriToModelObject(), Session.idToModelObject(userI));
                        } catch (CommandWrapperInputResolutionException e) {
                            throw new CommandWrapperInputResolutionException(e.getMessage(), externalInput);
                        }

                        if (aSession != null) {
                            if (log.isDebugEnabled()) {
                                log.debug("Setting resolvedValue to uri " + aSession.getUri());
                            }
                            resolvedValue = aSession.getUri();
                            try {
                                jsonRepresentation = mapper.writeValueAsString(aSession);
                            } catch (JsonProcessingException e) {
                                String message = "Could not serialize session";
                                if (log.isDebugEnabled()) {
                                    message += ": " + aSession;
                                } else {
                                    message += ".";
                                }
                                log.error(message, e);
                            }
                        }
                    } else if (type.equals(SCAN.getName())) {
                        // We must have been given the Scan as an archive URI
                        final Scan aScan;
                        try {
                            aScan = resolveXnatObjectUri(resolvedValue, resolvedMatcher, Scan.class,
                                    Scan.uriToModelObject(), Scan.idToModelObject(userI));
                        } catch (CommandWrapperInputResolutionException e) {
                            log.debug(e.getMessage());
                            throw new CommandWrapperInputResolutionException(e.getMessage(), externalInput);
                        }

                        if (aScan != null) {
                            if (log.isDebugEnabled()) {
                                log.debug("Setting resolvedValue to uri " + aScan.getUri());
                            }
                            resolvedValue = aScan.getUri();
                            try {
                                jsonRepresentation = mapper.writeValueAsString(aScan);
                            } catch (JsonProcessingException e) {
                                String message = "Could not serialize scan";
                                if (log.isDebugEnabled()) {
                                    message += ": " + aScan;
                                } else {
                                    message += ".";
                                }
                                log.error(message, e);
                            }
                        }
                    } else if (type.equals(ASSESSOR.getName())) {
                        // We were either given, A. an archive-style URI, or B. the assessor id
                        final Assessor anAssessor;
                        try {
                            anAssessor = resolveXnatObjectUri(resolvedValue, resolvedMatcher, Assessor.class,
                                    Assessor.uriToModelObject(), Assessor.idToModelObject(userI));
                        } catch (CommandWrapperInputResolutionException e) {
                            throw new CommandWrapperInputResolutionException(e.getMessage(), externalInput);
                        }

                        if (anAssessor != null) {
                            if (log.isDebugEnabled()) {
                                log.debug("Setting resolvedValue to uri " + anAssessor.getUri());
                            }
                            resolvedValue = anAssessor.getUri();
                            try {
                                jsonRepresentation = mapper.writeValueAsString(anAssessor);
                            } catch (JsonProcessingException e) {
                                String message = "Could not serialize assessor";
                                if (log.isDebugEnabled()) {
                                    message += ": " + anAssessor;
                                } else {
                                    message += ".";
                                }
                                log.error(message, e);
                            }
                        }
                    } else if (type.equals(RESOURCE.getName())) {
                        // We were either given, A. an archive-style URI, or B. the (globally unique integer) resource id
                        final Resource aResource;
                        try {
                            aResource = resolveXnatObjectUri(resolvedValue, resolvedMatcher, Resource.class,
                                    Resource.uriToModelObject(), Resource.idToModelObject(userI));
                        } catch (CommandWrapperInputResolutionException e) {
                            throw new CommandWrapperInputResolutionException(e.getMessage(), externalInput);
                        }

                        if (aResource != null) {
                            if (log.isDebugEnabled()) {
                                log.debug("Setting resolvedValue to uri " + aResource.getUri());
                            }
                            resolvedValue = aResource.getUri();
                            try {
                                jsonRepresentation = mapper.writeValueAsString(aResource);
                            } catch (JsonProcessingException e) {
                                String message = "Could not serialize resource";
                                if (log.isDebugEnabled()) {
                                    message += ": " + aResource;
                                } else {
                                    message += ".";
                                }
                                log.error(message, e);
                            }
                        }
                    } else if (type.equals(CONFIG.getName())) {
                        final String[] configProps = resolvedValue != null ? resolvedValue.split("/") : null;
                        if (configProps == null || configProps.length != 2) {
                            final String message = "Config inputs must have a value that can be interpreted as a config_toolname/config_filename string. Input value: " + resolvedValue;
                            log.debug(message);
                            throw new CommandWrapperInputResolutionException(message, externalInput);
                        }

                        final Scope configScope;
                        final String entityId;
                        // TODO Figure out how to resolve project config inputs vs sitewide
                        // final CommandInput.Type parentType = parent == null ? CommandInput.Type.STRING : parent.getType();
                        // switch (parentType) {
                        //     case PROJECT:
                        //         configScope = Scope.Project;
                        //         entityId = JsonPath.parse(getJsonValue(parent)).read("$.id");
                        //         break;
                        //     case SUBJECT:
                        //         // Intentional fallthrough
                        //     case SESSION:
                        //         // Intentional fallthrough
                        //     case SCAN:
                        //         // Intentional fallthrough
                        //     case ASSESSOR:
                        //         // TODO Is there any way to make this work? Can we find the project ID for these other input types?
                        //         //configScope = Scope.Project;
                        //         //final List<String> projectIds = JsonPath.parse(getJsonValue(parent)).read("$..projectId");
                        //         //entityId = (projectIds != null && !projectIds.isEmpty()) ? projectIds.get(0) : "";
                        //         //if (StringUtils.isBlank(entityId)) {
                        //         //    throw new CommandInputResolutionException("Could not determine project when resolving config value.", input);
                        //         //}
                        //         //break;
                        //         throw new XnatCommandInputResolutionException("Config inputs may only have parents of type Project.", input);
                        //     default:
                        //         configScope = Scope.Site;
                        //         entityId = null;
                        // }
                        //
                        // if (log.isDebugEnabled()) {
                        //     log.debug(String.format("Attempting to read config %s/%s from %s.", configProps[0], configProps[1],
                        //             configScope.equals(Scope.Site) ? "site" : "project " + entityId));
                        // }
                        // final String configContents = configService.getConfigContents(configProps[0], configProps[1], configScope, entityId);
                        // if (configContents == null) {
                        //     throw new XnatCommandInputResolutionException("Could not read config " + resolvedValue, input);
                        // }
                        //
                        // if (log.isDebugEnabled()) {
                        //     log.debug("Setting resolvedValue to config contents " + configContents);
                        // }
                        // resolvedValue = configContents;
                    } else {
                        log.debug("Nothing to do for simple types.");
                    }
                }

                // If resolved value is null, and input is required, that is an error
                if (resolvedValue == null && externalInput.required()) {
                    final String message = String.format("No value could be resolved for required input \"%s\".", externalInput.name());
                    log.debug(message);
                    throw new CommandWrapperInputResolutionException(message, externalInput);
                }
                if (log.isInfoEnabled()) {
                    log.info(String.format("Done resolving input \"%s\". Value: \"%s\".", externalInput.name(), resolvedValue));
                }
                setValue(externalInput, resolvedValue);;
                setJsonValue(externalInput, jsonRepresentation != null ? jsonRepresentation : resolvedValue);;

                resolvedWrapperInputObjects.put(externalInput.name(), externalInput);

                resolvedXnatWrapperInputValuesByName.put(externalInput.name(), getValue(externalInput));

                // If this xnat input provides any command input values, set them now
                final String commandInputName = externalInput.providesValueForCommandInput();
                if (StringUtils.isNotBlank(commandInputName)) {
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("Found value for command input \"%s\": \"%s\".",
                                commandInputName, getValue(externalInput)));
                    }
                    resolvedXnatInputValuesByCommandInputName.put(commandInputName, getValue(externalInput));
                }

                // If this xnat input provides files to a mount, note that now
                addInputToMountsList(externalInput);

                final String replacementKey = externalInput.replacementKey();
                if (StringUtils.isBlank(replacementKey)) {
                    continue;
                }
                resolvedInputValuesByReplacementKey.put(replacementKey, resolvedValue);
                // resolvedInputCommandLineValuesByReplacementKey.put(replacementKey, getValueForCommandLine(externalInput, resolvedValue));
            }
            log.info("Done resolving external xnat wrapper inputs.");

            if (hasDerivedInputs) {
                log.info("Resolving derived xnat wrapper inputs.");

                for (final CommandWrapperDerivedInput derivedInput : commandWrapper.derivedInputs()) {
                    log.info(String.format("Resolving input \"%s\".", derivedInput.name()));

                    if (StringUtils.isBlank(derivedInput.derivedFromXnatInput())) {
                        // TODO this should be caught during validation
                        final String message = String.format(
                                "Input \"%s\" is a derived input, but does not indicate the input from which it is to be derived.",
                                derivedInput.name()
                        );
                        log.error(message);
                        throw new CommandWrapperInputResolutionException(message, derivedInput);
                    }

                    final String prereq = derivedInput.derivedFromXnatInput();
                    if (!resolvedWrapperInputObjects.containsKey(prereq)) {
                        // TODO this should be caught during validation. If prereq exists, but is in the wrong order, re-order inputs. If not, then error.
                        final String message = String.format(
                                "Input \"%1$s\" is derived from input \"%2$s\" which has not been resolved. Re-order the derived inputs so \"%1$s\" appears after \"%2$s\".",
                                derivedInput.name(), prereq
                        );
                        log.error(message);
                        throw new CommandWrapperInputResolutionException(message, derivedInput);
                    }
                    final CommandWrapperInput parentInput = resolvedWrapperInputObjects.get(prereq);

                    String resolvedValue = null;
                    String jsonRepresentation = null;

                    // Give the input its default value
                    if (log.isDebugEnabled()) {
                        log.debug("Default value: " + derivedInput.defaultValue());
                    }
                    if (derivedInput.defaultValue() != null) {
                        resolvedValue = derivedInput.defaultValue();
                    }

                    // If a value was provided at runtime, use that over the default
                    // NOTE: I don't know if it is a good idea to allow "derived" inputs to check for outside values.
                    //       I feel like it would be more correct in a sense to force them to only get values that they derive from their parent.
                    //       But at the same time, I don't want to remove an escape hatch that I myself may want to use.
                    //       So this stays in for now. - JF
                    if (inputValues.containsKey(derivedInput.name()) && inputValues.get(derivedInput.name()) != null) {
                        if (log.isDebugEnabled()) {
                            log.debug("Runtime value: " + inputValues.get(derivedInput.name()));
                        }
                        resolvedValue = inputValues.get(derivedInput.name());
                    }

                    // Check for JSONPath substring in input value
                    resolvedValue = resolveJsonpathSubstring(resolvedValue);

                    // Resolve the matcher, if one was provided
                    if (log.isDebugEnabled()) {
                        log.debug("Matcher: " + derivedInput.matcher());
                    }
                    final String resolvedMatcher = derivedInput.matcher() != null ? resolveTemplate(derivedInput.matcher()) : null;

                    // Process the input based on its type
                    final String type = derivedInput.type();
                    if (log.isDebugEnabled()) {
                        log.debug("Processing input value as a " + type);
                    }
                    final String parentType = parentInput.type();
                    final String parentJson = getJsonValue(parentInput);

                    if (type.equals(STRING.getName())) {

                        final String propertyToGet = derivedInput.derivedFromXnatObjectProperty();

                        if (parentType.equals(PROJECT.getName()) || parentType.equals(SUBJECT.getName()) || parentType.equals(SESSION.getName()) ||
                                parentType.equals(SCAN.getName()) || parentType.equals(ASSESSOR.getName()) || parentType.equals(FILE.getName()) || parentType.equals(RESOURCE.getName())) {
                            final String jsonPathSearch = "$." + propertyToGet +
                                    (StringUtils.isNotBlank(resolvedMatcher) ? "[?(" + resolvedMatcher + ")]" : "");
                            if (log.isInfoEnabled()) {
                                log.info(String.format("Attempting to pull value from parent using matcher \"%s\".", jsonPathSearch));
                            }

                            try {
                                resolvedValue = JsonPath.parse(parentJson).read(jsonPathSearch, new TypeRef<String>() {
                                });
                            } catch (InvalidPathException | InvalidJsonException | MappingException e) {
                                String message = String.format("Error attempting to pull value using matcher \"%s\" from parent json", jsonPathSearch);
                                if (log.isDebugEnabled()) {
                                    message += ":\n" + parentJson;
                                } else {
                                    message += ".";
                                }
                                log.error(message, e);
                                throw new CommandWrapperInputResolutionException(message, derivedInput, e);
                            }
                        } else {
                            final String message = String.format("An input of type \"%s\" cannot be derived from an input of type \"%s\".",
                                    derivedInput.type(),
                                    parentInput.type());
                            log.error(message);
                            throw new CommandWrapperInputResolutionException(message, derivedInput);
                        }
                    } else if (type.equals(BOOLEAN.getName())) {
                        // TODO
                    } else if (type.equals(NUMBER.getName())) {
                        // TODO
                    } else if (type.equals(DIRECTORY.getName())) {
                        if (parentType.equals(RESOURCE.getName())) {

                            final String jsonPathSearch = "$.directory" +
                                    (StringUtils.isNotBlank(resolvedMatcher) ? "[?(" + resolvedMatcher + ")]" : "");
                            if (log.isInfoEnabled()) {
                                log.info(String.format("Attempting to pull value from parent using matcher \"%s\".", jsonPathSearch));
                            }

                            try {
                                resolvedValue = JsonPath.parse(parentJson).read(jsonPathSearch, new TypeRef<String>() {
                                });
                            } catch (InvalidPathException | InvalidJsonException | MappingException e) {
                                String message = String.format("Error attempting to pull value using matcher \"%s\" from parent json", jsonPathSearch);
                                if (log.isDebugEnabled()) {
                                    message += ":\n" + parentJson;
                                } else {
                                    message += ".";
                                }
                                log.error(message, e);
                                throw new CommandWrapperInputResolutionException(message, derivedInput, e);
                            }
                        // TODO
                        // } else if (parentType.equals(PROJECT.getName()) || parentType.equals(SUBJECT.getName()) || parentType.equals(SESSION.getName()) ||
                        //         parentType.equals(SCAN.getName()) || parentType.equals(ASSESSOR.getName())) {
                        } else {
                            final String message = String.format("An input of type \"%s\" cannot be derived from an input of type \"%s\".",
                                    derivedInput.type(),
                                    parentInput.type());
                            log.error(message);
                            throw new CommandWrapperInputResolutionException(message, derivedInput);
                        }
                    } else if (type.equals(FILES.getName())) {
                        List<XnatFile> files;

                        if (parentType.equals(RESOURCE.getName())) {
                            files = matchChildFromParent(parentJson,
                                    resolvedValue, "files", "name", resolvedMatcher, new TypeRef<List<XnatFile>>() {
                                    });
                        } else {
                            final String message = String.format("An input of type \"%s\" cannot be derived from an input of type \"%s\".",
                                    derivedInput.type(),
                                    parentInput.type());
                            log.error(message);
                            throw new CommandWrapperInputResolutionException(message, derivedInput);
                        }

                        if (files == null) {
                            final String message = String.format("Could not derive \"%s\" from \"%s\".", derivedInput.name(), parentInput.name());
                            log.error(message);
                            throw new CommandWrapperInputResolutionException(message, derivedInput);
                        }

                        if (log.isDebugEnabled()) {
                            log.debug("Setting resolvedValue to list of file json objects " + files);
                        }

                        try {
                            jsonRepresentation = mapper.writeValueAsString(files);
                            resolvedValue = jsonRepresentation;
                        } catch (JsonProcessingException e) {
                            log.error("Could not serialize file to json.", e);
                        }
                    } else if (type.equals(FILE.getName())) {
                        XnatFile file = null;

                        if (parentType.equals(RESOURCE.getName())) {
                            final List<XnatFile> childList = matchChildFromParent(parentJson,
                                    resolvedValue, "files", "name", resolvedMatcher, new TypeRef<List<XnatFile>>() {
                                    });
                            if (childList != null && !childList.isEmpty()) {
                                if (log.isDebugEnabled()) {
                                    log.debug("Selecting first matching result from list.");
                                }
                                file = childList.get(0);
                            }
                        } else {
                            final String message = String.format("An input of type \"%s\" cannot be derived from an input of type \"%s\".",
                                    derivedInput.type(),
                                    parentInput.type());
                            log.error(message);
                            throw new CommandWrapperInputResolutionException(message, derivedInput);
                        }

                        if (file == null) {
                            final String message = String.format("Could not derive \"%s\" from \"%s\".", derivedInput.name(), parentInput.name());
                            log.error(message);
                            throw new CommandWrapperInputResolutionException(message, derivedInput);
                        }

                        if (log.isDebugEnabled()) {
                            log.debug("Setting resolvedValue to uri " + file.getUri());
                        }
                        resolvedValue = file.getUri();
                        try {
                            jsonRepresentation = mapper.writeValueAsString(file);
                        } catch (JsonProcessingException e) {
                            log.error("Could not serialize file to json.", e);
                        }
                    } else if (type.equals(PROJECT.getName())) {
                        Project project;
                        try {
                            if (parentType.equals(SUBJECT.getName())) {
                                final Subject subject = mapper.readValue(parentJson, Subject.class);
                                project = subject.getProject(userI);
                            } else if (parentType.equals(SESSION.getName())) {
                                final Session session = mapper.readValue(parentJson, Session.class);
                                project = session.getProject(userI);
                            } else if (parentType.equals(SCAN.getName())) {
                                final Scan scan = mapper.readValue(parentJson, Scan.class);
                                project = scan.getProject(userI);
                            } else if (parentType.equals(ASSESSOR.getName())) {
                                final Assessor assessor = mapper.readValue(parentJson, Assessor.class);
                                project = assessor.getProject(userI);
                            } else {
                                final String message = String.format("An input of type \"%s\" cannot be derived from an input of type \"%s\".",
                                        derivedInput.type(),
                                        parentInput.type());
                                log.error(message);
                                throw new CommandWrapperInputResolutionException(message, derivedInput);
                            }
                        } catch (IOException e) {
                            final String message = String.format("Could not derive \"%s\" from \"%s\".", derivedInput.name(), parentInput.name());
                            log.error(message);
                            throw new CommandWrapperInputResolutionException(message, derivedInput, e);
                        }

                        if (project == null) {
                            final String message = String.format("Could not derive \"%s\" from \"%s\".", derivedInput.name(), parentInput.name());
                            log.error(message);
                            throw new CommandWrapperInputResolutionException(message, derivedInput);
                        }

                        if (log.isDebugEnabled()) {
                            log.debug("Setting resolvedValue to uri " + project.getUri());
                        }
                        resolvedValue = project.getUri();
                        try {
                            jsonRepresentation = mapper.writeValueAsString(project);
                        } catch (JsonProcessingException e) {
                            log.error("Could not serialize project to json.", e);
                        }
                    } else if (type.equals(SUBJECT.getName())) {
                        Subject subject = null;

                        if (parentType.equals(PROJECT.getName())) {
                            final List<Subject> childList = matchChildFromParent(parentJson,
                                    resolvedValue, "subjects", "id", resolvedMatcher, new TypeRef<List<Subject>>() {
                                    });
                            if (childList != null && !childList.isEmpty()) {
                                if (log.isDebugEnabled()) {
                                    log.debug("Selecting first matching result from list " + childList);
                                }
                                subject = childList.get(0);
                            }
                        } else if (parentType.equals(SESSION.getName())) {
                            try {
                                final Session session = mapper.readValue(parentJson, Session.class);
                                subject = session.getSubject(userI);
                            } catch (IOException e) {
                                final String message = String.format("Could not derive \"%s\" from \"%s\".", derivedInput.name(), parentInput.name());
                                log.error(message);
                                throw new CommandWrapperInputResolutionException(message, derivedInput, e);
                            }
                        } else {
                            final String message = String.format("An input of type \"%s\" cannot be derived from an input of type \"%s\".",
                                    derivedInput.type(),
                                    parentInput.type());
                            log.error(message);
                            throw new CommandWrapperInputResolutionException(message, derivedInput);
                        }

                        if (subject == null) {
                            final String message = String.format("Could not derive \"%s\" from \"%s\".", derivedInput.name(), parentInput.name());
                            log.error(message);
                            throw new CommandWrapperInputResolutionException(message, derivedInput);
                        }

                        if (log.isDebugEnabled()) {
                            log.debug("Setting resolvedValue to uri " + subject.getUri());
                        }
                        resolvedValue = subject.getUri();
                        try {
                            jsonRepresentation = mapper.writeValueAsString(subject);
                        } catch (JsonProcessingException e) {
                            log.error("Could not serialize subject to json.", e);
                        }
                    } else if (type.equals(SESSION.getName())) {
                        Session session = null;

                        if (parentType.equals(SUBJECT.getName())) {
                            final List<Session> childList = matchChildFromParent(parentJson,
                                    resolvedValue, "sessions", "id", resolvedMatcher, new TypeRef<List<Session>>() {
                                    });
                            if (childList != null && !childList.isEmpty()) {
                                if (log.isDebugEnabled()) {
                                    log.debug("Selecting first matching result from list " + childList);
                                }
                                session = childList.get(0);
                            }
                        } else if (parentType.equals(SCAN.getName())) {
                            try {
                                final Scan scan = mapper.readValue(parentJson, Scan.class);
                                session = scan.getSession(userI);
                            } catch (IOException e) {
                                final String message = String.format("Could not derive \"%s\" from \"%s\".", derivedInput.name(), parentInput.name());
                                log.error(message);
                                throw new CommandWrapperInputResolutionException(message, derivedInput, e);
                            }

                        } else {
                            final String message = String.format("An input of type \"%s\" cannot be derived from an input of type \"%s\".",
                                    derivedInput.type(),
                                    parentInput.type());
                            log.error(message);
                            throw new CommandWrapperInputResolutionException(message, derivedInput);
                        }

                        if (session == null) {
                            final String message = String.format("Could not derive \"%s\" from \"%s\".", derivedInput.name(), parentInput.name());
                            log.error(message);
                            throw new CommandWrapperInputResolutionException(message, derivedInput);
                        }

                        if (log.isDebugEnabled()) {
                            log.debug("Setting resolvedValue to uri " + session.getUri());
                        }
                        resolvedValue = session.getUri();
                        try {
                            jsonRepresentation = mapper.writeValueAsString(session);
                        } catch (JsonProcessingException e) {
                            log.error("Could not serialize session to json.", e);
                        }
                    } else if (type.equals(SCAN.getName())) {
                        Scan scan = null;

                        if (parentType.equals(SESSION.getName())) {
                            final List<Scan> childList = matchChildFromParent(parentJson,
                                    resolvedValue, "scans", "id", resolvedMatcher, new TypeRef<List<Scan>>() {
                                    });
                            if (childList != null && !childList.isEmpty()) {
                                if (log.isDebugEnabled()) {
                                    log.debug("Selecting first matching result from list.");
                                }
                                scan = childList.get(0);
                            }
                        } else {
                            final String message = String.format("An input of type \"%s\" cannot be derived from an input of type \"%s\".",
                                    derivedInput.type(),
                                    parentInput.type());
                            log.error(message);
                            throw new CommandWrapperInputResolutionException(message, derivedInput);
                        }

                        if (scan == null) {
                            final String message = String.format("Could not derive \"%s\" from \"%s\".", derivedInput.name(), parentInput.name());
                            log.error(message);
                            throw new CommandWrapperInputResolutionException(message, derivedInput);
                        }

                        if (log.isDebugEnabled()) {
                            log.debug("Setting resolvedValue to uri " + scan.getUri());
                        }
                        resolvedValue = scan.getUri();
                        try {
                            jsonRepresentation = mapper.writeValueAsString(scan);
                        } catch (JsonProcessingException e) {
                            log.error("Could not serialize scan to json.", e);
                        }
                    } else if (type.equals(ASSESSOR.getName())) {
                        Assessor assessor = null;

                        if (parentType.equals(SESSION.getName())) {
                            final List<Assessor> childList = matchChildFromParent(parentJson,
                                    resolvedValue, "assessors", "id", resolvedMatcher, new TypeRef<List<Assessor>>() {
                                    });
                            if (childList != null && !childList.isEmpty()) {
                                if (log.isDebugEnabled()) {
                                    log.debug("Selecting first matching result from list " + childList);
                                }
                                assessor = childList.get(0);
                            }
                        } else {
                            final String message = String.format("An input of type \"%s\" cannot be derived from an input of type \"%s\".",
                                    derivedInput.type(),
                                    parentInput.type());
                            log.error(message);
                            throw new CommandWrapperInputResolutionException(message, derivedInput);
                        }

                        if (assessor == null) {
                            final String message = String.format("Could not derive \"%s\" from \"%s\".", derivedInput.name(), parentInput.name());
                            log.error(message);
                            throw new CommandWrapperInputResolutionException(message, derivedInput);
                        }

                        if (log.isDebugEnabled()) {
                            log.debug("Setting resolvedValue to uri " + assessor.getUri());
                        }
                        resolvedValue = assessor.getUri();
                        try {
                            jsonRepresentation = mapper.writeValueAsString(assessor);
                        } catch (JsonProcessingException e) {
                            log.error("Could not serialize assessor to json.", e);
                        }
                    } else if (type.equals(RESOURCE.getName())) {
                        Resource resource = null;

                        if (parentType.equals(PROJECT.getName()) || parentType.equals(SUBJECT.getName()) || parentType.equals(SESSION.getName()) ||
                                parentType.equals(SCAN.getName()) || parentType.equals(ASSESSOR.getName())) {

                            final List<Resource> childStringList = matchChildFromParent(parentJson,
                                    resolvedValue, "resources", "id", resolvedMatcher, new TypeRef<List<Resource>>() {
                                    });
                            if (childStringList != null && !childStringList.isEmpty()) {
                                log.debug("Selecting first matching result from list.");
                                resource = childStringList.get(0);
                            }
                        } else {
                            final String message = String.format("An input of type \"%s\" cannot be derived from an input of type \"%s\".",
                                    derivedInput.type(),
                                    parentInput.type());
                            log.error(message);
                            throw new CommandWrapperInputResolutionException(message, derivedInput);
                        }

                        if (resource == null) {
                            final String message = String.format("Could not derive \"%s\" from \"%s\".", derivedInput.name(), parentInput.name());
                            log.error(message);
                            throw new CommandWrapperInputResolutionException(message, derivedInput);
                        }

                        if (log.isDebugEnabled()) {
                            log.debug("Setting resolvedValue to uri " + resource.getUri());
                        }
                        resolvedValue = resource.getUri();
                        try {
                            jsonRepresentation = mapper.writeValueAsString(resource);
                        } catch (JsonProcessingException e) {
                            log.error("Could not serialize resource to json.", e);
                        }
                    } else if (type.equals(CONFIG.getName())) {
                        // TODO
                    }

                    // If resolved value is null, and input is required, that is an error
                    if (resolvedValue == null && derivedInput.required()) {
                        final String message = String.format("No value could be resolved for required input \"%s\".", derivedInput.name());
                        log.debug(message);
                        throw new CommandWrapperInputResolutionException(message, derivedInput);
                    }
                    if (log.isInfoEnabled()) {
                        log.info(String.format("Done resolving input \"%s\". Value: \"%s\".", derivedInput.name(), resolvedValue));
                    }
                    setValue(derivedInput, resolvedValue);;
                    setJsonValue(derivedInput, jsonRepresentation != null ? jsonRepresentation : resolvedValue);;

                    resolvedWrapperInputObjects.put(derivedInput.name(), derivedInput);

                    resolvedXnatWrapperInputValuesByName.put(derivedInput.name(), getValue(derivedInput));

                    // If this xnat input provides any command input values, set them now
                    final String commandInputName = derivedInput.providesValueForCommandInput();
                    if (StringUtils.isNotBlank(commandInputName)) {
                        if (log.isDebugEnabled()) {
                            log.debug(String.format("Found value for command input \"%s\": \"%s\".",
                                    commandInputName, getValue(derivedInput)));
                        }
                        resolvedXnatInputValuesByCommandInputName.put(commandInputName, getValue(derivedInput));
                    }

                    // If this xnat input provides files to a mount, note that now
                    addInputToMountsList(derivedInput);

                    final String replacementKey = derivedInput.replacementKey();
                    if (StringUtils.isBlank(replacementKey)) {
                        continue;
                    }
                    resolvedInputValuesByReplacementKey.put(replacementKey, resolvedValue);
                    // resolvedInputCommandLineValuesByReplacementKey.put(replacementKey, getValueForCommandLine(derivedInput, resolvedValue));
                }

                log.info("Done resolving derived xnat wrapper inputs.");
            }

            log.info("Done resolving xnat wrapper inputs.");
            return resolvedXnatWrapperInputValuesByName;
        }

        private void addInputToMountsList(final @Nonnull CommandWrapperInput input) {
            final String mountName = input.providesFilesForCommandMount();
            if (StringUtils.isNotBlank(mountName)) {
                List<CommandWrapperInput> xnatInputs = commandMountsToReceiveFilesFromXnatInputs.get(mountName);
                if (xnatInputs == null) {
                    xnatInputs = Lists.newArrayList();
                }
                xnatInputs.add(input);
                commandMountsToReceiveFilesFromXnatInputs.put(mountName, xnatInputs);
            }
        }

        @Nonnull
        private Map<String, String> resolveInputs() throws CommandResolutionException {
            log.info("Resolving command inputs.");

            final Map<String, String> resolvedInputValuesByName = Maps.newHashMap();

            if (command.inputs() == null || command.inputs().isEmpty()) {
                log.info("No inputs.");
                return resolvedInputValuesByName;
            }

            for (final CommandInput commandInput : command.inputs()) {
                log.info(String.format("Resolving command input \"%s\".", commandInput.name()));

                // // Check that all prerequisites have already been resolved.
                // // TODO Move this to a command validation function. Command should not be saved unless inputs are in correct order. At this stage, we should be able to safely iterate.
                // final List<String> prerequisites = StringUtils.isNotBlank(input.getPrerequisites()) ?
                //         Lists.newArrayList(input.getPrerequisites().split("\\s*,\\s*")) :
                //         Lists.<String>newArrayList();
                // if (StringUtils.isNotBlank(input.getParent()) && !prerequisites.contains(input.getParent())) {
                //     // Parent is always a prerequisite
                //     prerequisites.add(input.getParent());
                // }
                //
                // if (log.isDebugEnabled()) {
                //     log.debug("Prerequisites: " + prerequisites.toString());
                // }
                // for (final String prereq : prerequisites) {
                //     if (!resolvedXnatInputObjects.containsKey(prereq)) {
                //         final String message = String.format(
                //                 "Input \"%1$s\" has prerequisite \"%2$s\" which has not been resolved. Re-order the command inputs so \"%1$s\" appears after \"%2$s\".",
                //                 input.getName(), prereq
                //         );
                //         log.error(message);
                //         throw new CommandInputResolutionException(message, input);
                //     }
                // }

                // // If input requires a parent, it must be resolved first
                // CommandInput parent = null;
                // if (StringUtils.isNotBlank(input.getParent())) {
                //     if (resolvedXnatInputObjects.containsKey(input.getParent())) {
                //         // Parent has already been resolved. We can continue.
                //         parent = resolvedXnatInputObjects.get(input.getParent());
                //     } else {
                //         // This exception should have been thrown already above, but just in case it wasn't...
                //         final String message = String.format(
                //                 "Input %1$s has prerequisite %2$s which has not been resolved. Re-order inputs so %1$s appears after %2$s.",
                //                 input.getName(), input.getParent()
                //         );
                //         log.error(message);
                //         throw new CommandInputResolutionException(message, input);
                //     }
                // }

                String resolvedValue = null;

                // Give the input its default value
                if (log.isDebugEnabled()) {
                    log.debug("Default value: " + commandInput.defaultValue());
                }
                if (commandInput.defaultValue() != null) {
                     resolvedValue = commandInput.defaultValue();
                }

                // If the input is supposed to get a value from an XNAT input, use that
                final String preresolvedValue = resolvedXnatInputValuesByCommandInputName.get(commandInput.name());
                if (preresolvedValue != null) {
                    if (log.isDebugEnabled()) {
                        log.debug("XNAT Wrapper value: " + preresolvedValue);
                    }
                    resolvedValue = preresolvedValue;
                }

                // If a value was provided at runtime, use that
                final String runtimeValue = inputValues.get(commandInput.name());
                if (runtimeValue != null) {
                    if (log.isDebugEnabled()) {
                        log.debug("Runtime value: " + runtimeValue);
                    }
                    resolvedValue = runtimeValue;
                }

                // Check for JSONPath substring in input value
                resolvedValue = resolveJsonpathSubstring(resolvedValue);

                if (log.isDebugEnabled()) {
                    log.debug("Matcher: " + commandInput.matcher());
                }
                final String resolvedMatcher = commandInput.matcher() != null ? resolveTemplate(commandInput.matcher()) : null;

                final String type = commandInput.type();
                if (log.isDebugEnabled()) {
                    log.debug("Processing input value as a " + type);
                }
                if (type.equals(BOOLEAN.getName())) {
                    // Parse the value as a boolean, and use the trueValue/falseValue
                    // If those haven't been set, just pass the value through
                    if (Boolean.parseBoolean(resolvedValue)) {
                        resolvedValue = commandInput.trueValue() != null ? commandInput.trueValue() : resolvedValue;
                    } else {
                        resolvedValue = commandInput.falseValue() != null ? commandInput.falseValue() : resolvedValue;
                    }
                } else if (type.equals(NUMBER.getName())) {
                    // TODO
                } else {
                    // TODO anything to do?
                }

                // If resolved value is null, and input is required, that is an error
                if (resolvedValue == null && commandInput.required()) {
                    final String message = String.format("No value could be resolved for required input \"%s\".", commandInput.name());
                    log.debug(message);
                    throw new CommandInputResolutionException(message, commandInput);
                }
                if (log.isInfoEnabled()) {
                    log.info(String.format("Done resolving input \"%s\". Value: %s", commandInput.name(), resolvedValue));
                }
                setValue(commandInput, resolvedValue);;
                // setJsonValue(input, jsonRepresentation != null ? jsonRepresentation : resolvedValue);;

                // resolvedXnatInputObjects.put(input.getName(), input);
                if (resolvedValue != null) {
                    // Only store the value as "resolved" if it is non-null
                    resolvedInputValuesByName.put(commandInput.name(), getValue(commandInput));
                }

                // Only substitute the input into the command line if a replacementKey is set
                final String replacementKey = commandInput.replacementKey();
                if (StringUtils.isBlank(replacementKey)) {
                    continue;
                }
                resolvedInputValuesByReplacementKey.put(replacementKey, resolvedValue);
                resolvedInputCommandLineValuesByReplacementKey.put(replacementKey, getValueForCommandLine(commandInput, resolvedValue));
            }

            return resolvedInputValuesByName;
        }

        private <T extends XnatModelObject> List<T> matchChildFromParent(final String parentJson, final String value, final String childKey, final String valueMatchProperty, final String matcherFromInput, final TypeRef<List<T>> typeRef) {
            final String matcherFromValue = StringUtils.isNotBlank(value) ?
                    String.format("@.%s == '%s'", valueMatchProperty, value) :
                    "";
            final boolean hasValueMatcher = StringUtils.isNotBlank(matcherFromValue);
            final boolean hasInputMatcher = StringUtils.isNotBlank(matcherFromInput);
            final String fullMatcher;
            if (hasValueMatcher && hasInputMatcher) {
                fullMatcher = matcherFromValue + " && " + matcherFromInput;
            } else if (hasValueMatcher) {
                fullMatcher = matcherFromValue;
            } else if (hasInputMatcher) {
                fullMatcher = matcherFromInput;
            } else {
                fullMatcher = "";
            }

            final String jsonPathSearch = String.format(
                    "$.%s[%s]",
                    childKey,
                    StringUtils.isNotBlank(fullMatcher) ? "?(" + fullMatcher + ")" : "*"
            );
            if (log.isInfoEnabled()) {
                log.info(String.format("Attempting to pull value from parent using matcher \"%s\".", jsonPathSearch));
            }

            try {
                return JsonPath.parse(parentJson).read(jsonPathSearch, typeRef);
            } catch (InvalidPathException | InvalidJsonException | MappingException e) {
                String message = String.format("Error attempting to pull value using matcher \"%s\" from parent json", jsonPathSearch);
                if (log.isDebugEnabled()) {
                    message += ":\n" + parentJson;
                } else {
                    message += ".";
                }
                log.error(message, e);
            }
            return null;
        }

        private String getValueForCommandLine(final CommandInput input, final String resolvedInputValue) {
            if (StringUtils.isBlank(input.commandLineFlag())) {
                return resolvedInputValue;
            } else {
                return input.commandLineFlag() +
                        (input.commandLineSeparator() == null ? " " : input.commandLineSeparator()) +
                        resolvedInputValue;
            }
        }

        private <T extends XnatModelObject> T resolveXnatObjectUri(final String value,
                                                                   final String matcher,
                                                                   final Class<T> model,
                                                                   final Function<ArchiveItemURI, T> uriToModelObjectFunction,
                                                                   final Function<String, T> stringToModelObjectFunction)
                throws CommandWrapperInputResolutionException {
            final String modelName = model.getSimpleName();
            log.info("Resolving " + modelName + " from value.");
            if (log.isDebugEnabled()) {
                log.debug("Value: " + value);
            }

            if (StringUtils.isBlank(value)) {
                final String message = "Not attempting to resolve blank value.";
                log.debug(message);
                throw new CommandWrapperInputResolutionException(message, null);
            }

    //        List<T> mayOrMayNotMatch = Lists.newArrayList();
            T newModelObject = null;
            if (value.startsWith("/")) {
                if (log.isInfoEnabled()) {
                    log.info(String.format("Attempting to create a \"%s\" using value as URI.", modelName));
                }
                if (uriToModelObjectFunction == null) {
                    throw new CommandWrapperInputResolutionException("ERROR: Cannot instantiate " + modelName + " without a function.", null);
                }

                URIManager.DataURIA uri;
                try {
                    uri = UriParserUtils.parseURI(value.startsWith("/archive") ? value : "/archive" + value);
                } catch (MalformedURLException e) {
                    throw new CommandWrapperInputResolutionException(String.format("Cannot interpret value as a URI: %s.", value), null, e);
                }

                if (uri == null || !(uri instanceof ArchiveItemURI)) {
                    throw new CommandWrapperInputResolutionException(String.format("Cannot interpret value as a URI: %s.", value), null);
                }

                newModelObject = uriToModelObjectFunction.apply((ArchiveItemURI) uri);

            } else if (value.startsWith("{")) {
                try {
                    if (log.isInfoEnabled()) {
                        log.info(String.format("Attempting to deserialize type \"%s\" from value", modelName));
                    }
                    newModelObject = mapper.readValue(value, model);
                } catch (IOException e) {
                    log.error(String.format("Could not deserialize value into type \"%s\".", modelName), e);
                }
            } else if (stringToModelObjectFunction != null) {
                if (log.isInfoEnabled()) {
                    log.info(String.format("Attempting to create a \"%s\" using value in function %s.", modelName, stringToModelObjectFunction));
                }
                newModelObject = stringToModelObjectFunction.apply(value);
            }

            if (newModelObject == null) {
                throw new CommandWrapperInputResolutionException("Could not instantiate " + modelName + " from value.", null);
            }

            String mayOrMayNotMatchJson;
            try {
                mayOrMayNotMatchJson = mapper.writeValueAsString(newModelObject);
            } catch (JsonProcessingException e) {
                throw new CommandWrapperInputResolutionException(String.format("Could not serialize object to JSON: %s", newModelObject), null, e);
            }

            if (StringUtils.isBlank(mayOrMayNotMatchJson)) {
                throw new CommandWrapperInputResolutionException(String.format("Could not serialize object to JSON: %s", newModelObject), null);
            }

            final T aMatch;
            if (StringUtils.isNotBlank(matcher)) {
                final List<T> doMatch;
                final String jsonPathSearch = String.format(
                        "$[?(%s)]", matcher
                );

                if (log.isInfoEnabled()) {
                    log.info(String.format("Using JSONPath matcher \"%s\" to search for matching items.", jsonPathSearch));
                }
                doMatch = JsonPath.parse(mayOrMayNotMatchJson).read(jsonPathSearch, new TypeRef<List<T>>(){});

                if (doMatch == null || doMatch.isEmpty()) {
                    throw new CommandWrapperInputResolutionException(String.format("Could not match any \"%s\" with matcher \"%s\".", modelName, matcher), null);
                }

                aMatch = doMatch.get(0);
            } else {
                aMatch = newModelObject;
            }

            log.info(String.format("Successfully instantiated matching %s.", modelName));
            if (log.isDebugEnabled()) {
                log.debug("Match: " + aMatch);
            }
            return aMatch;
        }

        @Nonnull
        private List<ResolvedCommandOutput> resolveOutputs() throws CommandResolutionException {
            log.info("Resolving command outputs.");
            final List<ResolvedCommandOutput> resolvedOutputs = Lists.newArrayList();
            if (command.outputs() == null) {
                return resolvedOutputs;
            }

            final Map<String, CommandWrapperOutput> xnatCommandOutputsByCommandOutputName = Maps.newHashMap();
            if (commandWrapper.outputHandlers() != null) {
                for (final CommandWrapperOutput commandWrapperOutput : commandWrapper.outputHandlers()) {
                    xnatCommandOutputsByCommandOutputName.put(commandWrapperOutput.commandOutputName(), commandWrapperOutput);
                }
            }

            for (final CommandOutput commandOutput : command.outputs()) {
                if (log.isInfoEnabled()) {
                    log.info(String.format("Resolving command output \"%s\"", commandOutput.name()));
                }
                if (log.isDebugEnabled()) {
                    log.debug(commandOutput.toString());
                }

                // TODO fix this in validation
                final CommandWrapperOutput commandOutputHandler = xnatCommandOutputsByCommandOutputName.get(commandOutput.name());
                if (commandOutputHandler == null) {
                    throw new CommandResolutionException(String.format("No XNAT object was configured to handle output \"%s\".", commandOutput.name()));
                }
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Found XNAT Output Handler for Command output \"%s\".", commandOutput.name()));
                }

                final ResolvedCommandOutput resolvedOutput = ResolvedCommandOutput.builder()
                        .name(commandOutput.name())
                        .required(commandOutput.required())
                        .mount(commandOutput.mount())
                        .glob(commandOutput.glob())
                        .type(commandOutputHandler.type())
                        .handledByXnatCommandInput(commandOutputHandler.xnatInputName())
                        .path(resolveTemplate(commandOutput.path()))
                        .label(resolveTemplate(commandOutputHandler.label()))
                        .build();

                // TODO Anything else needed to resolve an output?

                if (log.isDebugEnabled()) {
                    log.debug(String.format("Adding resolved output \"%s\" to resolved command.", resolvedOutput.name()));
                }

                resolvedOutputs.add(resolvedOutput);
            }

            log.info("Done resolving command outputs.");
            if (log.isDebugEnabled()) {
                String message = "Outputs: ";
                if (resolvedOutputs.size() >= 2) {
                    message += "\n";
                }
                message += resolvedOutputs;
                log.debug(message);
            }
            return resolvedOutputs;
        }

        @Nonnull
        private String resolveCommandLine() throws CommandResolutionException {
            log.info("Resolving command-line string.");

            final String resolvedCommandLine = resolveTemplate(command.commandLine(), resolvedInputCommandLineValuesByReplacementKey);

            log.info("Done resolving command-line string.");
            if (log.isDebugEnabled()) {
                log.debug("Command-line string: " + resolvedCommandLine);
            }
            return resolvedCommandLine;
        }

        @Nonnull
        private Map<String, String> resolveEnvironmentVariables()
                throws CommandResolutionException {
            log.info("Resolving environment variables.");

            final Map<String, String> resolvedMap = Maps.newHashMap();
            final Map<String, String> envTemplates = command.environmentVariables();
            if (envTemplates == null || envTemplates.isEmpty()) {
                log.info("No environment variables to resolve.");
                return resolvedMap;
            }

            resolvedMap.putAll(resolveTemplateMap(envTemplates));

            log.info("Done resolving environment variables.");
            if (log.isDebugEnabled()) {
                log.debug(mapDebugString("Environment variables: ", resolvedMap));
            }
            return resolvedMap;
        }

        @Nonnull
        private Map<String, String> resolvePorts()
                throws CommandResolutionException {
            log.info("Resolving ports.");

            final Map<String, String> resolvedMap = Maps.newHashMap();
            final Map<String, String> portTemplates = command.ports();
            if (portTemplates == null || portTemplates.isEmpty()) {
                log.info("No ports to resolve.");
                return resolvedMap;
            }

            resolvedMap.putAll(resolveTemplateMap(portTemplates));

            log.info("Done resolving ports.");
            if (log.isDebugEnabled()) {
                log.debug(mapDebugString("Ports: ", resolvedMap));
            }
            return resolvedMap;
        }

        private String mapDebugString(final String title, final Map<String, String> map) {
            final StringBuilder messageBuilder = new StringBuilder(title);
            for (Map.Entry<String, String> entry : map.entrySet()) {
                messageBuilder.append(entry.getKey());
                messageBuilder.append(": ");
                messageBuilder.append(entry.getValue());
                messageBuilder.append(", ");
            }
            return messageBuilder.substring(0, messageBuilder.length() - 2);
        }

        @Nonnull
        private Map<String, String> resolveTemplateMap(final Map<String, String> templateMap) throws CommandResolutionException {
            final Map<String, String> resolvedMap = Maps.newHashMap();
            if (templateMap == null || templateMap.isEmpty()) {
                return resolvedMap;
            }
            for (final Map.Entry<String, String> templateEntry : templateMap.entrySet()) {
                final String resolvedKey = resolveTemplate(templateEntry.getKey());
                final String resolvedValue = resolveTemplate(templateEntry.getValue());
                resolvedMap.put(resolvedKey, resolvedValue);
                if (!templateEntry.getKey().equals(resolvedKey) || !templateEntry.getValue().equals(resolvedValue)) {
                    if (log.isDebugEnabled()) {
                        final String message = String.format("Map %s: %s -> %s: %s",
                                templateEntry.getKey(), templateEntry.getValue(),
                                resolvedKey, resolvedValue);
                        log.debug(message);
                    }
                }
            }
            return resolvedMap;
        }

        @Nonnull
        private List<ResolvedCommandMount> resolveCommandMounts() throws CommandResolutionException {
            log.info("Resolving mounts.");
            final List<CommandMount> commandMounts = command.mounts();
            if (commandMounts == null || commandMounts.isEmpty()) {
                log.info("No mounts.");
                return Lists.newArrayList();
            }

            final List<ResolvedCommandMount> resolvedMounts = Lists.newArrayList();
            for (final CommandMount commandMount : commandMounts) {
                resolvedMounts.add(resolveCommandMount(commandMount));
            }

            log.info("Done resolving mounts.");
            if (log.isDebugEnabled()) {
                for (final ResolvedCommandMount mount : resolvedMounts) {
                    log.debug(mount.toString());
                }
            }
            return resolvedMounts;
        }

        @Nonnull
        private ResolvedCommandMount resolveCommandMount(final CommandMount commandMount)
                throws CommandResolutionException {
            if (log.isInfoEnabled()) {
                log.info(String.format("Resolving command mount \"%s\".", commandMount.name()));
            }

            final PartiallyResolvedCommandMount.Builder partiallyResolvedMountBuilder = PartiallyResolvedCommandMount.builder()
                    .name(commandMount.name())
                    .writable(commandMount.writable())
                    .containerPath(resolveTemplate(commandMount.path()));

            final List<CommandWrapperInput> sourceInputs = commandMountsToReceiveFilesFromXnatInputs.get(commandMount.name());
            if (sourceInputs == null || sourceInputs.isEmpty()) {
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Command mount \"%s\" has no inputs that provide it files. Assuming it is an output mount.", commandMount.name()));
                }
                partiallyResolvedMountBuilder.writable(true);
            } else {
                for (final CommandWrapperInput sourceInput : sourceInputs) {
                    if (sourceInput == null) {
                        final String message = String.format("Cannot resolve mount \"%s\". Source input is null.", commandMount.name());
                        log.error(message);
                        throw new CommandMountResolutionException(message, commandMount);
                    } else if (StringUtils.isBlank(getValue(sourceInput))) {
                        final String message = String.format("Cannot resolve mount \"%s\". Source input \"%s\" has no resolved value.", commandMount.name(), sourceInput.name());
                        log.error(message);
                        throw new CommandMountResolutionException(message, commandMount);
                    }

                    if (log.isDebugEnabled()) {
                        log.debug(String.format("Mount \"%s\" has source input \"%s\" with type \"%s\".", commandMount.name(), sourceInput.name(), sourceInput.type()));
                    }

                    String rootDirectory = null;
                    String uri = null;
                    final String type = sourceInput.type();
                    if (type.equals(DIRECTORY.getName())) {
                        // TODO
                    } else if (type.equals(FILES.getName())) {
                        // TODO
                    } else if (type.equals(FILE.getName())) {
                        // TODO
                    } else if (type.equals(PROJECT.getName()) || type.equals(SESSION.getName()) || type.equals(SCAN.getName())
                                || type.equals(ASSESSOR.getName()) || type.equals(RESOURCE.getName())) {
                        if (log.isDebugEnabled()) {
                            log.debug("Looking for directory on source input.");
                        }

                        rootDirectory = JsonPath.parse(getJsonValue(sourceInput)).read("directory", String.class);
                        uri = JsonPath.parse(getJsonValue(sourceInput)).read("uri", String.class);

                    } else {
                        final String message = String.format("I don't know how to provide files to a mount from an input of type \"%s\".", sourceInput.type());
                        log.error(message);
                        throw new CommandMountResolutionException(message, commandMount);
                    }

                    if (StringUtils.isBlank(rootDirectory)) {
                        String message = "Source input has no directory.";
                        if (log.isDebugEnabled()) {
                            message += "\ninput: " + sourceInput;
                        }
                        log.error(message);
                        throw new CommandMountResolutionException(message, commandMount);
                    }

                    if (StringUtils.isBlank(uri)) {
                        // throw new CommandMountResolutionException(String.format("Source input \"%s\" has no uri.", sourceInput.getName()), commandMount);
                        // I don't need to throw an exception here, right? This should be fine, right?
                    }

                    if (log.isDebugEnabled()) {
                        log.debug(String.format("Done resolving mount \"%s\", source input \"%s\".", commandMount.name(), sourceInput.name()));
                    }
                    partiallyResolvedMountBuilder.addInputFiles(
                            ResolvedCommandMountFiles.create(sourceInput.name(), uri, rootDirectory, null)
                    );
                }

            }

            final ResolvedCommandMount resolvedCommandMount = transportMount(partiallyResolvedMountBuilder.build());

            if (log.isInfoEnabled()) {
                log.info(String.format("Done resolving command mount \"%s\".", commandMount.name()));
            }
            return resolvedCommandMount;
        }

        private ResolvedCommandMount transportMount(final PartiallyResolvedCommandMount partiallyResolvedCommandMount) throws ContainerMountResolutionException {
            final ResolvedCommandMount.Builder resolvedCommandMountBuilder = partiallyResolvedCommandMount.toResolvedCommandMountBuilder();

            // First, figure out what we have.
            // Do we have source files? A source directory?
            // Can we mount a directory directly, or should we copy the contents to a build directory?
            final List<ResolvedCommandMountFiles> filesList = partiallyResolvedCommandMount.inputFiles();
            final String localDirectory;
            if (filesList != null && filesList.size() > 1) {
                // We have multiple sources of files. We must copy them into one common location to mount.
                localDirectory = getBuildDirectory();
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Mount \"%s\" has multiple sources of files.", partiallyResolvedCommandMount.name()));
                }

                // TODO figure out what to do with multiple sources of files
                if (log.isDebugEnabled()) {
                    log.debug("TODO");
                }
            } else if (filesList != null && filesList.size() == 1) {
                // We have one source of files. We may need to copy, or may be able to mount directly.
                final ResolvedCommandMountFiles files = filesList.get(0);
                final String path = files.path();
                final boolean hasPath = StringUtils.isNotBlank(path);

                if (StringUtils.isNotBlank(files.rootDirectory())) {
                    // That source of files does have a directory set.

                    if (hasPath || partiallyResolvedCommandMount.writable()) {
                        // In both of these conditions, we must copy some things to a build directory.
                        // Now we must find out what.
                        if (hasPath) {
                            // The source of files also has one or more paths set

                            localDirectory = getBuildDirectory();
                            if (log.isDebugEnabled()) {
                                log.debug(String.format("Mount \"%s\" has a root directory and a file. Copying the file from the root directory to build directory.", partiallyResolvedCommandMount.name()));
                            }

                            // TODO copy the file in "path", relative to the root directory, to the build directory
                            if (log.isDebugEnabled()) {
                                log.debug("TODO");
                            }
                        } else {
                            // The mount is set to "writable".
                            localDirectory = getBuildDirectory();
                            if (log.isDebugEnabled()) {
                                log.debug(String.format("Mount \"%s\" has a root directory, and is set to \"writable\". Copying all files from the root directory to build directory.", partiallyResolvedCommandMount.name()));
                            }

                            // TODO We must copy all files out of the root directory to a build directory.
                            if (log.isDebugEnabled()) {
                                log.debug("TODO");
                            }
                        }
                    } else {
                        // The source of files can be directly mounted
                        if (log.isDebugEnabled()) {
                            log.debug(String.format("Mount \"%s\" has a root directory, and is not set to \"writable\". The root directory can be mounted directly into the container.", partiallyResolvedCommandMount.name()));
                        }
                        localDirectory = files.rootDirectory();
                    }
                } else if (hasPath) {
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("Mount \"%s\" has a file. Copying it to build directory.", partiallyResolvedCommandMount.name()));
                    }
                    localDirectory = getBuildDirectory();
                    // TODO copy the file to the build directory
                    if (log.isDebugEnabled()) {
                        log.debug("TODO");
                    }

                } else {
                    final String message = String.format("Mount \"%s\" should have a file path or a directory or both but it does not.", partiallyResolvedCommandMount.name());
                    log.error(message);
                    throw new ContainerMountResolutionException(message, partiallyResolvedCommandMount);
                }

            } else {
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Mount \"%s\" has no input files. Ensuring mount is set to \"writable\" and creating new build directory.", partiallyResolvedCommandMount.name()));
                }
                localDirectory = getBuildDirectory();
                if (!partiallyResolvedCommandMount.writable()) {
                    resolvedCommandMountBuilder.writable(true);
                }
            }

            if (log.isDebugEnabled()) {
                log.debug(String.format("Setting mount \"%s\" xnat host path to \"%s\".", partiallyResolvedCommandMount.name(), localDirectory));
            }
            resolvedCommandMountBuilder.xnatHostPath(localDirectory);

            if (log.isDebugEnabled()) {
                log.debug(String.format("Transporting mount \"%s\".", partiallyResolvedCommandMount.name()));
            }
            // final Path pathOnContainerHost = transportService.transport(containerHost, Paths.get(buildDirectory));
            // TODO transporting is currently a no-op, and the code is simpler if we don't pretend that we are doing something here.
            final String containerHostPath = localDirectory;

            if (log.isDebugEnabled()) {
                log.debug(String.format("Setting mount \"%s\" container host path to \"%s\".", partiallyResolvedCommandMount.name(), localDirectory));
            }
            resolvedCommandMountBuilder.containerHostPath(containerHostPath);

            return resolvedCommandMountBuilder.build();
        }

        @Nonnull
        private String resolveTemplate(final String template)
                throws CommandResolutionException {
            return resolveTemplate(template, resolvedInputValuesByReplacementKey);
        }

        @Nonnull
        private String resolveTemplate(final String template, Map<String, String> valuesMap)
                throws CommandResolutionException {
            if (log.isInfoEnabled()) {
                log.info("Resolving template: " + template);
            }

            if (StringUtils.isBlank(template)) {
                log.info("Template is blank.");
                return template;
            }

            String toResolve = resolveJsonpathSubstring(template);

            for (final String replacementKey : valuesMap.keySet()) {
                final String replacementValue = valuesMap.get(replacementKey) != null ? valuesMap.get(replacementKey) : "";
                final String copyForLogging = log.isDebugEnabled() ? toResolve : null;

                toResolve = toResolve.replace(replacementKey, replacementValue);
                if (log.isDebugEnabled() && copyForLogging != null && !toResolve.equals(copyForLogging)) {
                    log.debug(String.format("%s -> %s", replacementKey, replacementValue));
                }
            }

            if (log.isInfoEnabled()) {
                log.info("Resolved template: " + toResolve);
            }
            return toResolve;
        }

        @Nonnull
        private String resolveJsonpathSubstring(final String stringThatMayContainJsonpathSubstring) throws CommandResolutionException {
            if (StringUtils.isNotBlank(stringThatMayContainJsonpathSubstring)) {
                if (log.isDebugEnabled()) {
                    log.debug("Checking for JSONPath substring in " + stringThatMayContainJsonpathSubstring);
                }

                final Matcher jsonpathSubstringMatcher = jsonpathSubstringPattern.matcher(stringThatMayContainJsonpathSubstring);

                if (jsonpathSubstringMatcher.find()) {

                    final String jsonpathSearchWithMarkers = jsonpathSubstringMatcher.group(0);
                    final String useWrapper = jsonpathSubstringMatcher.group(1);
                    final String jsonpathSearchWithoutMarkers = jsonpathSubstringMatcher.group(2);

                    if (log.isDebugEnabled()) {
                        log.debug("Found possible JSONPath substring " + jsonpathSearchWithMarkers);
                    }

                    if (StringUtils.isNotBlank(jsonpathSearchWithoutMarkers)) {

                        final List<String> searchResult;
                        if (StringUtils.isNotBlank(useWrapper)) {
                            log.debug("Performing JSONPath search through command wrapper with search string \"{}\".", jsonpathSearchWithoutMarkers);
                            searchResult = commandWrapperJsonpathSearchContext.read(jsonpathSearchWithoutMarkers);
                        } else {
                            log.debug("Performing JSONPath search through command with search string \"{}\".", jsonpathSearchWithoutMarkers);
                            searchResult = commandJsonpathSearchContext.read(jsonpathSearchWithoutMarkers);
                        }

                        if (searchResult != null && !searchResult.isEmpty() && searchResult.get(0) != null) {
                            log.debug("JSONPath search result: {}", searchResult);
                            if (searchResult.size() == 1) {
                                final String result = searchResult.get(0);
                                final String replacement = stringThatMayContainJsonpathSubstring.replace(jsonpathSearchWithMarkers, result);
                                if (log.isDebugEnabled()) {
                                    log.debug(String.format("Replacing %s with %s in %s.", jsonpathSearchWithMarkers, result, stringThatMayContainJsonpathSubstring));
                                    log.debug("Result: " + replacement);
                                }
                                return replacement;
                            } else {
                                final String message =
                                        String.format(
                                                "JSONPath search \"%s\" returned multiple results: %s. Cannot determine value to replace.",
                                                jsonpathSearchWithoutMarkers,
                                                searchResult.toString());
                                log.error(message);
                                throw new CommandResolutionException(message);
                            }
                        } else {
                            log.info("No result");
                        }
                    }
                }

                log.debug("No jsonpath substring found.");
            }
            return stringThatMayContainJsonpathSubstring;
        }

        @Nonnull
        private String getValue(final CommandWrapperInput commandWrapperInput) {
            return commandWrapperInputValues.get(commandWrapperInput);
        }

        private void setValue(final CommandWrapperInput commandWrapperInput,
                                final String value) {
            commandWrapperInputValues.put(commandWrapperInput, value);
        }

        @Nonnull
        private String getValue(final CommandInput commandInput) {
            return commandInputValues.get(commandInput);
        }

        private void setValue(final CommandInput commandInput,
                                final String value) {
            commandInputValues.put(commandInput, value);
        }

        @Nonnull
        private String getJsonValue(final CommandWrapperInput commandWrapperInput) {
            return commandWrapperInputJsonValues.get(commandWrapperInput);
        }

        private void setJsonValue(final CommandWrapperInput commandWrapperInput,
                                    final String jsonValue) {
            commandWrapperInputJsonValues.put(commandWrapperInput, jsonValue);
        }
    }

    private String getBuildDirectory() {
        String buildPath = siteConfigPreferences.getBuildPath();
        final String uuid = UUID.randomUUID().toString();
        return FilenameUtils.concat(buildPath, uuid);
    }
}
