package org.nrg.containers.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.nrg.framework.orm.hibernate.BaseHibernateEntity;

import java.util.List;
import java.util.Map;

@ApiModel(description = "Properties that define an image.")
public class Image {

    private String _name;
    private String _id;
    private List<String> _repoTags;
    private Long _size;
    private Map<String, String> _labels;

    public Image() {}

    public Image(final String name) {
        _name = name;
    }

    public Image(final String name, final String id, Long size, List<String> repoTags, Map<String, String> labels){
        _name = name;
        _id = id;
        _size = size;
        _repoTags = repoTags;
        _labels = labels;
    }

    /**
     * The image's name.
     **/
    @ApiModelProperty(value = "The image's name.")
    @JsonProperty("name")
    public String getName() {
        return _name;
    }

    public void setName(final String name) { _name = name; }

    /**
     * The image's id.
     **/
    @ApiModelProperty(value = "The image's id.")
    @JsonProperty("id")
    public String getId() { return _id; }

    public void setId(final String id) { _id = id; }

    /**
     * The image's size.
     **/
    @ApiModelProperty(value = "The image's size.")
    @JsonProperty("size")
    public Long getSize() { return _size; }

    public void setSize(final Long size) { _size = size; }

    /**
     * The image's repo tags.
     **/
    @ApiModelProperty(value = "The image's repo tags.")
    //TODO: How does json handle lists?
    // @JsonProperty("repotags")
    public List<String> getRepoTags() { return _repoTags; }

    public void setRepoTags(final List<String> repoTags) { _repoTags = repoTags; }

    /**
     * Image labels
     **/
    @ApiModelProperty(value = "Image labels")
    @JsonProperty("labels")
    public Map<String, String> getLabels() { return _labels; }

    public void setLabels(final Map<String, String> labels) { _labels = labels; }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Image {\n");

        sb.append("  id: ").append(_id).append("\n");
        sb.append("  name: ").append(_name).append("\n");
        sb.append("  size: ").append(_size).append("\n");
        sb.append("  tags: [");
        for (String tag : _repoTags.subList(0, _repoTags.size()-1)) {
            sb.append(tag).append(", ");
        }
        sb.append(_repoTags.get(_repoTags.size()-1)).append("]\n");
        sb.append("}\n");
        return sb.toString();
    }


}
