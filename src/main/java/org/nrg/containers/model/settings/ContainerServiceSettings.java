package org.nrg.containers.model.settings;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.nrg.containers.services.ContainerConfigService;

@AutoValue
@JsonInclude(JsonInclude.Include.ALWAYS)
public abstract class ContainerServiceSettings {

    @JsonProperty("optInToSiteCommands") public abstract Boolean optInToSiteCommands();

    @JsonCreator
    public static ContainerServiceSettings create(@JsonProperty("optInToSiteCommands") final Boolean optInToSiteCommands) {
        return new AutoValue_ContainerServiceSettings(
                optInToSiteCommands == null ?
                        ContainerConfigService.OPT_IN_DEFAULT_VALUE :
                        optInToSiteCommands
        );
    }
}
