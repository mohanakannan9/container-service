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
import org.nrg.containers.model.auto.Command;
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
        return toPojo(commandEntityService.create(fromPojo(command, null)));
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
    public void update(final Command updates) throws NotFoundException, CommandValidationException {
        if (updates == null) {
            return;
        }
        commandEntityService.update(fromPojo(updates));
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

    @Nonnull
    private Command toPojo(@Nonnull final CommandEntity commandEntity) {
        return Command.create(commandEntity);
    }

    @Nonnull
    private List<Command> toPojo(final List<CommandEntity> commandEntityList) {
        if (commandEntityList == null) {
            return Lists.newArrayList();
        }
        return Lists.transform(commandEntityList, new Function<CommandEntity, Command>() {
                    @Nullable
                    @Override
                    public Command apply(@Nullable final CommandEntity commandEntity) {
                        return commandEntity == null ? null : toPojo(commandEntity);
                    }
                });
    }

    @Nonnull
    private CommandEntity fromPojo(@Nonnull final Command command) throws CommandValidationException, NotFoundException {
        final CommandEntity template = commandEntityService.get(command.id());
        return fromPojo(command, template);
    }

    @Nonnull
    private CommandEntity fromPojo(@Nonnull final Command command, @Nullable final CommandEntity template) throws CommandValidationException {
        return CommandEntity.fromPojo(command, template);
    }

}
