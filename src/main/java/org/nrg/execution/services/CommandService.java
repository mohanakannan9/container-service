package org.nrg.execution.services;

import org.nrg.execution.exceptions.AceInputException;
import org.nrg.execution.exceptions.BadRequestException;
import org.nrg.execution.exceptions.CommandVariableResolutionException;
import org.nrg.execution.model.ActionContextExecution;
import org.nrg.execution.model.Command;
import org.nrg.execution.model.ResolvedCommand;
import org.nrg.execution.exceptions.DockerServerException;
import org.nrg.execution.exceptions.NoServerPrefException;
import org.nrg.execution.exceptions.NotFoundException;
import org.nrg.framework.orm.hibernate.BaseHibernateService;
import org.nrg.xdat.om.XnatImagescandata;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xft.exception.ElementNotFoundException;
import org.nrg.xft.exception.XFTInitException;

import java.util.List;
import java.util.Map;

public interface CommandService extends BaseHibernateService<Command> {
    String LABEL_KEY = "org.nrg.commands";

    List<Command> findByName(final String name);

    ResolvedCommand resolveCommand(final Long commandId) throws NotFoundException, CommandVariableResolutionException;
    ResolvedCommand resolveCommand(final Long commandId,
                                   final Map<String, String> variableRuntimeValues)
            throws NotFoundException, CommandVariableResolutionException;
    ResolvedCommand resolveCommand(final Command command) throws NotFoundException, CommandVariableResolutionException;
    ResolvedCommand resolveCommand(final Command command,
                                   final Map<String, String> variableRuntimeValues)
            throws NotFoundException, CommandVariableResolutionException;

    String launchCommand(final ResolvedCommand resolvedCommand) throws NoServerPrefException, DockerServerException;
    String launchCommand(final Long commandId)
            throws NoServerPrefException, DockerServerException, NotFoundException, CommandVariableResolutionException;
    String launchCommand(final Long commandId,
                         final Map<String, String> variableRuntimeValues)
            throws NoServerPrefException, DockerServerException, NotFoundException, CommandVariableResolutionException;

    ActionContextExecution launchCommand(XnatImagesessiondata session, Long commandId) throws NotFoundException, CommandVariableResolutionException, NoServerPrefException, DockerServerException, BadRequestException, XFTInitException, ElementNotFoundException, AceInputException;
    ActionContextExecution launchCommand(XnatImagescandata scan, Long commandId) throws NotFoundException, CommandVariableResolutionException, NoServerPrefException, DockerServerException, BadRequestException, XFTInitException, ElementNotFoundException, AceInputException;

    List<Command> parseLabels(final Map<String, String> labels);
    List<Command> saveFromLabels(final Map<String, String> labels);
}
