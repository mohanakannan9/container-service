package org.nrg.containers.services;

import org.nrg.containers.exceptions.CommandResolutionException;
import org.nrg.containers.model.command.auto.ResolvedCommand;
import org.nrg.containers.model.command.auto.Command;
import org.nrg.containers.model.command.auto.Command.CommandWrapper;
import org.nrg.containers.model.command.auto.ResolvedCommand.PartiallyResolvedCommand;
import org.nrg.framework.exceptions.NotFoundException;
import org.nrg.xft.security.UserI;

import java.util.Map;

public interface CommandResolutionService {
    PartiallyResolvedCommand preResolve(long wrapperId,
                                        Map<String, String> inputValues,
                                        final UserI userI)
            throws NotFoundException;
    PartiallyResolvedCommand preResolve(long commandId,
                                        String wrapperName,
                                        Map<String, String> inputValues,
                                        final UserI userI)
            throws NotFoundException;
    PartiallyResolvedCommand preResolve(String project,
                                        long wrapperId,
                                        Map<String, String> inputValues,
                                        final UserI userI)
            throws NotFoundException;
    PartiallyResolvedCommand preResolve(String project,
                                        long commandId,
                                        String wrapperName,
                                        Map<String, String> inputValues,
                                        final UserI userI)
            throws NotFoundException;
    PartiallyResolvedCommand preResolve(CommandWrapper commandWrapper,
                                        Command command,
                                        Map<String, String> inputValues,
                                        UserI userI);

    PartiallyResolvedCommand resolve(long commandId,
                                     String wrapperName,
                                     Map<String, String> inputValues,
                                     UserI userI)
            throws NotFoundException, CommandResolutionException;
    PartiallyResolvedCommand resolve(long wrapperId,
                                     Map<String, String> inputValues,
                                     UserI userI)
            throws NotFoundException, CommandResolutionException;
    PartiallyResolvedCommand resolve(String project,
                                     long commandId,
                                     String wrapperName,
                                     Map<String, String> inputValues,
                                     UserI userI)
            throws NotFoundException, CommandResolutionException;
    PartiallyResolvedCommand resolve(String project,
                                     long wrapperId,
                                     Map<String, String> inputValues,
                                     UserI userI)
            throws NotFoundException, CommandResolutionException;
    PartiallyResolvedCommand resolve(CommandWrapper commandWrapper,
                                     Command command,
                                     Map<String, String> inputValues,
                                     UserI userI)
            throws NotFoundException, CommandResolutionException;
}
