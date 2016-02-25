package org.nrg.containers.rest;

import io.swagger.annotations.*;
import oracle.jrockit.jfr.StringConstantPool;
import org.apache.commons.lang.StringUtils;
import org.nrg.containers.exceptions.BadRequestException;
import org.nrg.containers.exceptions.ContainerServerException;
import org.nrg.containers.exceptions.NotFoundException;
import org.nrg.containers.model.Image;
import org.nrg.containers.model.ImageParameters;
import org.nrg.containers.services.ContainerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import java.util.List;

//@Api(description = "XCS images API")
@RestController
@RequestMapping(value = "/images", produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
public class ImagesApi {
    private static final Logger _log = LoggerFactory.getLogger(ImagesApi.class);

//    @ApiOperation(value = "Get list of images.", notes = "Returns a list of all images on the container server.", response = Image.class, responseContainer = "List")
//    @ApiResponses({
//            @ApiResponse(code = 200, message = "A list of images on the server"),
//            @ApiResponse(code = 500, message = "Unexpected error")})
    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    public List<Image> getAllImages() {
        return service.getAllImages();
    }

    //    @ApiOperation(value = "Get list of images.", notes = "Returns a list of all images on the container server.", response = Image.class, responseContainer = "List")
//    @ApiResponses({
//            @ApiResponse(code = 200, message = "A list of images on the server"),
//            @ApiResponse(code = 500, message = "Unexpected error")})
    @RequestMapping(method = RequestMethod.GET, params = {"name"})
    @ResponseBody
    public Image getByName(final @RequestParam String name) throws NotFoundException {
        final Image image = service.getImageByName(name);
        if (image == null) {
            throw new NotFoundException("No image found with name "+name);
        }
        return image;
    }

    @RequestMapping(method = RequestMethod.GET, params = {"id"})
    @ResponseBody
    public Image getById(final @RequestParam String id) throws NotFoundException {
        final Image image = service.getImageById(id);
        if (image == null) {
            throw new NotFoundException("No image found with id "+id);
        }
        return image;
    }

    @RequestMapping(method = RequestMethod.DELETE, params = {"name"})
    @ResponseBody
    public String deleteByName(final @RequestParam String name) throws NotFoundException {
        final String id = service.deleteImageByName(name);
        if (id == null) {
            throw new NotFoundException("No image found with name "+name);
        }
        return id;
    }

    @RequestMapping(method = RequestMethod.DELETE, params = {"id"})
    @ResponseBody
    public String deleteById(final @RequestParam(name="id") String inputId) throws NotFoundException {
        final String id = service.deleteImageById(inputId);
        if (id == null) {
            throw new NotFoundException("No image found with id "+inputId);
        }
        return id;
    }

    @RequestMapping(method = RequestMethod.DELETE, params = {})
    @ResponseBody
    public void delete() throws BadRequestException {
        throw new BadRequestException("Include the name or id of an image to delete in the query parameters.");
    }

//    @ApiOperation(value = "Launches a container.", notes = "Returns the updated serialized image object with the specified image name.", response = Image.class)
//    @ApiResponses({
//            @ApiResponse(code = 200, message = "Image successfully created or updated."),
//            @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
//            @ApiResponse(code = 403, message = "Not authorized to create or update this image."),
//            @ApiResponse(code = 404, message = "Image not found."),
//            @ApiResponse(code = 500, message = "Unexpected error")})
    @RequestMapping(value = {"/launch"}, method = {RequestMethod.POST}, params = {"name"})
    public ResponseEntity<String> launch(@ApiParam(value = "The name of the image to launch.", required = true) @RequestParam("name") String name, @RequestBody ImageParameters launchArguments) {
        final String containerId =  service.launch(name, launchArguments);
        return StringUtils.isBlank(containerId) ?
                new ResponseEntity<String>(HttpStatus.INTERNAL_SERVER_ERROR) :
                new ResponseEntity<>(containerId, HttpStatus.OK);
    }

    @Inject
    private ContainerService service;
}
