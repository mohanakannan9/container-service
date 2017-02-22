package org.nrg.containers.helpers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.InvalidJsonException;
import com.jayway.jsonpath.InvalidPathException;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.TypeRef;
import com.jayway.jsonpath.spi.mapper.MappingException;
import org.apache.commons.lang3.StringUtils;
import org.nrg.config.services.ConfigService;
import org.nrg.containers.exceptions.CommandInputResolutionException;
import org.nrg.containers.exceptions.CommandMountResolutionException;
import org.nrg.containers.exceptions.CommandResolutionException;
import org.nrg.containers.exceptions.CommandWrapperInputResolutionException;
import org.nrg.containers.model.CommandEntity;
import org.nrg.containers.model.CommandInput;
import org.nrg.containers.model.CommandMount;
import org.nrg.containers.model.CommandOutput;
import org.nrg.containers.model.ContainerExecutionMount;
import org.nrg.containers.model.ContainerExecutionOutput;
import org.nrg.containers.model.ContainerMountFiles;
import org.nrg.containers.model.DockerCommandEntity;
import org.nrg.containers.model.ResolvedCommand;
import org.nrg.containers.model.ResolvedDockerCommand;
import org.nrg.containers.model.CommandWrapperInputEntity;
import org.nrg.containers.model.CommandWrapperOutputEntity;
import org.nrg.containers.model.CommandWrapperEntity;
import org.nrg.containers.model.xnat.Assessor;
import org.nrg.containers.model.xnat.Project;
import org.nrg.containers.model.xnat.Resource;
import org.nrg.containers.model.xnat.Scan;
import org.nrg.containers.model.xnat.Session;
import org.nrg.containers.model.xnat.Subject;
import org.nrg.containers.model.xnat.XnatFile;
import org.nrg.containers.model.xnat.XnatModelObject;
import org.nrg.framework.constants.Scope;
import org.nrg.xft.security.UserI;
import org.nrg.xnat.helpers.uri.URIManager;
import org.nrg.xnat.helpers.uri.UriParserUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandResolutionHelper {
    private static final Logger log = LoggerFactory.getLogger(CommandResolutionHelper.class);
    private static final String JSONPATH_SUBSTRING_REGEX = "\\^(wrapper:)?(.+)\\^";

    private final CommandWrapperEntity commandWrapperEntity;
    private final CommandEntity commandEntity;
    private final ResolvedCommand resolvedCommand;
    private CommandEntity cachedCommandEntity;
    private String commandJson;
    private CommandWrapperEntity cachedCommandWrapper;
    private String commandWrapperJson;
    private final Map<String, CommandWrapperInputEntity> resolvedXnatInputObjects = Maps.newHashMap();
    private final Map<String, String> resolvedXnatInputValuesByCommandInputName = Maps.newHashMap();
    private final Map<String, List<CommandWrapperInputEntity>> commandMountsToReceiveFilesFromXnatInputs = Maps.newHashMap();
    private final Map<String, String> resolvedInputValuesByReplacementKey = Maps.newHashMap();
    private final Map<String, String> resolvedInputCommandLineValuesByReplacementKey = Maps.newHashMap();
    private final UserI userI;
    private final ObjectMapper mapper;
    private final Map<String, String> inputValues;
    private final ConfigService configService;
    private final Pattern jsonpathSubstringPattern;

    private CommandResolutionHelper(final CommandWrapperEntity commandWrapperEntity,
                                    final CommandEntity commandEntity,
                                    final Map<String, String> inputValues,
                                    final UserI userI,
                                    final ConfigService configService) throws CommandResolutionException {
        this.commandWrapperEntity = commandWrapperEntity;
        this.commandEntity = commandEntity;
        switch (commandEntity.getType()) {
            case DOCKER:
                resolvedCommand = new ResolvedDockerCommand(commandWrapperEntity.getId(), (DockerCommandEntity) commandEntity);
                break;
            default:
                // If this happens, it is because I added a new CommandType and didn't add a case to this switch statement. Oops.
                throw new CommandResolutionException(String.format("Unknown command type: \"%s\".", commandEntity.getType()));
        }
        this.cachedCommandEntity = null;
        this.commandJson = null;
        this.cachedCommandWrapper = null;
        this.commandWrapperJson = null;
        // this.resolvedXnatInputObjects =
        // this.resolvedXnatInputValuesByCommandInputName = Maps.newHashMap();
        // this.resolvedInputValuesByReplacementKey = Maps.newHashMap();
        // this.resolvedInputCommandLineValuesByReplacementKey = Maps.newHashMap();
        this.userI = userI;
        this.mapper = new ObjectMapper();
        this.configService = configService;
        this.jsonpathSubstringPattern = Pattern.compile(JSONPATH_SUBSTRING_REGEX);

        this.inputValues = inputValues == null ?
                Maps.<String, String>newHashMap() :
                inputValues;
        resolvedCommand.setRawInputValues(this.inputValues);
    }

    public static ResolvedCommand resolve(final CommandWrapperEntity commandWrapperEntity,
                                          final CommandEntity commandEntity,
                                          final Map<String, String> inputValues,
                                          final UserI userI,
                                          final ConfigService configService)
            throws CommandResolutionException {
        final CommandResolutionHelper helper = new CommandResolutionHelper(commandWrapperEntity, commandEntity, inputValues, userI, configService);
        return helper.resolve();
    }

    private ResolvedCommand resolve() throws CommandResolutionException {
        log.info("Resolving command.");
        if (log.isDebugEnabled()) {
            log.debug(commandEntity.toString());
        }

        resolvedCommand.setXnatInputValues(resolveXnatWrapperInputs());

        resolvedCommand.setCommandInputValues(resolveInputs());
        resolvedCommand.setOutputs(resolveOutputs());
        resolvedCommand.setCommandLine(resolveCommandLine());
        resolvedCommand.setMounts(resolveCommandMounts());
        resolvedCommand.setEnvironmentVariables(resolveEnvironmentVariables());
        resolvedCommand.setWorkingDirectory(resolveTemplate(commandEntity.getWorkingDirectory()));

        switch (resolvedCommand.getType()) {
            case DOCKER:
                ((ResolvedDockerCommand) resolvedCommand).setPorts(resolvePorts());
                break;
            default:
                // Nothing to see here
        }

        log.info("Done resolving command.");
        if (log.isDebugEnabled()) {
            log.debug("Resolved command: \n" + resolvedCommand);
        }
        return resolvedCommand;
    }

    private Map<String, String> resolveXnatWrapperInputs() throws CommandResolutionException {
        log.info("Resolving xnat wrapper inputs.");

        final boolean hasExternalInputs = !(commandWrapperEntity.getExternalInputs() == null || commandWrapperEntity.getExternalInputs().isEmpty());
        final boolean hasDerivedInputs = !(commandWrapperEntity.getDerivedInputs() == null || commandWrapperEntity.getDerivedInputs().isEmpty());

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
        for (final CommandWrapperInputEntity externalInput : commandWrapperEntity.getExternalInputs()) {
            log.info(String.format("Resolving input \"%s\".", externalInput.getName()));

            String resolvedValue = null;
            String jsonRepresentation = null;

            // Give the input its default value
            if (log.isDebugEnabled()) {
                log.debug("Default value: " + externalInput.getDefaultValue());
            }
            if (externalInput.getDefaultValue() != null) {
                resolvedValue = externalInput.getDefaultValue();
            }

            // If a value was provided at runtime, use that over the default
            if (inputValues.containsKey(externalInput.getName()) && inputValues.get(externalInput.getName()) != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Runtime value: " + inputValues.get(externalInput.getName()));
                }
                resolvedValue = inputValues.get(externalInput.getName());
            }

            // Check for JSONPath substring in input value
            resolvedValue = resolveJsonpathSubstring(resolvedValue);

            // Resolve the matcher, if one was provided
            if (log.isDebugEnabled()) {
                log.debug("Matcher: " + externalInput.getMatcher());
            }
            final String resolvedMatcher = externalInput.getMatcher() != null ? resolveJsonpathSubstring(externalInput.getMatcher()) : null;

            if (StringUtils.isNotBlank(resolvedValue)) {
                // Process the input based on its type
                if (log.isDebugEnabled()) {
                    log.debug("Processing input value as a " + externalInput.getType().getName());
                }
                switch (externalInput.getType()) {
                    case PROJECT:
                        // We were either given, A. an archive-style URI, or B. the project id
                        final Project aProject;
                        try {
                            aProject = resolveXnatObjectUri(resolvedValue, resolvedMatcher, Project.class,
                                    Project.uriToModelObjectFunction(), Project.stringToModelObjectFunction(userI));
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
                        break;
                    case SUBJECT:
                        // We were either given, A. an archive-style URI, or B. the subject id
                        final Subject aSubject;
                        try {
                            aSubject = resolveXnatObjectUri(resolvedValue, resolvedMatcher, Subject.class,
                                    Subject.uriToModelObjectFunction(), Subject.stringToModelObjectFunction(userI));
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
                        break;
                    case SESSION:
                        // We were either given, A. an archive-style URI, or B. the session id
                        final Session aSession;
                        try {
                            aSession = resolveXnatObjectUri(resolvedValue, resolvedMatcher, Session.class,
                                    Session.uriToModelObjectFunction(), Session.stringToModelObjectFunction(userI));
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
                        break;
                    case SCAN:
                        // We must have been given the Scan as an archive URI
                        final Scan aScan;
                        try {
                            aScan = resolveXnatObjectUri(resolvedValue, resolvedMatcher, Scan.class,
                                    Scan.uriToModelObjectFunction(), Scan.stringToModelObjectFunction(userI));
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
                        break;
                    case ASSESSOR:
                        // We were either given, A. an archive-style URI, or B. the assessor id
                        final Assessor anAssessor;
                        try {
                            anAssessor = resolveXnatObjectUri(resolvedValue, resolvedMatcher, Assessor.class,
                                    Assessor.uriToModelObjectFunction(), Assessor.stringToModelObjectFunction(userI));
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

                        break;
                    case RESOURCE:
                        // We were either given, A. an archive-style URI, or B. the (globally unique integer) resource id
                        final Resource aResource;
                        try {
                            aResource = resolveXnatObjectUri(resolvedValue, resolvedMatcher, Resource.class,
                                    Resource.uriToModelObjectFunction(), Resource.stringToModelObjectFunction(userI));
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
                        break;
                    case CONFIG:
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
                        //         entityId = JsonPath.parse(parent.getJsonRepresentation()).read("$.id");
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
                        //         //final List<String> projectIds = JsonPath.parse(parent.getJsonRepresentation()).read("$..projectId");
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
                        break;
                    default:
                        log.debug("Nothing to do for simple types.");
                }
            }

            // If resolved value is null, and input is required, that is an error
            if (resolvedValue == null && externalInput.isRequired()) {
                final String message = String.format("No value could be resolved for required input \"%s\".", externalInput.getName());
                log.debug(message);
                throw new CommandWrapperInputResolutionException(message, externalInput);
            }
            if (log.isInfoEnabled()) {
                log.info(String.format("Done resolving input \"%s\". Value: \"%s\".", externalInput.getName(), resolvedValue));
            }
            externalInput.setValue(resolvedValue);
            externalInput.setJsonRepresentation(jsonRepresentation != null ? jsonRepresentation : resolvedValue);

            resolvedXnatInputObjects.put(externalInput.getName(), externalInput);

            resolvedXnatWrapperInputValuesByName.put(externalInput.getName(), externalInput.getValue());

            // If this xnat input provides any command input values, set them now
            final String commandInputName = externalInput.getProvidesValueForCommandInput();
            if (StringUtils.isNotBlank(commandInputName)) {
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Found value for command input \"%s\": \"%s\".",
                            commandInputName, externalInput.getValue()));
                }
                resolvedXnatInputValuesByCommandInputName.put(commandInputName, externalInput.getValue());
            }

            // If this xnat input provides files to a mount, note that now
            addInputToMountsList(externalInput);

            final String replacementKey = externalInput.getReplacementKey();
            if (StringUtils.isBlank(replacementKey)) {
                continue;
            }
            resolvedInputValuesByReplacementKey.put(replacementKey, resolvedValue);
            // resolvedInputCommandLineValuesByReplacementKey.put(replacementKey, getValueForCommandLine(externalInput, resolvedValue));
        }
        log.info("Done resolving external xnat wrapper inputs.");

        if (hasDerivedInputs) {
            log.info("Resolving derived xnat wrapper inputs.");

            for (final CommandWrapperInputEntity derivedInput : commandWrapperEntity.getDerivedInputs()) {
                log.info(String.format("Resolving input \"%s\".", derivedInput.getName()));

                if (StringUtils.isBlank(derivedInput.getDerivedFromXnatInput())) {
                    // TODO this should be caught during validation
                    final String message = String.format(
                            "Input \"%s\" is a derived input, but does not indicate the input from which it is to be derived.",
                            derivedInput.getName()
                    );
                    log.error(message);
                    throw new CommandWrapperInputResolutionException(message, derivedInput);
                }

                final String prereq = derivedInput.getDerivedFromXnatInput();
                if (!resolvedXnatInputObjects.containsKey(prereq)) {
                    // TODO this should be caught during validation. If prereq exists, but is in the wrong order, re-order inputs. If not, then error.
                    final String message = String.format(
                            "Input \"%1$s\" is derived from input \"%2$s\" which has not been resolved. Re-order the derived inputs so \"%1$s\" appears after \"%2$s\".",
                            derivedInput.getName(), prereq
                    );
                    log.error(message);
                    throw new CommandWrapperInputResolutionException(message, derivedInput);
                }
                final CommandWrapperInputEntity parentInput = resolvedXnatInputObjects.get(prereq);

                String resolvedValue = null;
                String jsonRepresentation = null;

                // Give the input its default value
                if (log.isDebugEnabled()) {
                    log.debug("Default value: " + derivedInput.getDefaultValue());
                }
                if (derivedInput.getDefaultValue() != null) {
                    resolvedValue = derivedInput.getDefaultValue();
                }

                // If a value was provided at runtime, use that over the default
                // NOTE: I don't know if it is a good idea to allow "derived" inputs to check for outside values.
                //       I feel like it would be more correct in a sense to force them to only get values that they derive from their parent.
                //       But at the same time, I don't want to remove an escape hatch that I myself may want to use.
                //       So this stays in for now. - JF
                if (inputValues.containsKey(derivedInput.getName()) && inputValues.get(derivedInput.getName()) != null) {
                    if (log.isDebugEnabled()) {
                        log.debug("Runtime value: " + inputValues.get(derivedInput.getName()));
                    }
                    resolvedValue = inputValues.get(derivedInput.getName());
                }

                // Check for JSONPath substring in input value
                resolvedValue = resolveJsonpathSubstring(resolvedValue);

                // Resolve the matcher, if one was provided
                if (log.isDebugEnabled()) {
                    log.debug("Matcher: " + derivedInput.getMatcher());
                }
                final String resolvedMatcher = derivedInput.getMatcher() != null ? resolveJsonpathSubstring(derivedInput.getMatcher()) : null;

                // Process the input based on its type
                if (log.isDebugEnabled()) {
                    log.debug("Processing input value as a " + derivedInput.getType().getName());
                }
                switch (derivedInput.getType()) {
                    case STRING:
                        final String propertyToGet = derivedInput.getDerivedFromXnatObjectProperty();
                        switch (parentInput.getType()) {
                            case PROJECT:
                                // Intentional fallthrough
                            case SUBJECT:
                                // Intentional fallthrough
                            case SESSION:
                                // Intentional fallthrough
                            case SCAN:
                                // Intentional fallthrough
                            case ASSESSOR:
                                // Intentional fallthrough
                            case FILE:
                                // Intentional fallthrough
                            case RESOURCE:

                                final String jsonPathSearch = "$." + propertyToGet +
                                        (StringUtils.isNotBlank(resolvedMatcher) ? "[?(" + resolvedMatcher + ")]" : "");
                                if (log.isInfoEnabled()) {
                                    log.info(String.format("Attempting to pull value from parent using matcher \"%s\".", jsonPathSearch));
                                }

                                final String parentJson = parentInput.getJsonRepresentation();
                                try {
                                    resolvedValue = JsonPath.parse(parentJson).read(jsonPathSearch, new TypeRef<String>(){});
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

                                break;
                            default:
                                final String message = String.format("An input of type \"%s\" cannot be derived from an input of type \"%s\".",
                                        derivedInput.getType().getName(),
                                        parentInput.getType().getName());
                                log.error(message);
                                throw new CommandWrapperInputResolutionException(message, derivedInput);
                        }
                        break;
                    case BOOLEAN:
                        // TODO
                        break;
                    case NUMBER:
                        // TODO
                        break;
                    case DIRECTORY:
                        switch (parentInput.getType()) {
                            case RESOURCE:

                                final String jsonPathSearch = "$.directory" +
                                        (StringUtils.isNotBlank(resolvedMatcher) ? "[?(" + resolvedMatcher + ")]" : "");
                                if (log.isInfoEnabled()) {
                                    log.info(String.format("Attempting to pull value from parent using matcher \"%s\".", jsonPathSearch));
                                }

                                final String parentJson = parentInput.getJsonRepresentation();
                                try {
                                    resolvedValue = JsonPath.parse(parentJson).read(jsonPathSearch, new TypeRef<String>(){});
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

                                break;
                            case PROJECT:
                                // TODO
                            case SESSION:
                                // TODO
                            case SCAN:
                                // TODO
                            case ASSESSOR:
                                // TODO
                            default:
                                final String message = String.format("An input of type \"%s\" cannot be derived from an input of type \"%s\".",
                                        derivedInput.getType().getName(),
                                        parentInput.getType().getName());
                                log.error(message);
                                throw new CommandWrapperInputResolutionException(message, derivedInput);
                        }
                        break;
                    case FILES:
                        List<XnatFile> files;
                        switch (parentInput.getType()) {
                            case RESOURCE:
                                files = matchChildFromParent(parentInput.getJsonRepresentation(),
                                        resolvedValue, "files", "name", resolvedMatcher, new TypeRef<List<XnatFile>>(){});
                                break;
                            default:
                                final String message = String.format("An input of type \"%s\" cannot be derived from an input of type \"%s\".",
                                        derivedInput.getType().getName(),
                                        parentInput.getType().getName());
                                log.error(message);
                                throw new CommandWrapperInputResolutionException(message, derivedInput);
                        }

                        if (files == null) {
                            final String message = String.format("Could not derive \"%s\" from \"%s\".", derivedInput.getName(), parentInput.getName());
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
                        break;
                    case FILE:
                        XnatFile file = null;
                        switch (parentInput.getType()) {
                            case RESOURCE:
                                final List<XnatFile> childList = matchChildFromParent(parentInput.getJsonRepresentation(),
                                        resolvedValue, "files", "name", resolvedMatcher, new TypeRef<List<XnatFile>>(){});
                                if (childList != null && !childList.isEmpty()) {
                                    if (log.isDebugEnabled()) {
                                        log.debug("Selecting first matching result from list.");
                                    }
                                    file = childList.get(0);
                                }
                                break;
                            default:
                                final String message = String.format("An input of type \"%s\" cannot be derived from an input of type \"%s\".",
                                        derivedInput.getType().getName(),
                                        parentInput.getType().getName());
                                log.error(message);
                                throw new CommandWrapperInputResolutionException(message, derivedInput);
                        }

                        if (file == null) {
                            final String message = String.format("Could not derive \"%s\" from \"%s\".", derivedInput.getName(), parentInput.getName());
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
                        break;
                    case PROJECT:
                        Project project;
                        try {
                            switch (parentInput.getType()) {
                                case SUBJECT:
                                    final Subject subject = mapper.readValue(parentInput.getJsonRepresentation(), Subject.class);
                                    project = subject.getProject(userI);
                                    break;
                                case SESSION:
                                    final Session session = mapper.readValue(parentInput.getJsonRepresentation(), Session.class);
                                    project = session.getProject(userI);
                                    break;
                                case SCAN:
                                    final Scan scan = mapper.readValue(parentInput.getJsonRepresentation(), Scan.class);
                                    project = scan.getProject(userI);
                                    break;
                                case ASSESSOR:
                                    final Assessor assessor = mapper.readValue(parentInput.getJsonRepresentation(), Assessor.class);
                                    project = assessor.getProject(userI);
                                    break;
                                default:
                                    final String message = String.format("An input of type \"%s\" cannot be derived from an input of type \"%s\".",
                                            derivedInput.getType().getName(),
                                            parentInput.getType().getName());
                                    log.error(message);
                                    throw new CommandWrapperInputResolutionException(message, derivedInput);
                            }
                        } catch (IOException e) {
                            final String message = String.format("Could not derive \"%s\" from \"%s\".", derivedInput.getName(), parentInput.getName());
                            log.error(message);
                            throw new CommandWrapperInputResolutionException(message, derivedInput, e);
                        }

                        if (project == null) {
                            final String message = String.format("Could not derive \"%s\" from \"%s\".", derivedInput.getName(), parentInput.getName());
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

                        break;
                    case SUBJECT:
                        Subject subject = null;
                        switch (parentInput.getType()) {
                            case PROJECT:
                                final List<Subject> childList = matchChildFromParent(parentInput.getJsonRepresentation(),
                                        resolvedValue, "subjects", "id", resolvedMatcher, new TypeRef<List<Subject>>(){});
                                if (childList != null && !childList.isEmpty()) {
                                    if (log.isDebugEnabled()) {
                                        log.debug("Selecting first matching result from list " + childList);
                                    }
                                    subject = childList.get(0);
                                }
                                break;
                            case SESSION:
                                try {
                                    final Session session = mapper.readValue(parentInput.getJsonRepresentation(), Session.class);
                                    subject = session.getSubject(userI);
                                } catch (IOException e) {
                                    final String message = String.format("Could not derive \"%s\" from \"%s\".", derivedInput.getName(), parentInput.getName());
                                    log.error(message);
                                    throw new CommandWrapperInputResolutionException(message, derivedInput, e);
                                }
                                break;
                            default:
                                final String message = String.format("An input of type \"%s\" cannot be derived from an input of type \"%s\".",
                                        derivedInput.getType().getName(),
                                        parentInput.getType().getName());
                                log.error(message);
                                throw new CommandWrapperInputResolutionException(message, derivedInput);
                        }

                        if (subject == null) {
                            final String message = String.format("Could not derive \"%s\" from \"%s\".", derivedInput.getName(), parentInput.getName());
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

                        break;
                    case SESSION:
                        Session session = null;
                        switch (parentInput.getType()) {
                            case SUBJECT:
                                final List<Session> childList = matchChildFromParent(parentInput.getJsonRepresentation(),
                                        resolvedValue, "sessions", "id", resolvedMatcher, new TypeRef<List<Session>>(){});
                                if (childList != null && !childList.isEmpty()) {
                                    if (log.isDebugEnabled()) {
                                        log.debug("Selecting first matching result from list " + childList);
                                    }
                                    session = childList.get(0);
                                }
                                break;
                            case SCAN:
                                try {
                                    final Scan scan = mapper.readValue(parentInput.getJsonRepresentation(), Scan.class);
                                    session = scan.getSession(userI);
                                } catch (IOException e) {
                                    final String message = String.format("Could not derive \"%s\" from \"%s\".", derivedInput.getName(), parentInput.getName());
                                    log.error(message);
                                    throw new CommandWrapperInputResolutionException(message, derivedInput, e);
                                }

                                break;
                            default:
                                final String message = String.format("An input of type \"%s\" cannot be derived from an input of type \"%s\".",
                                        derivedInput.getType().getName(),
                                        parentInput.getType().getName());
                                log.error(message);
                                throw new CommandWrapperInputResolutionException(message, derivedInput);
                        }

                        if (session == null) {
                            final String message = String.format("Could not derive \"%s\" from \"%s\".", derivedInput.getName(), parentInput.getName());
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
                        break;
                    case SCAN:
                        Scan scan = null;
                        switch (parentInput.getType()) {
                            case SESSION:
                                final List<Scan> childList = matchChildFromParent(parentInput.getJsonRepresentation(),
                                        resolvedValue, "scans", "id", resolvedMatcher, new TypeRef<List<Scan>>(){});
                                if (childList != null && !childList.isEmpty()) {
                                    if (log.isDebugEnabled()) {
                                        log.debug("Selecting first matching result from list.");
                                    }
                                    scan = childList.get(0);
                                }
                                break;
                            default:
                                final String message = String.format("An input of type \"%s\" cannot be derived from an input of type \"%s\".",
                                        derivedInput.getType().getName(),
                                        parentInput.getType().getName());
                                log.error(message);
                                throw new CommandWrapperInputResolutionException(message, derivedInput);
                        }

                        if (scan == null) {
                            final String message = String.format("Could not derive \"%s\" from \"%s\".", derivedInput.getName(), parentInput.getName());
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
                        break;
                    case ASSESSOR:
                        Assessor assessor = null;
                        switch (parentInput.getType()) {
                            case SESSION:
                                final List<Assessor> childList = matchChildFromParent(parentInput.getJsonRepresentation(),
                                        resolvedValue, "assessors", "id", resolvedMatcher, new TypeRef<List<Assessor>>() {
                                        });
                                if (childList != null && !childList.isEmpty()) {
                                    if (log.isDebugEnabled()) {
                                        log.debug("Selecting first matching result from list " + childList);
                                    }
                                    assessor = childList.get(0);
                                }
                                break;
                            default:
                                final String message = String.format("An input of type \"%s\" cannot be derived from an input of type \"%s\".",
                                        derivedInput.getType().getName(),
                                        parentInput.getType().getName());
                                log.error(message);
                                throw new CommandWrapperInputResolutionException(message, derivedInput);
                        }

                        if (assessor == null) {
                            final String message = String.format("Could not derive \"%s\" from \"%s\".", derivedInput.getName(), parentInput.getName());
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
                        break;
                    case RESOURCE:
                        Resource resource = null;
                        switch (parentInput.getType()) {
                            case PROJECT:
                                // Intentional fallthrough
                            case SUBJECT:
                                // Intentional fallthrough
                            case SESSION:
                                // Intentional fallthrough
                            case SCAN:
                                // Intentional fallthrough
                            case ASSESSOR:
                                final List<Resource> childStringList = matchChildFromParent(parentInput.getJsonRepresentation(),
                                        resolvedValue, "resources", "id", resolvedMatcher, new TypeRef<List<Resource>>(){});
                                if (childStringList != null && !childStringList.isEmpty()) {
                                    log.debug("Selecting first matching result from list.");
                                    resource = childStringList.get(0);
                                }
                                break;
                            default:
                                final String message = String.format("An input of type \"%s\" cannot be derived from an input of type \"%s\".",
                                        derivedInput.getType().getName(),
                                        parentInput.getType().getName());
                                log.error(message);
                                throw new CommandWrapperInputResolutionException(message, derivedInput);
                        }

                        if (resource == null) {
                            final String message = String.format("Could not derive \"%s\" from \"%s\".", derivedInput.getName(), parentInput.getName());
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
                        break;
                    case CONFIG:
                        // TODO
                        break;
                }

                // If resolved value is null, and input is required, that is an error
                if (resolvedValue == null && derivedInput.isRequired()) {
                    final String message = String.format("No value could be resolved for required input \"%s\".", derivedInput.getName());
                    log.debug(message);
                    throw new CommandWrapperInputResolutionException(message, derivedInput);
                }
                if (log.isInfoEnabled()) {
                    log.info(String.format("Done resolving input \"%s\". Value: \"%s\".", derivedInput.getName(), resolvedValue));
                }
                derivedInput.setValue(resolvedValue);
                derivedInput.setJsonRepresentation(jsonRepresentation != null ? jsonRepresentation : resolvedValue);

                resolvedXnatInputObjects.put(derivedInput.getName(), derivedInput);

                resolvedXnatWrapperInputValuesByName.put(derivedInput.getName(), derivedInput.getValue());

                // If this xnat input provides any command input values, set them now
                final String commandInputName = derivedInput.getProvidesValueForCommandInput();
                if (StringUtils.isNotBlank(commandInputName)) {
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("Found value for command input \"%s\": \"%s\".",
                                commandInputName, derivedInput.getValue()));
                    }
                    resolvedXnatInputValuesByCommandInputName.put(commandInputName, derivedInput.getValue());
                }

                // If this xnat input provides files to a mount, note that now
                addInputToMountsList(derivedInput);

                final String replacementKey = derivedInput.getReplacementKey();
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

    private void addInputToMountsList(final CommandWrapperInputEntity input) {
        if (input != null) {
            // TODO validate that there is a mount with this name
            final String mountName = input.getProvidesFilesForCommandMount();
            if (StringUtils.isNotBlank(mountName)) {
                List<CommandWrapperInputEntity> xnatInputs = commandMountsToReceiveFilesFromXnatInputs.get(mountName);
                if (xnatInputs == null) {
                    xnatInputs = Lists.newArrayList();
                }
                xnatInputs.add(input);
                commandMountsToReceiveFilesFromXnatInputs.put(mountName, xnatInputs);
            }
        }
    }

    private Map<String, String> resolveInputs() throws CommandResolutionException {
        log.info("Resolving command inputs.");

        if (commandEntity.getInputs() == null || commandEntity.getInputs().isEmpty()) {
            log.info("No inputs.");
            return null;
        }

        final Map<String, String> resolvedInputValuesByName = Maps.newHashMap();
        for (final CommandInput commandInput : commandEntity.getInputs()) {
            log.info(String.format("Resolving command input \"%s\".", commandInput.getName()));

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
                log.debug("Default value: " + commandInput.getDefaultValue());
            }
            if (commandInput.getDefaultValue() != null) {
                 resolvedValue = commandInput.getDefaultValue();
            }

            // If the input is supposed to get a value from an XNAT input, use that
            final String preresolvedValue = resolvedXnatInputValuesByCommandInputName.get(commandInput.getName());
            if (preresolvedValue != null) {
                if (log.isDebugEnabled()) {
                    log.debug("XNAT Wrapper value: " + preresolvedValue);
                }
                resolvedValue = preresolvedValue;
            }

            // If a value was provided at runtime, use that
            final String runtimeValue = inputValues.get(commandInput.getName());
            if (runtimeValue != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Runtime value: " + runtimeValue);
                }
                resolvedValue = runtimeValue;
            }

            // Check for JSONPath substring in input value
            resolvedValue = resolveJsonpathSubstring(resolvedValue);

            if (log.isDebugEnabled()) {
                log.debug("Matcher: " + commandInput.getMatcher());
            }
            final String resolvedMatcher = commandInput.getMatcher() != null ? resolveJsonpathSubstring(commandInput.getMatcher()) : null;

            if (log.isDebugEnabled()) {
                log.debug("Processing input value as a " + commandInput.getType().getName());
            }
            switch (commandInput.getType()) {
                case BOOLEAN:
                    // Parse the value as a boolean, and use the trueValue/falseValue
                    // If those haven't been set, just pass the value through
                    if (Boolean.parseBoolean(resolvedValue)) {
                        resolvedValue = commandInput.getTrueValue() != null ? commandInput.getTrueValue() : resolvedValue;
                    } else {
                        resolvedValue = commandInput.getFalseValue() != null ? commandInput.getFalseValue() : resolvedValue;
                    }
                    break;
                case NUMBER:
                    // TODO
                    break;
                default:
                    // TODO anything to do?
            }


            // If resolved value is null, and input is required, that is an error
            if (resolvedValue == null && commandInput.isRequired()) {
                final String message = String.format("No value could be resolved for required input \"%s\".", commandInput.getName());
                log.debug(message);
                throw new CommandInputResolutionException(message, commandInput);
            }
            if (log.isInfoEnabled()) {
                log.info(String.format("Done resolving input \"%s\". Value: %s", commandInput.getName(), resolvedValue));
            }
            commandInput.setValue(resolvedValue);
            // input.setJsonRepresentation(jsonRepresentation != null ? jsonRepresentation : resolvedValue);

            // resolvedXnatInputObjects.put(input.getName(), input);
            resolvedInputValuesByName.put(commandInput.getName(), commandInput.getValue());

            // Only substitute the input into the command line if a replacementKey is set
            final String replacementKey = commandInput.getReplacementKey();
            if (StringUtils.isBlank(replacementKey)) {
                continue;
            }
            resolvedInputValuesByReplacementKey.put(replacementKey, resolvedValue);
            resolvedInputCommandLineValuesByReplacementKey.put(replacementKey, getValueForCommandLine(commandInput, resolvedValue));
        }

        return resolvedInputValuesByName;
    }

    private String commandAsJson() throws CommandResolutionException {
        if (!commandEntity.equals(cachedCommandEntity)) {
            cachedCommandEntity = commandEntity;

            try {
                commandJson = mapper.writeValueAsString(cachedCommandEntity);
            } catch (JsonProcessingException e) {
                final String message = "Could not serialize command to json.";
                log.debug(message);
                throw new CommandResolutionException(message, e);
            }
        }

        return commandJson;
    }

    private String commandWrapperAsJson() throws CommandResolutionException {
        if (!commandWrapperEntity.equals(cachedCommandWrapper)) {
            cachedCommandWrapper = commandWrapperEntity;

            try {
                commandWrapperJson = mapper.writeValueAsString(cachedCommandWrapper);
            } catch (JsonProcessingException e) {
                final String message = "Could not serialize command wrapper to json.";
                log.debug(message);
                throw new CommandResolutionException(message, e);
            }
        }

        return commandWrapperJson;
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
        if (StringUtils.isBlank(input.getCommandLineFlag())) {
            return resolvedInputValue;
        } else {
            return input.getCommandLineFlag() +
                    (input.getCommandLineSeparator() == null ? " " : input.getCommandLineSeparator()) +
                    resolvedInputValue;
        }
    }

    private <T extends XnatModelObject> T resolveXnatObjectUri(final String value,
                                                               final String matcher,
                                                               final Class<T> model,
                                                               final Function<URIManager.ArchiveItemURI, T> uriToModelObjectFunction,
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

            if (uri == null || !(uri instanceof URIManager.ArchiveItemURI)) {
                throw new CommandWrapperInputResolutionException(String.format("Cannot interpret value as a URI: %s.", value), null);
            }

            newModelObject = uriToModelObjectFunction.apply((URIManager.ArchiveItemURI) uri);

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

    private List<ContainerExecutionOutput> resolveOutputs() throws CommandResolutionException {
        log.info("Resolving command outputs.");
        if (commandEntity.getOutputs() == null) {
            return null;
        }

        final Map<String, CommandWrapperOutputEntity> xnatCommandOutputsByCommandOutputName = Maps.newHashMap();
        if (commandWrapperEntity.getOutputHandlers() != null) {
            for (final CommandWrapperOutputEntity commandWrapperOutputEntity : commandWrapperEntity.getOutputHandlers()) {
                xnatCommandOutputsByCommandOutputName.put(commandWrapperOutputEntity.getCommandOutputName(), commandWrapperOutputEntity);
            }
        }

        final List<ContainerExecutionOutput> resolvedOutputs = Lists.newArrayList();
        for (final CommandOutput commandOutput : commandEntity.getOutputs()) {
            if (log.isInfoEnabled()) {
                log.info(String.format("Resolving command output \"%s\"", commandOutput.getName()));
            }
            if (log.isDebugEnabled()) {
                log.debug(commandOutput.toString());
            }

            // TODO fix this in validation
            final CommandWrapperOutputEntity commandOutputHandler = xnatCommandOutputsByCommandOutputName.get(commandOutput.getName());
            if (commandOutputHandler == null) {
                throw new CommandResolutionException(String.format("No XNAT object was configured to handle output \"%s\".", commandOutput.getName()));
            }
            if (log.isDebugEnabled()) {
                log.debug(String.format("Found XNAT Output Handler for Command output \"%s\".", commandOutput.getName()));
            }

            final ContainerExecutionOutput resolvedOutput = new ContainerExecutionOutput(commandOutput, commandOutputHandler);

            resolvedOutput.setPath(resolveTemplate(commandOutput.getPath()));
            resolvedOutput.setLabel(resolveTemplate(commandOutputHandler.getLabel()));

            // TODO Anything else needed to resolve an output?

            if (log.isDebugEnabled()) {
                log.debug(String.format("Adding resolved output \"%s\" to resolved command.", resolvedOutput.getName()));
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

    private String resolveCommandLine() throws CommandResolutionException {
        log.info("Resolving command-line string.");

        final String resolvedCommandLine = resolveTemplate(commandEntity.getCommandLine(), resolvedInputCommandLineValuesByReplacementKey);

        log.info("Done resolving command-line string.");
        if (log.isDebugEnabled()) {
            log.debug("Command-line string: " + resolvedCommandLine);
        }
        return resolvedCommandLine;
    }

    private Map<String, String> resolveEnvironmentVariables()
            throws CommandResolutionException {
        log.info("Resolving environment variables.");

        final Map<String, String> envTemplates = commandEntity.getEnvironmentVariables();
        if (envTemplates == null || envTemplates.isEmpty()) {
            log.info("No environment variables to resolve.");
            return null;
        }

        final Map<String, String> resolvedMap = resolveTemplateMap(envTemplates);

        log.info("Done resolving environment variables.");
        if (log.isDebugEnabled()) {
            String message = "Environment variables: ";
            for (Map.Entry<String, String> env : resolvedMap.entrySet()) {
                message += env.getKey() + ": " + env.getValue() + ", ";
            }
            log.debug(message);
        }
        return resolvedMap;
    }

    private Map<String, String> resolvePorts()
            throws CommandResolutionException {
        if (!DockerCommandEntity.class.isAssignableFrom(commandEntity.getClass())) {
            return null;
        }
        log.info("Resolving ports.");
        final DockerCommandEntity dockerCommand = (DockerCommandEntity) commandEntity;

        final Map<String, String> portTemplates = dockerCommand.getPorts();
        if (portTemplates == null || portTemplates.isEmpty()) {
            log.info("No ports to resolve.");
            return null;
        }

        final Map<String, String> resolvedMap = resolveTemplateMap(portTemplates);

        log.info("Done resolving ports.");
        if (log.isDebugEnabled()) {
            String message = "Ports: ";
            for (Map.Entry<String, String> env : resolvedMap.entrySet()) {
                message += env.getKey() + ": " + env.getValue() + ", ";
            }
            log.debug(message);
        }
        return resolvedMap;
    }

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

    private List<ContainerExecutionMount> resolveCommandMounts() throws CommandResolutionException {
        log.info("Resolving mounts.");
        final List<CommandMount> commandMounts = commandEntity.getMounts();
        if (commandMounts == null || commandMounts.isEmpty()) {
            log.info("No mounts.");
            return Lists.newArrayList();
        }

        final List<ContainerExecutionMount> resolvedMounts = Lists.newArrayList();
        for (final CommandMount commandMount : commandMounts) {
            resolvedMounts.add(resolveCommandMount(commandMount));
        }

        log.info("Done resolving mounts.");
        if (log.isDebugEnabled()) {
            for (final ContainerExecutionMount mount : resolvedMounts) {
                log.debug(mount.toString());
            }
        }
        return resolvedMounts;
    }

    private ContainerExecutionMount resolveCommandMount(final CommandMount commandMount)
            throws CommandResolutionException {
        if (log.isInfoEnabled()) {
            log.info(String.format("Resolving command mount \"%s\".", commandMount.getName()));
        }

        final ContainerExecutionMount resolvedMount = new ContainerExecutionMount(commandMount);
        resolvedMount.setContainerPath(resolveTemplate(commandMount.getContainerPath()));

        final List<CommandWrapperInputEntity> sourceInputs = commandMountsToReceiveFilesFromXnatInputs.get(commandMount.getName());
        if (sourceInputs == null || sourceInputs.isEmpty()) {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Command mount \"%s\" has no inputs that provide it files. Assuming it is an output mount.", commandMount.getName()));
            }
            resolvedMount.setWritable(true);
        } else {
            final List<ContainerMountFiles> filesList = Lists.newArrayList();
            for (final CommandWrapperInputEntity sourceInput : sourceInputs) {
                if (sourceInput == null) {
                    final String message = String.format("Cannot resolve mount \"%s\". Source input is null.", commandMount.getName());
                    log.error(message);
                    throw new CommandMountResolutionException(message, commandMount);
                } else if (StringUtils.isBlank(sourceInput.getValue())) {
                    final String message = String.format("Cannot resolve mount \"%s\". Source input \"%s\" has no resolved value.", commandMount.getName(), sourceInput.getName());
                    log.error(message);
                    throw new CommandMountResolutionException(message, commandMount);
                }

                if (log.isDebugEnabled()) {
                    log.debug(String.format("Mount \"%s\" has source input \"%s\" with type \"%s\".", commandMount.getName(), sourceInput.getName(), sourceInput.getType().getName()));
                }
                final ContainerMountFiles files = new ContainerMountFiles(sourceInput);
                switch (sourceInput.getType()) {
                    case DIRECTORY:
                        // TODO
                        break;
                    case FILES:
                        // TODO
                        break;
                    case FILE:
                        // TODO
                        break;
                    case PROJECT:
                        // Intentional fallthrough
                    case SESSION:
                        // Intentional fallthrough
                    case SCAN:
                        // Intentional fallthrough
                    case ASSESSOR:
                        // Intentional fallthrough
                    case RESOURCE:
                        if (log.isDebugEnabled()) {
                            log.debug("Looking for directory on source input.");
                        }

                        final String directory = JsonPath.parse(sourceInput.getJsonRepresentation()).read("directory", String.class);
                        if (StringUtils.isNotBlank(directory)) {
                            if (log.isDebugEnabled()) {
                                log.debug("Setting directory " + directory);
                            }
                            files.setRootDirectory(directory);
                        } else {
                            String message = "Source input has no directory.";
                            if (log.isDebugEnabled()) {
                                message += "\ninput: " + sourceInput;
                            }
                            log.error(message);
                            throw new CommandMountResolutionException(message, commandMount);
                        }

                        final String uri = JsonPath.parse(sourceInput.getJsonRepresentation()).read("uri", String.class);
                        if (StringUtils.isNotBlank(uri)) {
                            files.setFromUri(uri);
                        } else {
                            // throw new CommandMountResolutionException(String.format("Source input \"%s\" has no uri.", sourceInput.getName()), commandMount);
                            // I don't need to throw an exception here, right? This should be fine, right?
                        }

                        break;
                    default:
                        final String message = String.format("I don't know how to provide files to a mount from an input of type \"%s\".", sourceInput.getType().getName());
                        log.error(message);
                        throw new CommandMountResolutionException(message, commandMount);
                }

                if (log.isDebugEnabled()) {
                    log.debug(String.format("Done resolving mount \"%s\", source input \"%s\".", commandMount.getName(), sourceInput.getName()));
                }
                filesList.add(files);
            }
            resolvedMount.setInputFiles(filesList);


        }



        if (log.isInfoEnabled()) {
            log.info(String.format("Done resolving command mount \"%s\".", commandMount.getName()));
        }
        return resolvedMount;
    }

    private String resolveTemplate(final String template)
            throws CommandResolutionException {
        return resolveTemplate(template, resolvedInputValuesByReplacementKey);
    }

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
                    final Configuration c = Configuration.defaultConfiguration().addOptions(Option.ALWAYS_RETURN_LIST);
                    final List<String> searchResult;
                    if (StringUtils.isNotBlank(useWrapper)) {
                        if(log.isInfoEnabled()) {
                            log.info("Performing JSONPath search through command wrapper with search string " + jsonpathSearchWithoutMarkers);
                        }
                        searchResult = JsonPath.using(c).parse(commandWrapperAsJson()).read(jsonpathSearchWithoutMarkers);
                    } else {
                        if(log.isInfoEnabled()) {
                            log.info("Performing JSONPath search through command with search string " + jsonpathSearchWithoutMarkers);
                        }
                        searchResult = JsonPath.using(c).parse(commandAsJson()).read(jsonpathSearchWithoutMarkers);
                    }

                    if (searchResult != null && !searchResult.isEmpty() && searchResult.get(0) != null) {
                        if (log.isInfoEnabled()) {
                            log.info("Search result: " + searchResult);
                        }
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
                                            "JSONPath search %s resulted in multiple results: %s. Cannot determine value to replace into string %s.",
                                            jsonpathSearchWithoutMarkers,
                                            searchResult.toString(),
                                            stringThatMayContainJsonpathSubstring);
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
}