package org.nrg.containers.rest;

import org.nrg.containers.exceptions.BadRequestException;
import org.nrg.containers.exceptions.ContainerServerException;
import org.nrg.containers.exceptions.NoServerPrefException;
import org.nrg.containers.exceptions.NotFoundException;
import org.nrg.containers.model.Image;
import org.nrg.containers.services.ContainerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

//@Api(description = "XNAT Container Services REST API")
@RestController
@RequestMapping(value = "/containers/images")
public class ImagesApi {
    private static final Logger _log = LoggerFactory.getLogger(ImagesApi.class);

    @Autowired
    @SuppressWarnings("SpringJavaAutowiringInspection") // IntelliJ does not process the excludeFilter in ContainersSpringConfig @ComponentScan, erroneously marks this red
    private ContainerService service;

    public static final String JSON = MediaType.APPLICATION_JSON_UTF8_VALUE;
    public static final String PLAIN_TEXT = MediaType.TEXT_PLAIN_VALUE;

    //    @ApiOperation(value = "Get list of images.", notes = "Returns a list of all images on the container server.", response = Image.class, responseContainer = "List")
//    @ApiResponses({
//            @ApiResponse(code = 200, message = "A list of images on the server"),
//            @ApiResponse(code = 500, message = "Unexpected error")})
    @RequestMapping(method = GET, produces = {JSON, PLAIN_TEXT})
    public List<Image> getAllImages(final @RequestParam(value = "project", required = false) String projectParam,
                                    final @RequestParam(value = "metadata", required = false) Boolean metadata)
            throws NoServerPrefException {
        return service.getAllImages();
    }

    @RequestMapping(method = GET, params = "name", produces = {JSON, PLAIN_TEXT})
    @ResponseBody
    public Image getImageByName(final @RequestParam("name") String name,
                                final @RequestParam(name = "project", required = false) String projectParam,
                                final @RequestParam(name = "metadata", required = false) Boolean metadata)
            throws NotFoundException, ContainerServerException, NoServerPrefException {
        return service.getImageByName(name);
    }

    @RequestMapping(method = GET, params = "id", produces = {JSON, PLAIN_TEXT})
    @ResponseBody
    public Image getImageById(final @RequestParam("id") String id,
                              final @RequestParam(name = "project", required = false) String projectParam,
                              final @RequestParam(name = "metadata", required = false) Boolean metadata)
            throws NotFoundException, NoServerPrefException, ContainerServerException {
        return service.getImageById(id);
    }

    @RequestMapping(method = DELETE, params = "name", produces = PLAIN_TEXT)
    @ResponseBody
    public String deleteImageByName(final @RequestParam("name") String name,
                                    final @RequestParam(name = "project", required = false) String projectParam,
                                    final @RequestParam(name = "server", defaultValue = "false") Boolean deleteOnServer)
            throws NotFoundException, NoServerPrefException, ContainerServerException {
        return service.deleteImageByName(name, deleteOnServer);
    }

    @RequestMapping(method = DELETE, params = "id", produces = PLAIN_TEXT)
    @ResponseBody
    public String deleteImageById(final @RequestParam("id") String id,
                                  final @RequestParam(name = "project", required = false) String projectParam,
                                  final @RequestParam(name = "server", defaultValue = "false") Boolean deleteOnServer)
            throws NotFoundException, NoServerPrefException, ContainerServerException {
        return service.deleteImageById(id, deleteOnServer);
    }

    @RequestMapping(method = DELETE, params = {})
    @ResponseBody
    public void deleteImageNoParams() throws BadRequestException {
        throw new BadRequestException("Include the name or id of an image to delete in the query parameters.");
    }
}
