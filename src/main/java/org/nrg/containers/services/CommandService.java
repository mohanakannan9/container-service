package org.nrg.containers.services;

import org.nrg.containers.exceptions.CommandValidationException;
import org.nrg.containers.model.command.auto.Command;
import org.nrg.containers.model.command.auto.Command.CommandWrapper;
import org.nrg.containers.model.command.auto.CommandSummaryForContext;
import org.nrg.containers.model.configuration.CommandConfiguration;
import org.nrg.containers.services.ContainerConfigService.CommandConfigurationException;
import org.nrg.framework.exceptions.NotFoundException;
import org.nrg.xft.exception.ElementNotFoundException;
import org.nrg.xft.security.UserI;

import java.util.List;
import java.util.Map;

public interface CommandService {
    Command create(Command command) throws CommandValidationException;
    List<Command> getAll();
    Command retrieve(long id);
    Command get(long id) throws NotFoundException;
    List<Command> findByProperties(Map<String, Object> properties);
    Command update(Command updates) throws NotFoundException, CommandValidationException;
    void delete(long id);
    List<Command> save(final List<Command> commands);

    CommandWrapper addWrapper(long commandId, CommandWrapper commandWrapper) throws CommandValidationException, NotFoundException;
    CommandWrapper addWrapper(Command command, CommandWrapper commandWrapper) throws CommandValidationException, NotFoundException;
    CommandWrapper retrieve(long commandId, long wrapperId);
    CommandWrapper get(long commandId, long wrapperId) throws NotFoundException;
    CommandWrapper update(long commandId, CommandWrapper updates) throws CommandValidationException, NotFoundException;
    void delete(long commandId, long wrapperId);

    void configureForSite(CommandConfiguration commandConfiguration, long commandId, String wrapperName, boolean enable, String username, String reason) throws CommandConfigurationException, NotFoundException;
    void configureForProject(CommandConfiguration commandConfiguration, String project, long commandId, String wrapperName, boolean enable, String username, String reason) throws CommandConfigurationException, NotFoundException;

    CommandConfiguration getSiteConfiguration(long commandId, String wrapperName) throws NotFoundException;
    CommandConfiguration getProjectConfiguration(String project, long commandId, String wrapperName) throws NotFoundException;

    void deleteSiteConfiguration(long commandId, String wrapperName, final String username) throws CommandConfigurationException;
    void deleteProjectConfiguration(String project, long commandId, String wrapperName, final String username) throws CommandConfigurationException;
    void deleteAllConfiguration(long commandId, String wrapperName);
    void deleteAllConfiguration(long commandId);

    void enableForSite(long commandId, String wrapperName, final String username, final String reason) throws CommandConfigurationException, NotFoundException;
    void disableForSite(long commandId, String wrapperName, final String username, final String reason) throws CommandConfigurationException, NotFoundException;
    boolean isEnabledForSite(long commandId, String wrapperName) throws NotFoundException;
    void enableForProject(String project, long commandId, String wrapperName, final String username, final String reason) throws CommandConfigurationException, NotFoundException;
    void disableForProject(String project, long commandId, String wrapperName, final String username, final String reason) throws CommandConfigurationException, NotFoundException;
    boolean isEnabledForProject(String project, long commandId, String wrapperName) throws NotFoundException;

    List<CommandSummaryForContext> available(String project,
                                             String xsiType,
                                             UserI userI) throws ElementNotFoundException;

}
