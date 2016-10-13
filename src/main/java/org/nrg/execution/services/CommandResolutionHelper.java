package org.nrg.execution.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.Filter;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.ParseContext;
import com.jayway.jsonpath.TypeRef;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import org.apache.commons.lang3.StringUtils;
import org.nrg.execution.exceptions.CommandInputResolutionException;
import org.nrg.execution.exceptions.CommandMountResolutionException;
import org.nrg.execution.model.Command;
import org.nrg.execution.model.CommandInput;
import org.nrg.execution.model.CommandMount;
import org.nrg.execution.model.CommandRun;
import org.nrg.execution.model.ResolvedCommand;
import org.nrg.execution.model.xnat.Resource;
import org.nrg.execution.model.xnat.Scan;
import org.nrg.execution.model.xnat.Session;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xft.security.UserI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static com.jayway.jsonpath.Criteria.where;
import static com.jayway.jsonpath.Filter.filter;

class CommandResolutionHelper {
    private static final Logger log = LoggerFactory.getLogger(CommandResolutionHelper.class);
    private Command command;
    private LinkedList<CommandInput> notResolvedInputs;
    private Map<String, CommandInput> resolvedInputs;
    private Map<String, String> resolvedInputValues;
    private Map<String, String> resolvedInputValuesAsCommandLineArgs;
    private UserI userI;
    private ObjectMapper mapper;
    private ParseContext jsonPath;
    private Map<String, String> inputValues;

    private CommandResolutionHelper(final Command command,
                                    final Map<String, String> inputValues,
                                    final UserI userI) {
        this.command = command;
        this.resolvedInputs = Maps.newHashMap();
        this.resolvedInputValues = Maps.newHashMap();
        this.resolvedInputValuesAsCommandLineArgs = Maps.newHashMap();
        this.notResolvedInputs = Lists.newLinkedList();
        if (command.getInputs() != null) {
            for (final CommandInput input : command.getInputs()) {
                this.notResolvedInputs.push(input);
            }
        }
//            command.setInputs(Lists.<CommandInput>newArrayList());
        this.userI = userI;
        this.mapper = new ObjectMapper();

//            this.commandJson = JsonPath.parse(command);
        this.inputValues = inputValues == null ?
                Maps.<String, String>newHashMap() :
                inputValues;

        final Configuration configuration = Configuration.builder()
                .jsonProvider(new JacksonJsonProvider())
                .mappingProvider(new JacksonMappingProvider())
                .options(Option.ALWAYS_RETURN_LIST, Option.DEFAULT_PATH_LEAF_TO_NULL)
                .build();
        jsonPath = JsonPath.using(configuration);
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
        while (!notResolvedInputs.isEmpty()) {
            final CommandInput input = notResolvedInputs.pop();
            if (log.isDebugEnabled()) {
                log.debug("Resolving input " + input);
            }

            // If input requires a parent, it must be resolved first
            CommandInput parent = null;
            if (StringUtils.isNotBlank(input.getParent())) {
                if (resolvedInputs.containsKey(input.getParent())) {
                    // Parent has already been resolved. We can continue.
                    parent = resolvedInputs.get(input.getParent());
                } else {
                    // If parent has not been resolved, we...
                    // 1. find it in & remove it from the stack,
                    // 2. push the input back on,
                    // 3. push the parent on top, and
                    // 4. iterate again
                    boolean found = false;
                    for (final CommandInput potentialParent : notResolvedInputs) {
                        if (input.getParent().equals(potentialParent.getName())) {
                            if (StringUtils.isNotBlank(potentialParent.getParent()) && potentialParent.getParent().equalsIgnoreCase(input.getName())) {
                                final String message = String.format("Circular parent reference: input %s has parent %s, which has parent %s.",
                                        input.getName(), potentialParent.getName(), input.getName());
                                throw new CommandInputResolutionException(message, input);
                            }
                            notResolvedInputs.remove(potentialParent);
                            notResolvedInputs.push(input);
                            notResolvedInputs.push(potentialParent);
                            found = true;
                            break;
                        }
                    }

                    if (!found) {
                        final String message = String.format(
                                "Input %s requires parent %s, but parent was not found.",
                                input.getName(), input.getParent()
                        );
                        throw new CommandInputResolutionException(message, input);
                    } else {
                        if (log.isDebugEnabled()) {
                            log.debug(String.format("Input %s requires parent %s. Must resolve parent first.", input.getName(), input.getParent()));
                        }
                        continue;
                    }
                }
            }

            // Give the input its default value
            String resolvedValue = input.getDefaultValue();

            // If a value was provided at runtime, use that over the default
            if (inputValues.containsKey(input.getName()) && inputValues.get(input.getName()) != null) {
                resolvedValue = inputValues.get(input.getName());
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
                    if (parent == null) {
                        throw new CommandInputResolutionException(String.format("Inputs of type %s must have a parent.", input.getType()), input);
                    } else {
                        final List<Filter> filters = Lists.newArrayList();
                        String jsonPath = StringUtils.isNotBlank(input.getParentProperty()) ?
                                input.getParentProperty() :
                                "$.files";

                        if (StringUtils.isNotBlank(resolvedValue)) {
                            if (resolvedValue.startsWith("?")) {
                                // Allow the user to send in a jsonpath filter
                                jsonPath = jsonPath + "[" + resolvedValue + "]";
                            } else {
                                // TODO What would a user specify about the file? Name?
                            }
                        }

                        if (parent.getValue().startsWith("[")) {
                            jsonPath = jsonPath.replaceFirst("$", "$[*]");
                        }

                        resolvedValue = JsonPath.read(parent.getValue(), jsonPath, filters.toArray(new Filter[filters.size()]));
                    }
                    break;
                case PROJECT:
                    // TODO
                    break;
                case SUBJECT:
                    // TODO
                    break;
                case SESSION:
                    if (parent == null) {
                        // With no parent, we were either given, A. a Session in json, B. a list of Sessions in json, or C. the session id
                        Session session = null;
                        if (resolvedValue.startsWith("{")) {
                            try {
                                session = mapper.readValue(resolvedValue, Session.class);
                            } catch (IOException e) {
                                log.info(String.format("Could not deserialize %s into a Session object.", resolvedValue), e);
                            }
                        } else if (resolvedValue.matches("^\\[\\s*\\{")) {
                            try {
                                final List<Session> sessions = mapper.readValue(resolvedValue, new TypeReference<List<Session>>(){});
                                session = sessions.get(0);
                                log.warn(String.format("Cannot implicitly loop over Session objects. Selecting first session (%s) from list of sessions (%s).", session, resolvedValue));
                            } catch (IOException e) {
                                log.info(String.format("Could not deserialize %s into a list of Session objects.", resolvedValue), e);
                            }
                        } else {

                            final XnatImagesessiondata imagesessiondata = XnatImagesessiondata.getXnatImagesessiondatasById(resolvedValue, userI, true);
                            if (imagesessiondata == null) {
                                log.info("Could not instantiate image session from id " + resolvedValue);
                            }
                            session = new Session(imagesessiondata, userI);
                        }

                        if (session == null) {
                            throw new CommandInputResolutionException("Could not instantiate Session from value " + resolvedValue, input);
                        }

                        try {
                            resolvedValue = mapper.writeValueAsString(session);
                        } catch (JsonProcessingException e) {
                            log.error("Could not serialize session: " + session, e);
                        }
                    } else {
                        final List<Filter> filters = Lists.newArrayList();
                        String jsonPath = StringUtils.isNotBlank(input.getParentProperty()) ?
                                input.getParentProperty() :
                                "$.sessions";

                        if (StringUtils.isNotBlank(resolvedValue)) {
                            if (resolvedValue.startsWith("?")) {
                                // Allow the user to send in a jsonpath filter
                                jsonPath = jsonPath + "[" + resolvedValue + "]";
                            } else {
                                // Otherwise assume the value we were given is an id or a label
                                jsonPath = jsonPath + "[?]";
                                filters.add(filter(where("id").is(resolvedValue)).or(where("label").is(resolvedValue)));
                            }
                        }

                        if (parent.getValue().startsWith("[")) {
                            jsonPath = jsonPath.replaceFirst("$", "$[*]");
                        }

                        resolvedValue = JsonPath.read(parent.getValue(), jsonPath, filters.toArray(new Filter[filters.size()]));
                    }
                    break;
                case SCAN:
                    if (parent == null) {
                        // With no parent, we must have been given the Scan as json
                        Scan scan = null;
                        if (resolvedValue.startsWith("{")) {
                            try {
                                scan = mapper.readValue(resolvedValue, Scan.class);
                            } catch (IOException e) {
                                log.info(String.format("Could not deserialize %s into a Scan object.", resolvedValue), e);
                            }
                        } else if (resolvedValue.matches("^\\[\\s*\\{")) {
                            try {
                                final List<Scan> scans = mapper.readValue(resolvedValue, new TypeReference<List<Scan>>(){});
                                scan = scans.get(0);
                                log.warn(String.format("Cannot implicitly loop over Scan objects. Selecting first scan (%s) from list of scans (%s).", scan, resolvedValue));
                            } catch (IOException e) {
                                log.info(String.format("Could not deserialize %s into a list of Scan objects.", resolvedValue), e);
                            }
                        }

                        if (scan == null) {
                            throw new CommandInputResolutionException("Could not instantiate Scan from value " + resolvedValue, input);
                        }

                        try {
                            resolvedValue = mapper.writeValueAsString(scan);
                        } catch (JsonProcessingException e) {
                            log.error("Could not serialize session: " + scan, e);
                        }
                    } else {
                        final List<Filter> filters = Lists.newArrayList();
                        String jsonPathSearch = StringUtils.isNotBlank(input.getParentProperty()) ?
                                input.getParentProperty() :
                                "$.scans[*]";

                        if (StringUtils.isNotBlank(resolvedValue)) {
                            if (resolvedValue.startsWith("?")) {
                                // Allow the user to send in a jsonpath filter
                                jsonPathSearch = jsonPathSearch + "[" + resolvedValue + "]";
                            } else {
                                // Otherwise assume the value we were given is an id or a label
                                jsonPathSearch = jsonPathSearch + "[?]";
                                filters.add(filter(where("id").is(resolvedValue)));
                            }
                        }

                        if (parent.getValue().startsWith("[")) {
                            jsonPathSearch = jsonPathSearch.replaceFirst("$", "$[*]");
                        }

                        resolvedValue = jsonPath.parse(parent.getValue()).read(jsonPathSearch, filters.toArray(new Filter[filters.size()]));
                    }
                    break;
                case ASSESSOR:
                    // TODO
                    break;
                case CONFIG:
                    // TODO
                    break;
                case RESOURCE:
                    // TODO
                    break;
                default:
                    // TODO
            }


            // If resolved value is null, and input is required, that is an error
            if (resolvedValue == null && input.isRequired()) {
                final String message = String.format("Input \"%s\" has no provided or default value, but is required.", input.getName());
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