package org.nrg.containers.services;

import org.nrg.containers.exceptions.DockerServerException;
import org.nrg.containers.exceptions.NoHubException;
import org.nrg.containers.exceptions.NoServerPrefException;
import org.nrg.containers.exceptions.NotFoundException;
import org.nrg.containers.model.Container;
import org.nrg.containers.model.DockerHub;
import org.nrg.containers.model.DockerServer;
import org.nrg.containers.model.DockerImage;
import org.nrg.prefs.exceptions.InvalidPreferenceName;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface ContainerService {

//    List<DockerImage> getAllImages() throws NoServerPrefException, DockerServerException;
//
//    DockerImage getImageByName(String name) throws NoServerPrefException, NotFoundException, DockerServerException;
//
//    DockerImage getImageById(String id) throws NoServerPrefException, NotFoundException, DockerServerException;
//
//    String deleteImageById(String id, Boolean onServer) throws NoServerPrefException, NotFoundException, DockerServerException;
//
//    String deleteImageByName(String name, Boolean onServer) throws NoServerPrefException, NotFoundException, DockerServerException;

    List<Container> getContainers() throws NoServerPrefException, DockerServerException;

    String getContainerStatus(String id) throws NoServerPrefException, NotFoundException, DockerServerException;

    Container getContainer(String id) throws NoServerPrefException, NotFoundException, DockerServerException;

    String launch(String imageName, Map<String, String> params, Boolean wait)
            throws NoServerPrefException, NotFoundException, DockerServerException;

    String launchOn(String imageName, String xnatId, String type, Map<String, String> launchArguments, Boolean wait)
            throws NoServerPrefException, NotFoundException, DockerServerException;


    String launchFromScript(String scriptId, Map<String, String> launchArguments, Boolean wait) throws Exception;

    String getContainerLogs(String id) throws NoServerPrefException, NotFoundException, DockerServerException;

    String verbContainer(String id, String status) throws NoServerPrefException, NotFoundException, DockerServerException;

//    void pullByName(String image, String hub, String hubUsername, String hubPassword)
//        throws NoHubException, NotFoundException, DockerServerException, IOException, NoServerPrefException;
//
//    void pullByName(String image, String hub)
//        throws NoHubException, NotFoundException, DockerServerException, IOException, NoServerPrefException;

    DockerImage pullFromSource(String source, String name) throws NoHubException, NotFoundException, DockerServerException;

//    void setMetadataByName(String name, ImageMetadata metadata, String project, Boolean overwrite, Boolean ignoreBlank)
//            throws NoServerPrefException, NotFoundException, DockerServerException;
//
//    void setMetadataById(String id, ImageMetadata metadata, String project, Boolean overwrite, Boolean ignoreBlank)
//            throws NoServerPrefException, NotFoundException, DockerServerException;

    String setMetadataById(String id, Map<String, String> metadata, String project, Boolean overwrite, Boolean ignoreBlank)
            throws NoServerPrefException, NotFoundException;
}
