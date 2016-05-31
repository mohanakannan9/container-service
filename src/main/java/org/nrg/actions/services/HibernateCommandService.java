package org.nrg.actions.services;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.nrg.actions.daos.CommandDao;
import org.nrg.actions.model.Command;
import org.nrg.actions.model.CommandMount;
import org.nrg.actions.model.CommandVariable;
import org.nrg.actions.model.ResolvedCommand;
import org.nrg.actions.model.ScriptCommand;
import org.nrg.actions.model.ScriptEnvironment;
import org.nrg.containers.exceptions.NotFoundException;
import org.nrg.containers.model.DockerImage;
import org.nrg.containers.model.DockerImageCommand;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntityService;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class HibernateCommandService extends AbstractHibernateEntityService<Command, CommandDao>
        implements CommandService {
    @Override
    public ResolvedCommand resolveCommand(final Long id,
                                          final Map<String, String> variableRuntimeValues) throws NotFoundException {
        final Command command = retrieve(id);
        if (command == null) {
            throw new NotFoundException("Could not find Command with id " + id);
        }

        final ResolvedCommand resolvedCommand = new ResolvedCommand(command);

        final DockerImage dockerImage;
        if (DockerImageCommand.class.isAssignableFrom(command.getClass())) {
            final DockerImageCommand dockerImageCommand = (DockerImageCommand) command;
            dockerImage = dockerImageCommand.getDockerImage();
        } else if (ScriptCommand.class.isAssignableFrom(command.getClass())) {
            final ScriptCommand scriptCommand = (ScriptCommand) command;
            final ScriptEnvironment scriptEnvironment = scriptCommand.getScriptEnvironment();
            dockerImage = scriptEnvironment.getDockerImage();
        } else {
            // TODO There are no other kinds of command. How did we get here?
            throw new NotFoundException("Cannot find docker image id for command " + id);
        }
        resolvedCommand.setDockerImage(dockerImage);

        // Replace variable names in runTemplate, mounts, and environment variables
        final Map<String, String> variableValues = Maps.newHashMap();
        final Map<String, String> variableArgTemplateValues = Maps.newHashMap();
        if (command.getVariables() != null) {
            for (final CommandVariable variable : command.getVariables()) {
                if (variableRuntimeValues.containsKey(variable.getName())) {
                    variable.setValue(variableRuntimeValues.get(variable.getName()));
                }
                variableValues.put(variable.getName(), variable.getValueWithTrueOrFalseValue());
                variableArgTemplateValues.put(variable.getName(), variable.getArgTemplateValue());
            }
        }

        final String run = resolveTemplate(command.getRunTemplate(), variableArgTemplateValues);
        resolvedCommand.setRun(run);

        if (command.getMountsIn() != null) {
            resolvedCommand.setMountsIn(command.getMountsIn());
            for (final CommandMount mount : resolvedCommand.getMountsIn()) {
                mount.setPath(resolveTemplate(mount.getPath(), variableValues));
            }
        }
        if (command.getMountsOut() != null) {
            resolvedCommand.setMountsOut(command.getMountsOut());
            for (final CommandMount mount : resolvedCommand.getMountsOut()) {
                mount.setPath(resolveTemplate(mount.getPath(), variableValues));
            }
        }

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

    @VisibleForTesting
    String resolveTemplate(final String template,
                           final Map<String, String> variableArgTemplateValues) {
        String toResolve = template;
        final Set<String> matches = Sets.newHashSet();

        final Pattern p = Pattern.compile("(?<=\\s|\\A)#(\\w+)#(?=\\s|\\z)"); // Match #varname#
        final Matcher m = p.matcher(toResolve);
        while (m.find()) {
            matches.add(m.group(1));
        }

        for (final String match : matches) {
            if (variableArgTemplateValues.containsKey(match)) {
                toResolve = toResolve.replaceAll("#"+ match +"#",
                        variableArgTemplateValues.get(match));
            }
        }
        return toResolve;
    }
}
