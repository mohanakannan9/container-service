package org.nrg.execution.services;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.exception.ConstraintViolationException;
import org.nrg.containers.exceptions.CommandVariableResolutionException;
import org.nrg.execution.daos.CommandDao;
import org.nrg.execution.model.Command;
import org.nrg.execution.model.CommandVariable;
import org.nrg.execution.model.ResolvedCommand;
import org.nrg.execution.api.ContainerControlApi;
import org.nrg.containers.exceptions.DockerServerException;
import org.nrg.containers.exceptions.NoServerPrefException;
import org.nrg.containers.exceptions.NotFoundException;
import org.nrg.framework.exceptions.NrgRuntimeException;
import org.nrg.framework.exceptions.NrgServiceRuntimeException;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Transactional
public class HibernateCommandService extends AbstractHibernateEntityService<Command, CommandDao>
        implements CommandService {
    @Autowired
    private ContainerControlApi controlApi;

    private static final Pattern TEMPLATE_PATTERN = Pattern.compile("(?<=\\s|\\A)#(\\w+)#(?=\\s|\\z)"); // Match #varname#

    @Override
    public Command create(final Command command) throws NrgRuntimeException {
        try {
            return super.create(command);
        } catch (ConstraintViolationException e) {
            throw new NrgServiceRuntimeException("A command already exists with this name and docker image ID.");
        }
    }

    public List<Command> findByName(final String name) {
        return getDao().findByName(name);
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
                final String variableRawValueCouldBeNull =
                        variableValuesProvidedAtRuntime.containsKey(variable.getName()) ?
                                variableValuesProvidedAtRuntime.get(variable.getName()) :
                                variable.getDefaultValue();
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
        resolvedCommand.setMountsIn(resolveTemplateMap(command.getMountsIn(), resolvedVariableValues, false));
        resolvedCommand.setMountsOut(resolveTemplateMap(command.getMountsOut(), resolvedVariableValues, false));
        resolvedCommand.setEnvironmentVariables(resolveTemplateMap(command.getEnvironmentVariables(), resolvedVariableValues, true));

        // TODO What else do I need to do to resolve the command?

        return resolvedCommand;
    }

    @Override
    public String launchCommand(final ResolvedCommand resolvedCommand)
            throws NoServerPrefException, DockerServerException {
        return controlApi.launchImage(resolvedCommand);
    }

    @Override
    public String launchCommand(final Long commandId)
            throws NoServerPrefException, DockerServerException, NotFoundException, CommandVariableResolutionException {
        return launchCommand(commandId, Maps.<String, String>newHashMap());
    }

    @Override
    public String launchCommand(final Long commandId, final Map<String, String> variableRuntimeValues)
            throws NoServerPrefException, DockerServerException, NotFoundException, CommandVariableResolutionException {
        final ResolvedCommand resolvedCommand = resolveCommand(commandId, variableRuntimeValues);
        return controlApi.launchImage(resolvedCommand);
    }

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
}
