package org.nrg.containers.metadata;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.envers.Audited;
import org.nrg.containers.exceptions.ImageMetadataException;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntity;

import javax.persistence.Entity;
import javax.persistence.Transient;
import java.util.Set;

@Audited
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "nrg")
public class ImageMetadataArg extends AbstractHibernateEntity {

    private String name;
    private String type;
    private String description;
    private String flag;
    @JsonProperty("user-settable") private Boolean userSettable;
    private String value;
    @JsonProperty("validation-regex") private String validationRegex;
    private String bounds;
    @JsonProperty("boolean-values") private String booleanValues;

    public ImageMetadataArg() {}

    private ImageMetadataArg(final Builder builder) {
        this.name = builder.name;
        this.type = builder.type;
        this.description = builder.description;
        this.flag = builder.flag;
        this.userSettable = builder.userSettable;
        this.value = builder.value;
        this.validationRegex = builder.validationRegex;
        this.bounds = builder.bounds;
        this.booleanValues = builder.booleanValues;
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public String getFlag() {
        return flag;
    }

    public void setFlag(final String flag) {
        this.flag = flag;
    }

    public Boolean getUserSettable() {
        return userSettable;
    }

    public void setUserSettable(final Boolean userSettable) {
        this.userSettable = userSettable;
    }

    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
    }

    public String getValidationRegex() {
        return validationRegex;
    }

    public void setValidationRegex(final String validationRegex) {
        this.validationRegex = validationRegex;
    }

    public String getBounds() {
        return bounds;
    }

    public void setBounds(final String bounds) throws ImageMetadataException {
        try {
            this.bounds = BoundsUtil.validate(bounds);
        } catch (ImageMetadataException e) {
            throw new ImageMetadataException(String.format("Invalid bounds: %s. ", bounds) + e.getMessage());
        }
    }

    public String getBooleanValues() {
        return booleanValues;
    }

    public void setBooleanValues(final String booleanValues) {
        this.booleanValues = booleanValues;
    }

    @Transient
    @JsonIgnore
    public Boolean isLowerBoundExclusive() {
        return BoundsUtil.isLowerBoundExclusive(bounds);
    }

    @Transient
    @JsonIgnore
    public Boolean isUpperBoundExclusive() {
        return BoundsUtil.isUpperBoundExclusive(bounds);
    }

    @Transient
    @JsonIgnore
    public Number getLowerBound() {
        return BoundsUtil.lowerBound(bounds);
    }

    @Transient
    @JsonIgnore
    public void setLowerBound(final String lowerBoundValue, final Boolean exclusive) throws ImageMetadataException {
        try {
            this.bounds = BoundsUtil.setLowerBound(bounds, lowerBoundValue, exclusive);
        } catch (ImageMetadataException e) {
            throw new ImageMetadataException(String.format("Invalid bounds: %s .", bounds) + e.getMessage());
        }
    }

    @Transient
    @JsonIgnore
    public Number getUpperBound() {
        return BoundsUtil.upperBound(bounds);
    }

    @Transient
    @JsonIgnore
    public void setUpperBound(final String upperBoundValue, final Boolean exclusive) throws ImageMetadataException {
        try {
            this.bounds = BoundsUtil.setUpperBound(bounds, upperBoundValue, exclusive);
        } catch (ImageMetadataException e) {
            throw new ImageMetadataException(String.format("Invalid bounds: %s. ", bounds) + e.getMessage());
        }
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("name", name)
                .add("description", description)
                .add("flag", flag)
                .add("user-settable", userSettable)
                .add("value", value)
                .add("validation-regex", validationRegex)
                .add("bounds", bounds)
                .add("description", description)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ImageMetadataArg that = (ImageMetadataArg) o;

        return Objects.equal(this.getId(), that.getId()) &&
                Objects.equal(this.name, that.name) &&
                Objects.equal(this.type, that.type) &&
                Objects.equal(this.description, that.description) &&
                Objects.equal(this.flag, that.flag) &&
                Objects.equal(this.userSettable, that.userSettable) &&
                Objects.equal(this.value, that.value) &&
                Objects.equal(this.validationRegex, that.validationRegex) &&
                Objects.equal(this.bounds, that.bounds) &&
                Objects.equal(this.booleanValues, that.booleanValues);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId(), name, type, description,
                flag, userSettable, value, validationRegex, bounds, booleanValues);
    }

    private static class BoundsUtil {
        final static String EXCLUSIVE_LOWER_BRACKET = "(";
        final static String INCLUSIVE_LOWER_BRACKET = "[";
        final static String EXCLUSIVE_UPPER_BRACKET = ")";
        final static String INCLUSIVE_UPPER_BRACKET = "]";
        final static String NUMBER_REGEX = "([+-]?(?:(?:\\d+)|(?:\\d+\\.\\d+)|(?:Inf)))";
        final static String DOUBLE_REGEX = "\\.|(?:Inf)";
        final static String BOUNDS_REGEX =
                "(\\" + INCLUSIVE_LOWER_BRACKET + "|\\" + EXCLUSIVE_LOWER_BRACKET + ")" +
                        NUMBER_REGEX + ",\\s*" + NUMBER_REGEX +
                        "(\\" + INCLUSIVE_UPPER_BRACKET + "|\\" + EXCLUSIVE_UPPER_BRACKET + ")";

        final static Set<String> INCLUSIVE_BOUNDS = Sets.newHashSet(INCLUSIVE_LOWER_BRACKET, INCLUSIVE_UPPER_BRACKET);

        final static ImmutableMap<String, String> INCLUSIVE_TO_EXCLUSIVE_BOUNDS =
                ImmutableMap.of(INCLUSIVE_LOWER_BRACKET, EXCLUSIVE_LOWER_BRACKET,
                        INCLUSIVE_UPPER_BRACKET, EXCLUSIVE_UPPER_BRACKET);

        public static String validate(final String bounds) throws ImageMetadataException {
            if (StringUtils.isBlank(bounds)) {
                // 99% of the time, we won't have any explicit bounds. And that is ok.
                return bounds;
            } else {
                final String[] boundsGroups = splitBoundsStrIntoGroupsAndValidate(bounds);
                final String lowerBracketStr = boundsGroups[0];
                final String lowerValueStr = boundsGroups[1];
                final String upperValueStr = boundsGroups[2];
                final String upperBracketStr = boundsGroups[3];

                checkInfiniteValueMustHaveExclusiveBoundary(lowerValueStr, lowerBracketStr);
                checkInfiniteValueMustHaveExclusiveBoundary(upperValueStr, upperBracketStr);

                checkLowerBoundLessThanUpperBound(lowerValueStr, upperValueStr);

                return formatPartsIntoBoundsStr(lowerBracketStr, lowerValueStr, upperValueStr, upperBracketStr);
            }
        }

        public static Boolean isLowerBoundExclusive(final String bounds) {
            final String[] boundsGroups = splitBoundsStrIntoGroups(bounds);
            final String lowerBracketStr = boundsGroups[0];
            return !INCLUSIVE_BOUNDS.contains(lowerBracketStr);
        }

        public static Boolean isUpperBoundExclusive(final String bounds) {
            final String[] boundsGroups = splitBoundsStrIntoGroups(bounds);
            final String upperBracketStr = boundsGroups[3];
            return !INCLUSIVE_BOUNDS.contains(upperBracketStr);
        }

        public static Number lowerBound(final String bounds) {
            final String[] boundsGroups = splitBoundsStrIntoGroups(bounds);
            final String lowerValueStr = boundsGroups[1];
            return boundNumFromString(lowerValueStr);
        }

        public static String setLowerBound(final String bounds, final String newLowerValueStr, final Boolean exclusive)
                throws ImageMetadataException {
            final String[] boundsGroups = splitBoundsStrIntoGroupsAndValidate(bounds);
            // final String lowerBracketStr = boundsGroups[0];
            // final String lowerValueStr = boundsGroups[1];
            final String upperValueStr = boundsGroups[2];
            final String upperBracketStr = boundsGroups[3];

            final String proposedNewBounds =
                    formatPartsIntoBoundsStr(
                            exclusive ? EXCLUSIVE_LOWER_BRACKET : EXCLUSIVE_UPPER_BRACKET,
                            newLowerValueStr,
                            upperValueStr,
                            upperBracketStr);
            return validate(proposedNewBounds);
        }

        public static Number upperBound(final String bounds) {
            final String[] boundsGroups = splitBoundsStrIntoGroups(bounds);
            final String upperValueStr = boundsGroups[2];
            return boundNumFromString(upperValueStr);
        }

        public static String setUpperBound(final String bounds, final String newUpperValueStr, final Boolean exclusive)
                throws ImageMetadataException {
            final String[] boundsGroups = splitBoundsStrIntoGroupsAndValidate(bounds);
            final String lowerBracketStr = boundsGroups[0];
            final String lowerValueStr = boundsGroups[1];
            // final String upperValueStr = boundsGroups[2];
            // final String upperBracketStr = boundsGroups[3];

            final String proposedNewBounds =
                    formatPartsIntoBoundsStr(
                            lowerBracketStr,
                            lowerValueStr,
                            newUpperValueStr,
                            exclusive ? EXCLUSIVE_UPPER_BRACKET : INCLUSIVE_UPPER_BRACKET);
            return validate(proposedNewBounds);

        }

        private static void checkInfiniteValueMustHaveExclusiveBoundary(final String value, final String bracket)
                throws ImageMetadataException {
            if (value.contains("Inf") && INCLUSIVE_BOUNDS.contains(bracket)) {
                throw new ImageMetadataException(String.format("Infinite boundary must be exclusive, not inclusive. Use \"%s\", not \"%s\".",
                        INCLUSIVE_TO_EXCLUSIVE_BOUNDS.get(bracket), bracket));
            }
        }

        private static void checkLowerBoundLessThanUpperBound(final String lowerValueStr,
                                                              final String upperValueStr)
                throws ImageMetadataException{
            final Number lowerBoundNum = boundNumFromString(lowerValueStr);
            final Number upperBoundNum = boundNumFromString(upperValueStr);

            // I can't think of a better way to compare an Integer with a Double than just cast both to Double
            if (Double.compare(lowerBoundNum.doubleValue(), upperBoundNum.doubleValue()) >= 0) {
                throw new ImageMetadataException(String.format("Lower bound %s must be less than upper bound %s.", lowerValueStr, upperValueStr));
            }
        }

        private static String formatPartsIntoBoundsStr(final String lowerBracketStr,
                                                       final String lowerValueStr,
                                                       final String upperValueStr,
                                                       final String upperBracketStr) {
            return String.format("%s%s,%s%s", lowerBracketStr, lowerValueStr, upperValueStr, upperBracketStr);
        }

        private static String[] splitBoundsStrIntoGroupsAndValidate(final String bounds)
                throws ImageMetadataException {
            if (!bounds.matches(BOUNDS_REGEX)) {
                if (bounds.indexOf(',') < 0) {
                    throw new ImageMetadataException("Must specify upper and lower bounds separated by ','.");
                }
                if (!bounds.matches("^(\\(|\\[).*(\\)|\\])")) {
                    throw new ImageMetadataException("Must begin with ( or [ and end with ) or ].");
                }
                if (!bounds.matches("(\\(|\\[).*,\\s*.*(\\)|\\])")) {
                    throw new ImageMetadataException("Allowed numerical values: +/- integers or floats (with leading and/or trailing zeros as necessary), including Inf.");
                }
                throw new ImageMetadataException("Unknown error.");
            }

            return splitBoundsStrIntoGroups(bounds);
        }

        private static String[] splitBoundsStrIntoGroups(final String bounds) {
            return bounds.split(BOUNDS_REGEX);
        }

        private static Number boundNumFromString(final String valueStr) {
            return valueStr.matches(DOUBLE_REGEX) ?
                    Double.parseDouble(valueStr) :
                    Integer.parseInt(valueStr);
        }
    }

    public static class Builder {
        private String name;
        private String type;
        private String description;
        private String flag;
        private Boolean userSettable;
        private String value;
        private String validationRegex;
        private String bounds;
        private String booleanValues;

        private Builder() {}

        private Builder(final ImageMetadataArg imageMetadataArg) {
            this.name = imageMetadataArg.name;
            this.type = imageMetadataArg.type;
            this.description = imageMetadataArg.description;
            this.flag = imageMetadataArg.flag;
            this.userSettable = imageMetadataArg.userSettable;
            this.value = imageMetadataArg.value;
            this.validationRegex = imageMetadataArg.validationRegex;
            this.bounds = imageMetadataArg.bounds;
            this.booleanValues = imageMetadataArg.booleanValues;
        }

        public ImageMetadataArg build() {
            return new ImageMetadataArg(this);
        }

        public Builder name(final String name) {
            this.name = name;
            return this;
        }

        public String name() {
            return name;
        }

        public Builder type(final String type) {
            this.type = type;
            return this;
        }

        public String type() {
            return type;
        }

        public Builder description(final String description) {
            this.description = description;
            return this;
        }

        public String description() {
            return description;
        }

        public Builder flag(final String flag) {
            this.flag = flag;
            return this;
        }

        public String flag() {
            return flag;
        }

        public Builder userSettable(final Boolean userSettable) {
            this.userSettable = userSettable;
            return this;
        }

        public Boolean userSettable() {
            return userSettable;
        }

        public Builder value(final String value) {
            this.value = value;
            return this;
        }

        public String value() {
            return value;
        }

        public Builder validationRegex(final String validationRegex) {
            this.validationRegex = validationRegex;
            return this;
        }

        public String validationRegex() {
            return validationRegex;
        }

        public Builder bounds(final String bounds) throws ImageMetadataException {
            try {
                this.bounds = BoundsUtil.validate(bounds);
            } catch (ImageMetadataException e) {
                throw new ImageMetadataException(String.format("Invalid bounds: %s. ", bounds) + e.getMessage());
            }
            return this;
        }

        public String bounds() {
            return bounds;
        }

        public Builder booleanValues(final String booleanValues) {
            this.booleanValues = booleanValues;
            return this;
        }

        public String booleanValues() {
            return booleanValues;
        }
    }
}
