package org.nrg.containers.services;

import org.nrg.containers.model.command.auto.Command;
import org.nrg.framework.exceptions.NotFoundException;

public interface SetupCommandService {
    Command getSetupCommand(String image) throws NotFoundException;
}
