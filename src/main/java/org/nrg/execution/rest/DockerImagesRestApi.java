package org.nrg.execution.rest;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.nrg.execution.exceptions.BadRequestException;
import org.nrg.execution.exceptions.DockerServerException;
import org.nrg.execution.exceptions.NoServerPrefException;
import org.nrg.execution.exceptions.NotFoundException;
import org.nrg.execution.model.DockerImage;
import org.nrg.execution.services.DockerService;
import org.nrg.framework.annotations.XapiRestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;

import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

//@Api(description = "XNAT Container Services REST API")
@XapiRestController
@RequestMapping(value = "/docker/images")
public class DockerImagesRestApi {
    private static final Logger _log = LoggerFactory.getLogger(DockerImagesRestApi.class);

//    @Autowired
//    @SuppressWarnings("SpringJavaAutowiringInspection") // IntelliJ does not process the excludeFilter in ContainersSpringConfig @ComponentScan, erroneously marks this red
//    private ContainerService service;

    @Autowired
    private DockerService dockerService;

    public static final String ID_REGEX = "(?:[a-zA-Z0-9-_+.]+:)?[a-fA-F0-9]{6,}";

    public static final String JSON = MediaType.APPLICATION_JSON_UTF8_VALUE;
    public static final String TEXT = MediaType.TEXT_PLAIN_VALUE;

    @ApiOperation(value = "Get list of images.", notes = "Returns a list of all Docker images.")
    @ApiResponses({
            @ApiResponse(code = 200, message = "A list of images on the server"),
            @ApiResponse(code = 424, message = "Admin must set up Docker server."),
            @ApiResponse(code = 500, message = "Unexpected error")})
    @RequestMapping(method = GET, produces = JSON)
    @ResponseBody
    public List<DockerImage> getAllImages()
            throws NoServerPrefException, DockerServerException {
        return dockerService.getImages();
    }

    @ApiOperation(value = "Get Docker image",
            notes = "Retrieve information about a Docker image from the docker server")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Found the image"),
            @ApiResponse(code = 404, message = "No docker image with given id on the server"),
            @ApiResponse(code = 424, message = "Admin must set up Docker server."),
            @ApiResponse(code = 500, message = "Unexpected error")})
    @RequestMapping(value = "/{id}", method = GET, produces = JSON)
    @ResponseBody
    public DockerImage getImage(final @PathVariable("id") String id)
            throws NoServerPrefException, NotFoundException {
        return dockerService.getImage(id);
    }

    @ApiOperation(value = "Delete Docker image",
            notes = "Remove information about a Docker image")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Image was removed"),
            @ApiResponse(code = 404, message = "No docker image with given id on docker server"),
            @ApiResponse(code = 424, message = "Admin must set up Docker server."),
            @ApiResponse(code = 500, message = "Unexpected error")})
    @RequestMapping(value = "/{id}", method = DELETE)
    @ResponseBody
    public void deleteImage(final @PathVariable("id") String id,
                            final @RequestParam(value = "force", defaultValue = "false") Boolean force)
            throws NotFoundException, NoServerPrefException, DockerServerException {
        dockerService.removeImage(id, force);
    }

//    @RequestMapping(value = {"/{name:.*}", "/name/{name}"}, method = GET, produces = {JSON, PLAIN_TEXT})
//    @ResponseBody
//    public DockerImage getImageByName(final @PathVariable("name") String name,
//                                      final @RequestParam(name = "project", required = false) String projectParam,
//                                      final @RequestParam(name = "metadata", required = false) Boolean metadata)
//            throws NotFoundException, DockerServerException, NoServerPrefException {
//        return service.getImageByName(name);
//    }
//
//    @RequestMapping(value = {"/{repo}/{image}", "/name/{repo}/{image}"}, method = GET, produces = {JSON, PLAIN_TEXT})
//    @ResponseBody
//    public DockerImage getImageByNameWithRepo(final @PathVariable("image") String image,
//                                              final @PathVariable("repo") String repo,
//                                              final @RequestParam(name = "project", required = false) String projectParam,
//                                              final @RequestParam(name = "metadata", required = false) Boolean metadata)
//            throws NotFoundException, DockerServerException, NoServerPrefException {
//        final String name = repo + "/" + image;
//        return service.getImageByName(name);
//    }
//
//    @RequestMapping(value = {"/name/{name}"},
//            method = DELETE, produces = PLAIN_TEXT)
//    @ResponseBody
//    public String deleteImageByName(final @PathVariable("name") String name,
//                                    final @RequestParam(name = "project", required = false) String projectParam,
//                                    final @RequestParam(name = "server", defaultValue = "false") Boolean deleteOnServer)
//            throws NotFoundException, NoServerPrefException, DockerServerException {
//        return service.deleteImageByName(name, deleteOnServer);
//    }
//
//    @RequestMapping(value = {"/name/{repo}/{image}"},
//            method = DELETE, produces = PLAIN_TEXT)
//    @ResponseBody
//    public String deleteImageByNameWithRepo(final @PathVariable("image") String image,
//                                    final @PathVariable("repo") String repo,
//                                    final @RequestParam(name = "project", required = false) String projectParam,
//                                    final @RequestParam(name = "server", defaultValue = "false") Boolean deleteOnServer)
//            throws NotFoundException, NoServerPrefException, DockerServerException {
//        final String name = repo != null ? repo + "/" + image : image;
//        return service.deleteImageByName(name, deleteOnServer);
//    }
//
//    @RequestMapping(value = "/id/{id}", method = DELETE, produces = PLAIN_TEXT)
//    @ResponseBody
//    public String deleteImageById(final @PathVariable("id") String id,
//                                  final @RequestParam(name = "project", required = false) String projectParam,
//                                  final @RequestParam(name = "server", defaultValue = "false") Boolean deleteOnServer)
//            throws NotFoundException, NoServerPrefException, DockerServerException {
//        return service.deleteImageById(id, deleteOnServer);
//    }

//    @RequestMapping(value = {"/images/{name:.*}", "/images/name/{name}"}, method = POST, consumes = JSON)
//    @ResponseStatus(value = HttpStatus.CREATED)
//    public void setMetadataByName(final @PathVariable("name") String name,
//                                    final @RequestBody ImageMetadata metadata,
//                                    final @RequestParam(value = "project", required = false) String project,
//                                    final @RequestParam(value = "overwrite", defaultValue = "false") Boolean overwrite,
//                                    final @RequestParam(value = "ignoreBlank", defaultValue = "false") Boolean ignoreBlank)
//            throws NoServerPrefException, NotFoundException, DockerServerException {
//        service.setMetadataByName(name, metadata, project, overwrite, ignoreBlank);
//    }
//
//    @RequestMapping(value = {"/images/{repo}/{image}", "/images/name/{repo}/{image}"}, method = POST, consumes = JSON)
//    @ResponseStatus(value = HttpStatus.CREATED)
//    public void setMetadataByNameWithRepo(final @PathVariable("image") String image,
//                                            final @PathVariable("repo") String repo,
//                                            final @RequestBody ImageMetadata metadata,
//                                            final @RequestParam(value = "project", required = false) String project,
//                                            final @RequestParam(value = "overwrite", defaultValue = "false") Boolean overwrite,
//                                            final @RequestParam(value = "ignoreBlank", defaultValue = "false") Boolean ignoreBlank)
//            throws NoServerPrefException, NotFoundException, DockerServerException {
//        final String name = repo != null ? repo + "/" + image : image;
//        service.setMetadataByName(name, metadata, project, overwrite, ignoreBlank);
//    }
//
//    @RequestMapping(value = "/images/{id:"+ID_REGEX+"}", method = POST, consumes = JSON)
//    @ResponseStatus(value = HttpStatus.CREATED)
//    public void setMetadataByIdWithNameFallback(final @PathVariable("id") String imageId,
//                                    final @RequestBody ImageMetadata metadata,
//                                    final @RequestParam(value = "project", required = false) String project,
//                                    final @RequestParam(value = "overwrite", defaultValue = "false") Boolean overwrite,
//                                    final @RequestParam(value = "ignoreBlank", defaultValue = "false") Boolean ignoreBlank)
//            throws NoServerPrefException, NotFoundException, DockerServerException {
//        try {
//            service.setMetadataById(imageId, metadata, project, overwrite, ignoreBlank);
//        } catch (NotFoundException e) {
//            _log.warn("Could not find image with id " + imageId + ". Trying as name.");
//            service.setMetadataByName(imageId, metadata, project, overwrite, ignoreBlank);
//        }
//    }
//    @RequestMapping(value = "/images/id/{id}", method = POST, consumes = JSON)
//    @ResponseStatus(value = HttpStatus.CREATED)
//    public void setMetadataById(final @PathVariable("id") String imageId,
//                                    final @RequestBody ImageMetadata metadata,
//                                    final @RequestParam(value = "project", required = false) String project,
//                                    final @RequestParam(value = "overwrite", defaultValue = "false") Boolean overwrite,
//                                    final @RequestParam(value = "ignoreBlank", defaultValue = "false") Boolean ignoreBlank)
//            throws NoServerPrefException, NotFoundException, DockerServerException {
//        service.setMetadataById(imageId, metadata, project, overwrite, ignoreBlank);
//    }

    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(value = {DockerServerException.class})
    public String handleDockerServerError(final Exception e) {
        return "The Docker server returned an error:\n" + e.getMessage();
    }

    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    @ExceptionHandler(value = {BadRequestException.class})
    public String handleBadRequest(final Exception e) {
        return e.getMessage();
    }

    @ResponseStatus(value = HttpStatus.FAILED_DEPENDENCY)
    @ExceptionHandler(value = {NoServerPrefException.class})
    public String handleFailedDependency() {
        return "Set up Docker server before using this REST endpoint.";
    }

    @ResponseStatus(value = HttpStatus.NOT_FOUND)
    @ExceptionHandler(value = {NotFoundException.class})
    public String handleNotFound(final Exception e) {
        return e.getMessage();
    }

}
