package org.nrg.execution.services;

import org.nrg.execution.exceptions.BadRequestException;
import org.nrg.execution.exceptions.CommandVariableResolutionException;
import org.nrg.execution.exceptions.DockerServerException;
import org.nrg.execution.exceptions.NoServerPrefException;
import org.nrg.execution.exceptions.NotFoundException;
import org.nrg.execution.model.Command;
import org.nrg.execution.model.ResolvedCommand;
import org.nrg.framework.orm.hibernate.BaseHibernateService;
import org.nrg.xdat.om.XnatImagescandata;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xft.exception.ElementNotFoundException;
import org.nrg.xft.exception.XFTInitException;
import org.nrg.xft.security.UserI;

import java.util.List;
import java.util.Map;

public interface CommandService extends BaseHibernateService<Command> {

    Command retrieve(final String name, final String dockerImageId);

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

    String launchCommand(Long commandId, UserI user, XnatImagesessiondata session)
            throws NotFoundException, CommandVariableResolutionException, NoServerPrefException,
            DockerServerException, BadRequestException, XFTInitException, ElementNotFoundException;
    String launchCommand(Long commandId, UserI user, XnatImagesessiondata session, XnatImagescandata scan) throws NotFoundException, XFTInitException, CommandVariableResolutionException, NoServerPrefException, DockerServerException;

    List<Command> save(final List<Command> commands);
}
