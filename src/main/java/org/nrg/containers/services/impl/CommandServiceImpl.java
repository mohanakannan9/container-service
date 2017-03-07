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
import org.nrg.containers.model.CommandEntity;
import org.nrg.containers.model.CommandWrapperEntity;
import org.nrg.containers.model.auto.Command;
import org.nrg.containers.model.auto.Command.CommandWrapper;
import org.nrg.containers.services.CommandEntityService;
import org.nrg.containers.services.CommandService;
import org.nrg.framework.exceptions.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class CommandServiceImpl implements CommandService, InitializingBean {
    private static final Logger log = LoggerFactory.getLogger(CommandServiceImpl.class);

    private final CommandEntityService commandEntityService;

    @Autowired
    public CommandServiceImpl(final CommandEntityService commandEntityService) {
        this.commandEntityService = commandEntityService;
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
        commandEntityService.delete(id);
    }
    @Override
    @Nonnull
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
    public CommandWrapper addWrapper(final long commandId, final @Nonnull CommandWrapper wrapperToAdd) throws CommandValidationException, NotFoundException {
        return addWrapper(get(commandId), wrapperToAdd);
    }

    @Override
    @Nonnull
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
    @Nonnull
    public CommandWrapper get(final long commandId, final long wrapperId) throws NotFoundException {
        return toPojo(commandEntityService.get(commandId, wrapperId));
    }

    @Override
    @Nonnull
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
    public void delete(final long commandId, final long wrapperId) {
        commandEntityService.delete(commandId, wrapperId);
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
}
