package org.nrg.containers.services;

import org.nrg.containers.exceptions.CommandResolutionException;
import org.nrg.containers.model.command.auto.ResolvedCommand;
import org.nrg.containers.model.command.auto.Command;
import org.nrg.containers.model.command.auto.Command.CommandWrapper;
import org.nrg.containers.model.command.auto.ResolvedCommand.PartiallyResolvedCommand;
import org.nrg.xft.security.UserI;

import java.util.Map;

public interface CommandResolutionService {
    PartiallyResolvedCommand resolve(CommandWrapper commandWrapper,
                            Command command,
                            Map<String, String> inputValues,
                            UserI userI) throws CommandResolutionException;
    PartiallyResolvedCommand preResolveForUi(CommandWrapper commandWrapper,
                                             Command command,
                                             Map<String, String> inputValues,
                                             UserI userI);
}
