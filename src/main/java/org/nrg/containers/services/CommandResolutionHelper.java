package org.nrg.containers.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.ParseContext;
import com.jayway.jsonpath.TypeRef;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import org.apache.commons.lang3.StringUtils;
import org.nrg.config.services.ConfigService;
import org.nrg.containers.exceptions.CommandInputResolutionException;
import org.nrg.containers.exceptions.CommandMountResolutionException;
import org.nrg.containers.model.Command;
import org.nrg.containers.model.CommandInput;
import org.nrg.containers.model.CommandMount;
import org.nrg.containers.model.CommandRun;
import org.nrg.containers.model.ResolvedCommand;
import org.nrg.containers.model.xnat.Resource;
import org.nrg.containers.model.xnat.Scan;
import org.nrg.containers.model.xnat.Session;
import org.nrg.framework.constants.Scope;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xft.security.UserI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.jayway.jsonpath.Criteria.where;
import static com.jayway.jsonpath.Filter.filter;

class CommandResolutionHelper {
    private static final Logger log = LoggerFactory.getLogger(CommandResolutionHelper.class);
    private static final String JSONPATH_SUBSTRING_REGEX = "\\^(.+)\\^";

    private Command command;
    private Map<String, CommandInput> resolvedInputs;
    private Map<String, String> resolvedInputValues;
    private Map<String, String> resolvedInputValuesAsCommandLineArgs;
    private UserI userI;
    private ObjectMapper mapper;
    private ParseContext jsonPath;
    private Map<String, String> inputValues;
    private ConfigService configService;
    private Pattern jsonpathSubstringPattern;

    private CommandResolutionHelper(final Command command,
                                    final Map<String, String> inputValues,
                                    final UserI userI) {
        this.command = command;
        this.resolvedInputs = Maps.newHashMap();
        this.resolvedInputValues = Maps.newHashMap();
        this.resolvedInputValuesAsCommandLineArgs = Maps.newHashMap();
//            command.setInputs(Lists.<CommandInput>newArrayList());
        this.userI = userI;
        this.configService = XDAT.getConfigService();
        this.mapper = new ObjectMapper();
        this.inputValues = inputValues == null ?
                Maps.<String, String>newHashMap() :
                inputValues;

        final Configuration configuration = Configuration.builder()
                .jsonProvider(new JacksonJsonProvider())
                .mappingProvider(new JacksonMappingProvider())
                .options(Option.ALWAYS_RETURN_LIST, Option.DEFAULT_PATH_LEAF_TO_NULL)
                .build();
        jsonPath = JsonPath.using(configuration);
        jsonpathSubstringPattern = Pattern.compile(JSONPATH_SUBSTRING_REGEX);
    }

    public static ResolvedCommand resolve(final Command command, final UserI userI)
            throws CommandInputResolutionException, CommandMountResolutionException {
        return resolve(command, null, userI);
    }

    public static ResolvedCommand resolve(final Command command,
                                          final Map<String, String> inputValues,
                                          final UserI userI)
            throws CommandInputResolutionException, CommandMountResolutionException {
        final CommandResolutionHelper helper = new CommandResolutionHelper(command, inputValues, userI);
        return helper.resolve();
    }

    private ResolvedCommand resolve() throws CommandInputResolutionException, CommandMountResolutionException {


        resolveInputs();

        // Replace variable names in command line, mounts, and environment variables
        final ResolvedCommand resolvedCommand = new ResolvedCommand(command);
        final CommandRun run = command.getRun();
        resolvedCommand.setCommandLine(resolveTemplate(run.getCommandLine(), resolvedInputValuesAsCommandLineArgs));
        resolvedCommand.setMounts(resolveCommandMounts());
        resolvedCommand.setEnvironmentVariables(resolveTemplateMap(run.getEnvironmentVariables(), resolvedInputValues, true));

        // TODO What else do I need to do to resolve the command?

        return resolvedCommand;
    }

    private void resolveInputs() throws CommandInputResolutionException {
        if (command.getInputs() == null) {
            return;
        }

        for (final CommandInput input : command.getInputs()) {
            if (log.isDebugEnabled()) {
                log.debug("Resolving input " + input);
            }

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
                if (!resolvedInputs.containsKey(prereq)) {
                    final String message = String.format(
                            "Input %1$s has prerequisite %2$s which has not been resolved. Re-order inputs so %1$s appears after %2$s.",
                            input.getName(), prereq
                    );
                    throw new CommandInputResolutionException(message, input);
                }
            }

            // If input requires a parent, it must be resolved first
            CommandInput parent = null;
            if (StringUtils.isNotBlank(input.getParent())) {
                if (resolvedInputs.containsKey(input.getParent())) {
                    // Parent has already been resolved. We can continue.
                    parent = resolvedInputs.get(input.getParent());
                } else {
                    // This exception should have been thrown already above, but just in case it wasn't...
                    final String message = String.format(
                            "Input %1$s has prerequisite %2$s which has not been resolved. Re-order inputs so %1$s appears after %2$s.",
                            input.getName(), input.getParent()
                    );
                    throw new CommandInputResolutionException(message, input);
                }
            }

            // Give the input its default value
            String resolvedValue = resolveJsonpathSubstring(input.getDefaultValue());

            // If a value was provided at runtime, use that over the default
            if (inputValues.containsKey(input.getName()) && inputValues.get(input.getName()) != null) {
                resolvedValue = inputValues.get(input.getName());
            }

            final String resolvedMatcher = resolveJsonpathSubstring(input.getMatcher());

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
                        final String childString = matchChildFromParent(parent.getValue(), resolvedValue, "files", "name", resolvedMatcher);
                        if (childString != null) {
                            resolvedValue = childString;
                        }
                    } else {
                        throw new CommandInputResolutionException(String.format("Inputs of type %s must have a parent.", input.getType()), input);
                    }
                    break;
                case PROJECT:
                    // TODO
                    break;
                case SUBJECT:
                    // TODO
                    break;
                case SESSION:
                    if (parent != null) {
                        // We have a parent, so pull the value from it
                        // If we have any value set currently, assume it is an ID

                        final String childString = matchChildFromParent(parent.getValue(), resolvedValue, "sessions", "id", resolvedMatcher);
                        if (childString != null) {
                            resolvedValue = childString;
                        }
                    } else {
                        // With no parent, we were either given, A. a Session in json, B. a list of Sessions in json, or C. the session id
                        List<Session> mayOrMayNotMatch = Lists.newArrayList();
                        if (resolvedValue.startsWith("{")) {
                            try {
                                final Session session = mapper.readValue(resolvedValue, Session.class);
                                mayOrMayNotMatch.add(session);
                            } catch (IOException e) {
                                log.info(String.format("Could not deserialize %s into a Session object.", resolvedValue), e);
                            }
                        } else if (resolvedValue.matches("^\\[\\s*\\{")) {
                            try {
                                mayOrMayNotMatch = mapper.readValue(resolvedValue, new TypeReference<List<Session>>(){});
                            } catch (IOException e) {
                                log.info(String.format("Could not deserialize %s into a list of Session objects.", resolvedValue), e);
                            }
                        } else {
                            final XnatImagesessiondata imagesessiondata = XnatImagesessiondata.getXnatImagesessiondatasById(resolvedValue, userI, true);
                            if (imagesessiondata == null) {
                                log.info("Could not instantiate image session from id " + resolvedValue);
                            }
                            mayOrMayNotMatch.add(new Session(imagesessiondata, userI));
                        }

                        if (mayOrMayNotMatch == null || mayOrMayNotMatch.isEmpty()) {
                            throw new CommandInputResolutionException("Could not instantiate Session from value " + resolvedValue, input);
                        }

                        final List<Session> doMatch;
                        if (StringUtils.isNotBlank(input.getMatcher())) {
                            final String jsonPathSearch = String.format(
                                    "$[?(%s)]", resolvedMatcher
                            );
                            doMatch = jsonPath.parse(mayOrMayNotMatch).read(jsonPathSearch, new TypeRef<List<Session>>(){});

                            if (doMatch == null || doMatch.isEmpty()) {
                                throw new CommandInputResolutionException("Could not match any sessions with matcher " + resolvedMatcher, input);
                            }
                        } else {
                            doMatch = mayOrMayNotMatch;
                        }

                        final Session session = doMatch.get(0);
                        if (doMatch.size() > 1) {
                            log.warn(String.format("Cannot implicitly loop over Session objects. Selecting first session (%s) from list of sessions (%s).", session, resolvedValue));
                        }
                        try {
                            resolvedValue = mapper.writeValueAsString(session);
                        } catch (JsonProcessingException e) {
                            log.error("Could not serialize session: " + session, e);
                        }
                    }
                    break;
                case SCAN:
                    if (parent != null) {
                        // We have a parent, so pull the value from it
                        final String childString = matchChildFromParent(parent.getValue(), resolvedValue, "scans", "id", resolvedMatcher);
                        if (childString != null) {
                            resolvedValue = childString;
                        }
                    } else {
                        // With no parent, we must have been given the Scan as json
                        List<Scan> mayOrMayNotMatch = Lists.newArrayList();
                        if (resolvedValue.startsWith("{")) {
                            try {
                                if (log.isDebugEnabled()) {
                                    log.debug("Attempting to deserialize value %s as a Scan.", resolvedValue);
                                }
                                final Scan scan = mapper.readValue(resolvedValue, Scan.class);
                                mayOrMayNotMatch.add(scan);
                            } catch (IOException e) {
                                log.info(String.format("Could not deserialize %s into a Scan object.", resolvedValue), e);
                            }
                        } else if (resolvedValue.matches("^\\[\\s*\\{")) {
                            try {
                                if (log.isDebugEnabled()) {
                                    log.debug("Attempting to deserialize value %s as a list of Scans.", resolvedValue);
                                }
                                mayOrMayNotMatch = mapper.readValue(resolvedValue, new TypeReference<List<Scan>>(){});
                            } catch (IOException e) {
                                log.error(String.format("Could not deserialize %s into a list of Scan objects.", resolvedValue), e);
                            }
                        }

                        if (mayOrMayNotMatch == null || mayOrMayNotMatch.isEmpty()) {
                            throw new CommandInputResolutionException("Could not instantiate Scan from value " + resolvedValue, input);
                        }

                        final List<Scan> doMatch;
                        if (StringUtils.isNotBlank(resolvedMatcher)) {
                            final String jsonPathSearch = String.format(
                                    "$[?(%s)]", resolvedMatcher
                            );
                            doMatch = jsonPath.parse(mayOrMayNotMatch).read(jsonPathSearch, new TypeRef<List<Scan>>(){});

                            if (doMatch == null || doMatch.isEmpty()) {
                                throw new CommandInputResolutionException("Could not match any Scans with matcher " + resolvedMatcher, input);
                            }
                        } else {
                            doMatch = mayOrMayNotMatch;
                        }

                        final Scan scan = doMatch.get(0);
                        if (doMatch.size() > 1) {
                            log.warn(String.format("Refusing to implicitly loop over Scan objects. Selecting first scan (%s) from list (%s).", scan, resolvedValue));
                        }
                        try {
                            resolvedValue = mapper.writeValueAsString(scan);
                        } catch (JsonProcessingException e) {
                            log.error("Could not serialize scan: " + scan, e);
                        }
                    }
                    break;
                case ASSESSOR:
                    // TODO
                    break;
                case CONFIG:
                    final String[] configProps = input.getValue() != null ?
                            resolvedValue.split("/") :
                            null;

                    if (configProps == null || configProps.length != 2) {
                        throw new CommandInputResolutionException("Config inputs must have a value that can be interpreted as a config_toolname/config_filename string.", input);
                    }

                    final Scope configScope;
                    final String entityId;
                    final CommandInput.Type parentType = parent == null ? CommandInput.Type.STRING : parent.getType();
                    switch (parentType) {
                        case PROJECT:
                            configScope = Scope.Project;
                            entityId = jsonPath.parse(parent.getValue()).read("$.id");
                            break;
                        case SUBJECT:
                        case SESSION:
                        case SCAN:
                        case ASSESSOR:
                            // TODO This probably will not work. Figure out a way to get the project ID from these, or simply throw an error.
                            configScope = Scope.Project;
                            entityId = jsonPath.parse(parent.getValue()).read("$..projectId");
                            if (StringUtils.isBlank(entityId)) {
                                throw new CommandInputResolutionException("Could not determine project when resolving config value.", input);
                            }
                            break;
                        default:
                            configScope = Scope.Site;
                            entityId = null;
                    }


                    final org.nrg.config.entities.Configuration config = configService.getConfig(configProps[0], configProps[1], configScope, entityId);
                    if (config == null || config.getContents() == null) {
                        throw new CommandInputResolutionException("Could not read config " + resolvedValue, input);
                    }

                    resolvedValue = config.getContents();
                    break;
                case RESOURCE:
                    // TODO
                    break;
                default:
                    // TODO
            }


            // If resolved value is null, and input is required, that is an error
            if (resolvedValue == null && input.isRequired()) {
                final String message = String.format("No value could be resolved for required input \"%s\".", input.getName());
                throw new CommandInputResolutionException(message, input);
            }
            input.setValue(resolvedValue);

            resolvedInputs.put(input.getName(), input);

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

    private String matchChildFromParent(final String parentValue, final String value, final String childKey, final String valueMatchProperty, final String matcherFromInput) {
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
            fullMatcher = "*";
        }
        return getChildFromParent(parentValue, childKey, fullMatcher);
    }

    private String resolveJsonpathSubstring(final String stringThatMayContainJsonpathSubstring) {
        if (log.isDebugEnabled()) log.debug("Checking for jsonpath substring in " + stringThatMayContainJsonpathSubstring);
        if (StringUtils.isNotBlank(stringThatMayContainJsonpathSubstring)) {

            final Matcher jsonpathSubstringMatcher = jsonpathSubstringPattern.matcher(stringThatMayContainJsonpathSubstring);

            if (jsonpathSubstringMatcher.find()) {

                final String jsonpathSearchWithMarkers = jsonpathSubstringMatcher.group(0);
                final String jsonpathSearchWithoutMarkers = jsonpathSubstringMatcher.group(1);

                if (log.isDebugEnabled()) log.debug("Found possible jsonpath substring " + jsonpathSearchWithMarkers);

                if (StringUtils.isNotBlank(jsonpathSearchWithoutMarkers)) {

                    if(log.isInfoEnabled()) log.info("Performing jsonpath search through command with search string " + jsonpathSearchWithoutMarkers);

                    final String searchResult = jsonPath.parse(command).read(jsonpathSearchWithoutMarkers);
                    if (searchResult != null) {
                        if (log.isInfoEnabled()) log.info("Result: " + searchResult);
                        return stringThatMayContainJsonpathSubstring.replaceFirst(jsonpathSearchWithMarkers, searchResult);
                    } else {
                        if (log.isInfoEnabled()) log.debug("No result");
                    }
                }
            }
        }
        if (log.isDebugEnabled()) log.debug("Returning initial string without any replacement.");
        return stringThatMayContainJsonpathSubstring;
    }

    private String getValueFromParent(final String parent, final String parentProperty, final String rootValueType, final String currentResolvedValue) {
        // We have a parent, and we need to get the scan out of it.
        // We currently have implemented two possibilities:
        // 1. The user has set 'parentProperty', and we interpret that as a jsonpath search string
        // 2. The user has sent in the scan id as the value

        final String jsonPathSearch = StringUtils.isNotBlank(parentProperty) ?
                parentProperty :
                String.format("$.%s[?(@.id == '%s')]", rootValueType, currentResolvedValue);
        return jsonPath.parse(parent).read(jsonPathSearch);
    }

    private String getChildFromParent(final String parentJson, final String childKey, final String matcher) {
        final String jsonPathSearch = String.format(
                "$.%s[%s]",
                childKey,
                StringUtils.isNotBlank(matcher) ? "?(" + matcher + ")" : "*"
        );
        return jsonPath.parse(parentJson).read(jsonPathSearch);
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

    private List<CommandMount> resolveCommandMounts() throws CommandMountResolutionException {
        if (command.getRun() == null || command.getRun().getMounts() == null) {
            return Lists.newArrayList();
        }

        final List<CommandMount> commandMounts = Lists.newArrayList();
        for (final CommandMount mount : command.getRun().getMounts()) {
            if (mount.isInput()) {
                mount.setHostPath(resolveCommandMountHostPath(mount));
            }
//                mount.setRemotePath(resolveTemplate(mount.getRemotePath(), resolvedInputs));
            commandMounts.add(mount);
        }

        return commandMounts;
    }

    private String resolveCommandMountHostPath(final CommandMount mount) throws CommandMountResolutionException {
        String hostPath = "";
        if (StringUtils.isNotBlank(mount.getFileInput())) {
            if (resolvedInputs.containsKey(mount.getFileInput())) {
                final CommandInput source = resolvedInputs.get(mount.getFileInput());
                switch (source.getType()) {
                    case RESOURCE:
                        try {
                            final Resource resource = mapper.readValue(source.getValue(), Resource.class);
                            hostPath = resource.getDirectory();
                        } catch (IOException e) {
                            throw new CommandMountResolutionException(String.format("Could not get resource from parent %s", source), mount, e);
                        }
                        break;
                    case FILE:
                        hostPath = source.getValue();
                        break;
                    case PROJECT:
                    case SUBJECT:
                    case SESSION:
                    case SCAN:
                    case ASSESSOR:
                        final List<Resource> resources = jsonPath.parse(source.getValue()).read("$.resources[*]", new TypeRef<List<Resource>>(){});
                        if (resources == null || resources.isEmpty()) {
                            throw new CommandMountResolutionException(String.format("Could not find any resources for parent %s", source), mount);
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
                                throw new CommandMountResolutionException(String.format("Parent %s has no resource with name %s", source.getName(), mount.getResource()), mount);
                            }
                        }

                        break;
                    default:
                        throw new CommandMountResolutionException("I don't know how to resolve a mount from an input of type " + source.getType(), mount);
                }
            }
        } else {
            throw new CommandMountResolutionException("I don't know how to resolve a mount without a parent.", mount);
        }

        if (StringUtils.isBlank(hostPath)) {
            throw new CommandMountResolutionException("Could not resolve command mount host path.", mount);
        }

        return hostPath;
    }
}