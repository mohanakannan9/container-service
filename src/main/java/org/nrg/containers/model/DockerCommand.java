package org.nrg.containers.model;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.collect.Maps;
import org.hibernate.envers.Audited;

import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Transient;
import java.util.Map;
import java.util.Objects;

@Entity
@Audited
@DiscriminatorColumn(name = "type", discriminatorType = DiscriminatorType.STRING)
@DiscriminatorValue("docker")
public class DockerCommand extends Command {
    public static final Type type = Type.DOCKER;


    private String index;
    private String hash;
    private Map<String, String> ports;

    public String getIndex() {
        return index;
    }

    public void setIndex(final String index) {
        this.index = index;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(final String hash) {
        this.hash = hash;
    }

    @ElementCollection
    public Map<String, String> getPorts() {
        return ports;
    }

    public void setPorts(final Map<String, String> ports) {
        this.ports = ports == null ?
                Maps.<String, String>newHashMap() :
                ports;

    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        final DockerCommand that = (DockerCommand) o;
        return Objects.equals(this.index, that.index) &&
                Objects.equals(this.hash, that.hash) &&
                Objects.equals(this.ports, that.ports);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), index, hash, ports);
    }

    @Override
    public ToStringHelper addParentPropertiesToString(final ToStringHelper helper) {
        return super.addParentPropertiesToString(helper)
                .add("type", type)
                .add("index", index)
                .add("hash", hash)
                .add("ports", ports);
    }

    @Override
    public String toString() {
        return addParentPropertiesToString(MoreObjects.toStringHelper(this))
                .toString();
    }



    // @ApiModelProperty(value = "The ID of the docker image where this Command will run", required = true)
    // public String getDockerImage() {
    //     return dockerImage;
    // }
    //
    // public void setDockerImage(final String dockerImage) {
    //     this.dockerImage = dockerImage;
    // }


    @Transient
    public void update(final DockerCommand other, final Boolean ignoreNull) {
        super.update(other, ignoreNull);
        // if (other == null) {
        //     return;
        // }
        //
        // if (other.description != null || !ignoreNull) {
        //     this.description = other.description;
        // }
        // if (other.infoUrl != null || !ignoreNull) {
        //     this.infoUrl = other.infoUrl;
        // }
        //
        // if (this.run == null || (other.run == null && !ignoreNull)) {
        //     this.run = other.run;
        // } else {
        //     this.run.update(other.run, ignoreNull);
        // }
        //
        // if (this.inputs == null || this.inputs.isEmpty()) {
        //     // If we have no inputs, just take what we are given.
        //     // No need to go digging through lists when we're comparing to nothing.
        //     // Even if the "update" is empty too, no big deal.
        //     this.inputs = other.inputs;
        // } else {
        //     final Map<String, CommandInput> inputsToUpdateMap = Maps.newHashMap();
        //     if (other.inputs != null) {
        //         for (final CommandInput otherOutput : other.inputs) {
        //             inputsToUpdateMap.put(otherOutput.getName(), otherOutput);
        //         }
        //     }
        //
        //     // Update any inputs that already exist
        //     final Iterator<CommandInput> iterator = this.inputs.iterator();
        //     while (iterator.hasNext()) {
        //         final CommandInput thisInput = iterator.next();
        //         if (inputsToUpdateMap.containsKey(thisInput.getName())) {
        //             // The mount we are looking at is in the list of inputs to update
        //             // So update it and keep it in the list.
        //             thisInput.update(inputsToUpdateMap.get(thisInput.getName()), ignoreNull);
        //
        //             // Now that we've updated that mount, remove it from the map of ones to update.
        //             inputsToUpdateMap.remove(thisInput.getName());
        //         } else {
        //             // The mount we are looking at is not in the list of inputs to update.
        //             // If we ignore nulls, than that's fine, the mount can stay. Do nothing.
        //             // If ignoreNull==false, then not seeing the mount means "remove it".
        //             if (!ignoreNull) {
        //                 iterator.remove();
        //             }
        //         }
        //     }
        //
        //     // We have now updated/removed all inputs we already knew about.
        //     // Any inputs still in the toUpdateMap are ones we didn't know about before,
        //     // so we can just add them.
        //     this.inputs.addAll(inputsToUpdateMap.values());
        // }
        //
        // if (this.outputs == null || this.outputs.isEmpty()) {
        //     // If we have no outputs, just take what we are given.
        //     // No need to go digging through lists when we're comparing to nothing.
        //     // Even if the "update" is empty too, no big deal.
        //     this.outputs = other.outputs;
        // } else {
        //     final Map<String, CommandOutput> outputsToUpdateMap = Maps.newHashMap();
        //     if (other.outputs != null) {
        //         for (final CommandOutput otherOutput : other.outputs) {
        //             outputsToUpdateMap.put(otherOutput.getName(), otherOutput);
        //         }
        //     }
        //
        //     // Update any outputs that already exist
        //     final Iterator<CommandOutput> iterator = this.outputs.iterator();
        //     while (iterator.hasNext()) {
        //         final CommandOutput thisOutput = iterator.next();
        //         if (outputsToUpdateMap.containsKey(thisOutput.getName())) {
        //             // The mount we are looking at is in the list of outputs to update
        //             // So update it and keep it in the list.
        //             thisOutput.update(outputsToUpdateMap.get(thisOutput.getName()), ignoreNull);
        //
        //             // Now that we've updated that mount, remove it from the map of ones to update.
        //             outputsToUpdateMap.remove(thisOutput.getName());
        //         } else {
        //             // The mount we are looking at is not in the list of outputs to update.
        //             // If we ignore nulls, than that's fine, the mount can stay. Do nothing.
        //             // If ignoreNull==false, then not seeing the mount means "remove it".
        //             if (!ignoreNull) {
        //                 iterator.remove();
        //             }
        //         }
        //     }
        //
        //     // We have now updated/removed all outputs we already knew about.
        //     // Any outputs still in the toUpdateMap are ones we didn't know about before,
        //     // so we can just add them.
        //     this.outputs.addAll(outputsToUpdateMap.values());
        // }
    }

}
