package org.nrg.containers.metadata;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang.StringUtils;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntity;
import org.nrg.framework.orm.hibernate.annotations.Auditable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.util.Map;

@Auditable
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"imageId"}))
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "nrg")
public abstract class ImageMetadata extends AbstractHibernateEntity {
    public ImageMetadata() {}

    public ImageMetadata(final String imageId, final Map<String, String> imageLabels) {
        _imageId = imageId;
        _imageLabels = ImmutableMap.copyOf(imageLabels);
    }

    public void parse(final Map<String, String> meta) {}

    public String get(final String property) {
        if (_imageLabels.containsKey(property)) {
            return _imageLabels.get(property);
        } else {
            return null;
        }
    }

    @Column(unique = true, name = "imageId")
    public String getImageId() {
        return _imageId;
    }

    public void setImageId(final String imageId) {
        _imageId = imageId;
    }

    @Column
    public Map<String, String> getMetadata() {
        return _imageLabels;
    }

    public void setMetadata(final Map<String, String> metadata) {
        _imageLabels = ImmutableMap.copyOf(metadata);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ImageMetadata{\n")
                .append("\timageId : ").append(_imageId).append(",\n")
                .append("\timageLabels : {\n");
        
        Boolean needsComma = false;
        for (Map.Entry<String, String> kvPair : _imageLabels.entrySet()) {
            if (StringUtils.isBlank(kvPair.getKey())) {
                continue;
            }
            if (needsComma) {
                sb.append(",\n");
            }
            sb.append("\t\t\"").append(kvPair.getKey()).append("\"");
            needsComma = true;
            
            if (StringUtils.isNotBlank(kvPair.getValue())) {
                sb.append(" : \"").append(kvPair.getValue()).append("\"");
            }
        }
        
        sb.append("\n\t}\n}");
                
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ImageMetadata)) return false;

        ImageMetadata that = (ImageMetadata) o;

        return
            _imageId == null        ? that._imageId == null     : _imageId.equals(that._imageId) &&
            _imageLabels == null    ? that._imageLabels == null : _imageLabels.equals(that._imageLabels);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + (_imageId != null ? _imageId.hashCode() : 0);
        result = 31 * result + (_imageLabels != null ? _imageLabels.hashCode() : 0);
        return result;
    }

    private String _imageId;
    private ImmutableMap<String, String> _imageLabels;
}
