package org.nrg.containers.rest;

import org.apache.commons.io.FilenameUtils;
import org.nrg.containers.model.container.entity.ContainerEntity;
import org.nrg.containers.services.ContainerEntityService;
import org.nrg.containers.services.ContainerService;
import org.nrg.framework.annotations.XapiRestController;
import org.nrg.xapi.exceptions.InsufficientPrivilegesException;
import org.nrg.xapi.exceptions.NotFoundException;
import org.nrg.xapi.rest.AbstractXapiRestController;
import org.nrg.xapi.rest.XapiRequestMapping;
import org.nrg.xdat.preferences.SiteConfigPreferences;
import org.nrg.xdat.security.services.RoleHolder;
import org.nrg.xdat.security.services.UserManagementServiceI;
import org.nrg.xft.security.UserI;
import org.nrg.xnat.services.archive.CatalogService;
import org.nrg.xnat.web.http.ZipStreamingResponseBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

@XapiRestController
@RequestMapping(value = "/containerlogs")
public class ContainerLogsRestApi extends AbstractXapiRestController {
    private static final String JSON = MediaType.APPLICATION_JSON_UTF8_VALUE;

    private ContainerService containerService;
    private ContainerEntityService containerEntityService;

    @Autowired
    public ContainerLogsRestApi(final ContainerEntityService containerEntityService,
                                final ContainerService containerService,
                                final UserManagementServiceI userManagementService,
                                final RoleHolder roleHolder,
                                final SiteConfigPreferences preferences,
                                final CatalogService catalogService) {
        super(userManagementService, roleHolder);
        this.containerEntityService = containerEntityService;
        this.containerService = containerService;
        this._preferences = preferences;
        this.catalogService = catalogService;
    }

    @XapiRequestMapping(value = "current/all/{id}", method = GET, produces = ZipStreamingResponseBody.MEDIA_TYPE)
    @ResponseBody
    public ResponseEntity<StreamingResponseBody> getAll(final @PathVariable Long id) throws NotFoundException, InsufficientPrivilegesException, IOException {
        UserI user = getSessionUser();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, ZipStreamingResponseBody.MEDIA_TYPE)
                .header(HttpHeaders.CONTENT_DISPOSITION, getAttachmentDisposition(""+id, "zip"))
                .body((StreamingResponseBody) new ZipStreamingResponseBody(catalogService.getResourcesForCatalog(user, String.valueOf(id))));
    }

    @XapiRequestMapping(value = "current/{file}/{id}", method = GET)
    @ResponseBody
    public ResponseEntity<StreamingResponseBody> getOne(final @PathVariable String file, final @PathVariable Long id) throws NotFoundException, InsufficientPrivilegesException, IOException {
        UserI user = getSessionUser();
        Map<String, org.springframework.core.io.Resource> resources = catalogService.getResourcesForCatalog(user, String.valueOf(id));
        Map<String, org.springframework.core.io.Resource> specifiedResources = new HashMap<String,org.springframework.core.io.Resource>();
        for (Map.Entry<String, org.springframework.core.io.Resource> entry : resources.entrySet()){
            if(FilenameUtils.getBaseName(entry.getKey()).equalsIgnoreCase(file)){
                specifiedResources.put(entry.getKey(),entry.getValue());
            }
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, ZipStreamingResponseBody.MEDIA_TYPE)
                .header(HttpHeaders.CONTENT_DISPOSITION, getAttachmentDisposition(""+id, "zip"))
                .body((StreamingResponseBody) new ZipStreamingResponseBody(specifiedResources));
    }

    @XapiRequestMapping(value = "final/all/{id}", method = GET, produces = ZipStreamingResponseBody.MEDIA_TYPE)
    @ResponseBody
    public ResponseEntity<StreamingResponseBody> getAllFinal(final @PathVariable Long id) throws NotFoundException, IOException, InsufficientPrivilegesException {
        final ContainerEntity containerEntity = containerEntityService.retrieve(id);
        if (containerEntity == null) {
            throw new NotFoundException("ContainerExecution " + id + " not found.");
        }
        Map<String, org.springframework.core.io.Resource> specifiedResources = new HashMap<String,org.springframework.core.io.Resource>();
        Set<String> paths = containerEntity.getLogPaths();
        for(String path: paths) {
            specifiedResources.put(path,new FileSystemResource(path));
        }



        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, ZipStreamingResponseBody.MEDIA_TYPE)
                .header(HttpHeaders.CONTENT_DISPOSITION, getAttachmentDisposition(""+id, "zip"))
                .body((StreamingResponseBody) new ZipStreamingResponseBody(specifiedResources));
    }

    @XapiRequestMapping(value = "final/{file}/{id}", method = GET)
    @ResponseBody
    public ResponseEntity<StreamingResponseBody> getOneFinal(final @PathVariable String file, final @PathVariable Long id) throws NotFoundException {
        final ContainerEntity containerEntity = containerEntityService.retrieve(id);
        if (containerEntity == null) {
            throw new NotFoundException("ContainerExecution " + id + " not found.");
        }
        String path = containerEntity.getLogPathByFileName(file);
        Map<String, org.springframework.core.io.Resource> specifiedResources = new HashMap<String,org.springframework.core.io.Resource>();
        specifiedResources.put(path,new FileSystemResource(path));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, ZipStreamingResponseBody.MEDIA_TYPE)
                .header(HttpHeaders.CONTENT_DISPOSITION, getAttachmentDisposition(""+id, "zip"))
                .body((StreamingResponseBody) new ZipStreamingResponseBody(specifiedResources));
    }

    private static String getAttachmentDisposition(final String name, final String extension) {
        return String.format(ATTACHMENT_DISPOSITION, name, extension);
    }

    private static final String ATTACHMENT_DISPOSITION = "attachment; filename=\"%s.%s\"";
    private final SiteConfigPreferences _preferences;
    private CatalogService catalogService;
}
