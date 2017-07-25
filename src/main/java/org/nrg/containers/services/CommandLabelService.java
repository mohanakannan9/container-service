package org.nrg.containers.services;

import org.nrg.containers.model.command.auto.Command;
import org.nrg.containers.model.image.docker.DockerImage;

import java.util.List;

public interface CommandLabelService {
    String LABEL_KEY = "org.nrg.commands";

    List<Command> parseLabels(DockerImage dockerImage);
    List<Command> parseLabels(String imageName, DockerImage dockerImage);
}
