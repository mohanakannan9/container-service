package org.nrg.execution.rest;

import org.nrg.execution.exceptions.NotFoundException;
import org.nrg.execution.model.ContainerExecution;
import org.nrg.execution.services.ContainerExecutionService;
import org.nrg.framework.annotations.XapiRestController;
import org.nrg.xdat.XDAT;
import org.nrg.xft.security.UserI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@XapiRestController
@RequestMapping(value = "/containerexecutions")
public class ContainerExecutionRestApi {
    private static final String JSON = MediaType.APPLICATION_JSON_UTF8_VALUE;

    @Autowired private ContainerExecutionService containerExecutionService;

    @RequestMapping(method = GET)
    @ResponseBody
    public List<ContainerExecution> getAll() {
        return containerExecutionService.getAll();
    }

    @RequestMapping(value = "/{id}", method = GET)
    @ResponseBody
    public ContainerExecution getOne(final @PathVariable Long id) throws NotFoundException {
        final ContainerExecution containerExecution = containerExecutionService.retrieve(id);
        if (containerExecution == null) {
            throw new NotFoundException("ContainerExecution " + id + " not found.");
        }
        return containerExecution;
    }

    @RequestMapping(value = "/{id}/finalize", method = POST, produces = JSON)
    public void finalize(final @PathVariable Long id) throws NotFoundException {
        final UserI userI = XDAT.getUserDetails();
        containerExecutionService.finalize(id, userI);
    }
}
