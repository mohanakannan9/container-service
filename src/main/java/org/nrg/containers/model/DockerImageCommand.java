package org.nrg.containers.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.envers.Audited;
import org.nrg.actions.model.Command;

import javax.persistence.DiscriminatorValue;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import java.util.Map;
import java.util.Objects;

@Audited
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "nrg")
@DiscriminatorValue(DockerImageCommand.COMMAND_TYPE)
public class DockerImageCommand extends Command {

    static final String COMMAND_TYPE = "docker-image";

    @JsonProperty("docker-image") private DockerImage dockerImage;
    private String command;
    @JsonProperty("env") private Map<String, String> environmentVariables;

    @ManyToOne(fetch = FetchType.EAGER)
    public DockerImage getDockerImage() {
        return dockerImage;
    }

    public void setDockerImage(final DockerImage dockerImage) {
        this.dockerImage = dockerImage;
    }

    @ElementCollection
    public Map<String, String> getEnvironmentVariables() {
        return environmentVariables;
    }

    public void setEnvironmentVariables(final Map<String, String> environmentVariables) {
        this.environmentVariables = environmentVariables;
    }

    @Override
    public void run() {
        // TODO
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !super.equals(o) || getClass() != o.getClass()) {
            return false;
        }

        DockerImageCommand that = (DockerImageCommand) o;


        return Objects.equals(this.dockerImage, that.dockerImage) &&
            Objects.equals(this.environmentVariables, that.environmentVariables);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), dockerImage, environmentVariables);
    }


    @Override
    public String toString() {
        return addParentPropertiesToString(MoreObjects.toStringHelper(this))
                .add("dockerImage", dockerImage)
                .add("command", command)
                .add("environmentVariables", environmentVariables)
                .toString();
    }
}