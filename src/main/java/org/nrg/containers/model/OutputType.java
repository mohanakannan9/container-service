package org.nrg.containers.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum OutputType {
    @JsonProperty("Resource") RESOURCE,
    @JsonProperty("Assessor") ASSESSOR
}
