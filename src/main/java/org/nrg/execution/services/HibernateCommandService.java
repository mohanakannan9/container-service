package org.nrg.execution.services;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.nrg.execution.daos.CommandDao;
import org.nrg.execution.model.Command;
import org.nrg.execution.model.CommandVariable;
import org.nrg.execution.model.ResolvedCommand;
import org.nrg.execution.api.ContainerControlApi;
import org.nrg.containers.exceptions.DockerServerException;
import org.nrg.containers.exceptions.NoServerPrefException;
import org.nrg.containers.exceptions.NotFoundException;
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


    @Override
    public ResolvedCommand resolveCommand(final Long commandId) throws NotFoundException {
        return resolveCommand(commandId, Maps.<String, String>newHashMap());
    }

    @Override
    public ResolvedCommand resolveCommand(final Long commandId,
                                          final Map<String, String> variableRuntimeValues) throws NotFoundException {
        final Command command = retrieve(commandId);
        if (command == null) {
            throw new NotFoundException("Could not find Command with id " + commandId);
        }
        return resolveCommand(command, variableRuntimeValues);
    }

    @Override
    public ResolvedCommand resolveCommand(final Command command) throws NotFoundException {
        return resolveCommand(command, Maps.<String, String>newHashMap());
    }

    @Override
    public ResolvedCommand resolveCommand(final Command command,
                                          final Map<String, String> variableRuntimeValues) throws NotFoundException {

        if (variableRuntimeValues == null) {
            return resolveCommand(command);
        }

        final ResolvedCommand resolvedCommand = new ResolvedCommand();
        resolvedCommand.setCommandId(command.getId());
        resolvedCommand.setDockerImageId(command.getDockerImage());

        // Replace variable names in runTemplate, mounts, and environment variables
        final Map<String, String> variableValues = Maps.newHashMap();
        final Map<String, String> variableArgTemplateValues = Maps.newHashMap();
        if (command.getVariables() != null) {
            for (final CommandVariable variable : command.getVariables()) {
                if (variableRuntimeValues.containsKey(variable.getName())) {
                    variable.setDefaultValue(variableRuntimeValues.get(variable.getName()));
                }
                variableValues.put(variable.getName(), variable.getValueWithTrueOrFalseValue());
                variableArgTemplateValues.put(variable.getName(), variable.getArgTemplateValue());
            }
        }


        resolvedCommand.setRun(resolveTemplate(command.getRunTemplate(), variableArgTemplateValues));

        resolvedCommand.setMountsIn(resolveMounts(command.getMountsIn(), variableValues));
        resolvedCommand.setMountsOut(resolveMounts(command.getMountsOut(), variableValues));

        if (command.getEnvironmentVariables() != null) {
            final Map<String, String> resolvedEnv = Maps.newHashMap();
            for (Map.Entry<String, String> env : command.getEnvironmentVariables().entrySet()) {
                resolvedEnv.put(resolveTemplate(env.getKey(), variableValues),
                        resolveTemplate(env.getValue(), variableValues));
            }
            resolvedCommand.setEnvironmentVariables(resolvedEnv);
        }

        // TODO What else do I need to do to resolve the command?

        return resolvedCommand;
    }

    private Map<String, String> resolveMounts(final Map<String, String> mounts,
                                              final Map<String, String> variableValues) {
        if (mounts == null) {
            return null;
        }

        final Map<String, String> resolvedMounts = Maps.newHashMap();
        for (final Map.Entry<String, String> mount : mounts.entrySet()) {
            resolvedMounts.put(mount.getKey(),
                    resolveTemplate(mount.getValue(), variableValues));
        }
        return resolvedMounts;
    }

    @Override
    public String launchCommand(final ResolvedCommand resolvedCommand)
            throws NoServerPrefException, DockerServerException {
        return controlApi.launchImage(resolvedCommand);
    }

    @Override
    public String launchCommand(final Long commandId)
            throws NoServerPrefException, DockerServerException, NotFoundException {
        return launchCommand(commandId, Maps.<String, String>newHashMap());
    }

    @Override
    public String launchCommand(final Long commandId, final Map<String, String> variableRuntimeValues)
            throws NoServerPrefException, DockerServerException, NotFoundException {
        final ResolvedCommand resolvedCommand = resolveCommand(commandId, variableRuntimeValues);
        return controlApi.launchImage(resolvedCommand);
    }

    private List<String> resolveTemplate(final List<String> template,
                                         final Map<String, String> variableArgTemplateValues) {
        return Lists.transform(template, new Function<String, String>() {
            @Nullable
            @Override
            public String apply(@Nullable final String input) {
                return resolveTemplate(input, variableArgTemplateValues);
            }
        });
    }

    private String resolveTemplate(final String template,
                                   final Map<String, String> variableArgTemplateValues) {
        String toResolve = template;
        final Set<String> matches = Sets.newHashSet();

        final Pattern p = Pattern.compile("(?<=\\s|\\A)#(\\w+)#(?=\\s|\\z)"); // Match #varname#
        final Matcher m = p.matcher(toResolve);
        while (m.find()) {
            matches.add(m.group(1));
        }

        for (final String match : matches) {
            if (variableArgTemplateValues.containsKey(match) && variableArgTemplateValues.get(match) != null) {
                toResolve = toResolve.replaceAll("#"+ match +"#", variableArgTemplateValues.get(match));
            }
        }
        return toResolve;
    }
}
