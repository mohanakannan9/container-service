package org.nrg.containers.rest;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.swagger.annotations.ApiParam;
import org.apache.commons.io.IOUtils;
import org.nrg.containers.exceptions.DockerServerException;
import org.nrg.containers.exceptions.NoServerPrefException;
import org.nrg.containers.model.container.auto.Container;
import org.nrg.containers.services.ContainerService;
import org.nrg.framework.annotations.XapiRestController;
import org.nrg.framework.exceptions.NotFoundException;
import org.nrg.xapi.exceptions.InsufficientPrivilegesException;
import org.nrg.xapi.rest.AbstractXapiRestController;
import org.nrg.xapi.rest.XapiRequestMapping;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.security.services.RoleHolder;
import org.nrg.xdat.security.services.UserManagementServiceI;
import org.nrg.xft.security.UserI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.nrg.xdat.security.helpers.AccessLevel.Admin;
import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@XapiRestController
@RequestMapping(value = "/containers")
public class ContainerRestApi extends AbstractXapiRestController {
    private static final Logger log = LoggerFactory.getLogger(ContainerRestApi.class);

    private static final String JSON = MediaType.APPLICATION_JSON_UTF8_VALUE;
    private static final String TEXT = MediaType.TEXT_PLAIN_VALUE;
    private static final String ZIP = "application/zip";
    private static final String ATTACHMENT_DISPOSITION = "attachment; filename=\"%s.%s\"";

    private ContainerService containerService;

    @Autowired
    public ContainerRestApi(final ContainerService containerService,
                            final UserManagementServiceI userManagementService,
                            final RoleHolder roleHolder) {
        super(userManagementService, roleHolder);
        this.containerService = containerService;
    }

    @XapiRequestMapping(method = GET, restrictTo = Admin)
    @ResponseBody
    public List<Container> getAll() {
        return Lists.transform(containerService.getAll(), new Function<Container, Container>() {
            @Override
            public Container apply(final Container input) {
                return scrubPasswordEnv(input);
            }
        });
    }

    @XapiRequestMapping(value = "/{id}", method = GET, restrictTo = Admin)
    @ResponseBody
    public Container get(final @PathVariable String id) throws NotFoundException {
        return scrubPasswordEnv(containerService.get(id));
    }

    @XapiRequestMapping(value = "/{id}", method = DELETE, restrictTo = Admin)
    public ResponseEntity<Void> delete(final @PathVariable String id) throws NotFoundException {
        containerService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @XapiRequestMapping(value = "/{id}/finalize", method = POST, produces = JSON, restrictTo = Admin)
    public void finalize(final @PathVariable String id) throws NotFoundException {
        final UserI userI = XDAT.getUserDetails();
        containerService.finalize(id, userI);
    }

    @XapiRequestMapping(value = "/{id}/kill", method = POST, restrictTo = Admin)
    @ResponseBody
    public String kill(final @PathVariable String id)
            throws NotFoundException, NoServerPrefException, DockerServerException {
        final UserI userI = XDAT.getUserDetails();
        return containerService.kill(id, userI);
    }

    private Container scrubPasswordEnv(final Container container) {
        final Map<String, String> scrubbedEnvironmentVariables = Maps.newHashMap();
        for (final Map.Entry<String, String> env : container.environmentVariables().entrySet()) {
            scrubbedEnvironmentVariables.put(env.getKey(),
                    env.getKey().equals("XNAT_PASS") ? "******" : env.getValue());
        }
        return container.toBuilder().environmentVariables(scrubbedEnvironmentVariables).build();
    }

    @XapiRequestMapping(value = "/{containerId}/logs", method = GET, restrictTo = Admin)
    @ResponseBody
    public ResponseEntity<ZipOutputStream> getLogs(final @PathVariable String containerId)
            throws IOException, InsufficientPrivilegesException, NoServerPrefException, DockerServerException, NotFoundException {
        UserI userI = XDAT.getUserDetails();

        final Map<String, InputStream> logStreams = containerService.getLogStreams(containerId);
        final ZipOutputStream zipOutputStream = createZipFromStreams(logStreams);

        return zipOutputStream == null ?
                new ResponseEntity<ZipOutputStream>(HttpStatus.INTERNAL_SERVER_ERROR) :
                ResponseEntity.<ZipOutputStream>ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, getAttachmentDisposition(containerId, "zip"))
                        .header(HttpHeaders.CONTENT_TYPE, ZIP)
                        .body(zipOutputStream);
    }

    @XapiRequestMapping(value = "/{containerId}/logs/{file}", method = GET, restrictTo = Admin)
    @ResponseBody
    public ResponseEntity<String> getLog(final @PathVariable String containerId,
                                         final @PathVariable @ApiParam(allowableValues = "stdout, stderr") String file)
            throws NoServerPrefException, DockerServerException, NotFoundException {
        UserI userI = XDAT.getUserDetails();

        final InputStream logStream = containerService.getLogStream(containerId, file);
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        try {
            while ((length = logStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, length);
            }
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, getAttachmentDisposition(containerId + "-" + file, "log"))
                    .header(HttpHeaders.CONTENT_TYPE, TEXT)
                    .body(byteArrayOutputStream.toString(StandardCharsets.UTF_8.name()));
        } catch (IOException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    private ZipOutputStream createZipFromStreams(Map<String, InputStream> streams) {
        try(final ZipOutputStream zipStream = new ZipOutputStream(new ByteArrayOutputStream()) ) {

            for(final String streamName : streams.keySet()){
                final InputStream inputStream = streams.get(streamName);
                final ZipEntry entry = new ZipEntry(streamName);
                try {
                    zipStream.putNextEntry(entry);

                    byte[] readBuffer = new byte[2048];
                    int amountRead;

                    while ((amountRead = inputStream.read(readBuffer)) > 0) {
                        zipStream.write(readBuffer, 0, amountRead);
                    }
                } catch (IOException e) {
                    log.error("There was a problem writing %s to the zip. " + e.getMessage(), streamName);
                }
            }

            return zipStream;
        } catch (IOException e) {
            log.error("There was a problem opening the zip stream.", e);
        }

        return null;
    }

    private static String getAttachmentDisposition(final String name, final String extension) {
        return String.format(ATTACHMENT_DISPOSITION, name, extension);
    }

    @ResponseStatus(value = HttpStatus.NOT_FOUND)
    @ExceptionHandler(value = {NotFoundException.class})
    public String handleNotFound(final Exception e) {
        return e.getMessage();
    }

    @ResponseStatus(value = HttpStatus.FAILED_DEPENDENCY)
    @ExceptionHandler(value = {NoServerPrefException.class})
    public String handleFailedDependency() {
        return "Set up Docker server before using this REST endpoint.";
    }

    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(value = {DockerServerException.class})
    public String handleDockerServerException(final Exception e) {
        return e.getMessage();
    }
}
