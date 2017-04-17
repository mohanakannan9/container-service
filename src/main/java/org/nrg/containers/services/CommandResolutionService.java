package org.nrg.containers.services;

import org.nrg.containers.exceptions.CommandResolutionException;
import org.nrg.containers.model.PartiallyResolvedCommand;
import org.nrg.containers.model.ResolvedCommand;
import org.nrg.containers.model.command.auto.Command;
import org.nrg.containers.model.command.auto.Command.CommandWrapper;
import org.nrg.xft.security.UserI;

import java.util.Map;

public interface CommandResolutionService {
    ResolvedCommand resolve(CommandWrapper commandWrapper,
                            Command command,
                            Map<String, String> inputValues,
                            UserI userI) throws CommandResolutionException;
    PartiallyResolvedCommand partiallyResolve(CommandWrapper commandWrapper,
                                              Command command,
                                              Map<String, String> inputValues,
                                              UserI userI);
}
