package org.nrg.containers.rest;

import io.swagger.annotations.ApiParam;
import org.nrg.containers.exceptions.BadRequestException;
import org.nrg.containers.exceptions.ContainerServerException;
import org.nrg.containers.exceptions.NoServerPrefException;
import org.nrg.containers.exceptions.NotFoundException;
import org.nrg.containers.model.Container;
import org.nrg.containers.model.ContainerServer;
import org.nrg.containers.model.Image;
import org.nrg.containers.model.ImageParameters;
import org.nrg.containers.services.ContainerService;
import org.nrg.prefs.exceptions.InvalidPreferenceName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Inject;
import java.util.List;

//@Api(description = "XNAT Container Services REST API")
@RestController
@RequestMapping(value = ContainerService.CONTAINER_SERVICE_REST_PATH_PREFIX, 
        produces = {MediaType.APPLICATION_JSON_UTF8_VALUE, MediaType.TEXT_PLAIN_VALUE})
public class ContainersApi {
    private static final Logger _log = LoggerFactory.getLogger(ContainersApi.class);

    //    @ApiOperation(value = "Get list of images.", notes = "Returns a list of all images on the container server.", response = Image.class, responseContainer = "List")
//    @ApiResponses({
//            @ApiResponse(code = 200, message = "A list of images on the server"),
//            @ApiResponse(code = 500, message = "Unexpected error")})
    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    public List<Container> getAllContainers() throws NoServerPrefException, ContainerServerException {
        return service.getAllContainers();
    }

    //    @ApiOperation(value = "Gets the container with the specified id.", notes = "Returns the serialized container object with the specified id.", response = Container.class)
//    @ApiResponses({
//            @ApiResponse(code = 200, message = "Container successfully retrieved."),
//            @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
//            @ApiResponse(code = 403, message = "Not authorized to view this container."),
//            @ApiResponse(code = 404, message = "Container not found."),
//            @ApiResponse(code = 500, message = "Unexpected error")})
    @RequestMapping(value = {ContainerService.CONTAINERS_REST_PATH}, method = RequestMethod.GET, params = {"id"})
    @ResponseBody
    public Container getContainerById(@ApiParam(value = "Id of the container to fetch", required = true) @RequestParam("id") final String id) throws NoServerPrefException, NotFoundException, ContainerServerException {
        return service.getContainer(id);
    }

    //    @ApiOperation(value = "Gets the image with the specified name.", notes = "Returns the serialized image object with the specified name.", response = String.class)
//    @ApiResponses({
//            @ApiResponse(code = 200, message = "Container successfully retrieved."),
//            @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
//            @ApiResponse(code = 403, message = "Not authorized to view this container."),
//            @ApiResponse(code = 404, message = "Container not found."),
//            @ApiResponse(code = 500, message = "Unexpected error")})
    @RequestMapping(value = {ContainerService.CONTAINERS_REST_PATH + "/status"}, method = RequestMethod.GET, params = {"id"})
    @ResponseBody
    public String getContainerStatus(@ApiParam(value = "Id of the container to fetch", required = true) @RequestParam("id") final String id) throws NotFoundException, NoServerPrefException, ContainerServerException {
            return service.getContainerStatus(id);
    }

    //    @ApiOperation(value = "Get list of images.", notes = "Returns a list of all images on the container server.", response = Image.class, responseContainer = "List")
//    @ApiResponses({
//            @ApiResponse(code = 200, message = "A list of images on the server"),
//            @ApiResponse(code = 500, message = "Unexpected error")})
    @RequestMapping(value = {ContainerService.IMAGES_REST_PATH}, method = RequestMethod.GET)
    public List<Image> getAllImages() throws NoServerPrefException {
        return service.getAllImages();
    }

    //    @ApiOperation(value = "Get list of images.", notes = "Returns a list of all images on the container server.", response = Image.class, responseContainer = "List")
//    @ApiResponses({
//            @ApiResponse(code = 200, message = "A list of images on the server"),
//            @ApiResponse(code = 500, message = "Unexpected error")})
    @RequestMapping(value = {ContainerService.IMAGES_REST_PATH}, method = RequestMethod.GET, params = {"name"})
    @ResponseBody
    public Image getImageByName(final @RequestParam String name) throws NotFoundException, ContainerServerException, NoServerPrefException {
        return service.getImageByName(name);
    }

    @RequestMapping(value = {ContainerService.IMAGES_REST_PATH}, method = RequestMethod.GET, params = {"id"})
    @ResponseBody
    public Image getImageById(final @RequestParam String id) throws NotFoundException, NoServerPrefException, ContainerServerException {
        return service.getImageById(id);
    }

    @RequestMapping(value = {ContainerService.IMAGES_REST_PATH}, method = RequestMethod.DELETE, params = {"name"})
    @ResponseBody
    public String deleteImageByName(final @RequestParam String name) throws NotFoundException, NoServerPrefException, ContainerServerException {
        return service.deleteImageByName(name);
    }

    @RequestMapping(value = {ContainerService.IMAGES_REST_PATH}, method = RequestMethod.DELETE, params = {"id"})
    @ResponseBody
    public String deleteImageById(final @RequestParam(name="id") String inputId) throws NotFoundException, NoServerPrefException, ContainerServerException {
        return service.deleteImageById(inputId);
    }

    @RequestMapping(value = {ContainerService.IMAGES_REST_PATH}, method = RequestMethod.DELETE, params = {})
    @ResponseBody
    public void deleteImageNoParams() throws BadRequestException {
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
    @ResponseBody
    public String launch(@ApiParam(value = "The name of the image to launch.", required = true) @RequestParam("name") String name, @RequestBody ImageParameters launchArguments) throws NoServerPrefException, NotFoundException, ContainerServerException {
        return service.launch(name, launchArguments);
    }

    @RequestMapping(value = {ContainerService.SERVER_REST_PATH}, method = {RequestMethod.GET})
    @ResponseBody
    public ContainerServer getServer() throws NoServerPrefException {
        return service.getServer();
    }

    @RequestMapping(value = {ContainerService.SERVER_REST_PATH}, method = {RequestMethod.POST})
    @ResponseBody
    public void setServer(@RequestBody final ContainerServer containerServer) throws NoServerPrefException, InvalidPreferenceName {
        service.setServer(containerServer.host());
    }

    @Inject
    @SuppressWarnings("SpringJavaAutowiringInspection") // IntelliJ does not process the excludeFilter in ContainersSpringConfig @ComponentScan, erroneously marks this red
    private ContainerService service;
}
