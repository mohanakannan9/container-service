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
import org.nrg.containers.model.Command;
import org.nrg.containers.model.CommandInput;
import org.nrg.containers.model.CommandMount;
import org.nrg.containers.model.CommandOutput;
import org.nrg.containers.model.CommandOutputFiles;
import org.nrg.containers.model.ContainerExecutionMount;
import org.nrg.containers.model.ContainerExecutionOutput;
import org.nrg.containers.model.ResolvedCommand;
import org.nrg.containers.model.xnat.Assessor;
import org.nrg.containers.model.xnat.Project;
import org.nrg.containers.model.xnat.Resource;
import org.nrg.containers.model.xnat.Scan;
import org.nrg.containers.model.xnat.Session;
import org.nrg.containers.model.xnat.Subject;
import org.nrg.containers.model.xnat.XnatFile;
import org.nrg.containers.model.xnat.XnatModelObject;
import org.nrg.framework.constants.Scope;
import org.nrg.xdat.om.XnatExperimentdata;
import org.nrg.xdat.om.XnatImageassessordata;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.XnatSubjectdata;
import org.nrg.xft.security.UserI;
import org.nrg.xnat.helpers.uri.URIManager;
import org.nrg.xnat.helpers.uri.UriParserUtils;
import org.nrg.xnat.helpers.uri.archive.AssessorURII;
import org.nrg.xnat.helpers.uri.archive.ExperimentURII;
import org.nrg.xnat.helpers.uri.archive.ProjectURII;
import org.nrg.xnat.helpers.uri.archive.ScanURII;
import org.nrg.xnat.helpers.uri.archive.SubjectURII;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandResolutionHelper {
    private static final Logger log = LoggerFactory.getLogger(CommandResolutionHelper.class);
    private static final String JSONPATH_SUBSTRING_REGEX = "\\^(.+)\\^";

    private Command command;
    private ResolvedCommand resolvedCommand;
    private Command cachedCommand;
    private String commandJson;
    private Map<String, CommandInput> resolvedInputObjects;
    private Map<String, String> resolvedInputValuesByReplacementKey;
    private Map<String, String> resolvedInputCommandLineValuesByReplacementKey;
    private UserI userI;
    private ObjectMapper mapper;
    private Map<String, String> inputValues;
    private ConfigService configService;
    private Pattern jsonpathSubstringPattern;

    private CommandResolutionHelper(final Command command,
                                    final Map<String, String> inputValues,
                                    final UserI userI,
                                    final ConfigService configService) {
        this.command = command;
        resolvedCommand = new ResolvedCommand(command);
        this.cachedCommand = null;
        this.commandJson = null;
        this.resolvedInputObjects = Maps.newHashMap();
        this.resolvedInputValuesByReplacementKey = Maps.newHashMap();
        this.resolvedInputCommandLineValuesByReplacementKey = Maps.newHashMap();
//            command.setInputs(Lists.<CommandInput>newArrayList());
        this.userI = userI;
        this.mapper = new ObjectMapper();
        this.inputValues = inputValues == null ?
                Maps.<String, String>newHashMap() :
                inputValues;
        this.configService = configService;
        this.jsonpathSubstringPattern = Pattern.compile(JSONPATH_SUBSTRING_REGEX);
    }

    public static ResolvedCommand resolve(final Command command,
                                          final Map<String, String> inputValues,
                                          final UserI userI,
                                          final ConfigService configService)
            throws CommandResolutionException {
        final CommandResolutionHelper helper = new CommandResolutionHelper(command, inputValues, userI, configService);
        return helper.resolve();
    }

    private ResolvedCommand resolve() throws CommandResolutionException {
        log.info("Resolving command.");
        if (log.isDebugEnabled()) {
            log.debug(command.toString());
        }

        resolvedCommand.setInputValues(resolveInputs());
        resolvedCommand.setOutputs(resolveOutputs());
        resolvedCommand.setCommandLine(resolveCommandLine());
        resolvedCommand.setMounts(resolveCommandMounts());
        resolvedCommand.setEnvironmentVariables(resolveEnvironmentVariables());
        resolvedCommand.setPorts(resolvePorts());

        log.info("Done resolving command.");
        if (log.isDebugEnabled()) {
            log.debug("Resolved command: \n" + resolvedCommand);
        }
        return resolvedCommand;
    }

    private Map<String, String> resolveInputs() throws CommandResolutionException {
        log.info("Resolving command inputs.");

        if (command.getInputs() == null) {
            log.info("No inputs.");
            return null;
        }

        final Map<String, String> resolvedInputValuesByName = Maps.newHashMap();
        for (final CommandInput input : command.getInputs()) {
            log.info(String.format("Resolving input \"%s\".", input.getName()));

            // Check that all prerequisites have already been resolved.
            // TODO Move this to a command validation function. Command should not be saved unless inputs are in correct order. At this stage, we should be able to safely iterate.
            final List<String> prerequisites = StringUtils.isNotBlank(input.getPrerequisites()) ?
                    Lists.newArrayList(input.getPrerequisites().split("\\s*,\\s*")) :
                    Lists.<String>newArrayList();
            if (StringUtils.isNotBlank(input.getParent()) && !prerequisites.contains(input.getParent())) {
                // Parent is always a prerequisite
                prerequisites.add(input.getParent());
            }

            if (log.isDebugEnabled()) {
                log.debug("Prerequisites: " + prerequisites.toString());
            }
            for (final String prereq : prerequisites) {
                if (!resolvedInputObjects.containsKey(prereq)) {
                    final String message = String.format(
                            "Input \"%1$s\" has prerequisite \"%2$s\" which has not been resolved. Re-order the command inputs so \"%1$s\" appears after \"%2$s\".",
                            input.getName(), prereq
                    );
                    log.error(message);
                    throw new CommandInputResolutionException(message, input);
                }
            }

            // If input requires a parent, it must be resolved first
            CommandInput parent = null;
            if (StringUtils.isNotBlank(input.getParent())) {
                if (resolvedInputObjects.containsKey(input.getParent())) {
                    // Parent has already been resolved. We can continue.
                    parent = resolvedInputObjects.get(input.getParent());
                } else {
                    // This exception should have been thrown already above, but just in case it wasn't...
                    final String message = String.format(
                            "Input %1$s has prerequisite %2$s which has not been resolved. Re-order inputs so %1$s appears after %2$s.",
                            input.getName(), input.getParent()
                    );
                    log.error(message);
                    throw new CommandInputResolutionException(message, input);
                }
            }

            String resolvedValue = null;
            String jsonRepresentation = null;

            // Give the input its default value
            if (log.isDebugEnabled()) {
                log.debug("Default value: " + input.getDefaultValue());
            }
            if (input.getDefaultValue() != null) {
                 resolvedValue = input.getDefaultValue();
            }

            // If a value was provided at runtime, use that over the default
            if (inputValues.containsKey(input.getName()) && inputValues.get(input.getName()) != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Runtime value: " + inputValues.get(input.getName()));
                }
                resolvedValue = inputValues.get(input.getName());
            }

            // Check for JSONPath substring in input value
            resolvedValue = resolveJsonpathSubstring(resolvedValue);

            if (log.isDebugEnabled()) {
                log.debug("Matcher: " + input.getMatcher());
            }
            final String resolvedMatcher = input.getMatcher() != null ? resolveJsonpathSubstring(input.getMatcher()) : null;

            if (log.isDebugEnabled()) {
                log.debug("Processing input value as a " + input.getType().getName());
            }
            switch (input.getType()) {
                case BOOLEAN:
                    // Parse the value as a boolean, and use the trueValue/falseValue
                    // If those haven't been set, just pass the value through
                    if (Boolean.parseBoolean(resolvedValue)) {
                        resolvedValue = input.getTrueValue() != null ? input.getTrueValue() : resolvedValue;
                    } else {
                        resolvedValue = input.getFalseValue() != null ? input.getFalseValue() : resolvedValue;
                    }
                    break;
                case NUMBER:
                    // TODO
                    break;
                case FILE:
                    if (parent != null) {
                        final List<XnatFile> childList = matchChildFromParent(parent.getJsonRepresentation(),
                                resolvedValue, "files", "name", resolvedMatcher, new TypeRef<List<XnatFile>>(){});
                        if (childList != null && !childList.isEmpty()) {
                            if (log.isDebugEnabled()) {
                                log.debug("Selecting first matching result from list " + childList);
                            }
                            final XnatFile first = childList.get(0);
                            if (log.isDebugEnabled()) {
                                log.debug("Setting resolvedValue to uri " + first.getUri());
                            }
                            resolvedValue = first.getUri();
                            try {
                                jsonRepresentation = mapper.writeValueAsString(first);
                            } catch (JsonProcessingException e) {
                                log.error("Could not serialize file to json.", e);
                            }
                        }
                    } else {
                        throw new CommandInputResolutionException(String.format("Inputs of type %s must have a parent.", input.getType()), input);
                    }
                    break;
                case PROJECT:
                    if (parent != null) {
                        // TODO This should have already been fixed by command validation
                        final String message = "Project inputs cannot have parents.";
                        log.error(message);
                        throw new CommandInputResolutionException(message, input);
                    } else if (StringUtils.isNotBlank(resolvedValue)) {
                        // With no parent, we were either given, A. an archive-style URI, or B. the project id
                        final Project aMatch;
                        try {
                            aMatch = resolveXnatObjectUri(resolvedValue, resolvedMatcher, Project.class,
                                    new Function<URIManager.ArchiveItemURI, Project>() {
                                        @Nullable
                                        @Override
                                        public Project apply(@Nullable URIManager.ArchiveItemURI uri) {
                                            if (uri != null &&
                                                    ProjectURII.class.isAssignableFrom(uri.getClass())) {
                                                return new Project((ProjectURII) uri);
                                            }

                                            return null;
                                        }
                                    },
                                    new Function<String, Project>() {
                                        @Nullable
                                        @Override
                                        public Project apply(@Nullable String s) {
                                            if (StringUtils.isBlank(s)) {
                                                return null;
                                            }
                                            final XnatProjectdata xnatProjectdata = XnatProjectdata.getXnatProjectdatasById(s, userI, true);
                                            if (xnatProjectdata != null) {
                                                return new Project(xnatProjectdata);
                                            }
                                            return null;
                                        }
                                    });
                        } catch (CommandInputResolutionException e) {
                            throw new CommandInputResolutionException(e.getMessage(), input);
                        }

                        if (aMatch != null) {
                            if (log.isDebugEnabled()) {
                                log.debug("Setting resolvedValue to uri " + aMatch.getUri());
                            }
                            resolvedValue = aMatch.getUri();
                            try {
                                jsonRepresentation = mapper.writeValueAsString(aMatch);
                            } catch (JsonProcessingException e) {
                                String message = "Could not serialize project";
                                if (log.isDebugEnabled()) {
                                    message += ": " + aMatch;
                                } else {
                                    message += ".";
                                }
                                log.error(message, e);
                            }
                        }
                    } else {
                        // If value is blank, we will deal with that later
                    }
                    break;
                case SUBJECT:
                    if (parent != null) {
                        // We have a parent, so pull the value from it
                        // If we have any value set currently, assume it is an ID

                        final List<Subject> childList = matchChildFromParent(parent.getJsonRepresentation(),
                                resolvedValue, "subjects", "id", resolvedMatcher, new TypeRef<List<Subject>>(){});
                        if (childList != null && !childList.isEmpty()) {
                            if (log.isDebugEnabled()) {
                                log.debug("Selecting first matching result from list " + childList);
                            }
                            final Subject first = childList.get(0);

                            if (log.isDebugEnabled()) {
                                log.debug("Setting resolvedValue to uri " + first.getUri());
                            }
                            resolvedValue = first.getUri();
                            try {
                                jsonRepresentation = mapper.writeValueAsString(first);
                            } catch (JsonProcessingException e) {
                                log.error("Could not serialize subject to json.", e);
                            }
                        }
                    } else if (StringUtils.isNotBlank(resolvedValue)) {
                        // With no parent, we were either given, A. an archive-style URI, or B. the subject id
                        final Subject aMatch;
                        try {
                            aMatch = resolveXnatObjectUri(resolvedValue, resolvedMatcher, Subject.class,
                                    new Function<URIManager.ArchiveItemURI, Subject>() {
                                        @Nullable
                                        @Override
                                        public Subject apply(@Nullable URIManager.ArchiveItemURI uri) {
                                            if (uri != null &&
                                                    SubjectURII.class.isAssignableFrom(uri.getClass())) {
                                                return new Subject((SubjectURII) uri);
                                            }

                                            return null;
                                        }
                                    },
                                    new Function<String, Subject>() {
                                        @Nullable
                                        @Override
                                        public Subject apply(@Nullable String s) {
                                            if (StringUtils.isBlank(s)) {
                                                return null;
                                            }
                                            final XnatSubjectdata xnatSubjectdata = XnatSubjectdata.getXnatSubjectdatasById(s, userI, true);
                                            if (xnatSubjectdata != null) {
                                                return new Subject(xnatSubjectdata);
                                            }
                                            return null;
                                        }
                                    });
                        } catch (CommandInputResolutionException e) {
                            throw new CommandInputResolutionException(e.getMessage(), input);
                        }

                        if (aMatch != null) {
                            if (log.isDebugEnabled()) {
                                log.debug("Setting resolvedValue to uri " + aMatch.getUri());
                            }
                            resolvedValue = aMatch.getUri();
                            try {
                                jsonRepresentation = mapper.writeValueAsString(aMatch);
                            } catch (JsonProcessingException e) {
                                String message = "Could not serialize subject";
                                if (log.isDebugEnabled()) {
                                    message += ": " + aMatch;
                                } else {
                                    message += ".";
                                }
                                log.error(message, e);
                            }
                        }
                    } else {
                        // If value is blank, we will deal with that later
                    }
                    break;
                case SESSION:
                    if (parent != null) {
                        // We have a parent, so pull the value from it
                        // If we have any value set currently, assume it is an ID

                        final List<Session> childList = matchChildFromParent(parent.getJsonRepresentation(),
                                resolvedValue, "sessions", "id", resolvedMatcher, new TypeRef<List<Session>>(){});
                        if (childList != null && !childList.isEmpty()) {
                            if (log.isDebugEnabled()) {
                                log.debug("Selecting first matching result from list " + childList);
                            }
                            final Session first = childList.get(0);

                            if (log.isDebugEnabled()) {
                                log.debug("Setting resolvedValue to uri " + first.getUri());
                            }
                            resolvedValue = first.getUri();
                            try {
                                jsonRepresentation = mapper.writeValueAsString(first);
                            } catch (JsonProcessingException e) {
                                log.error("Could not serialize session to json.", e);
                            }
                        }
                    } else if (StringUtils.isNotBlank(resolvedValue)) {
                        // With no parent, we were either given, A. an archive-style URI, or B. the session id
                        final Session aMatch;
                        try {
                            aMatch = resolveXnatObjectUri(resolvedValue, resolvedMatcher, Session.class,
                                    new Function<URIManager.ArchiveItemURI, Session>() {
                                        @Nullable
                                        @Override
                                        public Session apply(@Nullable URIManager.ArchiveItemURI uri) {
                                            XnatExperimentdata experiment;
                                            if (uri != null &&
                                                    ExperimentURII.class.isAssignableFrom(uri.getClass())) {
                                                experiment = ((ExperimentURII) uri).getExperiment();

                                                if (experiment != null &&
                                                        XnatImagesessiondata.class.isAssignableFrom(experiment.getClass())) {
                                                    return new Session((ExperimentURII) uri);
                                                }
                                            }

                                            return null;
                                        }
                                    },
                                    new Function<String, Session>() {
                                        @Nullable
                                        @Override
                                        public Session apply(@Nullable String s) {
                                            if (StringUtils.isBlank(s)) {
                                                return null;
                                            }
                                            final XnatImagesessiondata imagesessiondata = XnatImagesessiondata.getXnatImagesessiondatasById(s, userI, true);
                                            if (imagesessiondata != null) {
                                                return new Session(imagesessiondata);
                                            }
                                            return null;
                                        }
                                    });
                        } catch (CommandInputResolutionException e) {
                            throw new CommandInputResolutionException(e.getMessage(), input);
                        }

                        if (aMatch != null) {
                            if (log.isDebugEnabled()) {
                                log.debug("Setting resolvedValue to uri " + aMatch.getUri());
                            }
                            resolvedValue = aMatch.getUri();
                            try {
                                jsonRepresentation = mapper.writeValueAsString(aMatch);
                            } catch (JsonProcessingException e) {
                                String message = "Could not serialize session";
                                if (log.isDebugEnabled()) {
                                    message += ": " + aMatch;
                                } else {
                                    message += ".";
                                }
                                log.error(message, e);
                            }
                        }
                    } else {
                        // If value is blank, we will deal with that later
                    }
                    break;
                case SCAN:
                    if (parent != null) {
                        // We have a parent, so pull the value from it
                        final List<Scan> childList = matchChildFromParent(parent.getJsonRepresentation(),
                                resolvedValue, "scans", "id", resolvedMatcher, new TypeRef<List<Scan>>(){});
                        if (childList != null && !childList.isEmpty()) {
                            if (log.isDebugEnabled()) {
                                log.debug("Selecting first matching result from list.");
                            }
                            final Scan first = childList.get(0);

                            if (log.isDebugEnabled()) {
                                log.debug("Setting resolvedValue to uri " + first.getUri());
                            }
                            resolvedValue = first.getUri();
                            try {
                                jsonRepresentation = mapper.writeValueAsString(first);
                            } catch (JsonProcessingException e) {
                                log.error("Could not serialize scan to json.", e);
                            }
                        }
                    } else if (StringUtils.isNotBlank(resolvedValue)) {
                        // With no parent, we must have been given the Scan as an archive URI
                        final Scan aMatch;
                        try {
                            aMatch = resolveXnatObjectUri(resolvedValue, resolvedMatcher, Scan.class,
                                    new Function<URIManager.ArchiveItemURI, Scan>() {
                                        @Nullable
                                        @Override
                                        public Scan apply(@Nullable URIManager.ArchiveItemURI uri) {
                                            if (uri != null &&
                                                    ScanURII.class.isAssignableFrom(uri.getClass())) {
                                                return new Scan((ScanURII) uri);
                                            }

                                            return null;
                                        }
                                    },null);
                        } catch (CommandInputResolutionException e) {
                            log.debug(e.getMessage());
                            throw new CommandInputResolutionException(e.getMessage(), input);
                        }

                        if (aMatch != null) {
                            if (log.isDebugEnabled()) {
                                log.debug("Setting resolvedValue to uri " + aMatch.getUri());
                            }
                            resolvedValue = aMatch.getUri();
                            try {
                                jsonRepresentation = mapper.writeValueAsString(aMatch);
                            } catch (JsonProcessingException e) {
                                String message = "Could not serialize scan";
                                if (log.isDebugEnabled()) {
                                    message += ": " + aMatch;
                                } else {
                                    message += ".";
                                }
                                log.error(message, e);
                            }
                        }
                    } else {
                        // If value is blank, we will deal with that later
                    }
                    break;
                case ASSESSOR:
                    if (parent != null) {
                        // We have a parent, so pull the value from it
                        // If we have any value set currently, assume it is an ID

                        final List<Assessor> childList = matchChildFromParent(parent.getJsonRepresentation(),
                                resolvedValue, "assessors", "id", resolvedMatcher, new TypeRef<List<Assessor>>(){});
                        if (childList != null && !childList.isEmpty()) {
                            if (log.isDebugEnabled()) {
                                log.debug("Selecting first matching result from list " + childList);
                            }
                            final Assessor first = childList.get(0);

                            if (log.isDebugEnabled()) {
                                log.debug("Setting resolvedValue to uri " + first.getUri());
                            }
                            resolvedValue = first.getUri();
                            try {
                                jsonRepresentation = mapper.writeValueAsString(first);
                            } catch (JsonProcessingException e) {
                                log.error("Could not serialize assessor to json.", e);
                            }
                        }
                    } else if (StringUtils.isNotBlank(resolvedValue)) {
                        // With no parent, we were either given, A. an archive-style URI, or B. the assessor id
                        final Assessor aMatch;
                        try {
                            aMatch = resolveXnatObjectUri(resolvedValue, resolvedMatcher, Assessor.class,
                                    new Function<URIManager.ArchiveItemURI, Assessor>() {
                                        @Nullable
                                        @Override
                                        public Assessor apply(@Nullable URIManager.ArchiveItemURI uri) {
                                            XnatImageassessordata assessor;
                                            if (uri != null &&
                                                    AssessorURII.class.isAssignableFrom(uri.getClass())) {
                                                assessor = ((AssessorURII) uri).getAssessor();

                                                if (assessor != null &&
                                                        XnatImageassessordata.class.isAssignableFrom(assessor.getClass())) {
                                                    return new Assessor((AssessorURII) uri);
                                                }
                                            }

                                            return null;
                                        }
                                    },
                                    new Function<String, Assessor>() {
                                        @Nullable
                                        @Override
                                        public Assessor apply(@Nullable String s) {
                                            if (StringUtils.isBlank(s)) {
                                                return null;
                                            }
                                            final XnatImageassessordata xnatImageassessordata =
                                                    XnatImageassessordata.getXnatImageassessordatasById(s, userI, true);
                                            if (xnatImageassessordata != null) {
                                                return new Assessor(xnatImageassessordata);
                                            }
                                            return null;
                                        }
                                    });
                        } catch (CommandInputResolutionException e) {
                            throw new CommandInputResolutionException(e.getMessage(), input);
                        }

                        if (aMatch != null) {
                            if (log.isDebugEnabled()) {
                                log.debug("Setting resolvedValue to uri " + aMatch.getUri());
                            }
                            resolvedValue = aMatch.getUri();
                            try {
                                jsonRepresentation = mapper.writeValueAsString(aMatch);
                            } catch (JsonProcessingException e) {
                                String message = "Could not serialize assessor";
                                if (log.isDebugEnabled()) {
                                    message += ": " + aMatch;
                                } else {
                                    message += ".";
                                }
                                log.error(message, e);
                            }
                        }
                    } else {
                        // If value is blank, we will deal with that later
                    }
                    break;
                case CONFIG:
                    final String[] configProps = resolvedValue != null ? resolvedValue.split("/") : null;
                    if (configProps == null || configProps.length != 2) {
                        final String message = "Config inputs must have a value that can be interpreted as a config_toolname/config_filename string. Input value: " + resolvedValue;
                        log.debug(message);
                        throw new CommandInputResolutionException(message, input);
                    }

                    final Scope configScope;
                    final String entityId;
                    final CommandInput.Type parentType = parent == null ? CommandInput.Type.STRING : parent.getType();
                    switch (parentType) {
                        case PROJECT:
                            configScope = Scope.Project;
                            entityId = JsonPath.parse(parent.getJsonRepresentation()).read("$.id");
                            break;
                        case SUBJECT:
                            // Intentional fallthrough
                        case SESSION:
                            // Intentional fallthrough
                        case SCAN:
                            // Intentional fallthrough
                        case ASSESSOR:
                            // TODO Is there any way to make this work? Can we find the project ID for these other input types?
                            //configScope = Scope.Project;
                            //final List<String> projectIds = JsonPath.parse(parent.getJsonRepresentation()).read("$..projectId");
                            //entityId = (projectIds != null && !projectIds.isEmpty()) ? projectIds.get(0) : "";
                            //if (StringUtils.isBlank(entityId)) {
                            //    throw new CommandInputResolutionException("Could not determine project when resolving config value.", input);
                            //}
                            //break;
                            throw new CommandInputResolutionException("Config inputs may only have parents of type Project.", input);
                        default:
                            configScope = Scope.Site;
                            entityId = null;
                    }

                    if (log.isDebugEnabled()) {
                        log.debug(String.format("Attempting to read config %s/%s from %s.", configProps[0], configProps[1],
                                configScope.equals(Scope.Site) ? "site" : "project " + entityId));
                    }
                    final String configContents = configService.getConfigContents(configProps[0], configProps[1], configScope, entityId);
                    if (configContents == null) {
                        throw new CommandInputResolutionException("Could not read config " + resolvedValue, input);
                    }

                    if (log.isDebugEnabled()) {
                        log.debug("Setting resolvedValue to config contents " + configContents);
                    }
                    resolvedValue = configContents;
                    break;
                case RESOURCE:
                    if (parent != null) {
                        // We have a parent, so pull the value from it
                        final List<Resource> childStringList = matchChildFromParent(parent.getJsonRepresentation(),
                                resolvedValue, "resources", "id", resolvedMatcher, new TypeRef<List<Resource>>(){});
                        if (childStringList != null && !childStringList.isEmpty()) {
                            log.debug("Selecting first matching result from list.");
                            final Resource first = childStringList.get(0);

                            if (log.isDebugEnabled()) {
                                log.debug("Setting resolvedValue to uri " + first.getUri());
                            }
                            resolvedValue = first.getUri();
                            try {
                                jsonRepresentation = mapper.writeValueAsString(first);
                            } catch (JsonProcessingException e) {
                                log.error("Could not serialize resource to json.", e);
                            }
                        }
                    } else {
                        throw new CommandInputResolutionException(String.format("Inputs of type \"%s\" must have a parent.", input.getType()), input);
                    }
                    break;
                default:
                    if (parent != null && StringUtils.isNotBlank(input.getParentProperty())) {
                        final String propertyToGetFromParent = resolveJsonpathSubstring(input.getParentProperty());
                        final String parentProperty = JsonPath.parse(parent.getJsonRepresentation()).read("$." + propertyToGetFromParent);

                        if (log.isDebugEnabled()) {
                            log.debug(String.format("Setting resolvedValue, derived from parent property %s, to value %s.",
                                    propertyToGetFromParent, parentProperty));
                        }
                        if (parentProperty != null) {
                            resolvedValue = parentProperty;
                        }
                    }
            }


            // If resolved value is null, and input is required, that is an error
            if (resolvedValue == null && input.isRequired()) {
                final String message = String.format("No value could be resolved for required input \"%s\".", input.getName());
                log.debug(message);
                throw new CommandInputResolutionException(message, input);
            }
            if (log.isInfoEnabled()) {
                log.info(String.format("Done resolving input \"%s\". Value: %s", input.getName(), resolvedValue));
            }
            input.setValue(resolvedValue);
            input.setJsonRepresentation(jsonRepresentation != null ? jsonRepresentation : resolvedValue);

            resolvedInputObjects.put(input.getName(), input);
            resolvedInputValuesByName.put(input.getName(), input.getValue());

            // Only substitute the input into the command line if a replacementKey is set
            final String replacementKey = input.getReplacementKey();
            if (StringUtils.isBlank(replacementKey)) {
                continue;
            }
            resolvedInputValuesByReplacementKey.put(replacementKey, resolvedValue);
            resolvedInputCommandLineValuesByReplacementKey.put(replacementKey, getValueForCommandLine(input, resolvedValue));
        }

        return resolvedInputValuesByName;
    }

    private String commandAsJson() throws CommandResolutionException {
        if (!command.equals(cachedCommand)) {
            cachedCommand = command;

            try {
                commandJson = mapper.writeValueAsString(cachedCommand);
            } catch (JsonProcessingException e) {
                final String message = "Could not serialize command to json.";
                log.debug(message);
                throw new CommandResolutionException(message, e);
            }
        }

        return commandJson;
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
            throws CommandInputResolutionException {
        final String modelName = model.getSimpleName();
        log.info("Resolving " + modelName + " from value.");
        if (log.isDebugEnabled()) {
            log.debug("Value: " + value);
        }

        if (StringUtils.isBlank(value)) {
            final String message = "Not attempting to resolve blank value.";
            log.debug(message);
            throw new CommandInputResolutionException(message, null);
        }

//        List<T> mayOrMayNotMatch = Lists.newArrayList();
        T newModelObject = null;
        if (value.startsWith("/")) {
            if (log.isInfoEnabled()) {
                log.info(String.format("Attempting to create a \"%s\" using value as URI.", modelName));
            }
            if (uriToModelObjectFunction == null) {
                throw new CommandInputResolutionException("ERROR: Cannot instantiate " + modelName + " without a function.", null);
            }

            URIManager.DataURIA uri;
            try {
                uri = UriParserUtils.parseURI(value.startsWith("/archive") ? value : "/archive" + value);
            } catch (MalformedURLException e) {
                throw new CommandInputResolutionException(String.format("Cannot interpret value as a URI: %s.", value), null, e);
            }

            if (uri == null || !(uri instanceof URIManager.ArchiveItemURI)) {
                throw new CommandInputResolutionException(String.format("Cannot interpret value as a URI: %s.", value), null);
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
            throw new CommandInputResolutionException("Could not instantiate " + modelName + " from value.", null);
        }

        String mayOrMayNotMatchJson;
        try {
            mayOrMayNotMatchJson = mapper.writeValueAsString(newModelObject);
        } catch (JsonProcessingException e) {
            throw new CommandInputResolutionException(String.format("Could not serialize object to JSON: %s", newModelObject), null, e);
        }

        if (StringUtils.isBlank(mayOrMayNotMatchJson)) {
            throw new CommandInputResolutionException(String.format("Could not serialize object to JSON: %s", newModelObject), null);
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
                throw new CommandInputResolutionException(String.format("Could not match any \"%s\" with matcher \"%s\".", modelName, matcher), null);
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
        if (command.getOutputs() == null) {
            return null;
        }

        final List<ContainerExecutionOutput> resolvedOutputs = Lists.newArrayList();
        for (final CommandOutput output : command.getOutputs()) {
            if (log.isInfoEnabled()) {
                log.info(String.format("Resolving output \"%s\"", output.getName()));
            }
            if (log.isDebugEnabled()) {
                log.debug(output.toString());
            }
            final ContainerExecutionOutput resolvedOutput = new ContainerExecutionOutput(output);

            final CommandOutputFiles files = output.getFiles();
            // TODO This should be noticed and fixed during command validation
            if (files == null) {
                throw new CommandResolutionException("Command output \"%s\" has no files.");
            }

            resolvedOutput.setPath(resolveTemplate(files.getPath()));

            // TODO Anything else needed to resolve an output?

            if (log.isDebugEnabled()) {
                log.debug(String.format("Adding resolved output \"%s\" to resolved command.", output.getName()));
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

        final String resolvedCommandLine = resolveTemplate(command.getRun() != null ? command.getRun().getCommandLine() : null,
                resolvedInputCommandLineValuesByReplacementKey);

        log.info("Done resolving command-line string.");
        if (log.isDebugEnabled()) {
            log.debug("Command-line string: " + resolvedCommandLine);
        }
        return resolvedCommandLine;
    }

    private Map<String, String> resolveEnvironmentVariables()
            throws CommandResolutionException {
        log.info("Resolving environment variables.");

        final Map<String, String> envTemplates = command.getRun() != null ? command.getRun().getEnvironmentVariables() : null;
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
        log.info("Resolving ports.");

        final Map<String, String> portTemplates = command.getRun() != null ? command.getRun().getPorts() : null;
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

    private List<ContainerExecutionMount> resolveCommandMounts() throws CommandMountResolutionException {
        log.info("Resolving mounts.");
        final List<CommandMount> mountTemplates = command.getRun() != null ? command.getRun().getMounts() : null;
        if (mountTemplates == null || mountTemplates.isEmpty()) {
            log.info("No mounts.");
            return Lists.newArrayList();
        }

        final List<ContainerExecutionMount> resolvedMounts = Lists.newArrayList();
        for (final CommandMount mount : mountTemplates) {
            log.info(String.format("Resolving mount \"%s\".", mount.getName()));
            final ContainerExecutionMount resolvedMount = new ContainerExecutionMount(mount);
            if (mount.isInput()) {
                resolvedMount.setHostPath(resolveCommandMountHostPath(mount));
            }
//                mount.setRemotePath(resolveTemplate(mount.getRemotePath(), resolvedInputs));
            resolvedMounts.add(resolvedMount);
        }

        log.info("Done resolving mounts.");
        if (log.isDebugEnabled()) {
            for (final ContainerExecutionMount mount : resolvedMounts) {
                log.debug(mount.toString());
            }
        }
        return resolvedMounts;
    }

    private String resolveCommandMountHostPath(final CommandMount mount) throws CommandMountResolutionException {
        log.info(String.format("Resolving hostPath for mount \"%s\".", mount.getName()));

        final String hostPath;
        if (StringUtils.isNotBlank(mount.getFileInput())) {

            final CommandInput sourceInput = resolvedInputObjects.get(mount.getFileInput());
            if (sourceInput == null || StringUtils.isBlank(sourceInput.getValue())) {
                final String message = String.format("Cannot resolve mount \"%s\". Source input \"%s\" has no resolved value.", mount.getName(), mount.getFileInput());
                throw new CommandMountResolutionException(message, mount);
            }
            final String sourceInputJson = sourceInput.getJsonRepresentation();
            if (log.isDebugEnabled()) {
                log.debug(String.format("Source input has type \"%s\".", sourceInput.getType()));
            }
            switch (sourceInput.getType()) {
                case RESOURCE:
                    try {
                        final Resource resource = mapper.readValue(sourceInputJson, Resource.class);
                        hostPath = resource.getDirectory();
                    } catch (IOException e) {
                        String message = "Source input is not a Resource.";
                        if (log.isDebugEnabled()) {
                            message += "\ninput: " + sourceInput;
                        }
                        throw new CommandMountResolutionException(message, mount, e);
                    }
                    break;
                case FILE:
                    hostPath = sourceInput.getValue();
                    break;
                case PROJECT:
                    // Intentional fallthrough
                case SUBJECT:
                    // Intentional fallthrough
                case SESSION:
                    // Intentional fallthrough
                case SCAN:
                    // Intentional fallthrough
                case ASSESSOR:
                    if (log.isDebugEnabled()) {
                        log.debug("Looking for child resources on source input.");
                    }
                    final List<Resource> resources = JsonPath.parse(sourceInputJson).read("$.resources[*]", new TypeRef<List<Resource>>(){});
                    if (resources == null || resources.isEmpty()) {
                        throw new CommandMountResolutionException(String.format("Could not find any resources for source input \"%s\".", sourceInput), mount);
                    }

                    if (StringUtils.isBlank(mount.getResource()) || resources.size() == 1) {
                        hostPath = resources.get(0).getDirectory();
                    } else {
                        String directory = null;
                        for (final Resource resource : resources) {
                            if (resource.getLabel().equals(mount.getResource())) {
                                directory = resource.getDirectory();
                                break;
                            }
                        }
                        if (StringUtils.isNotBlank(directory)) {
                            hostPath = directory;
                        } else {
                            throw new CommandMountResolutionException(String.format("Source input \"%s\" has no resource with label \"%s\".", sourceInput.getName(), mount.getResource()), mount);
                        }
                    }

                    break;
                default:
                    throw new CommandMountResolutionException("I don't know how to resolve a mount from an input of type " + sourceInput.getType(), mount);
            }
        } else {
            throw new CommandMountResolutionException("I don't know how to resolve a mount without a source input.", mount);
        }

        if (StringUtils.isBlank(hostPath)) {
            throw new CommandMountResolutionException("Could not resolve command mount host path.", mount);
        }

        log.info("Resolved host path: " + hostPath);
        return hostPath;
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
        if (log.isDebugEnabled()) {
            log.debug("Checking for JSONPath substring in " + stringThatMayContainJsonpathSubstring);
        }
        if (StringUtils.isNotBlank(stringThatMayContainJsonpathSubstring)) {

            final Matcher jsonpathSubstringMatcher = jsonpathSubstringPattern.matcher(stringThatMayContainJsonpathSubstring);

            if (jsonpathSubstringMatcher.find()) {

                final String jsonpathSearchWithMarkers = jsonpathSubstringMatcher.group(0);
                final String jsonpathSearchWithoutMarkers = jsonpathSubstringMatcher.group(1);

                if (log.isDebugEnabled()) {
                    log.debug("Found possible JSONPath substring " + jsonpathSearchWithMarkers);
                }

                if (StringUtils.isNotBlank(jsonpathSearchWithoutMarkers)) {

                    if(log.isInfoEnabled()) {
                        log.info("Performing JSONPath search through command with search string " + jsonpathSearchWithoutMarkers);
                    }

                    final Configuration c = Configuration.defaultConfiguration().addOptions(Option.ALWAYS_RETURN_LIST);
                    final List<String> searchResult = JsonPath.using(c).parse(commandAsJson()).read(jsonpathSearchWithoutMarkers);
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
        }

        log.debug("No jsonpath substring found.");
        return stringThatMayContainJsonpathSubstring;
    }
}