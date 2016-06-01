package org.nrg.actions.services;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.nrg.actions.daos.CommandDao;
import org.nrg.actions.model.Command;
import org.nrg.actions.model.CommandVariable;
import org.nrg.actions.model.ResolvedCommand;
import org.nrg.actions.model.ResolvedCommandMount;
import org.nrg.actions.model.ScriptCommand;
import org.nrg.actions.model.ScriptEnvironment;
import org.nrg.containers.api.ContainerControlApi;
import org.nrg.containers.exceptions.DockerServerException;
import org.nrg.containers.exceptions.NoServerPrefException;
import org.nrg.containers.exceptions.NotFoundException;
import org.nrg.containers.model.DockerImage;
import org.nrg.containers.model.DockerImageCommand;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
            throw new NotFoundException("Cannot find docker image id for command " + command.getId());
        }
        resolvedCommand.setDockerImageId(dockerImage.getImageId());

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
            resolvedCommand.setMountsInFromCommandMounts(command.getMountsIn());
            for (final ResolvedCommandMount mount : resolvedCommand.getMountsIn()) {
                mount.setRemotePath(resolveTemplate(mount.getRemotePath(), variableValues));
            }
        }
        if (command.getMountsOut() != null) {
            resolvedCommand.setMountsOutFromCommandMounts(command.getMountsOut());
            for (final ResolvedCommandMount mount : resolvedCommand.getMountsOut()) {
                mount.setRemotePath(resolveTemplate(mount.getRemotePath(), variableValues));
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

    @Override
    public String launchCommand(final ResolvedCommand resolvedCommand)
            throws NoServerPrefException, DockerServerException {
        return controlApi.launchImage(resolvedCommand);
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
