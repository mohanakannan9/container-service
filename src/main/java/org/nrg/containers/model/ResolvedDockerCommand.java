package org.nrg.containers.model;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Maps;

import java.util.Map;
import java.util.Objects;

public class ResolvedDockerCommand extends ResolvedCommand {
    public static final CommandType type = CommandType.DOCKER;

    private Map<String, String> ports;

    public ResolvedDockerCommand() {}

    public ResolvedDockerCommand(final DockerCommand dockerCommand) {
        super(dockerCommand);
    }

    public CommandType getType() {
        return type;
    }

    public Map<String, String> getPorts() {
        return ports;
    }

    public void setPorts(final Map<String, String> ports) {
        this.ports = ports == null ?
                Maps.<String, String>newHashMap() :
                Maps.newHashMap(ports);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        final ResolvedDockerCommand that = (ResolvedDockerCommand) o;
        return Objects.equals(this.ports, that.ports);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), ports);
    }

    public MoreObjects.ToStringHelper addPropertiesToString(final MoreObjects.ToStringHelper helper) {
        return super.addPropertiesToString(helper)
                .add("ports", ports);
    }

    @Override
    public String toString() {
        return addPropertiesToString(MoreObjects.toStringHelper(this)).toString();
    }
}
