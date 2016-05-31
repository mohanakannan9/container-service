package org.nrg.actions.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.envers.Audited;
import org.nrg.containers.model.DockerImage;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntity;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Transient;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Entity
@Audited
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "nrg")
public class ResolvedCommand extends AbstractHibernateEntity {

    private Command command;
    @JsonProperty("run") private String run;
    @JsonProperty("docker-image") private DockerImage dockerImage;
    @JsonProperty("env") private Map<String, String> environmentVariables;
    @JsonProperty("mounts-in") private List<CommandMount> mountsIn;
    @JsonProperty("mounts-out") private List<CommandMount> mountsOut;

    public ResolvedCommand() {}

    public ResolvedCommand(final Command command) {
        this.command = command;
    }

    public Command getCommand() {
        return command;
    }

    public void setCommand(final Command command) {
        this.command = command;
    }

    public String getRun() {
        return run;
    }

    public void setRun(final String run) {
        this.run = run;
    }

    public DockerImage getDockerImage() {
        return dockerImage;
    }

    public void setDockerImage(final DockerImage dockerImage) {
        this.dockerImage = dockerImage;
    }

    public Map<String, String> getEnvironmentVariables() {
        return environmentVariables;
    }

    public void setEnvironmentVariables(final Map<String, String> environmentVariables) {
        this.environmentVariables = environmentVariables;
    }

    @ElementCollection
    public List<CommandMount> getMountsIn() {
        return mountsIn;
    }

    public void setMountsIn(final List<CommandMount> mountsIn) {
        this.mountsIn = mountsIn;
    }

    @ElementCollection
    public List<CommandMount> getMountsOut() {
        return mountsOut;
    }

    public void setMountsOut(final List<CommandMount> mountsOut) {
        this.mountsOut = mountsOut;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        final ResolvedCommand that = (ResolvedCommand) o;
        return Objects.equals(this.command, that.command) &&
                Objects.equals(this.run, that.run) &&
                Objects.equals(this.dockerImage, that.dockerImage) &&
                Objects.equals(this.environmentVariables, that.environmentVariables) &&
                Objects.equals(this.mountsIn, that.mountsIn) &&
                Objects.equals(this.mountsOut, that.mountsOut);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), command, run, dockerImage, environmentVariables, mountsIn, mountsOut);
    }

    @Override
    public String toString() {
        return addParentPropertiesToString(MoreObjects.toStringHelper(this))
                .add("command", command)
                .add("run", run)
                .add("dockerImage", dockerImage)
                .add("environmentVariables", environmentVariables)
                .add("mountsIn", mountsIn)
                .add("mountsOut", mountsOut)
                .toString();
    }
}
