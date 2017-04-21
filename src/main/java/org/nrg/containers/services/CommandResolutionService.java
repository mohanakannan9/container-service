package org.nrg.containers.services;

import org.nrg.containers.exceptions.CommandResolutionException;
import org.nrg.containers.model.command.auto.Command.ConfiguredCommand;
import org.nrg.containers.model.command.auto.ResolvedCommand;
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
    PartiallyResolvedCommand preResolve(ConfiguredCommand configuredCommand,
                                        Map<String, String> inputValues,
                                        UserI userI);

    ResolvedCommand resolve(long commandId,
                            String wrapperName,
                            Map<String, String> inputValues,
                            UserI userI)
            throws NotFoundException, CommandResolutionException;
    ResolvedCommand resolve(long wrapperId,
                            Map<String, String> inputValues,
                            UserI userI)
            throws NotFoundException, CommandResolutionException;
    ResolvedCommand resolve(String project,
                            long commandId,
                            String wrapperName,
                            Map<String, String> inputValues,
                            UserI userI)
            throws NotFoundException, CommandResolutionException;
    ResolvedCommand resolve(String project,
                            long wrapperId,
                            Map<String, String> inputValues,
                            UserI userI)
            throws NotFoundException, CommandResolutionException;
    ResolvedCommand resolve(ConfiguredCommand configuredCommand,
                            Map<String, String> inputValues,
                            UserI userI)
            throws NotFoundException, CommandResolutionException;
}
