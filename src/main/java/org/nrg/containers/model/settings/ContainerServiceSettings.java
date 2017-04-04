package org.nrg.containers.model.settings;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.nrg.containers.services.impl.ContainerConfigServiceImpl;

import javax.annotation.Nullable;

@AutoValue
public abstract class ContainerServiceSettings {

    @JsonProperty("optInToSiteCommands") public abstract Boolean optInToSiteCommands();

    @Nullable
    @JsonProperty("allCommandsEnabled") public abstract Boolean allCommandsEnabled();

    @JsonCreator
    public static ContainerServiceSettings create(@JsonProperty("optInToSiteCommands") final Boolean optInToSiteCommands,
                                                  @JsonProperty("allCommandsEnabled") final Boolean allCommandsEnabled) {
        return new AutoValue_ContainerServiceSettings(
                optInToSiteCommands == null ? ContainerConfigServiceImpl.OPT_IN_DEFAULT_VALUE : optInToSiteCommands,
                allCommandsEnabled);
    }
}
