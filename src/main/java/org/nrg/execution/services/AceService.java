package org.nrg.execution.services;

import org.nrg.execution.exceptions.AceInputException;
import org.nrg.execution.model.ActionContextExecution;
import org.nrg.execution.model.ActionContextExecutionDto;
import org.nrg.execution.model.Context;
import org.nrg.execution.exceptions.BadRequestException;
import org.nrg.execution.exceptions.DockerServerException;
import org.nrg.execution.exceptions.NoServerPrefException;
import org.nrg.execution.exceptions.NotFoundException;
import org.nrg.framework.orm.hibernate.BaseHibernateService;
import org.nrg.xft.exception.ElementNotFoundException;
import org.nrg.xft.exception.XFTInitException;

import java.util.List;

public interface AceService extends BaseHibernateService<ActionContextExecution> {
    List<ActionContextExecutionDto> resolveAces(final Context context) throws XFTInitException, BadRequestException, NotFoundException;

    ActionContextExecution executeAce(final ActionContextExecutionDto ace)
            throws NotFoundException, NoServerPrefException, ElementNotFoundException, DockerServerException, AceInputException;
}
