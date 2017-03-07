package org.nrg.containers.services;

import org.nrg.containers.exceptions.CommandValidationException;
import org.nrg.containers.model.auto.Command;
import org.nrg.containers.model.auto.Command.CommandWrapper;
import org.nrg.framework.exceptions.NotFoundException;

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
}
