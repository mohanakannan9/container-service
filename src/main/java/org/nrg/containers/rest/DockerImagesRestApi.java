package org.nrg.containers.rest;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.commons.lang3.StringUtils;
import org.nrg.containers.exceptions.BadRequestException;
import org.nrg.containers.exceptions.DockerServerException;
import org.nrg.containers.exceptions.NoServerPrefException;
import org.nrg.containers.exceptions.NotFoundException;
import org.nrg.containers.model.DockerImageDto;
import org.nrg.containers.services.DockerImageService;
import org.nrg.containers.services.DockerService;
import org.nrg.framework.annotations.XapiRestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;

import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

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

    @Autowired
    private DockerImageService dockerImageService;

    public static final String ID_REGEX = "(?:[a-zA-Z0-9-_+.]+:)?[a-fA-F0-9]{6,}";

    public static final String JSON = MediaType.APPLICATION_JSON_UTF8_VALUE;
    public static final String TEXT = MediaType.TEXT_PLAIN_VALUE;

    @ApiOperation(value = "Get list of images.", notes = "Returns a list of all images on the container server.", response = DockerImageDto.class, responseContainer = "List")
    @ApiResponses({
            @ApiResponse(code = 200, message = "A list of images on the server"),
            @ApiResponse(code = 500, message = "Unexpected error")})
    @RequestMapping(method = GET, produces = JSON)
    @ResponseBody
    public List<DockerImageDto> getAllImages(final @RequestParam(value = "from-db", defaultValue = "true") Boolean fromDb,
                                             final @RequestParam(value = "from-docker-server", defaultValue = "true") Boolean fromDockerServer)
            throws NoServerPrefException, DockerServerException, BadRequestException {
        if (!(fromDb || fromDockerServer)) {
            throw new BadRequestException("At least one of the query params \"from-db\" or \"from-docker-server\" must be \"true\".");
        }
        return dockerService.getImages(fromDb, fromDockerServer);
    }

    @RequestMapping(value = {}, method = POST, consumes = JSON, produces = TEXT)
    public ResponseEntity<String> postImage(final @RequestBody DockerImageDto dockerImageDto)
            throws BadRequestException, DockerServerException, NotFoundException, NoServerPrefException {
        if (StringUtils.isBlank(dockerImageDto.getImageId())) {
            throw new BadRequestException("Cannot add Docker image. Please set image-id on request body.");
        }
        final DockerImageDto created = dockerService.createImage(dockerImageDto);
        return new ResponseEntity<>(String.valueOf(created.getId()), HttpStatus.CREATED);
    }

    @RequestMapping(value = "/{id}", method = GET, produces = JSON)
    @ResponseBody
    public DockerImageDto getImage(final @PathVariable("id") Long id,
                                   final @RequestParam(value = "from-docker-server", defaultValue = "true") Boolean fromDockerServer)
            throws NotFoundException {
        return dockerService.getImage(id, fromDockerServer);
    }

    @RequestMapping(value = "/{id}", method = DELETE)
    @ResponseBody
    public void deleteImage(final @PathVariable("id") Long id,
                            final @RequestParam(value = "from-docker-server", defaultValue = "false") Boolean fromDockerServer)
            throws NotFoundException, NoServerPrefException, DockerServerException {
        dockerService.removeImage(id, fromDockerServer);
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
