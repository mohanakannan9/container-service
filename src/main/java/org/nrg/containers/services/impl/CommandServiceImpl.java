package org.nrg.containers.services.impl;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;
import org.nrg.containers.exceptions.CommandValidationException;
import org.nrg.containers.model.command.auto.Command.CommandInput;
import org.nrg.containers.model.command.auto.Command.CommandOutput;
import org.nrg.containers.model.command.auto.Command.CommandWrapperDerivedInput;
import org.nrg.containers.model.command.auto.Command.CommandWrapperExternalInput;
import org.nrg.containers.model.command.auto.Command.CommandWrapperOutput;
import org.nrg.containers.model.command.auto.CommandSummaryForContext;
import org.nrg.containers.model.configuration.CommandConfiguration;
import org.nrg.containers.model.command.entity.CommandEntity;
import org.nrg.containers.model.command.entity.CommandWrapperEntity;
import org.nrg.containers.model.command.auto.Command;
import org.nrg.containers.model.command.auto.Command.CommandWrapper;
import org.nrg.containers.model.configuration.CommandConfiguration.CommandOutputConfiguration;
import org.nrg.containers.model.configuration.CommandConfigurationInternal;
import org.nrg.containers.services.CommandEntityService;
import org.nrg.containers.services.CommandService;
import org.nrg.containers.services.ContainerConfigService;
import org.nrg.containers.services.ContainerConfigService.CommandConfigurationException;
import org.nrg.framework.exceptions.NotFoundException;
import org.nrg.xdat.schema.SchemaElement;
import org.nrg.xft.exception.ElementNotFoundException;
import org.nrg.xft.exception.XFTInitException;
import org.nrg.xft.security.UserI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class CommandServiceImpl implements CommandService, InitializingBean {
    private static final Logger log = LoggerFactory.getLogger(CommandServiceImpl.class);

    private final CommandEntityService commandEntityService;
    private final ContainerConfigService containerConfigService;

    @Autowired
    public CommandServiceImpl(final CommandEntityService commandEntityService,
                              final ContainerConfigService containerConfigService) {
        this.commandEntityService = commandEntityService;
        this.containerConfigService = containerConfigService;
    }

    @Override
    public void afterPropertiesSet() {
        // Set the default JayWay JSONPath configuration
        Configuration.setDefaults(new Configuration.Defaults() {

            private final JsonProvider jsonProvider = new JacksonJsonProvider();
            private final MappingProvider mappingProvider = new JacksonMappingProvider();

            @Override
            public JsonProvider jsonProvider() {
                return jsonProvider;
            }

            @Override
            public MappingProvider mappingProvider() {
                return mappingProvider;
            }

            @Override
            public Set<Option> options() {
                return Sets.newHashSet(Option.DEFAULT_PATH_LEAF_TO_NULL);
            }
        });
    }

    @Override
    @Nonnull
    @Transactional
    public Command create(@Nonnull final Command command) throws CommandValidationException {
        final List<String> errors = command.validate();
        if (!errors.isEmpty()) {
            throw new CommandValidationException(errors);
        }
        return toPojo(commandEntityService.create(fromPojo(command)));
    }

    @Override
    @Nonnull
    public List<Command> getAll() {
        return toPojo(commandEntityService.getAll());
    }

    @Override
    @Nullable
    public Command retrieve(final long id) {
        return toPojo(commandEntityService.retrieve(id));
    }

    @Override
    @Nonnull
    public Command get(final long id) throws NotFoundException {
        return toPojo(commandEntityService.get(id));
    }

    @Override
    @Nonnull
    public List<Command> findByProperties(final Map<String, Object> properties) {
        return toPojo(commandEntityService.findByProperties(properties));
    }

    @Override
    @Nonnull
    @Transactional
    public Command update(final @Nonnull Command toUpdate) throws NotFoundException, CommandValidationException {
        final List<String> errors = toUpdate.validate();
        if (!errors.isEmpty()) {
            throw new CommandValidationException(errors);
        }
        final CommandEntity updatableEntity = fromPojo(toUpdate);
        commandEntityService.update(updatableEntity);
        commandEntityService.refresh(updatableEntity);
        return toPojo(updatableEntity);
    }

    @Override
    public void delete(final long id) {
        final Command command = retrieve(id);
        if (command != null) {
            for (final CommandWrapper commandWrapper : command.xnatCommandWrappers()) {
                containerConfigService.deleteAllConfiguration(id, commandWrapper.name());
            }

            commandEntityService.delete(id);
        }

        containerConfigService.deleteAllConfiguration(id);
    }

    @Override
    @Nonnull
    @Transactional
    public List<Command> save(final List<Command> commands) {
        final List<Command> created = Lists.newArrayList();
        if (!(commands == null || commands.isEmpty())) {
            for (final Command command : commands) {
                try {
                    created.add(create(command));
                } catch (CommandValidationException e) {
                    // TODO: should I "update" instead of erroring out if command already exists?
                    log.error("Could not save command " + command.name(), e);
                }
            }
        }
        return created;
    }

    @Override
    @Nonnull
    @Transactional
    public CommandWrapper addWrapper(final long commandId, final @Nonnull CommandWrapper wrapperToAdd) throws CommandValidationException, NotFoundException {
        return addWrapper(get(commandId), wrapperToAdd);
    }

    @Override
    @Nonnull
    @Transactional
    public CommandWrapper addWrapper(final @Nonnull Command command, final @Nonnull CommandWrapper wrapperToAdd) throws CommandValidationException, NotFoundException {
        final CommandWrapper created = toPojo(commandEntityService.addWrapper(fromPojo(command), fromPojo(wrapperToAdd)));

        final List<String> errors = get(command.id()).validate();
        if (!errors.isEmpty()) {
            throw new CommandValidationException(errors);
        }
        return created;
    }

    @Override
    @Nullable
    public CommandWrapper retrieve(final long commandId, final long wrapperId) {
        return toPojo(commandEntityService.retrieve(commandId, wrapperId));
    }

    @Override
    @Nullable
    public CommandWrapper retrieve(final long commandId, final String wrapperName) {
        return toPojo(commandEntityService.retrieve(commandId, wrapperName));
    }

    @Override
    @Nullable
    public CommandWrapper retrieve(final Command command, final long wrapperId) {
        for (final CommandWrapper commandWrapper : command.xnatCommandWrappers()) {
            if (commandWrapper.id() == wrapperId) {
                return commandWrapper;
            }
        }
        return null;
    }

    @Override
    public CommandWrapper retrieve(final Command command, final String wrapperName) {
        for (final CommandWrapper commandWrapper : command.xnatCommandWrappers()) {
            if ((commandWrapper.name() == null && wrapperName == null) ||
                    commandWrapper.name().equals(wrapperName)) {
                return commandWrapper;
            }
        }
        return null;
    }

    @Override
    @Nonnull
    public CommandWrapper get(final long commandId, final long wrapperId) throws NotFoundException {
        return toPojo(commandEntityService.get(commandId, wrapperId));
    }

    @Override
    @Nonnull
    public CommandWrapper get(final long commandId, final String wrapperName) throws NotFoundException {
        return toPojo(commandEntityService.get(commandId, wrapperName));
    }

    @Override
    @Nonnull
    public CommandWrapper get(final @Nonnull Command command, final long wrapperId) throws NotFoundException {
        final CommandWrapper commandWrapper = retrieve(command, wrapperId);
        if (commandWrapper == null) {
            throw new NotFoundException(String.format("No command wrapper for command id %d, wrapper id %d", command.id(), wrapperId));
        }
        return commandWrapper;
    }

    @Override
    @Nonnull
    public CommandWrapper get(final @Nonnull Command command, final String wrapperName) throws NotFoundException {
        final CommandWrapper commandWrapper = retrieve(command, wrapperName);
        if (commandWrapper == null) {
            throw new NotFoundException(String.format("No command wrapper for command id %d, wrapper name %s", command.id(), wrapperName));
        }
        return commandWrapper;
    }
    @Override
    @Nonnull
    @Transactional
    public CommandWrapper update(final long commandId, final @Nonnull CommandWrapper toUpdate) throws CommandValidationException, NotFoundException {
        final CommandEntity commandEntity = commandEntityService.get(commandId);
        final CommandWrapperEntity template = commandEntityService.get(commandEntity, toUpdate.id());
        final CommandWrapper updated = toPojo(commandEntityService.update(template.update(toUpdate)));

        final List<String> errors = toPojo(commandEntity).validate();
        if (!errors.isEmpty()) {
            throw new CommandValidationException(errors);
        }
        return updated;
    }

    @Override
    @Transactional
    public void delete(final long commandId, final long wrapperId) {
        commandEntityService.delete(commandId, wrapperId);
    }

    @Override
    @Nonnull
    public Command getCommandWithOneWrapper(final long commandId, final long wrapperId) throws NotFoundException {
        final Command command = get(commandId);
        return getCommandWithOneWrapper(command, get(command, wrapperId));
    }

    @Override
    @Nonnull
    public Command getCommandWithOneWrapper(final long commandId, final String wrapperName) throws NotFoundException {
        final Command command = get(commandId);
        return getCommandWithOneWrapper(command, get(command, wrapperName));
    }

    @Override
    public void configureForSite(final CommandConfiguration commandConfiguration, final long commandId, final String wrapperName, final boolean enable, final String username, final String reason)
            throws CommandConfigurationException, NotFoundException {
        assertPairExists(commandId, wrapperName);
        // If the "enable" param is true, we enable the configuration.
        // Otherwise, we leave the existing "enabled" setting alone (even if it is null).
        // We will never change "enabled" to "false" here.
        final Boolean enabledStatusToSet = enable ? Boolean.TRUE : isEnabledForSite(commandId, wrapperName);
        containerConfigService.configureForSite(
                CommandConfigurationInternal.create(enabledStatusToSet, commandConfiguration),
                commandId, wrapperName, username, reason);
    }

    @Override
    public void configureForProject(final CommandConfiguration commandConfiguration, final String project, final long commandId, final String wrapperName, final boolean enable, final String username, final String reason) throws CommandConfigurationException, NotFoundException {
        assertPairExists(commandId, wrapperName);

        // If the "enable" param is true, we enable the configuration.
        // Otherwise, we leave the existing "enabled" setting alone (even if it is null).
        // We will never change "enabled" to "false" here.
        final Boolean enabledStatusToSet = enable ? Boolean.TRUE : isEnabledForProject(project, commandId, wrapperName);
        containerConfigService.configureForProject(
                CommandConfigurationInternal.create(enabledStatusToSet, commandConfiguration),
                project, commandId, wrapperName, username, reason);
    }

    @Override
    @Nonnull
    public CommandConfiguration getSiteConfiguration(final long commandId, final String wrapperName) throws NotFoundException {
        assertPairExists(commandId, wrapperName);
        final CommandConfigurationInternal commandConfigurationInternal = containerConfigService.getSiteConfiguration(commandId, wrapperName);
        return CommandConfiguration.create(get(commandId), commandConfigurationInternal, wrapperName);
    }

    @Override
    @Nonnull
    public CommandConfiguration getProjectConfiguration(final String project, final long commandId, final String wrapperName) throws NotFoundException {
        assertPairExists(commandId, wrapperName);
        final CommandConfigurationInternal commandConfigurationInternal = containerConfigService.getProjectConfiguration(project, commandId, wrapperName);
        return CommandConfiguration.create(get(commandId), commandConfigurationInternal, wrapperName);
    }

    @Override
    @Nonnull
    public Command getAndConfigure(final long commandId, final String wrapperName) throws NotFoundException {
        final Command originalCommand = get(commandId);
        final Command commandWithOneWrapper = getCommandWithOneWrapper(originalCommand, get(originalCommand, wrapperName));
        final CommandConfiguration commandConfiguration = getSiteConfiguration(commandId, wrapperName);
        return applyConfiguration(commandWithOneWrapper, commandConfiguration);
    }

    // @Override
    // public Command getAndConfigure(final long commandId, final long wrapperId) throws NotFoundException {
    //     final Command originalCommand = get(commandId);
    //     final Command commandWithOneWrapper = getCommandWithOneWrapper(originalCommand, get(originalCommand, wrapperId));
    //     final CommandConfiguration commandConfiguration = getSiteConfiguration(commandId, wrapperId);
    //     return null;
    // }

    @Override
    @Nonnull
    public Command getAndConfigure(final String project, final long commandId, final String wrapperName) throws NotFoundException {
        final Command originalCommand = get(commandId);
        final Command commandWithOneWrapper = getCommandWithOneWrapper(originalCommand, get(originalCommand, wrapperName));
        final CommandConfiguration commandConfiguration = getProjectConfiguration(project, commandId, wrapperName);
        return applyConfiguration(commandWithOneWrapper, commandConfiguration);
    }

    // @Override
    // public Command getAndConfigure(final String project, final long commandId, final long wrapperId) throws NotFoundException {
    //     return null;
    // }

    @Nonnull
    private Command getCommandWithOneWrapper(final @Nonnull Command originalCommand,
                                             final @Nonnull CommandWrapper commandWrapper) {
        return Command.builder()
                .name(originalCommand.name())
                .id(originalCommand.id())
                .name(originalCommand.name())
                .label(originalCommand.label())
                .description(originalCommand.description())
                .version(originalCommand.version())
                .schemaVersion(originalCommand.schemaVersion())
                .infoUrl(originalCommand.infoUrl())
                .image(originalCommand.image())
                .type(originalCommand.type())
                .workingDirectory(originalCommand.workingDirectory())
                .commandLine(originalCommand.commandLine())
                .environmentVariables(originalCommand.environmentVariables())
                .mounts(originalCommand.mounts())
                .inputs(originalCommand.inputs())
                .outputs(originalCommand.outputs())
                .index(originalCommand.index())
                .hash(originalCommand.hash())
                .ports(originalCommand.ports())
                .addCommandWrapper(commandWrapper)
                .build();
    }

    @Nonnull
    private Command applyConfiguration(final @Nonnull Command originalCommand,
                                       final @Nonnull CommandConfiguration commandConfiguration) {
        // Initialize the command builder copy
        final Command.Builder commandBuilder = Command.builder()
                .name(originalCommand.name())
                .id(originalCommand.id())
                .name(originalCommand.name())
                .label(originalCommand.label())
                .description(originalCommand.description())
                .version(originalCommand.version())
                .schemaVersion(originalCommand.schemaVersion())
                .infoUrl(originalCommand.infoUrl())
                .image(originalCommand.image())
                .type(originalCommand.type())
                .workingDirectory(originalCommand.workingDirectory())
                .commandLine(originalCommand.commandLine())
                .environmentVariables(originalCommand.environmentVariables())
                .mounts(originalCommand.mounts())
                .index(originalCommand.index())
                .hash(originalCommand.hash())
                .ports(originalCommand.ports())
                .outputs(originalCommand.outputs());

        // Things we need to apply configuration to:
        // command inputs
        // wrapper inputs
        // wrapper outputs

        for (final CommandInput commandInput : originalCommand.inputs()) {
            commandBuilder.addInput(
                    commandConfiguration.inputs().containsKey(commandInput.name()) ?
                            commandInput.applyConfiguration(commandConfiguration.inputs().get(commandInput.name())) :
                            commandInput
            );
        }

        final CommandWrapper originalCommandWrapper = originalCommand.xnatCommandWrappers().get(0);
        final CommandWrapper.Builder commandWrapperBuilder = CommandWrapper.builder()
                .id(originalCommandWrapper.id())
                .name(originalCommandWrapper.name())
                .description(originalCommandWrapper.description())
                .contexts(originalCommandWrapper.contexts());

        for (final CommandWrapperExternalInput externalInput : originalCommandWrapper.externalInputs()) {
            commandWrapperBuilder.addExternalInput(
                    commandConfiguration.inputs().containsKey(externalInput.name()) ?
                            externalInput.applyConfiguration(commandConfiguration.inputs().get(externalInput.name())) :
                            externalInput
            );
        }

        for (final CommandWrapperDerivedInput derivedInput : originalCommandWrapper.derivedInputs()) {
            commandWrapperBuilder.addDerivedInput(
                    commandConfiguration.inputs().containsKey(derivedInput.name()) ?
                            derivedInput.applyConfiguration(commandConfiguration.inputs().get(derivedInput.name())) :
                            derivedInput
            );
        }

        for (final CommandWrapperOutput output : originalCommandWrapper.outputHandlers()) {
            commandWrapperBuilder.addOutputHandler(
                    commandConfiguration.outputs().containsKey(output.name()) ?
                            output.applyConfiguration(commandConfiguration.outputs().get(output.name())) :
                            output
            );
        }

        return commandBuilder.addCommandWrapper(commandWrapperBuilder.build()).build();
    }

    @Override
    public void deleteSiteConfiguration(final long commandId, final String wrapperName, final String username) throws CommandConfigurationException {
        containerConfigService.deleteSiteConfiguration(commandId, wrapperName, username);
    }

    @Override
    public void deleteProjectConfiguration(final String project, final long commandId, final String wrapperName, final String username) throws CommandConfigurationException {
        containerConfigService.deleteProjectConfiguration(project, commandId, wrapperName, username);
    }

    @Override
    public void deleteAllConfiguration(final long commandId, final String wrapperName) {
        containerConfigService.deleteAllConfiguration(commandId, wrapperName);
    }

    @Override
    public void deleteAllConfiguration(final long commandId) {
        containerConfigService.deleteAllConfiguration(commandId);
    }

    @Override
    public void enableForSite(final long commandId, final String wrapperName, final String username, final String reason) throws CommandConfigurationException, NotFoundException {
        assertPairExists(commandId, wrapperName);
        containerConfigService.enableForSite(commandId, wrapperName, username, reason);
    }

    @Override
    public void disableForSite(final long commandId, final String wrapperName, final String username, final String reason) throws CommandConfigurationException, NotFoundException {
        assertPairExists(commandId, wrapperName);
        containerConfigService.disableForSite(commandId, wrapperName, username, reason);
    }

    @Override
    public boolean isEnabledForSite(final long commandId, final String wrapperName) throws NotFoundException {
        assertPairExists(commandId, wrapperName);
        return containerConfigService.isEnabledForSite(commandId, wrapperName);
    }

    @Override
    public void enableForProject(final String project, final long commandId, final String wrapperName, final String username, final String reason) throws CommandConfigurationException, NotFoundException {
        assertPairExists(commandId, wrapperName);
        containerConfigService.enableForProject(project, commandId, wrapperName, username, reason);
    }

    @Override
    public void disableForProject(final String project, final long commandId, final String wrapperName, final String username, final String reason) throws CommandConfigurationException, NotFoundException {
        assertPairExists(commandId, wrapperName);
        containerConfigService.disableForProject(project, commandId, wrapperName, username, reason);
    }

    @Override
    public boolean isEnabledForProject(final String project, final long commandId, final String wrapperName) throws NotFoundException {
        assertPairExists(commandId, wrapperName);
        return containerConfigService.isEnabledForProject(project, commandId, wrapperName);
    }

    @Override
    @Nonnull
    public List<CommandSummaryForContext> available(final String project,
                                                    final String xsiType,
                                                    final UserI userI) throws ElementNotFoundException {
        final List<CommandSummaryForContext> available = new ArrayList<>();

        for (final Command command : getAll()) {
            for (final CommandWrapper wrapper : command.xnatCommandWrappers()) {

                // Can only launch if the user gave us an xsiType that matches
                // one of the wrapper's contexts
                if (!xsiTypesMatch(xsiType, wrapper.contexts())) {
                    continue;
                }

                // Can only launch if this user has permission
                if (!userCanLaunch(userI, project, wrapper)) {
                    continue;
                }

                // Can only launch with a single external input
                // It seems iffy to me to bake this into the code, but I don't know a way around it.
                // We don't have any UI right now where a user can sensibly launch
                //   on two completely unconnected objects.
                final String externalInputName;
                if (wrapper.externalInputs().size() == 1) {
                    externalInputName = wrapper.externalInputs().get(0).name();
                } else {
                    continue;
                }

                available.add(CommandSummaryForContext.create(command, wrapper,
                        containerConfigService.isEnabledForProject(project, command.id(), wrapper.name()),
                        externalInputName));
            }
        }

        return available;
    }

    // Cache the pairs of (parent, child) xsiType relationships.
    // If child is descended from parent, return true. Else return false.
    private Map<XsiTypePair, Boolean> xsiTypePairCache = new HashMap<>();

    /**
     * Check if the xsiType that the user gave us is equal to *or* *descended* *from*
     * one of the xsiTypes in the wrapper's contexts set.
     *
     * Example
     * If a wrapper can run on {"xnat:mrSessionData", "xnat:petSessionData"}, and
     * the user asks 'what can I run on an "xnat:mrSessionData"?' we return true.
     *
     * If a wrapper can run on {"xnat:imageSessionData", "xnat:imageAssessorData"}, and
     * the user asks 'what can I run on an "xnat:mrSessionData"?' we return true.

     * If a wrapper can run on {"xnat:mrSessionData"}, and
     * the user asks 'what can I run on an "xnat:imageSessionData"?' we return false.
     *
     * @param xsiType A user asked "what commands can run on this xsiType"?
     * @param wrapperXsiTypes For a particular command wrapper, there are the xsiTypes it can run on.
     *                        This may include "parent" xsiTypes. We want all the "child" types of that
     *                        "parent" type to match as well.
     * @return Can this wrapper run on this xsiType?
     */
    private boolean xsiTypesMatch(final @Nonnull String xsiType,
                                  final @Nonnull Set<String> wrapperXsiTypes) throws ElementNotFoundException {
        if (wrapperXsiTypes.contains(xsiType)) {
            return true;
        }

        for (final String wrapperXsiType : wrapperXsiTypes) {
            final XsiTypePair xsiTypeKey = new XsiTypePair(wrapperXsiType, xsiType);

            // Return a result from the cache if it exists.
            if (xsiTypePairCache.containsKey(xsiTypeKey)) {
                final Boolean cached = xsiTypePairCache.get(xsiTypeKey);
                return cached == null ? false : cached; // Should never be null. But let's check for safety.
            }

            // Compute new result
            boolean match = false;
            try {
                match = SchemaElement.GetElement(xsiType).getGenericXFTElement().instanceOf(wrapperXsiType);
            } catch (XFTInitException e) {
                log.error("XFT not initialized."); // If this happens, we have a lot of other problems.
            }

            // Add result to cache
            xsiTypePairCache.put(xsiTypeKey, match);

            // Shortcut loop if a result is true
            if (match) {
                return true;
            }
        }
        return false;
    }

    private boolean userCanLaunch(final UserI userI, final String project, final CommandWrapper wrapper) {
        // TODO How do we know if the user can launch this command wrapper in this project?
        return true;
    }


    @Nonnull
    private Command toPojo(@Nonnull final CommandEntity commandEntity) {
        return Command.create(commandEntity);
    }

    @Nonnull
    private List<Command> toPojo(final List<CommandEntity> commandEntityList) {
        final List<Command> returnList = Lists.newArrayList();
        if (commandEntityList != null) {
            returnList.addAll(Lists.transform(commandEntityList, new Function<CommandEntity, Command>() {
                @Nullable
                @Override
                public Command apply(@Nullable final CommandEntity commandEntity) {
                    return commandEntity == null ? null : toPojo(commandEntity);
                }
            }));
        }
        return returnList;
    }

    @Nonnull
    private CommandEntity fromPojo(@Nonnull final Command command) {
        final CommandEntity template = commandEntityService.retrieve(command.id());
        return template == null ? CommandEntity.fromPojo(command) : template.update(command);
    }

    @Nonnull
    private CommandWrapperEntity fromPojo(@Nonnull final CommandWrapper commandWrapper) {
        return CommandWrapperEntity.fromPojo(commandWrapper);
    }

    @Nonnull
    private CommandWrapper toPojo(@Nonnull final CommandWrapperEntity commandWrapperEntity) {
        return CommandWrapper.create(commandWrapperEntity);
    }

    private void assertPairExists(final long commandId, final long wrapperId) throws NotFoundException {
        commandEntityService.assertPairExists(commandId, wrapperId);
    }

    private void assertPairExists(final long commandId, final String wrapperName) throws NotFoundException {
        commandEntityService.assertPairExists(commandId, wrapperName);
    }

    private static class XsiTypePair {
        private String wrapperXsiType;
        private String userRequestedXsiType;

        XsiTypePair(final String wrapperXsiType,
                    final String userRequestedXsiType) {
            this.wrapperXsiType = wrapperXsiType;
            this.userRequestedXsiType = userRequestedXsiType;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            final XsiTypePair that = (XsiTypePair) o;
            return Objects.equals(this.wrapperXsiType, that.wrapperXsiType) &&
                    Objects.equals(this.userRequestedXsiType, that.userRequestedXsiType);
        }

        @Override
        public int hashCode() {
            return Objects.hash(wrapperXsiType, userRequestedXsiType);
        }
    }
}
