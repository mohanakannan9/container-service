package org.nrg.containers.model.auto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@AutoValue
@ApiModel(description = "Properties that define an image.")
public abstract class DockerImage {

    @Nullable @JsonProperty("image-id") public abstract String imageId();
    @JsonProperty("tags") public abstract List<String> tags();
    @JsonProperty("labels") public abstract Map<String, String> labels();

    @JsonCreator
    public static DockerImage create(@JsonProperty("image-id") final String imageId,
                                     @JsonProperty("tags") final List<String> repoTags,
                                     @JsonProperty("labels") final Map<String, String> labels) {
        return new AutoValue_DockerImage(imageId,
                repoTags == null ? Lists.<String>newArrayList() : repoTags,
                labels == null ? Maps.<String, String>newHashMap() : labels);
    }

    public void addTag(final String tag) {
        if (StringUtils.isBlank(tag)) {
            return;
        }
        tags().add(tag);
    }
}
